package com.poshan.tablettime.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY hour ASC, minute ASC")
    suspend fun getActiveReminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getReminderByIdFlow(id: Long): Flow<Reminder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder): Int

    @Delete
    suspend fun deleteReminder(reminder: Reminder): Int

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long): Int

    @Query("UPDATE reminders SET snoozeUntilMillis = :snoozeMillis WHERE id = :id")
    suspend fun updateSnooze(id: Long, snoozeMillis: Long): Int

    @Query("UPDATE reminders SET snoozeUntilMillis = 0 WHERE id = :id")
    suspend fun clearSnooze(id: Long): Int

    // Settings queries
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting): Long
}
