package com.ksound.atmos.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * System-wide audio DSP engine attached to Android's global output mix (session 0).
 * Combines 10-band DynamicsProcessing EQ + 3-band MBC (Sub-Bass Punch) + Anti-Clipping Limiter
 * + Hardware BassBoost + 3D Virtualizer + LoudnessEnhancer.
 */
object GlobalAudioEngine {
  private var prefs: SoundPreferences? = null
  private val _state = MutableStateFlow(SoundState())
  val state: StateFlow<SoundState> = _state.asStateFlow()

  private var dynamics: DynamicsProcessing? = null
  private var bassBoost: BassBoost? = null
  private var virtualizer: Virtualizer? = null
  private var loudness: LoudnessEnhancer? = null

  @Synchronized
  fun init(context: Context) {
    if (prefs == null) {
      prefs = SoundPreferences(context.applicationContext)
      _state.value = prefs!!.load()
    }
  }

  @Synchronized
  fun updateState(transform: (SoundState) -> SoundState): SoundState {
    val updated = transform(_state.value)
    _state.value = updated
    prefs?.save(updated)
    applyToHardware(updated)
    return updated
  }

  @Synchronized
  fun selectProfile(profile: AudioProfile): SoundState {
    return updateState { current ->
      if (profile == AudioProfile.CUSTOM) {
        val savedCustom = prefs?.loadCustom()
        if (savedCustom != null) {
          current.copy(
            profile = AudioProfile.CUSTOM,
            bassBoost = savedCustom.bassBoost,
            virtualizer = savedCustom.virtualizer,
            loudness = savedCustom.loudness,
            subBassPunch = savedCustom.subBassPunch,
            eqBandsDb = savedCustom.eqBandsDb,
          )
        } else {
          current.copy(profile = AudioProfile.CUSTOM)
        }
      } else {
        current.copy(
          profile = profile,
          bassBoost = profile.bassBoost,
          virtualizer = profile.virtualizer,
          loudness = profile.loudness,
          subBassPunch = profile.subBassPunch,
          eqBandsDb = profile.eqBandsDb.toList(),
        )
      }
    }
  }

  @Synchronized
  fun applyCurrent() {
    applyToHardware(_state.value)
  }

  @Synchronized
  fun release() {
    runCatching { dynamics?.enabled = false }
    runCatching { dynamics?.release() }
    dynamics = null

    runCatching {
      bassBoost?.setStrength(0)
      bassBoost?.enabled = false
      bassBoost?.release()
    }
    bassBoost = null

    runCatching {
      @Suppress("DEPRECATION")
      virtualizer?.setStrength(0)
      virtualizer?.enabled = false
      virtualizer?.release()
    }
    virtualizer = null

    runCatching {
      loudness?.setTargetGain(0)
      loudness?.enabled = false
      loudness?.release()
    }
    loudness = null
  }

  private fun ensureEffectsCreated() {
    if (dynamics == null) {
      runCatching {
        val config = DynamicsProcessing.Config.Builder(
          DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
          2,    // stereo channels
          true, // preEqInUse
          10,   // preEqBandCount
          true, // mbcInUse (3-band multiband compressor for tight sub-bass punch)
          3,    // mbcBandCount
          false,
          0,
          true, // limiterInUse (prevents speaker clipping even at max bass)
        ).build()
        dynamics = DynamicsProcessing(0, 0, config)
      }
    }
    if (bassBoost == null) {
      runCatching { bassBoost = BassBoost(0, 0) }
    }
    if (virtualizer == null) {
      runCatching {
        @Suppress("DEPRECATION")
        virtualizer = Virtualizer(0, 0)
      }
    }
    if (loudness == null) {
      runCatching { loudness = LoudnessEnhancer(0) }
    }
  }

  private fun applyToHardware(s: SoundState) {
    if (!s.enabled) {
      // Immediately zero, disable, AND release all session-0 effects so AudioFlinger
      // bypasses them on the very next audio buffer (~20ms) instead of lingering.
      release()
      if (_state.value.activeEffectsCount != 0) {
        _state.value = _state.value.copy(activeEffectsCount = 0)
      }
      return
    }

    ensureEffectsCreated()
    var active = 0

    // 1. 10-band Pre-EQ + Sub-Bass MBC + Anti-Clipping Limiter
    // Tuned with proper makeup gain so Samsung One UI / SoundAlive never ducks overall volume.
    dynamics?.let { dp ->
      runCatching {
        dp.setInputGainAllChannelsTo(2.0f)

        val preEq = DynamicsProcessing.Eq(true, true, 10)
        for (i in 0 until 10) {
          val gain = s.eqBandsDb.getOrElse(i) { 0f }.coerceIn(-12f, 12f)
          preEq.setBand(i, DynamicsProcessing.EqBand(true, EQ_FREQUENCIES_HZ[i], gain))
        }
        dp.setPreEqAllChannelsTo(preEq)

        val punch = s.subBassPunch / 100f
        val mbc = DynamicsProcessing.Mbc(true, true, 3)
        mbc.setBand(
          0,
          DynamicsProcessing.MbcBand(
            true,
            150f,
            10f,
            85f,
            1.0f + 0.55f * punch, // gentle ratio so Samsung HAL doesn't pump/duck volume
            -12f,                 // higher threshold prevents compressing normal listening levels
            6f,
            -80f,
            1.0f,
            punch * 2.5f,
            1.5f + punch * 3.0f,  // makeup postGain for punchy sub-bass
          )
        )
        mbc.setBand(
          1,
          DynamicsProcessing.MbcBand(
            true, 3200f, 12f, 100f, 1.15f, -10f, 6f, -80f, 1.0f, 0f, 1.8f
          )
        )
        mbc.setBand(
          2,
          DynamicsProcessing.MbcBand(
            true, 20000f, 8f, 80f, 1.15f, -10f, 6f, -80f, 1.0f, 0f, 1.8f
          )
        )
        dp.setMbcAllChannelsTo(mbc)

        val limiter = DynamicsProcessing.Limiter(
          true, true, 0, 1.5f, 40.0f, 6.0f, -0.5f, 1.5f
        )
        dp.setLimiterAllChannelsTo(limiter)
        dp.enabled = true
        active++
      }
    }

    // 2. Native Hardware BassBoost (0..1000)
    bassBoost?.let { bb ->
      runCatching {
        val strength = (s.bassBoost.coerceIn(0, 100) * 10).toShort()
        bb.setStrength(strength)
        bb.enabled = strength > 0
        if (strength > 0) active++
      }
    }

    // 3. Native 3D Virtualizer (0..1000)
    virtualizer?.let { vz ->
      runCatching {
        val strength = (s.virtualizer.coerceIn(0, 100) * 10).toShort()
        @Suppress("DEPRECATION")
        vz.setStrength(strength)
        vz.enabled = strength > 0
        if (strength > 0) active++
      }
    }

    // 4. Smart Loudness Enhancer (up to 600 mB for clean headroom without clipping)
    loudness?.let { le ->
      runCatching {
        val gainMb = (s.loudness.coerceIn(0, 100) * 6.0f).toInt()
        le.setTargetGain(gainMb)
        le.enabled = gainMb > 0
        if (gainMb > 0) active++
      }
    }

    if (_state.value.activeEffectsCount != active) {
      _state.value = _state.value.copy(activeEffectsCount = active)
    }
  }
}
