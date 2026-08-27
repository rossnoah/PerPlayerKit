'use strict'

/**
 * Joins a running server as a real client and records what arrives in chat.
 *
 * The rest of the integration test drives the console, which is a CommandSender
 * and not a Player - a different branch in Lang.deliver. That is the branch that
 * broke on Paper 26, silently, so it is the one worth watching from the outside.
 *
 * Talks the protocol directly rather than through a bot framework. Nothing here
 * needs pathfinding or world state, and the lower layer supports newer
 * Minecraft versions than the bot libraries built on top of it - which matters,
 * because the newest versions are exactly where this kind of thing breaks.
 *
 * Prints a JSON report on stdout. Exit 0 when the chat was captured, 2 when this
 * Minecraft version is not supported by the protocol library (the caller should
 * skip, not fail), 1 for anything else.
 *
 *   node chat-probe.js --port 25700 --version 1.21.8 --username PPKProbe --seconds 16
 */

const args = new Map()
for (let i = 2; i < process.argv.length; i += 2) {
  args.set(process.argv[i].replace(/^--/, ''), process.argv[i + 1])
}

const host = args.get('host') || '127.0.0.1'
const port = Number(args.get('port') || 25565)
const version = args.get('version')
const username = args.get('username') || 'PPKProbe'
const seconds = Number(args.get('seconds') || 60)
// Text that marks the last line of what we are waiting for. Once it lands there
// is nothing further to wait for, so the probe stops early instead of sitting
// out the full window.
const awaitText = args.get('await') || null

// The protocol library trails new Minecraft releases, and says so in several
// different ways depending on how far it got. None of them are plugin failures,
// so they all have to map to a skip rather than a red run.
const UNSUPPORTED = /unsupported protocol|unknown version|is not supported|no data available for version|unsupported version/i

let finished = false
const messages = []

// stdout carries the report and nothing else. The protocol library narrates to
// console.log - loudly, when borrowed packet definitions do not line up byte for
// byte - and that would land in the middle of the JSON. Keep it, but on stderr.
console.log = (...parts) => process.stderr.write(parts.join(' ') + '\n')

function report(payload, code) {
  process.stdout.write(JSON.stringify(payload, null, 2) + '\n')
  process.exit(code)
}

let mc, nbt
try {
  mc = require('minecraft-protocol')
  nbt = require('prismarine-nbt')
} catch (err) {
  report({ ok: false, skipped: true, reason: 'minecraft-protocol is not installed' }, 2)
}

/**
 * Chat crosses the wire as a JSON string on older versions and as an NBT
 * component from 1.20.3 on. Normalise both into plain objects so the caller can
 * search one shape.
 */
function toComponent(content) {
  if (content == null) return null
  if (typeof content === 'string') {
    try { return JSON.parse(content) } catch (err) { return { text: content } }
  }
  if (typeof content !== 'object') return null
  if (content.type !== undefined && content.value !== undefined) {
    try { return nbt.simplify(content) } catch (err) { return content }
  }
  return content
}

/** Flattens a component tree to the text a player would read. */
function flatten(node) {
  if (node == null) return ''
  if (typeof node === 'string') return node
  if (Array.isArray(node)) return node.map(flatten).join('')
  if (typeof node !== 'object') return ''
  // NBT simplification can leave the literal text under an empty key.
  let out = node.text || node[''] || ''
  if (node.extra) out += flatten(node.extra)
  return out
}

/**
 * Works out how to talk to a version the library may not know yet.
 *
 * Packet definitions lag new releases, but the version index lists them with
 * their protocol numbers well before the definitions land. A release that only
 * bumps the protocol - 26.2 over 26.1, say - still speaks the older packets for
 * everything here, so borrow those definitions and announce the real protocol
 * number. Get that number wrong and the server rejects the client outright, so
 * it has to come from the index rather than from the definitions being used.
 *
 * Chat is not a subtle packet. If a future release does reshape it, the
 * assertions fail loudly rather than passing on garbage.
 */
