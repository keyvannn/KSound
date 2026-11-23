package com.ksound.atmos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ksound.atmos.audio.AudioProfile
import com.ksound.atmos.audio.EQ_LABELS
import com.ksound.atmos.audio.GlobalAudioEngine
import com.ksound.atmos.service.AudioEnhancerService
import kotlin.math.roundToInt

private val AppleBlue = Color(0xFF0A84FF)
private val AppleCyan = Color(0xFF64D2FF)
private val AppleIndigo = Color(0xFF5E5CE6)
private val DeepBg = Color(0xFF070B18)
private val GlassCard = Color(0xFF11182E)
private val GlassCardActive = Color(0xFF172448)
private val GlassBorder = Color(0x28FFFFFF)

private val KSoundColorScheme = darkColorScheme(
  primary = AppleBlue,
  secondary = AppleIndigo,
  tertiary = AppleCyan,
  background = DeepBg,
  surface = GlassCard,
  onBackground = Color.White,
  onSurface = Color.White,
  onSurfaceVariant = Color(0xFFB4C0D9),
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    GlobalAudioEngine.init(this)

    setContent {
      MaterialTheme(colorScheme = KSoundColorScheme) {
        KSoundScreen()
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KSoundScreen() {
  val context = LocalContext.current
  val state by GlobalAudioEngine.state.collectAsState()

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (state.enabled) {
      AudioEnhancerService.syncService(context)
    }
  }

  val bgBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF0B132B), DeepBg, Color(0xFF050712))
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgBrush)
      .systemBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 860.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header + Master Toggle Card
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (state.enabled) GlassCardActive else GlassCard,
        border = BorderStroke(
          1.dp,
          if (state.enabled) AppleBlue.copy(alpha = 0.65f) else GlassBorder
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    if (state.enabled) listOf(AppleCyan, AppleBlue, AppleIndigo)
                    else listOf(Color(0xFF222A42), Color(0xFF181F33))
                  )
                )
                .clickable {
                  GlobalAudioEngine.updateState { it.copy(enabled = !it.enabled) }
                  AudioEnhancerService.syncService(context)
                },
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "KSound Atmos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(Modifier.height(2.dp))
              Text(
                text = if (state.enabled) {
                  "Active • ${state.profile.title} (${state.activeEffectsCount} DSP)"
                } else {
                  "Off • Tap switch to enable"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.enabled) AppleCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Switch(
            checked = state.enabled,
            onCheckedChange = { on ->
              GlobalAudioEngine.updateState { it.copy(enabled = on) }
              AudioEnhancerService.syncService(context)
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = AppleBlue,
              uncheckedThumbColor = Color(0xFFB4C0D9),
              uncheckedTrackColor = Color(0xFF1F2842),
            ),
          )
        }
      }

      // Audio Profiles Card
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = GlassCard,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Filled.SurroundSound, null, tint = AppleCyan, modifier = Modifier.size(22.dp))
            Text(
              text = "Audio Profiles",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            AudioProfile.entries.forEach { profile ->
              val selected = state.profile == profile
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) AppleBlue.copy(alpha = 0.24f) else Color(0xFF18223D),
                border = BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) AppleBlue else Color(0x1FFFFFFF),
                ),
                modifier = Modifier.clickable {
                  GlobalAudioEngine.selectProfile(profile)
                  if (!state.enabled) {
                    GlobalAudioEngine.updateState { it.copy(enabled = true) }
                  }
                  AudioEnhancerService.syncService(context)
                },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = profileIcon(profile),
                    contentDescription = null,
                    tint = if (selected) AppleCyan else Color(0xFFAAB7D4),
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = profile.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else Color(0xFFD6DFEF),
                  )
                }
              }
            }
          }

          Text(
            text = state.profile.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AppleCyan,
          )
        }
      }

      // Bass, Sub-Bass Punch, 3D Virtualizer & Loudness Sliders Card
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = GlassCard,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Filled.Speaker, null, tint = AppleCyan, modifier = Modifier.size(22.dp))
            Text(
              text = "Bass & 3D Spatial Engine",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          SoundSliderRow(
            title = "Hardware Bass Boost",
            value = state.bassBoost,
            icon = Icons.Filled.Speaker,
            accent = Color(0xFFBF5AF2),
            onValueChange = { v ->
              GlobalAudioEngine.updateState {
                it.copy(profile = AudioProfile.CUSTOM, bassBoost = v)
              }
              if (state.enabled) AudioEnhancerService.syncService(context)
            },
          )

          SoundSliderRow(
            title = "Sub-Bass Punch (MBC)",
            value = state.subBassPunch,
            icon = Icons.Filled.GraphicEq,
            accent = Color(0xFFFF375F),
            onValueChange = { v ->
              GlobalAudioEngine.updateState {
                it.copy(profile = AudioProfile.CUSTOM, subBassPunch = v)
              }
              if (state.enabled) AudioEnhancerService.syncService(context)
            },
          )

          SoundSliderRow(
            title = "3D Atmos Virtualizer",
            value = state.virtualizer,
            icon = Icons.Filled.Headphones,
            accent = AppleCyan,
            onValueChange = { v ->
              GlobalAudioEngine.updateState {
                it.copy(profile = AudioProfile.CUSTOM, virtualizer = v)
              }
              if (state.enabled) AudioEnhancerService.syncService(context)
            },
          )

          SoundSliderRow(
            title = "Smart Loudness Leveler",
            value = state.loudness,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            accent = AppleBlue,
            onValueChange = { v ->
              GlobalAudioEngine.updateState {
                it.copy(profile = AudioProfile.CUSTOM, loudness = v)
              }
              if (state.enabled) AudioEnhancerService.syncService(context)
            },
          )
        }
      }

      // 10-Band Equalizer Card
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = GlassCard,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Filled.Equalizer, null, tint = AppleCyan, modifier = Modifier.size(22.dp))
              Text(
                text = "10-Band Equalizer (dB)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
            TextButton(
              onClick = {
                GlobalAudioEngine.updateState {
                  it.copy(
                    profile = AudioProfile.CUSTOM,
                    eqBandsDb = List(10) { 0f },
                  )
                }
                if (state.enabled) AudioEnhancerService.syncService(context)
              }
            ) {
              Icon(Icons.Filled.RestartAlt, null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Reset EQ")
            }
          }

          state.eqBandsDb.forEachIndexed { idx, gainDb ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "${EQ_LABELS[idx]}Hz",
                style = MaterialTheme.typography.labelMedium,
                color = if (idx <= 2) Color(0xFFBF5AF2) else AppleCyan,
                modifier = Modifier.width(48.dp),
              )
              Slider(
                value = gainDb,
                onValueChange = { newVal ->
                  val updated = state.eqBandsDb.toMutableList()
                  updated[idx] = (newVal * 2f).roundToInt() / 2f
                  GlobalAudioEngine.updateState {
                    it.copy(profile = AudioProfile.CUSTOM, eqBandsDb = updated)
                  }
                },
                onValueChangeFinished = {
                  if (state.enabled) AudioEnhancerService.syncService(context)
                },
                valueRange = -12f..12f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                  thumbColor = Color.White,
                  activeTrackColor = if (idx <= 2) Color(0xFFBF5AF2) else AppleBlue,
                  inactiveTrackColor = Color(0xFF232E4D),
                ),
              )
              Text(
                text = if (gainDb > 0f) "+${gainDb}" else "$gainDb",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD6DFEF),
                modifier = Modifier.width(38.dp),
              )
            }
          }
        }
      }

      // Footer note
      Text(
        text = "Tip: Tap the KSound Atmos Quick Settings tile in the notification shade to pick any audio profile or turn it off instantly. KSound v1.0.4 • 100% Offline",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
      )

      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun SoundSliderRow(
  title: String,
  value: Int,
  icon: ImageVector,
  accent: Color,
  onValueChange: (Int) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White,
        )
      }
      Text(
        text = "$value%",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = accent,
      )
    }
    Slider(
      value = value.toFloat(),
      onValueChange = { onValueChange(it.roundToInt()) },
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = accent,
        inactiveTrackColor = Color(0xFF232E4D),
      ),
    )
  }
}

private fun profileIcon(profile: AudioProfile): ImageVector = when (profile) {
  AudioProfile.DOLBY_CINEMA -> Icons.Filled.Movie
  AudioProfile.MEGA_BASS -> Icons.Filled.Speaker
  AudioProfile.CLUB_SUB -> Icons.Filled.GraphicEq
  AudioProfile.ATMOS_MUSIC -> Icons.Filled.MusicNote
  AudioProfile.CLEAR_VOICE -> Icons.Filled.RecordVoiceOver
  AudioProfile.GAMING_3D -> Icons.Filled.SportsEsports
  AudioProfile.CUSTOM -> Icons.Filled.Tune
}
