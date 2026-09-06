package com.poshan.tablettime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poshan.tablettime.alarm.AlarmScheduler
import com.poshan.tablettime.data.AppDatabase
import com.poshan.tablettime.data.AppSetting
import com.poshan.tablettime.data.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReminderFormState(
    val id: Long = 0L,
    val label: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val daysMask: Int = Reminder.ALL_DAYS_MASK,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).reminderDao()
    private val scheduler = AlarmScheduler(application)

    private val _formState = MutableStateFlow(
        // Default to next whole hour or 8:00 AM
        ReminderFormState(
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            minute = 0
        )
    )
    val formState: StateFlow<ReminderFormState> = _formState.asStateFlow()

    fun initForNew() {
        val now = Calendar.getInstance()
        _formState.value = ReminderFormState(
            id = 0L,
            label = "",
            hour = now.get(Calendar.HOUR_OF_DAY),
            minute = 0,
            daysMask = Reminder.ALL_DAYS_MASK,
            isEditing = false
        )
    }

    fun loadReminder(id: Long) {
        if (id <= 0) {
            initForNew()
            return
        }
        viewModelScope.launch {
            val reminder = dao.getReminderById(id)
            if (reminder != null) {
                _formState.value = ReminderFormState(
                    id = reminder.id,
                    label = reminder.label,
                    hour = reminder.hour,
                    minute = reminder.minute,
                    daysMask = reminder.daysOfWeek,
                    isEditing = true
                )
            }
        }
    }

    fun setLabel(label: String) {
        _formState.update { it.copy(label = label) }
    }

    fun setTime(hour: Int, minute: Int) {
        _formState.update { it.copy(hour = hour, minute = minute) }
    }

    fun toggleDay(dayIndex: Int) {
        _formState.update { state ->
            val bit = 1 shl dayIndex
            val newMask = state.daysMask xor bit
            state.copy(daysMask = newMask)
        }
    }

    fun setEveryDay() {
        _formState.update { it.copy(daysMask = Reminder.ALL_DAYS_MASK) }
    }

    fun setWeekdays() {
        _formState.update { it.copy(daysMask = Reminder.WEEKDAYS_MASK) }
    }

    fun setWeekends() {
        _formState.update { it.copy(daysMask = Reminder.WEEKENDS_MASK) }
    }

    fun saveReminder(onSaved: () -> Unit) {
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            val state = _formState.value

            val reminder = Reminder(
                id = if (state.isEditing) state.id else 0L,
                label = state.label.trim(),
                hour = state.hour,
                minute = state.minute,
                daysOfWeek = state.daysMask,
                isEnabled = true,
                snoozeUntilMillis = 0L
            )

            val savedId = if (state.isEditing) {
                dao.updateReminder(reminder)
                reminder.id
            } else {
                dao.insertReminder(reminder)
            }

            val savedReminder = reminder.copy(id = savedId)

            // Immediately schedule if master switch is ON
            val masterSetting = dao.getSetting(AppSetting.KEY_MASTER_REMINDERS_ENABLED)
            val isMasterEnabled = masterSetting != "false"
            if (isMasterEnabled) {
                scheduler.schedule(savedReminder)
            }

            _formState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }

    fun deleteReminder(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val state = _formState.value
            if (state.isEditing && state.id > 0) {
                scheduler.cancel(state.id)
                dao.deleteReminderById(state.id)
            }
            onDeleted()
        }
    }
}
