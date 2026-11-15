from PIL import Image, ImageDraw, ImageFilter
INSET, RAD = 4, 60
BOX = {'power': (0, 866, 127, 172, 328), 'mini_battery': (2, 454, 127, 378, 328), 'dashboard': (2, 42, 489, 790, 690),
       'ring': (2, 42, 1213, 378, 690), 'big': (2, 454, 1213, 378, 690), 'modebar': (3, 42, 127, 790, 328),
       'mini_modes': (3, 42, 489, 378, 328)}
def crop(m, name):
    i, x, y, w, h = BOX[name]; x, y, w, h = x + INSET, y + INSET, w - 2 * INSET, h - 2 * INSET
    im = Image.open(f'q-{m}-{i}.png').convert('RGBA').crop((x, y, x + w, y + h))
    mask = Image.new('L', (w * 4, h * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w * 4 - 1, h * 4 - 1), radius=RAD * 4, fill=255)
    im.putalpha(mask.resize((w, h), Image.LANCZOS)); return im
P, G = 60, 34
dw, dh = 782, 682; mh = 320; th = 682; col2 = 412
layout = [('dashboard', 0, 0), ('modebar', 0, dh + G), ('ring', 0, dh + mh + 2 * G), ('big', col2, dh + mh + 2 * G),
          ('mini_battery', 0, dh + mh + th + 3 * G), ('power', col2, dh + mh + th + 3 * G)]
W = dw + 2 * P; H = dh + mh + th + mh + 3 * G + 2 * P
def grad(w, h, top, bot):
    g = Image.new('RGB', (1, h))
    for y in range(h):
        t = y / (h - 1); g.putpixel((0, y), tuple(int(top[j] + (bot[j] - top[j]) * t) for j in range(3)))
    return g.resize((w, h))
panels = []
for m, (top, bot) in [('no', ((236, 232, 226), (214, 206, 195))), ('yes', ((52, 58, 70), (20, 22, 28)))]:
    bg = grad(W, H, top, bot).convert('RGBA')
    for name, x, y in layout:
        im = crop(m, name); px, py = P + x, P + y
        sh = Image.new('RGBA', bg.size, (0, 0, 0, 0)); a = im.split()[3].point(lambda v: int(v * 0.22))
        sh.paste(Image.new('RGBA', im.size, (0, 0, 0, 255)), (px, py + 14), a); sh = sh.filter(ImageFilter.GaussianBlur(22))
        bg = Image.alpha_composite(bg, sh); bg.alpha_composite(im, (px, py))
    panels.append(bg)
out = Image.new('RGBA', (2 * W + 40, H), (0, 0, 0, 0)); out.paste(panels[0], (0, 0)); out.paste(panels[1], (W + 40, 0))
mask = Image.new('L', out.size, 0); d = ImageDraw.Draw(mask)
d.rounded_rectangle((0, 0, W - 1, H - 1), 60, fill=255); d.rounded_rectangle((W + 40, 0, 2 * W + 39, H - 1), 60, fill=255)
out.putalpha(mask); out = out.resize((out.width // 2, out.height // 2), Image.LANCZOS); out.save('widgets.png', optimize=True); print(out.size)
# launcher previews (light)
for name in BOX:
    crop('no', name).save(f'preview_{name}.png', optimize=True)
