#!/usr/bin/env bash
#
# Boots a real server for each supported platform and Minecraft version, drives
# PerPlayerKit through the console, and checks what actually happened.
#
# The plugin compiles against the 1.19 API but has to keep working on servers
# years newer, on two platforms whose internals differ. Unit tests cannot see
# any of that: the failures this catches are class loading, shading and API
# drift, which only show up once a server is running.
#
#   ./tools/integration-test/run.sh                  # everything
#   ./tools/integration-test/run.sh paper            # one platform
#   ./tools/integration-test/run.sh paper 1.21.8     # one target
#   KEEP_WORK=1 ./tools/integration-test/run.sh ...  # keep server dirs to poke at
#
set -uo pipefail

readonly REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly CACHE_DIR="${CACHE_DIR:-$REPO_ROOT/tools/integration-test/.cache}"
readonly WORK_DIR="${WORK_DIR:-$REPO_ROOT/target/integration-test}"
readonly BOOT_TIMEOUT="${BOOT_TIMEOUT:-180}"
readonly STOP_TIMEOUT="${STOP_TIMEOUT:-120}"
readonly BASE_PORT="${BASE_PORT:-25700}"
readonly PROBE_USER="${PROBE_USER:-PPKProbe}"

# platform:version:java-major - the Java each Minecraft version demands.
readonly TARGETS=(
  "paper:1.19.4:17"
  "paper:1.20.6:21"
  "paper:1.21.4:21"
  "paper:1.21.8:21"
  "paper:26.1.2:25"
  "paper:26.2:25"
  "spigot:1.19.4:17"
  "spigot:1.20.6:21"
  "spigot:1.21.4:21"
  "spigot:1.21.8:21"
  "spigot:26.1.2:25"
  "spigot:26.2:25"
)

PASSED=(); FAILED=(); SKIPPED=()

log()  { printf '  %s\n' "$*"; }
head1() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
ok()   { printf '  \033[32mPASS\033[0m %s\n' "$*"; }
bad()  { printf '  \033[31mFAIL\033[0m %s\n' "$*"; }

# --- toolchain -------------------------------------------------------------

# Finds a JDK of the requested major version. Works with the JAVA_HOME_<n>_<arch>
# variables actions/setup-java exports in CI, and with java_home on a Mac.
is_java_major() {
  local java_bin="$1" want="$2" reported
  [[ -x "$java_bin" ]] || return 1
  # "openjdk version "21.0.7" ..." and the 1.8 style "1.8.0_452".
  reported="$("$java_bin" -version 2>&1 | head -1 | sed -E 's/.*"(1\.)?([0-9]+).*/\2/')"
  [[ "$reported" == "$want" ]]
}

find_java() {
  local want="$1" candidate home

  for candidate in $(env | grep -oE "^JAVA_HOME_${want}_[A-Z0-9]+" || true); do
    home="${!candidate:-}"
    is_java_major "$home/bin/java" "$want" && { echo "$home/bin/java"; return 0; }
  done

  # java_home treats -v as a preference, not a requirement: ask it for a version
  # that does not exist and it hands back the newest one installed. Every
  # candidate therefore has to be checked, or a target quietly runs on the wrong
  # JDK instead of being skipped.
  if [[ -x /usr/libexec/java_home ]]; then
    home="$(/usr/libexec/java_home -v "$want" 2>/dev/null)" || home=""
    is_java_major "$home/bin/java" "$want" && { echo "$home/bin/java"; return 0; }
  fi

  if [[ -n "${JAVA_HOME:-}" ]] && is_java_major "$JAVA_HOME/bin/java" "$want"; then
    echo "$JAVA_HOME/bin/java"; return 0
  fi

  if command -v java >/dev/null && is_java_major "$(command -v java)" "$want"; then
    command -v java; return 0
  fi
  return 1
}

plugin_jar() {
  local jar
  jar="$(ls -t "$REPO_ROOT"/target/PerPlayerKit-*.jar 2>/dev/null | grep -v original | head -1)"
  [[ -n "$jar" ]] && echo "$jar"
}

# --- server jars -----------------------------------------------------------

fetch_paper() {
  local version="$1" out="$CACHE_DIR/paper-$version.jar"
  [[ -f "$out" ]] && { echo "$out"; return 0; }

  local url
  url="$(curl -fsS "https://fill.papermc.io/v3/projects/paper/versions/$version/builds/latest" 2>/dev/null \
        | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])' 2>/dev/null)" || return 1
  [[ -z "$url" ]] && return 1

  curl -fsS -o "$out.tmp" "$url" && mv "$out.tmp" "$out" && echo "$out"
}

