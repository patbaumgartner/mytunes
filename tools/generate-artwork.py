#!/usr/bin/env python3
"""Generates myTunes' original background and icon artwork.

The backgrounds are flat-design vector landscapes authored for this project, which is why the
repository can license them itself rather than inheriting someone else's wallpaper terms. Keeping
them as a generator makes the palettes reviewable and the artwork reproducible. Each background
carries a SMIL day-night cycle: the sun arcs across the sky left to right, hands over to a moon,
and the scene darkens towards midnight. SMIL is declarative animation inside the image, so the
zero-JavaScript rule is untouched.
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


def rabbit(x, base, height, colour, flip=False):
    """A flat sitting rabbit silhouette, drawn in local coordinates and placed by
    translate/scale. Local space: paws on y=0, facing positive x, ~112 units tall
    to the ear tips. One ear twitches occasionally.
    """
    scale = height / 112.0
    mirror = " scale(-1 1)" if flip else ""
    body = ('<circle cx="-14" cy="-30" r="30"/>'
            '<ellipse cx="8" cy="-36" rx="22" ry="26"/>'
            '<circle cx="30" cy="-62" r="16"/>'
            '<circle cx="-42" cy="-22" r="7"/>'
            '<path d="M14 0 L44 0 Q44 -9 33 -9 L20 -9 Q14 -6 14 0 Z"/>')
    ear_back = '<path d="M22 -74 Q13 -100 19 -112 Q29 -108 30 -76 Z"/>'
    ear_front = ('<g transform="rotate(0 37 -76)">'
                 '<path d="M34 -74 Q34 -104 43 -112 Q49 -102 40 -74 Z"/>'
                 '<animateTransform attributeName="transform" type="rotate" additive="replace" '
                 'values="0 37 -76;0 37 -76;-12 37 -76;0 37 -76;0 37 -76" '
                 'keyTimes="0;0.42;0.5;0.58;1" dur="11s" repeatCount="indefinite"/></g>')
    return (f'<g transform="translate({x} {base}) scale({scale:.3f}){mirror}" fill="{colour}">'
            f'{body}{ear_back}{ear_front}</g>')


def bush(x, base, size, colour):
    return (f'<g fill="{colour}"><circle cx="{x - size * 0.5:.1f}" cy="{base - size * 0.38:.1f}" r="{size * 0.42:.1f}"/>'
            f'<circle cx="{x:.1f}" cy="{base - size * 0.52:.1f}" r="{size * 0.55:.1f}"/>'
            f'<circle cx="{x + size * 0.55:.1f}" cy="{base - size * 0.34:.1f}" r="{size * 0.38:.1f}"/></g>')


def reeds(x, base, height, colour):
    """A clump of waterside reeds swaying gently around its rootline."""
    stems = "".join(
        f'<path d="M{dx} 0 Q{dx + lean} {-height * 0.55:.1f} {dx + lean * 2} {-height * f:.1f}" '
        f'fill="none" stroke="{colour}" stroke-width="3" stroke-linecap="round"/>'
        f'<ellipse cx="{dx + lean * 2}" cy="{-height * f:.1f}" rx="3.4" ry="9" fill="{colour}"/>'
        for dx, lean, f in ((-8, 2, 0.9), (0, -1, 1.0), (8, 3, 0.8))
    )
    return (f'<g transform="translate({x} {base})">'
            f'<g transform="rotate(0)">{stems}'
            f'<animateTransform attributeName="transform" type="rotate" '
            f'values="-2;2;-2" dur="7s" repeatCount="indefinite"/></g></g>')


def ripples(cy):
    """Faint highlight lines drifting on the water."""
    lines = []
    for cx, rx, delay, dur in ((620, 110, 0, 9), (830, 70, 3, 11), (720, 150, 6, 13)):
        lines.append(
            f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="3.2" fill="#ffffff" opacity="0.1">'
            f'<animate attributeName="opacity" values="0.04;0.16;0.04" begin="-{delay}s" '
            f'dur="{dur}s" repeatCount="indefinite"/></ellipse>'
        )
    return "".join(lines)


def fireflies(cycle):
    """A few fireflies over the foreground, visible only around midnight. The outer
    group gates them to the night part of the cycle; each inner dot flickers on its own.
    """
    dots = "".join(
        f'<circle cx="{x}" cy="{y}" r="2.4" fill="#ffe9a8">'
        f'<animate attributeName="opacity" values="0.1;1;0.1" begin="-{d}s" dur="{f}s" '
        f'repeatCount="indefinite"/></circle>'
        for x, y, d, f in ((360, 740, 0, 3.2), (540, 780, 1.1, 4.1), (890, 756, 2.3, 3.7), (1120, 786, 0.7, 4.6))
    )
    return (f'<g opacity="0">{dots}'
            f'<animate attributeName="opacity" dur="{cycle}" repeatCount="indefinite" '
            'values="0;0;0;1;0" keyTimes="0;0.3;0.55;0.75;1"/></g>')


def birds(colour, cycle):
    """Three birds gliding right to left at different heights, phases and speeds."""
    wing = 'M0 0 Q5 -7 10 0 Q5 -4 0 0 Z M10 0 Q15 -7 20 0 Q15 -4 10 0 Z'
    flock = []
    for begin, dur, y, scale in (("0s", "47s", 150, 1.0), ("-18s", "59s", 210, 0.8), ("-33s", "53s", 110, 1.2)):
        # The motion stays on the unscaled group so every bird enters from off-canvas.
        flock.append(
            f'<g opacity="0.85"><g transform="scale({scale})"><path d="{wing}" fill="{colour}"/></g>'
            f'<animateMotion dur="{dur}" begin="{begin}" repeatCount="indefinite" '
            f'path="M1500 {y} L-80 {y - 24}"/></g>'
        )
    return "".join(flock)


def cloud(x_offset, y, scale, opacity, dur):
    """A soft cloud drifting left to right across the whole sky, wrapping around."""
    blob = ('M0 0 Q8 -22 34 -22 Q44 -40 70 -36 Q92 -46 104 -28 Q126 -26 126 -8 Q126 0 114 0 Z')
    return (f'<g opacity="{opacity}"><g transform="scale({scale})"><path d="{blob}" fill="#ffffff"/></g>'
            f'<animateMotion dur="{dur}" begin="-{x_offset}s" repeatCount="indefinite" '
            f'path="M-140 {y} L1580 {y}"/></g>')


def background(name, palette):
    sky_top, sky_bottom, sun, ridges, water = palette
    horizon = 560
    # One day-night cycle. Sun and moon travel the same left-to-right day arc; keyPoints let
    # each cross during its half of the cycle and park off-canvas for the other half, so both
    # always move in the same direction and the handover at the horizon is seamless. SMIL runs
    # in SVG loaded as a CSS background image and involves no JavaScript.
    cycle_seconds = 120
    cycle = f"{cycle_seconds}s"
    day_arc = "M-60 640 Q720 50 1500 640"
    crossing = (f'calcMode="linear" keyPoints="0;1;1" keyTimes="0;0.5;1" '
                f'dur="{cycle}" repeatCount="indefinite" path="{day_arc}"')
    out = [
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice" '
        'width="1440" height="900" role="img" aria-label="myTunes background">',
        '<defs>',
        f'<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="{sky_top}"/><stop offset="1" stop-color="{sky_bottom}"/></linearGradient>',
        f'<radialGradient id="glow" cx="0.5" cy="0.5" r="0.5">'
        f'<stop offset="0" stop-color="{sun}" stop-opacity="0.95"/>'
        f'<stop offset="1" stop-color="{sun}" stop-opacity="0"/></radialGradient>',
        '<radialGradient id="moonglow" cx="0.5" cy="0.5" r="0.5">'
        '<stop offset="0" stop-color="#e8edf8" stop-opacity="0.6"/>'
        '<stop offset="1" stop-color="#e8edf8" stop-opacity="0"/></radialGradient>',
        '</defs>',
        '<rect width="1440" height="900" fill="url(#sky)"/>',
        # The sun crosses during the first half of the cycle: it rises at the left canvas edge,
        # culminates around y=345 (clear of the first ridge top at 436), sets at the right edge
        # and waits off-canvas until the next dawn. The opacity fade keeps the parked disc's
        # glow from bleeding in from the edge overnight, and doubles as dusk/dawn afterglow.
        f'<g><circle r="250" fill="url(#glow)"/><circle r="38" fill="{sun}"/>'
        f'<animate attributeName="opacity" dur="{cycle}" repeatCount="indefinite" '
        f'values="1;1;0;0;1" keyTimes="0;0.5;0.56;0.995;1"/>'
        f'<animateMotion {crossing}/></g>',
        # The moon crosses during the second half, rising where the sun rose and travelling
        # the same way. A flat disc with craters reads as a moon at any palette, and the glow
        # keeps it visible through the midnight darkening; its fade hides the parked disc
        # during the day.
        f'<g><circle r="130" fill="url(#moonglow)"/><circle r="30" fill="#f2f4f8"/>'
        f'<circle cx="-9" cy="-6" r="6" fill="#ccd3e4"/><circle cx="8" cy="9" r="4" fill="#ccd3e4"/>'
        f'<circle cx="11" cy="-11" r="3" fill="#ccd3e4"/>'
        f'<animate attributeName="opacity" dur="{cycle}" repeatCount="indefinite" '
        f'values="0;0;1;1" keyTimes="0;0.495;0.5;1"/>'
        f'<animateMotion begin="-{cycle_seconds // 2}s" {crossing}/></g>',
        # Slow clouds and a small flock keep the sky alive between the big events.
        cloud(0, 150, 1.6, 0.30, "170s"),
        cloud(70, 265, 1.1, 0.20, "230s"),
        cloud(130, 330, 0.8, 0.15, "200s"),
        birds(ridges[3], cycle),
    ]
    # Far ridges sit above the waterline; near ridges frame the foreground.
    for index, (fraction, template) in enumerate(RIDGES):
        base = int(900 * fraction)
        out.append(f'<path d="{ridge_path(template, base)}" fill="{ridges[index]}"/>')
    out.append(f'<ellipse cx="720" cy="{horizon + 190}" rx="420" ry="86" fill="{water}" opacity="0.85"/>')
    out.append(ripples(horizon + 196))
    out.append(f'<path d="M0 720 L240 664 L520 726 L820 672 L1120 730 L1440 676 L1440 900 L0 900 Z" fill="{ridges[3]}"/>')
    out.append(f'<path d="M0 806 L300 764 L640 812 L980 762 L1440 806 L1440 900 L0 900 Z" fill="{ridges[4]}"/>')
    for x, height in ((92, 210), (168, 150), (250, 118), (1272, 226), (1352, 162), (1198, 132), (1080, 108)):
        out.append(tree(x, 826, height, ridges[4]))
    for x, base, size in ((320, 824, 30), (610, 816, 26), (952, 810, 32), (1140, 830, 24)):
        out.append(bush(x, base, size, ridges[4]))
    out.append(reeds(348, 776, 44, ridges[4]))
    out.append(reeds(1076, 782, 38, ridges[4]))
    # A rabbit pair watches from the mid-left knoll, in the same silhouette colour as the
    # trees; the smaller one faces the larger.
    out.append(rabbit(420, 792, 64, ridges[4]))
    out.append(rabbit(510, 798, 42, ridges[4], flip=True))
    out.append(fireflies(cycle))
    # Night: the scene darkens towards midnight (cycle three-quarter point) and clears by noon.
    out.append(
        '<rect width="1440" height="900" fill="#040812">'
        f'<animate attributeName="opacity" dur="{cycle}" repeatCount="indefinite" '
        'values="0.32;0;0.32;0.55;0.32" keyTimes="0;0.25;0.5;0.75;1"/></rect>'
    )
    # Stars sit above the darkness so the night sky stays alive.
    stars = "".join(
        f'<circle cx="{x}" cy="{y}" r="{r}" fill="#ffffff"/>'
        for x, y, r in ((120, 90, 2), (300, 170, 1.5), (470, 60, 2), (640, 210, 1.5), (810, 110, 2),
                        (960, 50, 1.5), (1090, 180, 2), (1240, 80, 1.5), (1380, 150, 2), (210, 300, 1.5),
                        (560, 330, 1.5), (900, 290, 1.5), (1170, 320, 1.5), (60, 220, 1.5),
                        (390, 130, 1), (720, 70, 1), (1030, 240, 1), (150, 380, 1), (1310, 260, 1),
                        (860, 380, 1))
    )
    out.append(
        f'<g opacity="0">{stars}'
        f'<animate attributeName="opacity" dur="{cycle}" repeatCount="indefinite" '
        'values="0.4;0;0;0.9;0.4" keyTimes="0;0.2;0.5;0.75;1"/></g>'
    )
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
