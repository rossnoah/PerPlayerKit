#!/usr/bin/env python3
"""Render a Minecraft item icon the way the game does.

Vanilla decides an item's inventory icon from its model, not from a single
texture. Flat items composite one or more tinted layers; block items are drawn
as an isometric cube built from three faces. Doing the same here is what makes
obsidian look like a block and a potion look like a potion.
"""
import json, os, functools
from PIL import Image

ASSETS = os.path.join(".cache", "assets")

# Vanilla shades cube faces by direction so the block reads as 3D.
FACE_SHADE = {"up": 1.0, "north": 0.79, "east": 0.61}


def _strip(ref):
    return ref.split(":", 1)[-1] if ref else ref


@functools.lru_cache(maxsize=None)
def _json(rel):
    with open(os.path.join(ASSETS, rel)) as f:
        return json.load(f)


@functools.lru_cache(maxsize=None)
def _tex(ref):
    """Load a texture by its model reference, e.g. minecraft:block/obsidian."""
    p = os.path.join(ASSETS, "textures", _strip(ref) + ".png")
    if not os.path.exists(p):
        return None
    im = Image.open(p).convert("RGBA")
    if im.height > im.width:          # animated strip: first frame only
        im = im.crop((0, 0, im.width, im.width))
    return im


def _pick_model(item_id):
    """Walk the item definition down to a concrete model plus any tints."""
    try:
        node = _json(f"items/{item_id}.json")["model"]
    except (FileNotFoundError, KeyError):
        return None, [], False
    for _ in range(10):
        t = _strip(node.get("type", ""))
        if t == "model":
            return node["model"], node.get("tints", []), False
        if t == "special":                     # shield, chest: entity-rendered
            return node.get("base"), [], True
        if t == "condition":
            node = node.get("on_false") or node.get("on_true")
        elif t == "select":
            node = node.get("fallback") or (node.get("cases") or [{}])[0].get("model")
        elif t == "range_dispatch":
            node = node.get("fallback") or (node.get("entries") or [{}])[0].get("model")
        elif t == "composite":
            node = (node.get("models") or [None])[0]
        else:
            return None, [], False
        if node is None:
            return None, [], False
    return None, [], False


def _model_textures(model_ref):
    """Merge the textures map down the model's parent chain."""
    tex, p, depth = {}, _strip(model_ref), 0
    while p and depth < 12:
        try:
            j = _json(f"models/{p}.json")
        except FileNotFoundError:
            break
        for k, v in j.get("textures", {}).items():
            tex.setdefault(k, v)
        parent = _strip(j.get("parent", ""))
        if not parent or parent.startswith("builtin"):
            break
        p, depth = parent, depth + 1
    # resolve #references
    for _ in range(4):
        for k, v in list(tex.items()):
            if isinstance(v, str) and v.startswith("#"):
                tex[k] = tex.get(v[1:], v)
    return tex


