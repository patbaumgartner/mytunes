#!/usr/bin/env python3
"""Generates myTunes' own audio loops, one per station category.

Third-party radio streams may be blocked, rate limited or withdrawn at any time, and several are
served by hosts that refuse browser requests outright. These loops are synthesised here, so the
repository owns them outright and every category in the station menu can always demonstrate real
playback offline, with no third-party dependency and no licensing question.

Each channel is a small deterministic arrangement over a chord progression: a detuned pad, an
optional sub bass, an optional arpeggio, and an optional kick/hat pattern. The noise source is
seeded per channel, so regenerating the files is reproducible.

Every oscillator frequency is snapped to a whole number of cycles per loop and every tempo to a
whole number of beats per loop, so the waveform is exactly periodic and the loop restart is
inaudible — no fade to silence is needed at the edges.
"""
import math
import pathlib
import random
import struct
import subprocess
import tempfile
import wave

WEB = pathlib.Path(__file__).resolve().parents[1] / "src" / "main" / "web" / "audio"
RATE = 44100
SECONDS = 24

A_MINOR = [(220.00, 261.63, 329.63), (174.61, 220.00, 261.63),
           (261.63, 329.63, 392.00), (196.00, 246.94, 293.66)]
D_MINOR = [(146.83, 174.61, 220.00), (116.54, 146.83, 174.61),
           (174.61, 220.00, 261.63), (130.81, 164.81, 196.00)]
C_MAJOR = [(261.63, 329.63, 392.00), (220.00, 261.63, 329.63),
           (174.61, 220.00, 261.63), (196.00, 246.94, 293.66)]
E_MINOR = [(164.81, 196.00, 246.94), (130.81, 164.81, 196.00),
           (196.00, 246.94, 293.66), (146.83, 185.00, 220.00)]

# name -> (chords, bpm, pad, bass, arp, beat) — bpm 0 means beatless.
CHANNELS = {
    "mytunes-signal":     (A_MINOR,   0, 1.00, 0.28, 0.00, 0.00),
    "mytunes-lofi":       (C_MAJOR,  72, 0.80, 0.30, 0.00, 0.35),
    "mytunes-beats":      (D_MINOR,  92, 0.55, 0.35, 0.00, 0.80),
    "mytunes-subsignal":  (E_MINOR,  70, 0.45, 0.85, 0.00, 0.55),
    "mytunes-pulse":      (A_MINOR, 124, 0.45, 0.40, 0.75, 0.70),
    "mytunes-drift":      (C_MAJOR, 136, 0.70, 0.35, 0.65, 0.45),
    "mytunes-nightdrive": (E_MINOR, 108, 0.60, 0.55, 0.55, 0.60),
    "mytunes-terminal":   (D_MINOR, 100, 0.35, 0.45, 0.85, 0.65),
}


def cycles(freq):
    """Snap a frequency to a whole number of cycles per loop so its phase wraps seamlessly."""
    return max(1, round(freq * SECONDS)) / SECONDS


def render(name, chords, bpm, pad, bass, arp, beat):
    rng = random.Random(name)
    frames = RATE * SECONDS
    per_chord = frames // len(chords)
    if bpm:
        # Snap the tempo so a whole number of beats (a multiple of the chord count, hence even)
        # fits in the loop and the kick/hat/arp grid lines up at the wrap point.
        beats = max(len(chords), len(chords) * round(SECONDS * bpm / 60 / len(chords)))
        bpm = beats * 60 / SECONDS
    samples = []
    for index in range(frames):
        chord = chords[(index // per_chord) % len(chords)]
        position = (index % per_chord) / per_chord
        t = index / RATE
        # Raised-cosine envelope keeps chord changes from clicking and makes the loop seamless.
        envelope = 0.5 - 0.5 * math.cos(2 * math.pi * position)
        value = 0.0
        for voice, freq in enumerate(chord):
            detune = 1.0 + (voice - 1) * 0.0016
            value += pad * math.sin(2 * math.pi * cycles(freq * detune) * t) / (voice + 2.2)
        value += bass * 0.6 * math.sin(2 * math.pi * cycles(chord[0] * 0.5) * t) * envelope
        if arp:
            # Sixteenth-note arpeggio over the chord tones, each note with a plucked decay.
            step = int(t * bpm / 60 * 4)
            note = cycles(chord[step % len(chord)] * 2)
            note_pos = (t * bpm / 60 * 4) % 1.0
            value += arp * 0.5 * math.sin(2 * math.pi * note * t) * math.exp(-5.0 * note_pos)
        if beat and bpm:
            beat_float = t * bpm / 60
            beat_no = int(beat_float)
            kick_pos = (beat_float - beat_no) * 60 / bpm
            value += beat * 0.8 * math.sin(2 * math.pi * 52 * kick_pos) * math.exp(-22.0 * kick_pos)
            if beat_no % 2 == 1:
                value += beat * 0.12 * (rng.random() * 2 - 1) * math.exp(-60.0 * kick_pos)
        samples.append(int(max(-1.0, min(1.0, value * 0.42 * (0.4 + 0.6 * envelope))) * 32767))
    return samples


def encode(name, samples):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as handle:
        raw = handle.name
    with wave.open(raw, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(b"".join(struct.pack("<h", s) for s in samples))
    target = WEB / f"{name}.mp3"
    # libmp3lame writes the LAME/Xing gapless header, so browsers trim the codec's own
    # delay/padding when the <audio loop> element wraps around.
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", raw, "-codec:a", "libmp3lame",
         "-b:a", "112k", "-ar", "44100", str(target)],
        check=True,
    )
    pathlib.Path(raw).unlink()
    print(f"wrote {target} ({target.stat().st_size} bytes)")


def main():
    WEB.mkdir(parents=True, exist_ok=True)
    for name, (chords, bpm, pad, bass, arp, beat) in CHANNELS.items():
        encode(name, render(name, chords, bpm, pad, bass, arp, beat))


if __name__ == "__main__":
    main()
