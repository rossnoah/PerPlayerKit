#!/usr/bin/env python3
"""Render a captured PerPlayerKit GUI into a PNG for the docs.

Input is the JSON capture.js dumps off a live server, so the image always
matches what the plugin actually shows. The container panel is the vanilla
GUI sheet, the title uses the vanilla bitmap font, and item icons come from
mcicon, which follows each item's real model.
"""
import json, os, sys
from PIL import Image
import mcicon

ASSETS = os.path.join(".cache", "assets")
SCALE = 3            # 3x is crisp on retina without dominating the page
GUI_W = 176          # vanilla container width, in gui pixels
SLOT0 = (7, 17)      # top-left of the first slot
PITCH = 18
TITLE_AT = (8, 6)
TITLE_RGB = (63, 63, 63)


def _panel(rows):
    """Crop a rows-tall container out of the vanilla generic_54 sheet."""
    sheet = Image.open(os.path.join(ASSETS, "textures/gui/container/generic_54.png")).convert("RGBA")
    k = sheet.width // 256
    body_h = SLOT0[1] + PITCH * rows          # title bar + slot rows
    border = 7                                 # reuse the sheet's bottom border
    out = Image.new("RGBA", (GUI_W * k, (body_h + border) * k))
    out.paste(sheet.crop((0, 0, GUI_W * k, body_h * k)), (0, 0))
    out.paste(sheet.crop((0, (222 - border) * k, GUI_W * k, 222 * k)), (0, body_h * k))
    return out, k


class Font:
    """The vanilla ASCII bitmap font: a 16x16 grid of 8x8 glyphs."""

    def __init__(self):
        self.sheet = Image.open(os.path.join(ASSETS, "textures/font/ascii.png")).convert("RGBA")
        self.cell = self.sheet.width // 16
        self._w = {}

    def glyph(self, ch):
        c = ord(ch)
        if c > 255:
            c = ord("?")
        return self.sheet.crop(((c % 16) * self.cell, (c // 16) * self.cell,
                                (c % 16 + 1) * self.cell, (c // 16 + 1) * self.cell))

    def advance(self, ch):
        if ch == " ":
            return 4
        if ch in self._w:
            return self._w[ch]
        g = self.glyph(ch)
        bbox = g.split()[3].getbbox()
        px = self.cell // 8
        self._w[ch] = (bbox[2] // px + 1) if bbox else 4
        return self._w[ch]

    def width(self, text):
        return sum(self.advance(ch) for ch in text)

    def draw(self, canvas, text, x, y, scale, rgb):
        """Draw text at `scale` output pixels per font pixel."""
        size = 8 * scale
        for ch in text:
            g = self.glyph(ch).resize((size, size), Image.NEAREST)
            alpha = g.split()[3].point(lambda v: 255 if v > 32 else 0)
            canvas.paste(Image.new("RGBA", g.size, rgb + (255,)), (int(x), int(y)), alpha)
            x += self.advance(ch) * scale
        return x


def render(cap, out_path):
    rows = cap["rows"]
    panel, k = _panel(rows)
    up = SCALE / k                       # sheet pixels -> output pixels
    W, H = int(panel.width * up), int(panel.height * up)
    img = panel.resize((W, H), Image.NEAREST)

    f = Font()
    missing = set()
    for s in cap["slots"]:
        if s["i"] >= rows * 9:
            continue
        ic = mcicon.icon(s["id"], 16 * SCALE)
        if ic is None:
            missing.add(s["id"])
            continue
        x = (SLOT0[0] + 1 + (s["i"] % 9) * PITCH) * SCALE
        y = (SLOT0[1] + 1 + (s["i"] // 9) * PITCH) * SCALE
        img.paste(ic, (x, y), ic)
        if s.get("count", 1) > 1:
            # Vanilla draws the stack size bottom-right of the icon, with a shadow.
            txt = str(s["count"])
            tx = x + (17 - f.width(txt)) * SCALE
            ty = y + 9 * SCALE
            f.draw(img, txt, tx + SCALE, ty + SCALE, SCALE, (63, 63, 63))
            f.draw(img, txt, tx, ty, SCALE, (255, 255, 255))

    f.draw(img, cap.get("title") or "", TITLE_AT[0] * SCALE, TITLE_AT[1] * SCALE,
           SCALE, TITLE_RGB)

    img.convert("RGB").save(out_path)
    return missing


if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    os.makedirs(os.path.dirname(dst) or ".", exist_ok=True)
    cap = json.load(open(src))
    miss = render(cap, dst)
    im = Image.open(dst)
    print(f"{os.path.basename(dst):<22} {im.width}x{im.height}"
          + (f"  MISSING: {', '.join(sorted(miss))}" if miss else ""))
