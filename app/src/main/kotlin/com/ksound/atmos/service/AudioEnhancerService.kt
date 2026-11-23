package com.ksound.atmos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.ksound.atmos.MainActivity
import com.ksound.atmos.R
import com.ksound.atmos.audio.GlobalAudioEngine
import com.ksound.atmos.audio.SoundState

class AudioEnhancerService : Service() {

  companion object {
    const val CHANNEL_ID = "ksound_dsp_channel"
    const val NOTIFICATION_ID = 1001

    const val ACTION_START_OR_UPDATE = "com.ksound.atmos.ACTION_START_OR_UPDATE"
    const val ACTION_TOGGLE_ENABLED = "com.ksound.atmos.ACTION_TOGGLE_ENABLED"
    const val ACTION_NEXT_PROFILE = "com.ksound.atmos.ACTION_NEXT_PROFILE"
    const val ACTION_STOP = "com.ksound.atmos.ACTION_STOP"

    fun syncService(context: Context) {
      val intent = Intent(context, AudioEnhancerService::class.java).apply {
        action = ACTION_START_OR_UPDATE
      }
      runCatching {
        context.startForegroundService(intent)
      }.onFailure {
        runCatching { context.startService(intent) }
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    GlobalAudioEngine.init(this)
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    GlobalAudioEngine.init(this)
    when (intent?.action) {
      ACTION_TOGGLE_ENABLED -> {
        GlobalAudioEngine.updateState { it.copy(enabled = !it.enabled) }
      }
      ACTION_NEXT_PROFILE -> {
        val next = GlobalAudioEngine.state.value.profile.nextPreset()
        GlobalAudioEngine.selectProfile(next)
        if (!GlobalAudioEngine.state.value.enabled) {
          GlobalAudioEngine.updateState { it.copy(enabled = true) }
        }
      }
      ACTION_STOP -> {
        GlobalAudioEngine.updateState { it.copy(enabled = false) }
        GlobalAudioEngine.release()
        requestTileUpdate()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      else -> {
        GlobalAudioEngine.applyCurrent()
      }
    }

    val state = GlobalAudioEngine.state.value
    val notification = buildNotification(state)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    requestTileUpdate()
    return START_STICKY
  }

  override fun onDestroy() {
    GlobalAudioEngine.release()
    requestTileUpdate()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun requestTileUpdate() {
    KSoundTileService.refreshIfListening()
    runCatching {
      TileService.requestListeningState(
        this,
        ComponentName(this, KSoundTileService::class.java),
      )
    }
  }

  private fun createNotificationChannel() {
    val nm = getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
      CHANNEL_ID,
      "KSound Spatial Audio",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "Quick toggle & profile switcher for KSound Atmos"
      setShowBadge(false)
      setSound(null, null)
    }
    nm.createNotificationChannel(channel)
  }

  private fun buildNotification(state: SoundState): Notification {
    val openIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val toggleIntent = PendingIntent.getService(
      this,
      1,
      Intent(this, AudioEnhancerService::class.java).apply { action = ACTION_TOGGLE_ENABLED },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val nextProfileIntent = PendingIntent.getService(
      this,
      2,
      Intent(this, AudioEnhancerService::class.java).apply { action = ACTION_NEXT_PROFILE },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val stopIntent = PendingIntent.getService(
      this,
      3,
      Intent(this, AudioEnhancerService::class.java).apply { action = ACTION_STOP },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val statusText = if (state.enabled) {
      "ON • ${state.profile.title} (Bass ${state.bassBoost}% | 3D ${state.virtualizer}%)"
    } else {
      "OFF • Tap Turn On to enable spatial audio"
    }

    val toggleLabel = if (state.enabled) "Turn Off" else "Turn On"

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notif_sound)
      .setContentTitle("KSound Atmos")
      .setContentText(statusText)
      .setContentIntent(openIntent)
      .setOngoing(state.enabled)
      .setSilent(true)
      .setOnlyAlertOnce(true)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .addAction(0, toggleLabel, toggleIntent)
      .addAction(0, "Next Profile", nextProfileIntent)
      .addAction(0, "Close", stopIntent)
      .build()
  }
}