function resolveTarget(requested) {
  const md = require('minecraft-data')
  if (!requested) return { data: false, protocol: null, note: 'version auto-detected' }
  if (md(requested)) return { data: requested, protocol: null, note: null }

  const indexed = md.versionsByMinecraftVersion.pc[requested]
  const entry = Array.isArray(indexed) ? indexed[0] : indexed
  if (!entry) return { data: false, protocol: null, note: 'version not in the index' }

  // Newest first, so the first release with definitions is the closest one.
  const donor = md.versions.pc.find((candidate) =>
    candidate.releaseType === 'release' &&
    candidate.version <= entry.version &&
    md(candidate.minecraftVersion))
  if (!donor) return { data: false, protocol: null, note: 'no usable packet definitions' }

  return {
    data: donor.minecraftVersion,
    protocol: entry.version,
    note: `packets from ${donor.minecraftVersion}, announced as protocol ${entry.version}`
  }
}

const target = resolveTarget(version)

let client
const clientOptions = { host, port, username, auth: 'offline', version: target.data }
try {
  client = mc.createClient(clientOptions)
  // Set after construction: createClient overwrites this from the packet
  // definitions, and the handshake only reads it once the socket connects.
  if (target.protocol) clientOptions.protocolVersion = target.protocol
} catch (err) {
  const text = String(err && err.message)
  report({ ok: false, skipped: UNSUPPORTED.test(text), reason: text }, UNSUPPORTED.test(text) ? 2 : 1)
}

function finish(code, extra) {
  if (finished) return
  finished = true
  try { client.end() } catch (err) { /* already gone */ }
  const payload = Object.assign(
    { ok: code === 0, messages, count: messages.length, versionNote: target.note },
    extra || {})
  setTimeout(() => report(payload, code), 150)
}

// Keeping the raw component alongside the flat text is the point: click and
// hover events are what a legacy-string fallback would quietly drop, so the
// caller can assert they survived the trip.
for (const packet of ['system_chat', 'player_chat', 'chat']) {
  client.on(packet, (data) => {
    const json = toComponent(data.content !== undefined ? data.content : data.message)
    if (json === null) return
    const text = flatten(json)
    messages.push({ packet, text, json })

    // Waiting a fixed period only works on an idle machine: the plugin holds
    // its intro back until after the MOTD, and under load that drifts past any
    // window short enough to be worth waiting. Stop on the content instead, and
    // let the window be generous, so the fast case stays fast and the slow case
    // still succeeds. A short grace period catches anything sent right after.
    if (awaitText && text.includes(awaitText)) {
      setTimeout(() => finish(0, { joined: true, stoppedEarly: true }), 1500)
    }
  })
}

// No keep_alive handler here on purpose: the library answers them itself, and a
// second reply gets the client kicked for answering a challenge already used.

client.on('login', () => {
  // Backstop for when the awaited text never arrives - the report then shows
  // what did, which is what makes the failure diagnosable.
  setTimeout(() => finish(0, { joined: true, stoppedEarly: false }), seconds * 1000)
})

client.on('kick_disconnect', (data) => finish(1, { kicked: String(data && data.reason) }))
client.on('disconnect', (data) => finish(1, { kicked: String(data && data.reason) }))

client.on('error', (err) => {
  const text = String(err && err.message)
  const unsupported = UNSUPPORTED.test(text)
  finish(unsupported ? 2 : 1, { skipped: unsupported, reason: text })
})

client.on('end', () => {
  if (!finished) finish(1, { reason: 'connection closed before the wait was over' })
})

// Some version failures throw from inside the protocol layer rather than
// arriving as an error event, which would kill the process with no report.
process.on('uncaughtException', (err) => {
  const text = String(err && err.message)
  const unsupported = UNSUPPORTED.test(text)
  finish(unsupported ? 2 : 1, { skipped: unsupported, reason: text })
})

// Never hang a CI job.
setTimeout(() => finish(1, { reason: 'timed out before joining' }), (seconds + 45) * 1000)
