package com.poshan.tablettime.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poshan.tablettime.alarm.AlarmScheduler
import com.poshan.tablettime.data.AppDatabase
import com.poshan.tablettime.data.AppSetting
import com.poshan.tablettime.data.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val reminders: List<Reminder> = emptyList(),
    val isMasterEnabled: Boolean = true,
    val needsExactAlarmPermission: Boolean = false,
    val needsNotificationPermission: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).reminderDao()
    private val scheduler = AlarmScheduler(application)

    private val _permissionState = MutableStateFlow(
        Pair(
            first = !scheduler.canScheduleExactAlarms(),
            second = !checkNotificationPermission(application)
        )
    )

    val uiState: StateFlow<HomeUiState> = combine(
        dao.getAllReminders(),
        dao.getSettingFlow(AppSetting.KEY_MASTER_REMINDERS_ENABLED),
        _permissionState
    ) { reminders, masterSetting, perms ->
        val masterEnabled = masterSetting != "false"
        HomeUiState(
            reminders = reminders,
            isMasterEnabled = masterEnabled,
            needsExactAlarmPermission = perms.first,
            needsNotificationPermission = perms.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun refreshPermissions() {
        val app = getApplication<Application>()
        _permissionState.value = Pair(
            first = !scheduler.canScheduleExactAlarms(),
            second = !checkNotificationPermission(app)
        )
    }

    fun toggleMasterReminders(enabled: Boolean) {
        viewModelScope.launch {
            dao.setSetting(
                AppSetting(
                    key = AppSetting.KEY_MASTER_REMINDERS_ENABLED,
                    value = enabled.toString()
                )
            )
            if (enabled) {
                scheduler.rescheduleAll()
            } else {
                scheduler.cancelAll()
            }
        }
    }

    fun toggleReminder(reminder: Reminder, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = isEnabled)
            dao.updateReminder(updated)

            val masterSetting = dao.getSetting(AppSetting.KEY_MASTER_REMINDERS_ENABLED)
            val isMasterEnabled = masterSetting != "false"

            if (isMasterEnabled) {
                if (isEnabled) {
                    scheduler.schedule(updated)
                } else {
                    scheduler.cancel(updated.id)
                }
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            scheduler.cancel(reminder.id)
            dao.deleteReminder(reminder)
        }
    }

    companion object {
        fun checkNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            } else {
                true
            }
        }
    }
}