# Spigot is not redistributable, so it has to be compiled here. Slow the first
# time for a version, then cached.
build_spigot() {
  local version="$1" java_bin="$2" out="$CACHE_DIR/spigot-$version.jar"
  [[ -f "$out" ]] && { echo "$out"; return 0; }

  local tools="$CACHE_DIR/buildtools"
  mkdir -p "$tools"
  [[ -f "$tools/BuildTools.jar" ]] || curl -fsS -o "$tools/BuildTools.jar" \
    "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar" || return 1

  ( cd "$tools" && "$java_bin" -jar BuildTools.jar --rev "$version" --compile spigot ) \
    > "$tools/build-$version.log" 2>&1 || return 1

  [[ -f "$tools/spigot-$version.jar" ]] && cp "$tools/spigot-$version.jar" "$out" && echo "$out"
}

# --- one target ------------------------------------------------------------

run_target() {
  local platform="$1" version="$2" java_major="$3" port="$4"
  local label="$platform $version"
  head1 "$label"

  local java_bin
  if ! java_bin="$(find_java "$java_major")"; then
    log "no Java $java_major available"
    SKIPPED+=("$label (needs Java $java_major)")
    return 0
  fi

  local server_jar
  if [[ "$platform" == "paper" ]]; then
    server_jar="$(fetch_paper "$version")"
  else
    log "building spigot $version (slow on a cache miss)"
    server_jar="$(build_spigot "$version" "$java_bin")"
  fi
  if [[ -z "${server_jar:-}" || ! -f "${server_jar:-/nonexistent}" ]]; then
    log "could not obtain a $platform $version server jar"
    SKIPPED+=("$label (no server jar)")
    return 0
  fi

  local dir="$WORK_DIR/$platform-$version"
  rm -rf "$dir"; mkdir -p "$dir/plugins"

  # Paper downloads the vanilla jar into <serverdir>/cache on first start. The
  # server dir is rebuilt every run, so without this every Paper target
  # re-downloads ~50MB each time - slow, and it corrupts under load, which
  # surfaces as a hash check failure that looks nothing like its cause.
  if [[ "$platform" == "paper" ]]; then
    mkdir -p "$CACHE_DIR/paperclip/$version"
    ln -s "$CACHE_DIR/paperclip/$version" "$dir/cache"
  fi
  cp "$(plugin_jar)" "$dir/plugins/"
  echo "eula=true" > "$dir/eula.txt"
  cat > "$dir/server.properties" <<EOF
server-port=$port
online-mode=false
enforce-secure-profile=false
level-type=flat
difficulty=peaceful
spawn-monsters=false
max-players=3
view-distance=4
simulation-distance=4
EOF

  local log_file="$dir/console.log" pipe="$dir/console.pipe"
  mkfifo "$pipe"
  # exec so the recorded pid is the server itself. Without it the pid belongs to
  # the subshell, the last-resort kill hits that instead, and an orphaned server
  # keeps holding its port - which the next target then fails on for reasons
  # that have nothing to do with it.
  ( cd "$dir" && exec "$java_bin" -Xms1G -Xmx2G -Dperplayerkit.debug=true -jar "$server_jar" nogui ) \
    < "$pipe" > "$log_file" 2>&1 &
  local server_pid=$!
  exec 3> "$pipe"

  local waited=0
  while ! grep -q 'Done (' "$log_file" 2>/dev/null; do
    if ! kill -0 "$server_pid" 2>/dev/null; then
      # Worth naming: a leftover server from an interrupted run holds the port,
      # and "died during startup" sends you looking at the plugin instead.
      if grep -q 'FAILED TO BIND TO PORT' "$log_file" 2>/dev/null; then
        bad "$label: port $port is already in use"
        FAILED+=("$label: port $port in use"); exec 3>&-; return 1
      fi
      bad "$label: server died during startup"
      FAILED+=("$label: died during startup"); exec 3>&-; return 1
    fi
    sleep 2; waited=$((waited + 2))
    if (( waited > BOOT_TIMEOUT )); then
      bad "$label: did not start within ${BOOT_TIMEOUT}s"
      FAILED+=("$label: boot timeout"); kill -9 "$server_pid" 2>/dev/null; exec 3>&-; return 1
    fi
  done
  log "started in ${waited}s"

  drive_and_check "$label" "$dir" "$log_file" "$server_pid" "$version" "$port"
  local result=$?

  echo "stop" >&3 2>/dev/null
  local stopping=0
  while kill -0 "$server_pid" 2>/dev/null && (( stopping < STOP_TIMEOUT )); do
    sleep 2; stopping=$((stopping + 2))
  done
  kill -9 "$server_pid" 2>/dev/null
  exec 3>&-
  wait "$server_pid" 2>/dev/null

  # Shutdown releases the port a moment after the process goes, and targets run
  # back to back.
  local waited_port=0
  while lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1 && (( waited_port < 30 )); do
    sleep 1; waited_port=$((waited_port + 1))
  done

  [[ -z "${KEEP_WORK:-}" && $result -eq 0 ]] && rm -rf "$dir"
  return $result
}

