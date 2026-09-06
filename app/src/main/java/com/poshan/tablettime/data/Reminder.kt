package com.poshan.tablettime.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

/**
 * Represents a medication reminder in TabletTime.
 *
 * @property id Unique primary key.
 * @property label Optional custom label / question (e.g. "Morning tablet", "Breakfast").
 * @property hour Alarm hour in 24h format (0-23).
 * @property minute Alarm minute (0-59).
 * @property daysOfWeek Bitmask of active days: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64.
 *                        127 represents every day (1+2+4+8+16+32+64).
 * @property isEnabled Whether this individual reminder is active.
 * @property snoozeUntilMillis Timestamp for a one-time snooze alarm for today only, or 0 if not snoozed.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val label: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val daysOfWeek: Int = ALL_DAYS_MASK,
    val isEnabled: Boolean = true,
    val snoozeUntilMillis: Long = 0L
) {
    companion object {
        const val MON = 1 shl 0 // 1
        const val TUE = 1 shl 1 // 2
        const val WED = 1 shl 2 // 4
        const val THU = 1 shl 3 // 8
        const val FRI = 1 shl 4 // 16
        const val SAT = 1 shl 5 // 32
        const val SUN = 1 shl 6 // 64
        const val ALL_DAYS_MASK = MON or TUE or WED or THU or FRI or SAT or SUN // 127
        const val WEEKDAYS_MASK = MON or TUE or WED or THU or FRI // 31
        const val WEEKENDS_MASK = SAT or SUN // 96

        /**
         * Returns mask corresponding to java.util.Calendar day of week (SUNDAY=1..SATURDAY=7).
         */
        fun maskForCalendarDay(calendarDayOfWeek: Int): Int {
            return when (calendarDayOfWeek) {
                Calendar.MONDAY -> MON
                Calendar.TUESDAY -> TUE
                Calendar.WEDNESDAY -> WED
                Calendar.THURSDAY -> THU
                Calendar.FRIDAY -> FRI
                Calendar.SATURDAY -> SAT
                Calendar.SUNDAY -> SUN
                else -> 0
            }
        }
    }

    /**
     * Checks if a specific day is selected in the bitmask.
     * [dayIndex]: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun.
     */
    fun isDaySelected(dayIndex: Int): Boolean {
        require(dayIndex in 0..6) { "Day index must be 0..6" }
        return (daysOfWeek and (1 shl dayIndex)) != 0
    }

    /**
     * Returns true if all 7 days are active.
     */
    val isEveryDay: Boolean
        get() = (daysOfWeek and ALL_DAYS_MASK) == ALL_DAYS_MASK

    /**
     * Returns formatted 12-hour or 24-hour time string (e.g. "08:00 AM" or "08:00").
     */
    fun formattedTime(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val isPm = cal.get(Calendar.AM_PM) == Calendar.PM
        val displayHour = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
    }

    /**
     * Calculates the next trigger timestamp in milliseconds for regular recurring alarm.
     * If [fromMillis] is not supplied, uses the current system time.
     */
    fun getNextRegularTriggerMillis(fromMillis: Long = System.currentTimeMillis()): Long? {
        if (!isEnabled) return null

        val now = Calendar.getInstance().apply {
            timeInMillis = fromMillis
        }

        // If no days selected, treat as one-time alarm for today or tomorrow
        if (daysOfWeek == 0) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = fromMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis > fromMillis) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1)
            return candidate.timeInMillis
        }

        // Check today and the next 7 days
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = fromMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (candidate.timeInMillis <= fromMillis) {
                continue
            }

            val dayMask = maskForCalendarDay(candidate.get(Calendar.DAY_OF_WEEK))
            if ((daysOfWeek and dayMask) != 0) {
                return candidate.timeInMillis
            }
        }

        return null
    }

    /**
     * Returns the earliest active alarm time, considering any active one-time snooze.
     */
    fun getNextEffectiveTriggerMillis(fromMillis: Long = System.currentTimeMillis()): Long? {
        if (!isEnabled) return null

        // Check if there is an active snooze today
        if (snoozeUntilMillis > fromMillis) {
            return snoozeUntilMillis
        }

        return getNextRegularTriggerMillis(fromMillis)
    }
}
