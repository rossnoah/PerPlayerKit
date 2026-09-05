#!/usr/bin/env bash
#
# Regenerate every PerPlayerKit documentation screenshot, start to finish,
# with no human in the loop.
#
#   ./auto.sh
#
# Builds the plugin, boots a throwaway Paper server, drives a headless bot that
# stocks the kit room, saves public kits and player kits, dumps every GUI, then
# renders each one to docs/images/.
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
SRV="$HERE/.server"; CACHE="$HERE/.cache"; OUT="$ROOT/docs/images"
PORT=${PORT:-25599}
SERVER_PID=""
TMUX=(tmux -L ppkshots)

say(){ printf '\033[36m==>\033[0m %s\n' "$*"; }
die(){ printf '\033[31m!!\033[0m %s\n' "$*" >&2; exit 1; }
cleanup(){
  if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    "${TMUX[@]}" send-keys -t srv "stop" Enter 2>/dev/null || true
    for _ in $(seq 1 60); do
      kill -0 "$SERVER_PID" 2>/dev/null || break
      sleep 0.5
    done
    kill "$SERVER_PID" 2>/dev/null || true
  fi
  "${TMUX[@]}" kill-server 2>/dev/null || true
  SERVER_PID=""
}
trap cleanup EXIT

for c in java mvn tmux node curl python3; do command -v "$c" >/dev/null || die "$c is required"; done
python3 -c "import PIL, yaml" 2>/dev/null || die "Pillow and PyYAML are required: python3 -m pip install pillow pyyaml"

cd "$HERE"
[ -d node_modules/mineflayer ] || { say "Installing mineflayer"; npm i --silent mineflayer; }

if [[ "${BUILD:-1}" != 0 ]]; then
  say "Building the plugin"
  (cd "$ROOT" && mvn -q package -DskipTests)
else
  say "Using the built plugin"
fi
JAR=$(ls -t "$ROOT"/target/PerPlayerKit-*.jar | head -1)
[ -n "$JAR" ] || die "no jar in target/"

# Pin Paper to the newest version the bot library can actually speak.
mkdir -p "$CACHE"
node -e "console.log(JSON.stringify(require('minecraft-protocol').supportedVersions))" > "$CACHE/supported.json"
curl -fsSL https://fill.papermc.io/v3/projects/paper -o "$CACHE/projects.json"
MCVER=$(python3 -c "
import json
sup=set(json.load(open('$CACHE/supported.json')))
d=json.load(open('$CACHE/projects.json'))
print(next((v for grp in d['versions'].values() for v in grp if v in sup), ''))")
[ -n "$MCVER" ] || die "no Paper build matches a protocol version mineflayer supports"

if [ "$(cat "$CACHE/paper-version.txt" 2>/dev/null || true)" != "$MCVER" ] || [ ! -f "$CACHE/paper.jar" ]; then
  say "Downloading Paper $MCVER"
  curl -fsSL "https://fill.papermc.io/v3/projects/paper/versions/$MCVER/builds/latest" -o "$CACHE/build.json"
  curl -fsSL -o "$CACHE/paper.jar" \
    "$(python3 -c "import json;print(json.load(open('$CACHE/build.json'))['downloads']['server:default']['url'])")"
  echo "$MCVER" > "$CACHE/paper-version.txt"
else
  say "Using cached Paper $MCVER"
fi

say "Fetching vanilla textures"
python3 assets.py "$MCVER"

boot_server() {
  cleanup
  : > "$SRV/console.log" 2>/dev/null || true
  "${TMUX[@]}" new-session -d -s srv -x 200 -y 50 \
    "cd '$SRV' && exec java -Xms1G -Xmx2G -jar paper.jar nogui > console.log 2>&1"
  SERVER_PID=$("${TMUX[@]}" display-message -p -t srv '#{pane_pid}')
  printf '    %s' "${1:-starting}"
  for _ in $(seq 1 90); do
    grep -q 'Done (' "$SRV/console.log" 2>/dev/null && break
    printf '.'; sleep 2
  done
  echo
  grep -q 'Done (' "$SRV/console.log" || die "server did not start; see $SRV/console.log"
}

say "Booting a throwaway server on port $PORT"
cleanup
rm -rf "$SRV"; mkdir -p "$SRV/plugins"
cp "$CACHE/paper.jar" "$SRV/"; cp "$JAR" "$SRV/plugins/"
echo "eula=true" > "$SRV/eula.txt"
cat > "$SRV/server.properties" <<EOF
online-mode=false
server-port=$PORT
level-type=flat
generate-structures=false
spawn-protection=0
gamemode=creative
difficulty=peaceful
allow-flight=true
EOF
boot_server "waiting for startup"

# The first boot generated config.yml. Declare the public kits the bot fills in
# and silence chat, then boot again so it takes effect.
say "Preparing config.yml"
cleanup
python3 patch_config.py "$SRV/plugins/PerPlayerKit/config.yml"
boot_server "restarting"

# The bot needs admin rights to open the kit room and save public kits.
"${TMUX[@]}" send-keys -t srv "op Notch" Enter
sleep 1

say "Seeding and capturing every GUI"
rm -rf captures; PPK_PORT="$PORT" node capture.js

say "Rendering"
mkdir -p "$OUT"
shopt -s nullglob
for f in captures/*.json; do
  python3 render.py "$f" "$OUT/$(basename "${f%.json}").png"
done

say "Done. Screenshots written to docs/images/"
ls -la "$OUT"/*.png | awk '{printf "    %-14s %s\n", $5" bytes", $9}'