# Joins as a real client and checks what actually lands in chat.
#
# This is the branch the console cannot reach: Lang.deliver treats Players
# differently from the console, and that player branch is the one that broke on
# Paper 26 without throwing anything. Asserting the click event survives is the
# point - components reach a client as JSON, and a fallback to a legacy string
# would still show the text while quietly dropping the thing you can click.
#
# Sets PROBE_RESULT to pass, fail or skip.
probe_chat() {
  local dir="$1" version="$2" port="$3"
  local script_dir="$REPO_ROOT/tools/integration-test"
  PROBE_RESULT=skip

  if ! command -v node >/dev/null || [[ ! -d "$script_dir/node_modules/minecraft-protocol" ]]; then
    log "chat probe skipped (run: npm install --prefix tools/integration-test)"
    return 0
  fi

  local out="$dir/chat-probe.json"
  # The docs link is the last line of the intro, so seeing it means the whole
  # message arrived and there is nothing left to wait for.
  node "$script_dir/chat-probe.js" --port "$port" --version "$version" \
    --username "$PROBE_USER" --seconds 90 --await "perplayerkit.com" \
    > "$out" 2> "$dir/chat-probe.err"
  local rc=$?

  if (( rc == 2 )); then
    # The client library trails new Minecraft releases; not a plugin failure.
    log "chat probe skipped ($(python3 -c "import json,sys;print(json.load(open('$out')).get('reason','unsupported'))" 2>/dev/null || echo unsupported))"
    return 0
  fi
  if (( rc != 0 )); then
    bad "chat probe connected"
    PROBE_RESULT=fail
    return 0
  fi

  # Say when the client is not speaking this version natively, so a pass here is
  # read for what it is.
  local note
  note="$(python3 -c "import json,sys;print(json.load(open('$out')).get('versionNote') or '')" 2>/dev/null)"
  [[ -n "$note" ]] && log "chat probe: $note"

  local verdict
  verdict="$(python3 - "$out" <<'PY'
import json, sys
report = json.load(open(sys.argv[1]))
messages = report.get("messages", [])
text = "\n".join(m.get("text", "") for m in messages)
blob = json.dumps(messages)
# Key names changed across versions (clickEvent then click_event), so match on
# the action and the command rather than the shape.
print("intro" if "kit room is empty" in text else "no-intro")
print("permissions" if "perplayerkit.use" in text else "no-permissions")
print("clickable" if ("run_command" in blob and "/perplayerkit autosetup" in blob) else "not-clickable")
PY
)" || verdict=""

  local got_intro got_perms got_click
  got_intro="$(sed -n 1p <<< "$verdict")"
  got_perms="$(sed -n 2p <<< "$verdict")"
  got_click="$(sed -n 3p <<< "$verdict")"

  PROBE_RESULT=pass
  if [[ "$got_intro" == "intro" ]]; then ok "a real client receives the intro"
  else bad "a real client receives the intro"; PROBE_RESULT=fail; fi

  if [[ "$got_perms" == "permissions" ]]; then ok "the client sees the permission list"
  else bad "the client sees the permission list"; PROBE_RESULT=fail; fi

  if [[ "$got_click" == "clickable" ]]; then ok "the autosetup offer is still clickable"
  else bad "the autosetup offer is still clickable"; PROBE_RESULT=fail; fi
}