def _tint(img, argb):
    """Multiply an RGBA layer by a packed colour, as vanilla does for potions."""
    r, g, b = (argb >> 16) & 255, (argb >> 8) & 255, argb & 255
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            pr, pg, pb, pa = px[x, y]
            if pa:
                px[x, y] = (pr * r // 255, pg * g // 255, pb * b // 255, pa)
    return out


def _shade(img, f):
    if f >= 1.0:
        return img
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (int(r * f), int(g * f), int(b * f), a)
    return out


def _affine_into(canvas, tile, p0, p1, p2):
    """Paste `tile` into `canvas` so its corners land on p0 (origin), p1 (+x), p2 (+y)."""
    w, h = tile.size
    ax, ay = (p1[0] - p0[0]) / w, (p1[1] - p0[1]) / w
    bx, by = (p2[0] - p0[0]) / h, (p2[1] - p0[1]) / h
    det = ax * by - bx * ay
    if abs(det) < 1e-9:
        return
    ia, ib = by / det, -bx / det
    ic, id_ = -ay / det, ax / det
    coeffs = (ia, ib, -(ia * p0[0] + ib * p0[1]),
              ic, id_, -(ic * p0[0] + id_ * p0[1]))
    warped = tile.transform(canvas.size, Image.AFFINE, coeffs, resample=Image.NEAREST)
    mask = Image.new("L", canvas.size, 0)
    from PIL import ImageDraw
    ImageDraw.Draw(mask).polygon(
        [p0, p1, (p1[0] + p2[0] - p0[0], p1[1] + p2[1] - p0[1]), p2], fill=255)
    canvas.paste(warped, (0, 0), Image.composite(warped.split()[3], mask, mask.point(lambda v: 255 - v)))


def _isometric(up, north, east, size):
    """Draw the three visible faces of a cube into a size x size icon."""
    s = size
    out = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    q = s / 4.0
    top_n, top_e, top_s, top_w = (s / 2, 0), (s, q), (s / 2, s / 2), (0, q)
    if up:
        _affine_into(out, _shade(up, FACE_SHADE["up"]), top_w, top_n, top_s)
    if north:
        _affine_into(out, _shade(north, FACE_SHADE["north"]),
                     top_w, top_s, (0, 3 * q))
    if east:
        _affine_into(out, _shade(east, FACE_SHADE["east"]),
                     top_s, top_e, (s / 2, s))
    return out


def _entity_chest(fname):
    """Build an isometric chest from its entity texture.

    Chest UVs come from the vanilla model: the lid is a 14x5x14 box at texture
    offset (0,0) and the base a 14x10x14 box at (0,19).
    """
    src = _tex("entity/chest/" + fname)
    if src is None:
        return None
    k = src.width // 64
    def c(x, y, w, h):
        return src.crop((x * k, y * k, (x + w) * k, (y + h) * k))
    top = c(28, 0, 14, 14)          # up face; (14,0) is the lid underside
    front = Image.new("RGBA", (14 * k, 15 * k))
    front.paste(c(14, 14, 14, 5), (0, 0))
    front.paste(c(14, 33, 14, 10), (0, 5 * k))
    side = Image.new("RGBA", (14 * k, 15 * k))
    side.paste(c(0, 14, 14, 5), (0, 0))
    side.paste(c(0, 33, 14, 10), (0, 5 * k))
    return top, side, front


def _entity_shield():
    """Crop the shield face out of its entity texture.

    The vanilla shield model is a 12x22x1 plate at texture offset (0,0), so the
    front face sits at (1,1) once the 1px depth border is accounted for.
    """
    src = _tex("entity/shield_base_nopattern")
    if src is None:
        return None
    k = src.width // 64
    return src.crop((1 * k, 1 * k, 13 * k, 23 * k))


@functools.lru_cache(maxsize=None)
def icon(item_id, size=64):
    """Return an RGBA icon for an item id, or None if it cannot be rendered."""
    if item_id in ("chest", "trapped_chest", "ender_chest"):
        parts = _entity_chest("ender" if item_id == "ender_chest" else "normal")
        if parts:
            top, side, front = (p.resize((max(16, size),) * 2, Image.NEAREST) for p in parts)
            return _isometric(top, side, front, size)
    if item_id == "shield":
        face = _entity_shield()
        if face:
            out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            w = int(size * 0.62)
            h = int(w * face.height / face.width)
            out.paste(face.resize((w, h), Image.NEAREST),
                      ((size - w) // 2, (size - h) // 2))
            return out

    model_ref, tints, special = _pick_model(item_id)
    if not model_ref:
        return None
    tex = _model_textures(model_ref)

    if "layer0" in tex:                                   # flat sprite, possibly layered
        base = None
        i = 0
        while f"layer{i}" in tex:
            layer = _tex(tex[f"layer{i}"])
            if layer is not None:
                if i < len(tints) and isinstance(tints[i], dict) and "default" in tints[i]:
                    layer = _tint(layer, tints[i]["default"] & 0xFFFFFF)
                layer = layer.resize((size, size), Image.NEAREST)
                base = layer if base is None else Image.alpha_composite(base, layer)
            i += 1
        return base

    faces = {}
    for key, side in (("up", "up"), ("north", "north"), ("east", "east")):
        generic = (tex.get("all") or tex.get("side") or tex.get("texture")
                   or tex.get("particle"))
        if side == "up":
            ref = tex.get("up") or tex.get("top") or tex.get("end") or generic
        else:
            ref = tex.get(side) or generic
        faces[key] = _tex(ref) if ref else None
    if any(faces.values()):
        px = max(16, size)
        faces = {k: (v.resize((px, px), Image.NEAREST) if v else None) for k, v in faces.items()}
        return _isometric(faces["up"], faces["north"], faces["east"], size)
    return None
