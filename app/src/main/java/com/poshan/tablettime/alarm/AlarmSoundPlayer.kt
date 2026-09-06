package com.poshan.tablettime.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.poshan.tablettime.R

/**
 * Manages the alarm sound and vibration during the trigger flow.
 * Plays a looping alarm-style escalating tone for up to 60 seconds or until dismissed.
 */
object AlarmSoundPlayer {

    private const val TAG = "AlarmSoundPlayer"
    private const val AUTO_DISMISS_TIMEOUT_MS = 60_000L // 60 seconds timeout

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPlayingSound = false

    private val timeoutRunnable = Runnable {
        Log.d(TAG, "Alarm sound timed out after 60s")
        stop()
    }

    /**
     * Starts playing alarm sound and vibration.
     */
    @Synchronized
    fun play(context: Context) {
        if (isPlayingSound) return
        isPlayingSound = true

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // 1. Play custom peaceful, gentle tablet reminder tone
            mediaPlayer = try {
                MediaPlayer.create(context.applicationContext, R.raw.peaceful_tablet_reminder, audioAttributes, 0)?.apply {
                    isLooping = true
                    start()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed playing peaceful raw audio, falling back to system tone", e)
                null
            }

            // 2. Fallback to system tone if custom sound could not be loaded
            if (mediaPlayer == null) {
                var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }

                if (alarmUri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(context.applicationContext, alarmUri)
                        setAudioAttributes(audioAttributes)
                        isLooping = true
                        prepare()
                        start()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }

        // Start gentle vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            // Gentle, calming pulse pattern (300ms pulse, 700ms rest)
            val pattern = longArrayOf(0, 300, 700, 300, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibrator", e)
        }

        // Automatically stop after 60 seconds if unanswered
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, AUTO_DISMISS_TIMEOUT_MS)
    }

    /**
     * Immediately stops alarm sound and vibration.
     */
    @Synchronized
    fun stop() {
        handler.removeCallbacks(timeoutRunnable)

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator", e)
        } finally {
            vibrator = null
        }

        isPlayingSound = false
    }

    fun isPlaying(): Boolean = isPlayingSound
}
