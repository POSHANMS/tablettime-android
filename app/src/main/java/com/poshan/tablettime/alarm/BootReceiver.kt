package com.poshan.tablettime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered after device reboot, app update, or time zone change.
 * Restores and re-registers all active medication alarms from Room.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received system broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Restoring alarms after system event: $action")
                    AlarmScheduler(context).rescheduleAll()
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
