#!/usr/bin/env python3
"""Generates myTunes' own ambient audio loop.

Third-party radio streams may be blocked, rate limited or withdrawn at any time, and several are
served by hosts that refuse browser requests outright. This loop is synthesised here, so the
repository owns it outright and a reviewer can always demonstrate real playback offline, with no
third-party dependency and no licensing question.
"""
import math
import pathlib
import struct
import subprocess
import tempfile
import wave

WEB = pathlib.Path(__file__).resolve().parents[1] / "src" / "main" / "web" / "audio"
RATE = 44100
SECONDS = 32

# A slow i - VI - III - VII progression in A minor, as frequency triads.
CHORDS = [
    (220.00, 261.63, 329.63),
    (174.61, 220.00, 261.63),
    (261.63, 329.63, 392.00),
    (196.00, 246.94, 293.66),
]


def render():
    frames = RATE * SECONDS
    per_chord = frames // len(CHORDS)
    samples = []
    for index in range(frames):
        chord = CHORDS[(index // per_chord) % len(CHORDS)]
        position = (index % per_chord) / per_chord
        # Raised-cosine envelope keeps chord changes from clicking and makes the loop seamless.
        envelope = 0.5 - 0.5 * math.cos(2 * math.pi * position)
        value = 0.0
        for voice, freq in enumerate(chord):
            detune = 1.0 + (voice - 1) * 0.0016
            value += math.sin(2 * math.pi * freq * detune * index / RATE) / (voice + 2.2)
            value += 0.28 * math.sin(2 * math.pi * freq * 0.5 * index / RATE) / (voice + 3.0)
        # Global fade at the very start and end so the file loops without a seam.
        edge = min(1.0, index / (RATE * 2.0), (frames - index) / (RATE * 2.0))
        samples.append(int(max(-1.0, min(1.0, value * envelope * 0.42 * edge)) * 32767))
    return samples


def main():
    WEB.mkdir(parents=True, exist_ok=True)
    samples = render()
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as handle:
        raw = handle.name
    with wave.open(raw, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(b"".join(struct.pack("<h", s) for s in samples))

    target = WEB / "mytunes-signal.mp3"
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", raw, "-codec:a", "libmp3lame",
         "-b:a", "128k", "-ar", "44100", str(target)],
        check=True,
    )
    pathlib.Path(raw).unlink()
    print(f"wrote {target} ({target.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
