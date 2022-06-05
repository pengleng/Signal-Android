package asia.coolapp.chat.components.voice

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.signal.core.util.logging.Log
import asia.coolapp.chat.util.ServiceUtil
import java.util.concurrent.TimeUnit

private val TAG = Log.tag(VoiceNoteProximityWakeLockManager::class.java)
private const val PROXIMITY_THRESHOLD = 5f

/**
 * Manages the WakeLock while a VoiceNote is playing back in the target activity.
 */
class VoiceNoteProximityWakeLockManager(
  private val activity: FragmentActivity,
  private val mediaController: MediaControllerCompat
) : DefaultLifecycleObserver {

  private val wakeLock: PowerManager.WakeLock? = if (Build.VERSION.SDK_INT >= 21) {
    ServiceUtil.getPowerManager(activity.applicationContext).newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG)
  } else {
    null
  }

  private val sensorManager: SensorManager = ServiceUtil.getSensorManager(activity)
  private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

  private val mediaControllerCallback = MediaControllerCallback()
  private val hardwareSensorEventListener = HardwareSensorEventListener()

  private var startTime: Long = -1

  init {
    if (proximitySensor != null) {
      activity.lifecycle.addObserver(this)
    }
  }

  override fun onResume(owner: LifecycleOwner) {
    if (proximitySensor != null) {
      mediaController.registerCallback(mediaControllerCallback)
    }
  }

  override fun onPause(owner: LifecycleOwner) {
    if (proximitySensor != null) {
      unregisterCallbacksAndRelease()
    }
  }

  fun unregisterCallbacksAndRelease() {
    mediaController.unregisterCallback(mediaControllerCallback)
    cleanUpWakeLock()
  }

  fun unregisterFromLifecycle() {
    if (proximitySensor != null) {
      activity.lifecycle.removeObserver(this)
    }
  }

  private fun isActivityResumed() = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

  private fun isPlayerActive() = mediaController.playbackState.state == PlaybackStateCompat.STATE_BUFFERING ||
    mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING

  private fun cleanUpWakeLock() {
    startTime = -1L
    sensorManager.unregisterListener(hardwareSensorEventListener)

    if (wakeLock?.isHeld == true) {
      wakeLock.release()
    }

    sendNewStreamTypeToPlayer(AudioManager.STREAM_MUSIC)
  }

  private fun sendNewStreamTypeToPlayer(newStreamType: Int) {
    val params = Bundle()
    params.putInt(VoiceNotePlaybackService.ACTION_SET_AUDIO_STREAM, newStreamType)
    mediaController.sendCommand(VoiceNotePlaybackService.ACTION_SET_AUDIO_STREAM, params, null)
  }

  inner class MediaControllerCallback : MediaControllerCompat.Callback() {
    override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
      if (!isActivityResumed()) {
        return
      }

      if (isPlayerActive()) {
        if (startTime == -1L) {
          startTime = System.currentTimeMillis()
          sensorManager.registerListener(hardwareSensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
      } else {
        cleanUpWakeLock()
      }
    }
  }

  inner class HardwareSensorEventListener : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
      if (startTime == -1L ||
        System.currentTimeMillis() - startTime <= 500 ||
        !isActivityResumed() ||
        !isPlayerActive() ||
        event.sensor.type != Sensor.TYPE_PROXIMITY
      ) {
        return
      }

      val newStreamType = if (event.values[0] < PROXIMITY_THRESHOLD && event.values[0] != proximitySensor?.maximumRange) {
        AudioManager.STREAM_VOICE_CALL
      } else {
        AudioManager.STREAM_MUSIC
      }

      sendNewStreamTypeToPlayer(newStreamType)

      if (newStreamType == AudioManager.STREAM_VOICE_CALL) {
        if (wakeLock?.isHeld == false) {
          wakeLock.acquire(TimeUnit.MINUTES.toMillis(30))
        }

        startTime = System.currentTimeMillis()
      } else {
        if (wakeLock?.isHeld == true) {
          wakeLock.release()
        }
      }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
  }
}
