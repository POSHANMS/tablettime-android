package com.poshan.tablettime.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.poshan.tablettime.MainActivity
import com.poshan.tablettime.data.AppDatabase
import com.poshan.tablettime.data.AppSetting
import com.poshan.tablettime.data.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exact alarm scheduler for TabletTime.
 * Uses AlarmManager.setAlarmClock to guarantee reliable delivery through Doze,
 * displaying the system alarm clock indicator.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"

        const val ACTION_FIRE_ALARM = "com.poshan.tablettime.ACTION_FIRE_ALARM"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_LABEL = "extra_reminder_label"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        // Request code offset for snooze alarms
        private const val SNOOZE_OFFSET = 1_000_000
    }

    /**
     * Checks if the app can schedule exact alarms on Android 12+ (API 31+).
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules the next exact alarm for the given [reminder].
     */
    fun schedule(reminder: Reminder) {
        if (!reminder.isEnabled) {
            cancel(reminder.id)
            return
        }

        val triggerTime = reminder.getNextRegularTriggerMillis() ?: run {
            Log.w(TAG, "No upcoming trigger time for reminder #${reminder.id}")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_ALARM
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_LABEL, reminder.label)
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent),
                pendingIntent
            )
            Log.d(TAG, "Scheduled exact alarm for reminder #${reminder.id} at $triggerTime (${reminder.formattedTime()})")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: SCHEDULE_EXACT_ALARM not granted", e)
        }
    }

    /**
     * Schedules a one-time snooze alarm for the specified [snoozeMinutes] today only.
     */
    suspend fun scheduleSnooze(reminderId: Long, label: String, snoozeMinutes: Int) {
        val snoozeMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        // Persist snooze timestamp in Room
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).reminderDao()
            dao.updateSnooze(reminderId, snoozeMillis)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_ALARM
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_LABEL, label)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        val requestCode = (reminderId + SNOOZE_OFFSET).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(snoozeMillis, showPendingIntent),
                pendingIntent
            )
            Log.d(TAG, "Scheduled snooze alarm for reminder #$reminderId in $snoozeMinutes min ($snoozeMillis)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while setting snooze", e)
        }
    }

    /**
     * Cancels any scheduled alarm (regular and snooze) for [reminderId].
     */
    fun cancel(reminderId: Long) {
        // Cancel regular alarm
        val regularIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_ALARM
        }
        val regularPending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            regularIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        regularPending?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        // Cancel snooze alarm
        val snoozePending = PendingIntent.getBroadcast(
            context,
            (reminderId + SNOOZE_OFFSET).toInt(),
            regularIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        snoozePending?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        Log.d(TAG, "Cancelled alarms for reminder #$reminderId")
    }

    /**
     * Re-registers all active alarms from Room if the master switch is enabled.
     */
    suspend fun rescheduleAll() = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).reminderDao()
        val masterSetting = dao.getSetting(AppSetting.KEY_MASTER_REMINDERS_ENABLED)
        val isMasterEnabled = masterSetting != "false" // default true

        if (!isMasterEnabled) {
            Log.d(TAG, "Master switch is OFF; canceling all alarms")
            cancelAll()
            return@withContext
        }

        val activeReminders = dao.getActiveReminders()
        Log.d(TAG, "Rescheduling ${activeReminders.size} active reminders")
        for (reminder in activeReminders) {
            schedule(reminder)
        }
    }

    /**
     * Cancels all scheduled alarms for all reminders in the database.
     */
    suspend fun cancelAll() = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).reminderDao()
        val all = dao.getActiveReminders()
        for (reminder in all) {
            cancel(reminder.id)
        }
    }
}
