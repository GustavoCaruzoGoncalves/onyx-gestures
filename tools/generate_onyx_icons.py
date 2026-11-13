#!/usr/bin/env python3
"""Generate Android adaptive + legacy launcher icons from Onyx Gestures artwork."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "artwork" / "a_high_contrast_neon_app_icon_design_on_a_dark_pur.png"
RES = ROOT / "app" / "src" / "main" / "res"
ART = ROOT / "artwork"

MASTER = 1080

LEGACY = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

ADAPTIVE = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def make_transparent_fg(im: Image.Image) -> Image.Image:
    arr = np.array(im.convert("RGBA"), dtype=np.int16)
    r, g, b, a = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]
    keep = (
        (r > 140)
        | (g > 140)
        | (b > 160)
        | ((b > 100) & ((r > 60) | (g > 40)))
        | ((r + g + b) >= 90)
    )
    arr[~keep, 3] = 0
    soft = (~keep) | ((r + g + b) < 120)
    # keep already handled; soft fringe on dim kept pixels
    dim = keep & ((r + g + b) < 120)
    lum = np.clip(r + g + b, 0, 120)
    arr[dim, 3] = np.maximum(arr[dim, 3], (lum[dim] * 255 // 120).astype(np.int16))
    return Image.fromarray(arr.astype(np.uint8), "RGBA")


def content_bbox(im: Image.Image) -> tuple[int, int, int, int]:
    arr = np.array(im)
    alpha = arr[:, :, 3]
    ys, xs = np.where(alpha > 10)
    if len(xs) == 0:
        return (0, 0, im.width, im.height)
    pad = int(min(im.width, im.height) * 0.02)
    return (
        max(0, int(xs.min()) - pad),
        max(0, int(ys.min()) - pad),
        min(im.width, int(xs.max()) + pad + 1),
        min(im.height, int(ys.max()) + pad + 1),
    )


def extract_symbol_only(im: Image.Image) -> Image.Image:
    """Drop lower text band — launcher uses central symbol only."""
    w, h = im.size
    return im.crop((0, 0, w, int(h * 0.62)))


def fit_on_canvas(src: Image.Image, size: int, scale: float = 0.72) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    target = int(size * scale)
    fitted = src.copy()
    fitted.thumbnail((target, target), Image.Resampling.LANCZOS)
    x = (size - fitted.width) // 2
    y = (size - fitted.height) // 2
    canvas.paste(fitted, (x, y), fitted)
    return canvas


def make_background(size: int) -> Image.Image:
    """Fast dark purple / navy gradient (no white)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    nx, ny = xx / size, yy / size
    pr = np.clip(1.0 - ((nx - 0.25) ** 2 + (ny - 0.2) ** 2) * 4, 0, 1)
    cy = np.clip(1.0 - ((nx - 0.85) ** 2 + (ny - 0.45) ** 2) * 5, 0, 1)
    d = np.sqrt((nx - 0.5) ** 2 + (ny - 0.5) ** 2)
    r = np.clip(8 + 70 * pr + 10 * (1 - d), 0, 255).astype(np.uint8)
    g = np.clip(2 + 20 * pr + 40 * cy, 0, 255).astype(np.uint8)
    b = np.clip(18 + 110 * pr + 60 * cy + 20 * (1 - d), 0, 255).astype(np.uint8)
    a = np.full((size, size), 255, dtype=np.uint8)
    img = Image.fromarray(np.dstack([r, g, b, a]), "RGBA")
    return img.filter(ImageFilter.GaussianBlur(radius=max(1, size // 80)))


def circle_mask(size: int) -> Image.Image:
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).ellipse((0, 0, size - 1, size - 1), fill=255)
    return m


def save_png(im: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG", optimize=True)
    print("wrote", path.relative_to(ROOT), im.size)


def main() -> None:
    assert SRC.exists(), f"missing {SRC}"
    src = Image.open(SRC).convert("RGBA")
    save_png(src, ART / "onyx_gestures_full_brand.png")

    symbol = extract_symbol_only(src)
    symbol_fg = make_transparent_fg(symbol)
    symbol_fg = symbol_fg.crop(content_bbox(symbol_fg))

    bg_master = make_background(MASTER)
    fg_master = fit_on_canvas(symbol_fg, MASTER, scale=0.78)
    legacy_master = bg_master.copy()
    legacy_master.alpha_composite(fg_master)

    save_png(bg_master, ART / "ic_launcher_background_master.png")
    save_png(fg_master, ART / "ic_launcher_foreground_master.png")
    save_png(legacy_master, ART / "ic_launcher_legacy_master.png")

    draw = RES / "drawable"
    save_png(bg_master.resize((432, 432), Image.Resampling.LANCZOS), draw / "ic_launcher_background.png")
    save_png(fg_master.resize((432, 432), Image.Resampling.LANCZOS), draw / "ic_launcher_foreground.png")

    for folder, px in ADAPTIVE.items():
        d = RES / folder
        save_png(bg_master.resize((px, px), Image.Resampling.LANCZOS), d / "ic_launcher_background.png")
        save_png(fg_master.resize((px, px), Image.Resampling.LANCZOS), d / "ic_launcher_foreground.png")

    for folder, px in LEGACY.items():
        d = RES / folder
        icon = legacy_master.resize((px, px), Image.Resampling.LANCZOS).convert("RGBA")
        save_png(icon, d / "ic_launcher.png")
        round_icon = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        round_icon.paste(icon, mask=circle_mask(px))
        # ensure circular alpha
        rgba = np.array(icon)
        mask = np.array(circle_mask(px))
        rgba[:, :, 3] = (rgba[:, :, 3].astype(np.uint16) * mask // 255).astype(np.uint8)
        save_png(Image.fromarray(rgba, "RGBA"), d / "ic_launcher_round.png")

    print("done")


if __name__ == "__main__":
    main()