# Sends commands, then checks the log and the database for what should have
# happened. Console output goes through the same Lang path players use, so a
# missing line here means components stopped rendering on this version.
drive_and_check() {
  local label="$1" dir="$2" log_file="$3" pid="$4" version="$5" port="$6"
  local platform="${label%% *}"
  local failures=()

  send_and_settle() { echo "$1" >&3; sleep 3; }

  expect() {
    local what="$1" pattern="$2"
    if grep -qF "$pattern" "$log_file"; then ok "$what"; else bad "$what"; failures+=("$what"); fi
  }

  # A real client, before autosetup runs - the offer only goes out while the kit
  # room is still empty.
  send_and_settle "op $PROBE_USER"
  probe_chat "$dir" "$version" "$port"

  send_and_settle "perplayerkit"
  send_and_settle "perplayerkit autosetup"
  send_and_settle "perplayerkit autosetup"

  expect "plugin enabled"            "Enabling PerPlayerKit"
  expect "startup guide printed"     "Getting started:"
  expect "permissions listed"        "perplayerkit.use    - give this one to your default group"
  expect "empty kit room detected"   "Your kit room is empty."
  expect "subcommands listed"        "PerPlayerKit admin commands:"
  expect "autosetup filled content"  "Autosetup complete! Filled 5 kit room page(s) and 3 public kit(s)."
  expect "public kits named"         "Public kits filled: crystal, axe, sword"
  expect "autosetup is idempotent"   "Everything is already set up."

  # Which path components take, which is silent when it breaks. Paper has a
  # native Audience and must use it - relocation rewriting the class names in
  # AudienceCompat would drop it back to the bundled platform, which does not
  # work on Paper 26+. Spigot has no native Audience and must not claim one.
  if [[ "$platform" == "paper" ]]; then
    expect "uses the server's own chat API" "Chat delivery: the server's own chat API"
  else
    expect "falls back to bundled adventure" "Chat delivery: bundled adventure-platform"
  fi

  # Anything the plugin threw. Server-level noise is not ours to fail on.
  local plugin_errors
  plugin_errors="$(grep -nE "PerPlayerKit|perplayerkit" "$log_file" \
    | grep -iE "exception|linkageerror|noclassdeffound|nosuchmethod|severe|failed to" || true)"
  if [[ -n "$plugin_errors" ]]; then
    bad "no plugin errors"; failures+=("plugin errors")
    printf '       %s\n' "$plugin_errors" | head -5
  else
    ok "no plugin errors"
  fi

  # The console lines above prove components render; this proves the items
  # survived a serialize/deserialize round trip into real storage.
  local db="$dir/plugins/PerPlayerKit/database.db" rows=""
  if command -v sqlite3 >/dev/null && [[ -f "$db" ]]; then
    rows="$(sqlite3 "$db" "SELECT count(*) FROM kits;" 2>/dev/null)"
    if [[ "$rows" == "8" ]]; then
      ok "storage holds 5 kit room pages + 3 public kits"
    else
      bad "storage holds 5 kit room pages + 3 public kits (found ${rows:-none})"
      failures+=("storage rows")
    fi
  else
    log "sqlite3 unavailable, skipping storage check"
  fi

  [[ "${PROBE_RESULT:-skip}" == "fail" ]] && failures+=("chat probe")

  if (( ${#failures[@]} )); then
    FAILED+=("$label: ${failures[*]}")
    return 1
  fi
  # Say so in the summary rather than letting a target look fully covered when
  # the client half never ran.
  if [[ "${PROBE_RESULT:-skip}" == "skip" ]]; then
    PASSED+=("$label (console only, no client check)")
  else
    PASSED+=("$label")
  fi
  return 0
}

# --- main ------------------------------------------------------------------

main() {
  local want_platform="${1:-all}" want_version="${2:-all}"

  mkdir -p "$CACHE_DIR" "$WORK_DIR"

  if [[ -z "$(plugin_jar)" || "${BUILD:-1}" == "1" ]]; then
    head1 "building the plugin"
    ( cd "$REPO_ROOT" && mvn -q package -DskipTests ) || { echo "plugin build failed"; exit 1; }
  fi
  log "using $(basename "$(plugin_jar)")"

  local index=0
  for target in "${TARGETS[@]}"; do
    IFS=: read -r platform version java_major <<< "$target"
    [[ "$want_platform" != "all" && "$want_platform" != "$platform" ]] && continue
    [[ "$want_version" != "all" && "$want_version" != "$version" ]] && continue
    run_target "$platform" "$version" "$java_major" $((BASE_PORT + index))
    index=$((index + 1))
  done

  head1 "summary"
  printf '  %s passed, %s failed, %s skipped\n' "${#PASSED[@]}" "${#FAILED[@]}" "${#SKIPPED[@]}"
  local entry
  for entry in "${PASSED[@]}";  do printf '  \033[32m✓\033[0m %s\n' "$entry"; done
  for entry in "${SKIPPED[@]}"; do printf '  \033[33m-\033[0m %s\n' "$entry"; done
  for entry in "${FAILED[@]}";  do printf '  \033[31m✗\033[0m %s\n' "$entry"; done

  (( ${#FAILED[@]} )) && exit 1
  exit 0
}

main "$@"
