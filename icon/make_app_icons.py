#!/usr/bin/env python3
# GPL-3.0-or-later
"""从单张母版图生成 Android 全套启动图标。

用法:
    python icon/make_app_icons.py                      # 用 icon/src/memi_icon_master.png
    python icon/make_app_icons.py <source.png>         # 指定别的母版

产出（直接覆盖 app/src/main/res 下的资源）:
    mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp        圆角方形
    mipmap-{...}/ic_launcher_round.webp                              圆形
    drawable-nodpi/ic_launcher_foreground_material.png               自适应前景（108dp，内容落在 72dp 安全区）
    drawable-nodpi/ic_launcher_monochrome_material.png               主题图标（按亮度取 alpha）
    drawable-nodpi/ic_memi_splash.png                                启动图（84dp，圆形）

约定:
- 母版必须是正方形、纯黑底，图案允许出血（圆形遮罩会切掉四角，和旧版一致）。
- 自适应前景把母版缩到 72/108 居中，四角露出的是背景色（#000000），
  这样圆/方/水滴各种遮罩下图案大小都不变。
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
DEFAULT_SOURCE = ROOT / "icon" / "src" / "memi_icon_master.png"

LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# 自适应图标画布 108dp，安全区 72dp —— 内容和旧版保持同一比例
ADAPTIVE_CANVAS = 1024
ADAPTIVE_SAFE_RATIO = 72 / 108

# 圆角方形：实测旧图标半径约 21%
CORNER_RATIO = 0.215

# 传统图标留 3% 黑边：母版的图案本就出血，压到边上会让右下角的小字贴边发毛
LEGACY_INSET = 0.97

# 主题图标：亮度高于该值算前景
MONO_THRESHOLD = 96


def load_master(path: Path) -> Image.Image:
    img = Image.open(path).convert("RGB")
    side = min(img.size)
    if img.size[0] != img.size[1]:
        left = (img.width - side) // 2
        top = (img.height - side) // 2
        img = img.crop((left, top, left + side, top + side))
    return img


def rounded_mask(size: int, radius_ratio: float) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle(
        (0, 0, size - 1, size - 1),
        radius=round(size * radius_ratio),
        fill=255,
    )
    return mask


def circle_mask(size: int) -> Image.Image:
    # 4 倍超采样再缩小，避免小尺寸出现锯齿
    scale = 4
    big = Image.new("L", (size * scale, size * scale), 0)
    ImageDraw.Draw(big).ellipse((0, 0, size * scale - 1, size * scale - 1), fill=255)
    return big.resize((size, size), Image.LANCZOS)


def resize_art(art: Image.Image, box: int) -> Image.Image:
    return art.resize((box, box), Image.LANCZOS, reducing_gap=3.0)


def write_webp(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "WEBP", quality=95, method=6)


def build_legacy(art: Image.Image) -> list[Path]:
    written = []
    for density, size in LEGACY_SIZES.items():
        inner = round(size * LEGACY_INSET)
        plate = Image.new("RGB", (size, size), (0, 0, 0))
        plate.paste(resize_art(art, inner), ((size - inner) // 2, (size - inner) // 2))

        square = plate.convert("RGBA")
        square.putalpha(rounded_mask(size, CORNER_RATIO))
        p = RES / f"mipmap-{density}" / "ic_launcher.webp"
        write_webp(square, p)
        written.append(p)

        round_icon = plate.convert("RGBA")
        round_icon.putalpha(circle_mask(size))
        p = RES / f"mipmap-{density}" / "ic_launcher_round.webp"
        write_webp(round_icon, p)
        written.append(p)
    return written


def adaptive_foreground(art: Image.Image) -> Image.Image:
    canvas = Image.new("LA", (ADAPTIVE_CANVAS, ADAPTIVE_CANVAS), (0, 0))
    box = round(ADAPTIVE_CANVAS * ADAPTIVE_SAFE_RATIO)
    scaled = resize_art(art, box).convert("L")
    offset = (ADAPTIVE_CANVAS - box) // 2
    # 图案区域整体不透明，四角留透明给背景色
    layer = Image.new("LA", (box, box), (0, 255))
    layer.paste(scaled, (0, 0))
    canvas.paste(layer, (offset, offset))
    return canvas


def monochrome(art: Image.Image) -> Image.Image:
    box = round(ADAPTIVE_CANVAS * ADAPTIVE_SAFE_RATIO)
    lum = resize_art(art, box).convert("L")
    # 阈值处做一点羽化，避免小尺寸下的硬边
    mask = lum.point(lambda v: 255 if v >= MONO_THRESHOLD else 0)
    mask = mask.filter(ImageFilter.GaussianBlur(0.6))
    offset = (ADAPTIVE_CANVAS - box) // 2
    full = Image.new("L", (ADAPTIVE_CANVAS, ADAPTIVE_CANVAS), 0)
    full.paste(mask, (offset, offset))
    white = Image.new("L", (ADAPTIVE_CANVAS, ADAPTIVE_CANVAS), 255)
    return Image.merge("LA", (white, full))


def splash(art: Image.Image, size: int = 512) -> Image.Image:
    scaled = resize_art(art, size).convert("RGBA")
    scaled.putalpha(circle_mask(size))
    return scaled.convert("LA")


def main() -> int:
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SOURCE
    if not src.exists():
        print(f"母版不存在: {src}")
        return 1
    art = load_master(src)
    written = build_legacy(art)

    fg = RES / "drawable-nodpi" / "ic_launcher_foreground_material.png"
    adaptive_foreground(art).save(fg, "PNG", optimize=True)
    written.append(fg)

    mono = RES / "drawable-nodpi" / "ic_launcher_monochrome_material.png"
    monochrome(art).save(mono, "PNG", optimize=True)
    written.append(mono)

    sp = RES / "drawable-nodpi" / "ic_memi_splash.png"
    splash(art).save(sp, "PNG", optimize=True)
    written.append(sp)

    for p in written:
        print(f"  {p.relative_to(ROOT)}  {p.stat().st_size:,} B")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
