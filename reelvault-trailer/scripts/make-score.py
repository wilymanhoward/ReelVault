"""Original 120 BPM electronic cue, synthesized locally for the ReelVault trailer."""
import math
import wave
import numpy as np
from pathlib import Path

rate = 44100
duration = 24
mix = np.zeros((rate * duration, 2), dtype=np.float64)
rng = np.random.default_rng(20261024)

def add(signal, start, gain=1, pan=0):
    pos = int(start * rate)
    count = min(len(signal), len(mix) - pos)
    if count > 0:
        mix[pos:pos+count, 0] += signal[:count] * gain * math.sqrt((1-pan)/2)
        mix[pos:pos+count, 1] += signal[:count] * gain * math.sqrt((1+pan)/2)

def note(midi, length, bright=False):
    t = np.arange(int(length * rate)) / rate
    freq = 440 * 2 ** ((midi-69)/12)
    tone = np.sin(2*np.pi*freq*t) + .26*np.sin(2*np.pi*freq*2*t) + .09*np.sin(2*np.pi*freq*3*t)
    return tone * np.minimum(t/.008, 1) * np.exp(-t*(5 if bright else 2.4))

for beat in range(48):
    start = beat * .5
    t = np.arange(int(.32*rate))/rate
    kick = np.sin(2*np.pi*(48*t + 5*(1-np.exp(-t*35)))) * np.exp(-t*16)
    if start < 22:
        add(kick, start, .55)
    if beat % 2 and 3 <= start < 22:
        noise = rng.normal(0, 1, int(.14*rate))
        clap = np.diff(noise, prepend=0) * np.exp(-np.arange(len(noise))/rate*36)
        add(clap, start, .11)
    for offset in [0, .25]:
        if 3 <= start < 22:
            t = np.arange(int(.07*rate))/rate
            hat = np.diff(rng.normal(0, 1, len(t)), prepend=0)*np.exp(-t*85)
            add(hat, start+offset, .045, .25 if offset else -.25)

chords = [[57,60,64,71], [53,57,60,64], [60,64,67,71], [55,59,62,69]]
for step in range(88):
    chord = chords[(step//16)%4]
    midi = chord[[0,2,1,3,2,1,3,2][step%8]] + 12
    add(note(midi, .65, True), step*.25, .13, .32*math.sin(step))
for beat in range(44):
    chord = chords[(beat//8)%4]
    add(note(chord[0]-24, .47), beat*.5, .24)
for start in [3,8,12,17,20]:
    t = np.arange(int(.32*rate))/rate
    noise = rng.normal(0,1,len(t))
    smooth = np.convolve(noise, np.ones(24)/24, mode='same')
    add(smooth * np.sin(np.pi*t/.32)**2, start-.16, .32)
for midi in [57,60,64,71,76]:
    add(note(midi, 3.8), 20, .18)
fade = np.minimum(np.arange(len(mix))/rate/.04, 1) * np.clip((duration-np.arange(len(mix))/rate)/1.2, 0, 1)
mix *= fade[:, None]
mix = np.tanh(mix * 1.2)
mix = mix / max(1, np.max(np.abs(mix))/.82)
path = Path('public/audio/reelvault-original.wav')
path.parent.mkdir(parents=True, exist_ok=True)
with wave.open(str(path), 'wb') as out:
    out.setnchannels(2)
    out.setsampwidth(2)
    out.setframerate(rate)
    out.writeframes((mix*32767).astype('<i2').tobytes())
print(f'Created {duration}s original stereo cue: {path}')
