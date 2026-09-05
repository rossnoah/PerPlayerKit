// Seeds a throwaway PerPlayerKit server and dumps every GUI to JSON.
// Layout constants mirror src/main/java/dev/noah/perplayerkit/gui/GUI.java.
const mineflayer = require('mineflayer')
const fs = require('fs')

const HOST = '127.0.0.1', PORT = Number(process.env.PPK_PORT || 25599), USER = 'Notch'
const VERSION = fs.readFileSync('.cache/paper-version.txt', 'utf8').trim()
const OUT = 'captures'

const NETHER_STAR = 37, PUBLIC_KITS_BTN = 38
const KIT_SLOT = c => 27 + c          // main menu kit buttons
const PAGE_TAB = p => 47 + p          // kit room page tabs
const SAVE_BARRIER = 53               // kit room save
const EDITABLE = 45                   // slots 0..44 hold items
const CHEST = 54                      // window slots 0..53 = chest, 54.. = player inv

const KITS = JSON.parse(fs.readFileSync(__dirname + '/kits.json', 'utf8'))
const PAGES = KITS.kitRoomPages
const PUBLIC_KITS = Object.fromEntries(
  Object.entries(KITS.publicKits).map(([id, k]) => [id, k.items]))

const sleep = ms => new Promise(r => setTimeout(r, ms))
const log = (...a) => console.log('[capture]', ...a)

const bot = mineflayer.createBot({ host: HOST, port: PORT, username: USER, version: VERSION, auth: 'offline' })
let mcData, Item
bot.on('error', e => { console.error('ERROR', e.message); process.exit(1) })
bot.on('kicked', r => { console.error('KICKED', JSON.stringify(r)); process.exit(1) })

function plainName(txt) {
  if (txt == null) return null
  if (typeof txt === 'string') { try { return plainName(JSON.parse(txt)) } catch { return txt } }
  if (txt.value !== undefined) return plainName(txt.value)
  if (Array.isArray(txt)) return txt.map(plainName).join('')
  if (typeof txt === 'object') {
    let s = txt.text ? plainName(txt.text) : ''
    if (txt.extra) s += plainName(txt.extra)
    return s
  }
  return String(txt)
}

function dumpWindow(w, name) {
  const slots = w.slots.slice(0, CHEST).map((s, i) => {
    if (!s) return null
    let label = null
    try { label = plainName(s.customName ?? s.nbt?.value?.display?.value?.Name) } catch {}
    return { i, id: s.name, count: s.count, label: label || null }
  }).filter(Boolean)
  const out = { name, title: plainName(w.title), rows: CHEST / 9, slots }
  fs.mkdirSync(OUT, { recursive: true })
  fs.writeFileSync(`${OUT}/${name}.json`, JSON.stringify(out, null, 1))
  log(`captured ${name}: ${slots.length} items, title="${out.title}"`)
}

function waitWindow(timeout = 8000) {
  return new Promise((res, rej) => {
    const t = setTimeout(() => rej(new Error('no window opened')), timeout)
    bot.once('windowOpen', w => { clearTimeout(t); setTimeout(() => res(w), 350) })
  })
}
async function closeWin() { try { bot.closeWindow(bot.currentWindow) } catch {} await sleep(250) }

// Put items into the bot's own inventory (creative), one per hotbar/inv slot.
async function giveItems(ids) {
  for (let i = 0; i < ids.length && i < 36; i++) {
    const d = mcData.itemsByName[ids[i]]
    if (!d) { log('unknown item', ids[i]); continue }
    await bot.creative.setInventorySlot(9 + i, new Item(d.id, 1))
    await sleep(25)
  }
}
async function clearInv() {
  for (let i = 9; i < 45; i++) { await bot.creative.setInventorySlot(i, null); await sleep(8) }
}

async function seedPublicKits() {
  for (const [id, items] of Object.entries(PUBLIC_KITS)) {
    await clearInv(); await giveItems(items); await sleep(200)
    bot.chat(`/savepublickit ${id}`); await sleep(500)
    log('saved public kit', id)
  }
  await clearInv()
}

async function seedKitRoom() {
  for (let page = 0; page < PAGES.length; page++) {
    await clearInv(); await giveItems(PAGES[page]); await sleep(250)
    bot.chat('/kit'); const main = await waitWindow()
    await bot.clickWindow(NETHER_STAR, 0, 0); await sleep(600)
    if (page > 0) { await bot.clickWindow(PAGE_TAB(page), 0, 0); await sleep(600) }
    const n = Math.min(PAGES[page].length, EDITABLE)
    for (let k = 0; k < n; k++) {
      await bot.clickWindow(CHEST + k, 0, 0); await sleep(60)   // pick up from inventory
      await bot.clickWindow(k, 0, 0); await sleep(60)               // place into kit room
    }
    await bot.clickWindow(SAVE_BARRIER, 1, 1); await sleep(500)     // shift-right-click = save
    log(`kit room page ${page + 1} saved (${n} items)`)
    await closeWin()
  }
  await clearInv()
}

async function seedPlayerKits() {
  const loadouts = KITS.playerKits.map(id => PUBLIC_KITS[id])
  for (let s = 0; s < loadouts.length; s++) {
    await clearInv(); await giveItems(loadouts[s]); await sleep(250)
    bot.chat('/kit'); await waitWindow()
    await bot.clickWindow(KIT_SLOT(s), 0, 0); await sleep(700)      // open kit editor
    const n = loadouts[s].length
    for (let k = 0; k < n; k++) {
      await bot.clickWindow(CHEST + k, 0, 0); await sleep(60)
      await bot.clickWindow(k, 0, 0); await sleep(60)
    }
    await closeWin()                                                 // close saves the kit
    log('saved player kit', s + 1)
  }
  await clearInv()
}

async function capture(name, cmd, clicks = []) {
  bot.chat(cmd)
  let w = await waitWindow()
  for (const c of clicks) { await bot.clickWindow(c, 0, 0); w = await waitWindow() }
  dumpWindow(w, name)
  await closeWin()
}

bot.once('spawn', async () => {
  mcData = require('minecraft-data')(bot.version)
  Item = require('prismarine-item')(bot.version)
  log('spawned', bot.username, '| version', bot.version)
  await sleep(1200)
  bot.chat('/gamemode creative'); await sleep(400)
  try {
    await seedPublicKits()
    await seedKitRoom()
    await seedPlayerKits()
    await capture('MainMenu', '/kit')
    await capture('PublicKits', '/publickit')
    await capture('EnderchestMenu', '/kit', [18])
    await capture('KitRoomEditor', '/kit', [NETHER_STAR])
    await capture('KitEditor', '/kit', [KIT_SLOT(0)])
    log('DONE')
    process.exit(0)
  } catch (e) { console.error('FAILED', e.stack); process.exit(1) }
})
