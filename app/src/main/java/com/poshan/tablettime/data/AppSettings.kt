package com.poshan.tablettime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Key-value settings entity to persist app preferences (such as Master Reminders switch)
 * purely inside Room without third-party dependencies.
 */
@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey
    val key: String,
    val value: String
) {
    companion object {
        const val KEY_MASTER_REMINDERS_ENABLED = "master_reminders_enabled"
    }
}
