package com.poshan.tablettime.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.poshan.tablettime.R
import com.poshan.tablettime.data.AppDatabase
import com.poshan.tablettime.ui.checkin.CheckInActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager exact alarms.
 * Plays looping alarm audio, starts vibration, issues full-screen notification,
 * and launches CheckInActivity.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "tablettime_alarms"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val reminderLabel = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_LABEL) ?: ""
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        Log.d(TAG, "Alarm triggered: id=$reminderId, label=$reminderLabel, isSnooze=$isSnooze")

        // 1. Acquire temporary partial wake lock
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TabletTime:AlarmWakeLock"
        )
        wakeLock.acquire(60_000L) // 60 seconds max matching alarm timeout

        // 2. Play alarm sound and vibration
        AlarmSoundPlayer.play(context)

        // 3. Create notification channel for high-priority alarms
        createNotificationChannel(context)

        // 4. Build full screen intent to launch CheckInActivity
        val checkInIntent = Intent(context, CheckInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CheckInActivity.EXTRA_REMINDER_ID, reminderId)
            putExtra(CheckInActivity.EXTRA_REMINDER_LABEL, reminderLabel)
            putExtra(CheckInActivity.EXTRA_IS_SNOOZE, isSnooze)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = if (reminderLabel.isNotBlank()) {
            reminderLabel
        } else {
            context.getString(R.string.checkin_question_default)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle(notificationTitle)
            .setContentText(context.getString(R.string.checkin_subtitle))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), notification)

        // Also try direct activity launch (works in foreground and background on many versions)
        try {
            context.startActivity(checkInIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Direct activity start failed, relying on fullScreenIntent: ${e.message}")
        }

        // 5. Update database and reschedule next regular alarm safely under wake lock
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                if (isSnooze) {
                    // Snooze has fired; clear the snooze record
                    dao.clearSnooze(reminderId)
                }

                // Reschedule next regular recurring alarm for tomorrow / next scheduled day
                val reminder = dao.getReminderById(reminderId)
                if (reminder != null && reminder.isEnabled) {
                    AlarmScheduler(context).schedule(reminder)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating reminder state on alarm fire", e)
            } finally {
                try {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing wake lock", e)
                }
                pendingResult.finish()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
                setSound(
                    null, // Sound is played synchronously by AlarmSoundPlayer with looping control
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
