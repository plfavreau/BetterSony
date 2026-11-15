"""Generates the "Quiet s" launcher icon + notification icon as vector drawables.

Design units are the 100x100 SVG (variant 1) in design/logo-variants.html: one stroke s made of
two stacked half-ellipses, the sibling of BetterBose's "Quiet b". The adaptive icon shows the
middle 72dp of its 108dp canvas, so design unit u maps to 18 + 0.72u dp.
Run from the repo root: python3 design/gen_icon.py
"""
import math

RES = "app/src/main/res"
S = 0.72
CX, TOP_CY, BOTTOM_CY = 50, 37.5, 62.5
RX, RY = 14, 12.5
OPEN = math.radians(30)
STROKE = 7
HEAD = '<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"'


def path(scale, offset):
    p = lambda u: offset + u * scale
    sx, sy = CX + RX * math.cos(OPEN), TOP_CY - RY * math.sin(OPEN)
    ex, ey = CX - RX * math.cos(OPEN), BOTTOM_CY + RY * math.sin(OPEN)
    rx, ry = RX * scale, RY * scale
    return (f"M{p(sx):.2f},{p(sy):.2f} A{rx:.2f},{ry:.2f} 0 1,0 {p(CX):.2f},{p(TOP_CY + RY):.2f} "
            f"A{rx:.2f},{ry:.2f} 0 1,1 {p(ex):.2f},{p(ey):.2f}")


def glyph(scale, offset):
    return (f'    <path android:pathData="{path(scale, offset)}" android:strokeColor="#FFFFFF" '
            f'android:strokeWidth="{STROKE * scale:.2f}" android:strokeLineCap="round" android:fillColor="#00000000"/>')


def vector(size, body, extra=""):
    return (f'{HEAD}{extra}\n    android:width="{size}dp" android:height="{size}dp" '
            f'android:viewportWidth="{size}" android:viewportHeight="{size}">\n{body}\n</vector>\n')


def write(name, text):
    with open(f"{RES}/drawable/{name}.xml", "w") as f:
        f.write(text)


write("ic_launcher_fg", vector(108, glyph(S, 18)))
write("ic_launcher_mono", vector(108, glyph(S, 18)))
write("ic_launcher_bg", vector(108, """    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient android:type="radial" android:centerX="54" android:centerY="46.8" android:gradientRadius="54"
                android:startColor="#3A3A3E" android:endColor="#0C0C0E"/>
        </aapt:attr>
    </path>""", '\n    xmlns:aapt="http://schemas.android.com/aapt"'))

# Notification icon: the glyph spans ~u 25..75 vertically; map u 22..78 onto 24dp.
NOTIFY_SCALE = 24 / 56
write("ic_notify", vector(24, glyph(NOTIFY_SCALE, -22 * NOTIFY_SCALE)))
