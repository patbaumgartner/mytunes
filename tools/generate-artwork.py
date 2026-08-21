#!/usr/bin/env python3
"""Generates myTunes' original background and icon artwork.

The backgrounds are flat-design vector landscapes authored for this project, which is why the
repository can license them itself rather than inheriting someone else's wallpaper terms. Keeping
them as a generator makes the palettes reviewable and the artwork reproducible.
"""
import pathlib

WEB = pathlib.Path(__file__).resolve().parents[1] / "src" / "main" / "web"
BG = WEB / "backgrounds"
IC = WEB / "icons"

# id -> (sky top, sky bottom, sun, ridge colours far->near, water)
PALETTES = {
    "dawn-lake":    ("#2aa9c9", "#ffd9a0", "#fff3c4", ["#6fc7d6", "#4fa8bd", "#2f7f9a", "#1d5b74", "#12405a"], "#8fd3e0"),
    "dusk-ridge":   ("#2a1b3d", "#eb6f92", "#ffd6b0", ["#a86a8f", "#7d4a6d", "#57334f", "#3a2238", "#241528"], "#8a5578"),
    "night-pines":  ("#0b1026", "#25406b", "#dfe7ff", ["#33507f", "#27406a", "#1c3053", "#13223d", "#0b1628"], "#1d3457"),
    "deep-current": ("#04283b", "#0f7490", "#a8e6f0", ["#0e6b85", "#0b5a71", "#08475b", "#053546", "#032430"], "#0a5f79"),
    "ember-dunes":  ("#3b1d16", "#f6ad55", "#fff0c9", ["#c97a3c", "#a85f2e", "#833f24", "#5c2a1c", "#3a1a13"], "#c98a4b"),
}

RIDGES = [
    (0.56, "M0 {y} L120 {a} L260 {b} L380 {c} L520 {d} L680 {a} L820 {c} L960 {b} L1080 {d} L1200 {a} L1320 {c} L1440 {b} L1440 900 L0 900 Z"),
    (0.63, "M0 {y} L150 {c} L300 {a} L460 {d} L620 {b} L780 {a} L940 {c} L1100 {b} L1260 {d} L1440 {a} L1440 900 L0 900 Z"),
    (0.71, "M0 {y} L180 {b} L360 {d} L540 {a} L720 {c} L900 {b} L1080 {d} L1260 {a} L1440 {c} L1440 900 L0 900 Z"),
]


def ridge_path(template, base):
    return template.format(y=base, a=base - 46, b=base - 22, c=base - 68, d=base - 8)


def tree(x, base, height, colour):
    w = height * 0.42
    parts = []
    for i in range(3):
        top = base - height + i * height * 0.26
        spread = w * (0.55 + i * 0.22)
        parts.append(
            f'<path d="M{x:.1f} {top:.1f} L{x - spread:.1f} {top + height * 0.34:.1f} '
            f'L{x + spread:.1f} {top + height * 0.34:.1f} Z" fill="{colour}"/>'
        )
    parts.append(f'<rect x="{x - height * 0.035:.1f}" y="{base - height * 0.12:.1f}" '
                 f'width="{height * 0.07:.1f}" height="{height * 0.12:.1f}" fill="{colour}"/>')
    return "".join(parts)


def background(name, palette):
    sky_top, sky_bottom, sun, ridges, water = palette
    horizon = 560
    out = [
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice" '
        'width="1440" height="900" role="img" aria-label="myTunes background">',
        '<defs>',
        f'<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="{sky_top}"/><stop offset="1" stop-color="{sky_bottom}"/></linearGradient>',
        f'<radialGradient id="glow" cx="0.5" cy="0.5" r="0.5">'
        f'<stop offset="0" stop-color="{sun}" stop-opacity="0.95"/>'
        f'<stop offset="1" stop-color="{sun}" stop-opacity="0"/></radialGradient>',
        '</defs>',
        '<rect width="1440" height="900" fill="url(#sky)"/>',
        # The disc sits well clear of the first ridge, which starts at 0.56 of the height.
        # Drawn any lower it is swallowed by the mountains and only the glow remains visible.
        f'<circle cx="742" cy="{horizon - 150}" r="250" fill="url(#glow)"/>',
        f'<circle cx="742" cy="{horizon - 150}" r="38" fill="{sun}"/>',
    ]
    # Far ridges sit above the waterline; near ridges frame the foreground.
    for index, (fraction, template) in enumerate(RIDGES):
        base = int(900 * fraction)
        out.append(f'<path d="{ridge_path(template, base)}" fill="{ridges[index]}"/>')
    out.append(f'<ellipse cx="720" cy="{horizon + 190}" rx="420" ry="86" fill="{water}" opacity="0.85"/>')
    out.append(f'<path d="M0 720 L240 664 L520 726 L820 672 L1120 730 L1440 676 L1440 900 L0 900 Z" fill="{ridges[3]}"/>')
    out.append(f'<path d="M0 806 L300 764 L640 812 L980 762 L1440 806 L1440 900 L0 900 Z" fill="{ridges[4]}"/>')
    for x, height in ((92, 210), (168, 150), (1272, 226), (1352, 162), (1198, 132)):
        out.append(tree(x, 826, height, ridges[4]))
    out.append('</svg>')
    (BG / f"{name}.svg").write_text("".join(out), encoding="utf-8")


ICONS = {
    "play": '<path d="M8 5v14l11-7z"/>',
    "pause": '<path d="M7 5h3.5v14H7zm6.5 0H17v14h-3.5z"/>',
    "chevron": '<path d="M7 10l5 5 5-5z"/>',
    "headphones": '<path d="M12 3a8 8 0 0 0-8 8v6a3 3 0 0 0 3 3h2v-8H6v-1a6 6 0 1 1 12 0v1h-3v8h2a3 3 0 0 0 3-3v-6a8 8 0 0 0-8-8z"/>',
    "volume": '<path d="M4 9v6h4l5 4V5L8 9H4zm12.5 3a4.5 4.5 0 0 0-2.5-4v8a4.5 4.5 0 0 0 2.5-4zM14 2.2v2.1a7.5 7.5 0 0 1 0 15.4v2.1a9.5 9.5 0 0 0 0-19.6z"/>',
    "volume-off": '<path d="M4 9v6h4l5 4V5L8 9H4zm15.7 3l2.1-2.1-1.4-1.4-2.1 2.1-2.1-2.1-1.4 1.4 2.1 2.1-2.1 2.1 1.4 1.4 2.1-2.1 2.1 2.1 1.4-1.4z"/>',
    "image": '<path d="M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zm1 12h14l-4.5-6-3.5 4.5-2.5-3z"/><circle cx="8.5" cy="9.5" r="1.5"/>',
    "pip": '<path d="M3 5h18a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zm1 2v10h16V7zm7 3h7v5h-7z"/>',
    "info": '<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm1 15h-2v-6h2zm0-8h-2V7h2z"/>',
}


def icon(name, body):
    (IC / f"{name}.svg").write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">'
        f'{body}</svg>',
        encoding="utf-8",
    )


def main():
    BG.mkdir(parents=True, exist_ok=True)
    IC.mkdir(parents=True, exist_ok=True)
    for name, palette in PALETTES.items():
        background(name, palette)
    for name, body in ICONS.items():
        icon(name, body)
    print(f"wrote {len(PALETTES)} backgrounds and {len(ICONS)} icons to {WEB}")


if __name__ == "__main__":
    main()
