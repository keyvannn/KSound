package com.ksound.atmos.audio

enum class AudioProfile(
  val title: String,
  val subtitle: String,
  val eqBandsDb: FloatArray, // 10 bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz
  val bassBoost: Int,        // 0..100
  val virtualizer: Int,      // 0..100
  val loudness: Int,         // 0..100
  val subBassPunch: Int,     // 0..100
) {
  DOLBY_CINEMA(
    title = "Dolby Cinema",
    subtitle = "3D spatial surround, deep bass & crystal-clear dialogue",
    eqBandsDb = floatArrayOf(5.5f, 6.0f, 3.0f, -1.5f, 0.0f, 1.5f, 3.0f, 3.5f, 3.0f, 2.5f),
    bassBoost = 72,
    virtualizer = 80,
    loudness = 35,
    subBassPunch = 65,
  ),
  MEGA_BASS(
    title = "Mega Bass",
    subtitle = "Heavy punchy bass with anti-distortion limiter",
    eqBandsDb = floatArrayOf(9.5f, 10.0f, 7.0f, 2.0f, -1.0f, 0.0f, 1.5f, 2.5f, 3.0f, 2.5f),
    bassBoost = 100,
    virtualizer = 55,
    loudness = 40,
    subBassPunch = 95,
  ),
  CLUB_SUB(
    title = "Club Sub",
    subtitle = "Deep sub-bass rumble (31–62Hz) with crisp highs",
    eqBandsDb = floatArrayOf(11.0f, 9.0f, 4.5f, -1.5f, -1.0f, 0.5f, 2.0f, 3.0f, 3.5f, 3.0f),
    bassBoost = 95,
    virtualizer = 65,
    loudness = 30,
    subBassPunch = 100,
  ),
  ATMOS_MUSIC(
    title = "Atmos Music",
    subtitle = "Wide stereo soundstage, balanced bass & sparkling treble",
    eqBandsDb = floatArrayOf(4.5f, 5.0f, 2.5f, 0.0f, -0.5f, 0.5f, 2.0f, 3.5f, 4.5f, 4.0f),
    bassBoost = 60,
    virtualizer = 70,
    loudness = 25,
    subBassPunch = 50,
  ),
  CLEAR_VOICE(
    title = "Clear Dialogue",
    subtitle = "Vocal clarity boost for movies, podcasts & YouTube",
    eqBandsDb = floatArrayOf(-3.0f, -2.0f, -1.0f, 0.5f, 2.5f, 4.5f, 5.0f, 3.5f, 1.5f, 0.0f),
    bassBoost = 15,
    virtualizer = 25,
    loudness = 45,
    subBassPunch = 0,
  ),
  GAMING_3D(
    title = "Gaming 3D",
    subtitle = "Sharp footsteps & directional cues with tight bass",
    eqBandsDb = floatArrayOf(4.0f, 4.5f, 2.0f, -1.0f, 0.5f, 2.0f, 4.5f, 5.5f, 4.0f, 3.0f),
    bassBoost = 55,
    virtualizer = 90,
    loudness = 30,
    subBassPunch = 45,
  ),
  CUSTOM(
    title = "Custom",
    subtitle = "Manual 10-band EQ & custom bass tuning",
    eqBandsDb = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    bassBoost = 50,
    virtualizer = 50,
    loudness = 20,
    subBassPunch = 40,
  );

  fun nextPreset(): AudioProfile {
    val cycle = listOf(DOLBY_CINEMA, MEGA_BASS, CLUB_SUB, ATMOS_MUSIC, CLEAR_VOICE, GAMING_3D)
    val idx = cycle.indexOf(this)
    return if (idx == -1) DOLBY_CINEMA else cycle[(idx + 1) % cycle.size]
  }
}

val EQ_FREQUENCIES_HZ = floatArrayOf(
  31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
)

val EQ_LABELS = listOf(
  "31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"
)

data class SoundState(
  val enabled: Boolean = false,
  val profile: AudioProfile = AudioProfile.DOLBY_CINEMA,
  val bassBoost: Int = AudioProfile.DOLBY_CINEMA.bassBoost,
  val virtualizer: Int = AudioProfile.DOLBY_CINEMA.virtualizer,
  val loudness: Int = AudioProfile.DOLBY_CINEMA.loudness,
  val subBassPunch: Int = AudioProfile.DOLBY_CINEMA.subBassPunch,
  val eqBandsDb: List<Float> = AudioProfile.DOLBY_CINEMA.eqBandsDb.toList(),
  val activeEffectsCount: Int = 0,
)
