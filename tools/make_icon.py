"""Renders the launcher icon (a 3x3 grid of tracker dots on paper) into android/res/mipmap-*/ic_launcher.png."""
import os
from PIL import Image, ImageDraw

SIZES = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
DOTS = ['#8fbf9f', '#b3a4ef', None, '#f2b2cc', '#e9a35a', '#f0d84a', None, '#5d4fd6', '#4cc552']

root = os.path.join(os.path.dirname(__file__), '..', 'android', 'res')
S = 768
img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([24, 24, S - 24, S - 24], radius=170, fill='#fbf8f3', outline='#e6dfd3', width=10)
step, r = 190, 66
for i, c in enumerate(DOTS):
    cx = S // 2 + (i % 3 - 1) * step
    cy = S // 2 + (i // 3 - 1) * step
    box = [cx - r, cy - r, cx + r, cy + r]
    if c:
        d.ellipse(box, fill=c)
    else:
        d.ellipse(box, outline='#3a3530', width=14)

for name, size in SIZES.items():
    out = os.path.join(root, 'mipmap-' + name)
    os.makedirs(out, exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(os.path.join(out, 'ic_launcher.png'))
print('icons written')
