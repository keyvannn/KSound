package com.ksound.atmos.service

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ksound.atmos.MainActivity
import com.ksound.atmos.audio.AudioProfile
import com.ksound.atmos.audio.GlobalAudioEngine
import java.lang.ref.WeakReference

class KSoundTileService : TileService() {

  companion object {
    @Volatile
    private var activeTileRef: WeakReference<KSoundTileService>? = null

    fun refreshIfListening() {
      val service = activeTileRef?.get() ?: return
      Handler(Looper.getMainLooper()).post {
        service.updateTileUi()
      }
    }
  }

  private val mainHandler = Handler(Looper.getMainLooper())

  override fun onStartListening() {
    super.onStartListening()
    activeTileRef = WeakReference(this)
    GlobalAudioEngine.init(this)
    updateTileUi()
  }

  override fun onStopListening() {
    if (activeTileRef?.get() === this) {
      activeTileRef = null
    }
    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()
    GlobalAudioEngine.init(this)

    val current = GlobalAudioEngine.state.value
    val profiles = AudioProfile.entries
    val items = Array(profiles.size + 1) { idx ->
      if (idx == 0) {
        "Off (Original Audio)"
      } else {
        val p = profiles[idx - 1]
        "${p.title}  —  Bass ${p.bassBoost}%"
      }
    }
    val checkedIndex = if (!current.enabled) 0 else profiles.indexOf(current.profile) + 1

    val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
      .setTitle("KSound Atmos")
      .setSingleChoiceItems(items, checkedIndex) { dlg, which ->
        if (which == 0) {
          GlobalAudioEngine.updateState { it.copy(enabled = false) }
        } else {
          val selectedProfile = profiles[which - 1]
          GlobalAudioEngine.selectProfile(selectedProfile)
          GlobalAudioEngine.updateState { it.copy(enabled = true) }
        }
        AudioEnhancerService.syncService(this)
        dlg.dismiss()
        // Update immediately AND after SystemUI finishes dismissing the dialog window
        updateTileUi()
        mainHandler.postDelayed({ updateTileUi() }, 120L)
      }
      .setNeutralButton("Open App") { dlg, _ ->
        dlg.dismiss()
        val intent = Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          val pi = PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
          )
          startActivityAndCollapse(pi)
        } else {
          @Suppress("DEPRECATION")
          startActivityAndCollapse(intent)
        }
      }
      .setNegativeButton("Close", null)
      .create()

    dialog.setOnDismissListener {
      updateTileUi()
      mainHandler.postDelayed({ updateTileUi() }, 150L)
    }

    showDialog(dialog)
  }

  fun updateTileUi() {
    val tile = qsTile ?: return
    val s = GlobalAudioEngine.state.value
    tile.state = if (s.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.label = "KSound Atmos"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      tile.subtitle = if (s.enabled) s.profile.title else "Off"
    }
    tile.updateTile()
  }
}
