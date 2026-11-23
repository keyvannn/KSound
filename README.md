# KSound Atmos (`v1.0.4`)

System-Wide Spatial Audio, Sub-Bass Booster & 10-Band Equalizer for Android.

> **Target Device:** Tuned and tested on **Xiaomi Pad 5 (`nabu`)** running **crDroid 16 (Android 16)**. Works without root on Android 9+ (API 28+).

---

## Features

- **System-Wide DSP Engine** (global output mix `session 0`):
  - **10-Band Pre-EQ** (`31Hz – 16kHz`, `-12dB` to `+12dB`) via `DynamicsProcessing`
  - **3-Band Multi-Band Compressor (Sub-Bass Punch)** with makeup gain
  - **Anti-Clipping Limiter (`-0.5dB`)** to prevent speaker distortion at high bass
  - **Hardware Bass Boost + 3D Virtualizer + Loudness Leveler**
- **7 Audio Profiles:**
  - `Dolby Cinema` — 3D spatial surround, deep bass & clear dialogue
  - `Mega Bass` — Heavy punchy bass with anti-distortion limiter
  - `Club Sub` — Deep sub-bass (`31–62Hz`) with crisp highs
  - `Atmos Music` — Wide stereo soundstage & balanced bass
  - `Clear Dialogue` — Vocal clarity boost for movies, podcasts & YouTube
  - `Gaming 3D` — Directional cues with tight bass
  - `Custom` — Persistent custom 10-band EQ & slider settings
- **Quick Settings Tile & Notification Controls:**
  - Tap the **KSound Atmos** Quick Settings tile to pick any profile or turn it off directly
  - Notification controls for **Turn On/Off**, **Next Profile**, and **Close**

---

## Download

- Release APK: [`releases/KSound-v1.0.4-release.apk`](releases/KSound-v1.0.4-release.apk)

---

## Build from Source

```bash
./gradlew assembleRelease
```

---

## Tested Environment

- **Device:** Xiaomi Pad 5 (`nabu`)
- **ROM:** crDroid 16 (Android 16)
- **Root Required:** No
