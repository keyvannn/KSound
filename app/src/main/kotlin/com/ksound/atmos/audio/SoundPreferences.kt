package com.ksound.atmos.audio

import android.content.Context

data class CustomPresetData(
  val bassBoost: Int,
  val virtualizer: Int,
  val loudness: Int,
  val subBassPunch: Int,
  val eqBandsDb: List<Float>,
)

class SoundPreferences(context: Context) {
  private val prefs = context.getSharedPreferences("ksound_prefs", Context.MODE_PRIVATE)

  fun load(): SoundState {
    val enabled = prefs.getBoolean("enabled", false)
    val profileName = prefs.getString("profile", AudioProfile.DOLBY_CINEMA.name)
    val profile = runCatching { AudioProfile.valueOf(profileName ?: "") }
      .getOrDefault(AudioProfile.DOLBY_CINEMA)

    if (profile == AudioProfile.CUSTOM) {
      val custom = loadCustom()
      return SoundState(
        enabled = enabled,
        profile = AudioProfile.CUSTOM,
        bassBoost = custom.bassBoost,
        virtualizer = custom.virtualizer,
        loudness = custom.loudness,
        subBassPunch = custom.subBassPunch,
        eqBandsDb = custom.eqBandsDb,
      )
    }

    return SoundState(
      enabled = enabled,
      profile = profile,
      bassBoost = prefs.getInt("bass", profile.bassBoost),
      virtualizer = prefs.getInt("virtualizer", profile.virtualizer),
      loudness = prefs.getInt("loudness", profile.loudness),
      subBassPunch = prefs.getInt("sub_punch", profile.subBassPunch),
      eqBandsDb = prefs.getString("eq_bands", null)
        ?.split(",")
        ?.mapNotNull { it.toFloatOrNull() }
        ?.takeIf { it.size == 10 }
        ?: profile.eqBandsDb.toList(),
    )
  }

  fun loadCustom(): CustomPresetData {
    val def = AudioProfile.CUSTOM
    val eqList = prefs.getString("custom_eq_bands", null)
      ?.split(",")
      ?.mapNotNull { it.toFloatOrNull() }
      ?.takeIf { it.size == 10 }
      ?: def.eqBandsDb.toList()
    return CustomPresetData(
      bassBoost = prefs.getInt("custom_bass", def.bassBoost),
      virtualizer = prefs.getInt("custom_virtualizer", def.virtualizer),
      loudness = prefs.getInt("custom_loudness", def.loudness),
      subBassPunch = prefs.getInt("custom_sub_punch", def.subBassPunch),
      eqBandsDb = eqList,
    )
  }

  fun save(state: SoundState) {
    val editor = prefs.edit()
      .putBoolean("enabled", state.enabled)
      .putString("profile", state.profile.name)
      .putInt("bass", state.bassBoost)
      .putInt("virtualizer", state.virtualizer)
      .putInt("loudness", state.loudness)
      .putInt("sub_punch", state.subBassPunch)
      .putString("eq_bands", state.eqBandsDb.joinToString(","))

    if (state.profile == AudioProfile.CUSTOM) {
      editor
        .putInt("custom_bass", state.bassBoost)
        .putInt("custom_virtualizer", state.virtualizer)
        .putInt("custom_loudness", state.loudness)
        .putInt("custom_sub_punch", state.subBassPunch)
        .putString("custom_eq_bands", state.eqBandsDb.joinToString(","))
    }
    editor.apply()
  }
}
