from PIL import Image, ImageDraw, ImageFilter
TOP, H, RAD, G, P = 90, 2140, 70, 50, 40
def panel(name):
    im = Image.open(f'raw-{name}.png').convert('RGBA').crop((0, TOP, 1080, TOP + H))
    m = Image.new('L', im.size, 0); ImageDraw.Draw(m).rounded_rectangle((0, 0, im.width - 1, im.height - 1), RAD, fill=255)
    im.putalpha(m); return im
for out, (l, r) in {'screens-1': ('a', 'b'), 'screens-2': ('c', 'd')}.items():
    W = 2 * 1080 + G + 2 * P; Ht = H + 2 * P
    bg = Image.new('RGBA', (W, Ht), (255, 255, 255, 0))
    for i, n in enumerate((l, r)):
        im = panel(n); x = P + i * (1080 + G)
        sh = Image.new('RGBA', bg.size, (0, 0, 0, 0)); sh.paste(Image.new('RGBA', im.size, (0, 0, 0, 60)), (x, P + 10), im.split()[3])
        bg = Image.alpha_composite(bg, sh.filter(ImageFilter.GaussianBlur(18))); bg.alpha_composite(im, (x, P))
    bg = bg.resize((1200, round(Ht * 1200 / W)), Image.LANCZOS); bg.save(f'{out}.png', optimize=True); print(out, bg.size)
