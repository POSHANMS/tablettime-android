package com.poshan.tablettime

import com.poshan.tablettime.data.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderTest {

    @Test
    fun testDaySelectionBitmask() {
        val allDaysReminder = Reminder(daysOfWeek = Reminder.ALL_DAYS_MASK)
        assertTrue(allDaysReminder.isEveryDay)
        for (i in 0..6) {
            assertTrue(allDaysReminder.isDaySelected(i))
        }

        val weekdaysReminder = Reminder(daysOfWeek = Reminder.WEEKDAYS_MASK)
        assertTrue(weekdaysReminder.isDaySelected(0)) // Mon
        assertTrue(weekdaysReminder.isDaySelected(4)) // Fri
        assertTrue(!weekdaysReminder.isDaySelected(5)) // Sat
        assertTrue(!weekdaysReminder.isDaySelected(6)) // Sun
    }

    @Test
    fun testFormattedTime() {
        val morningReminder = Reminder(hour = 8, minute = 5)
        assertEquals("8:05 AM", morningReminder.formattedTime())

        val noonReminder = Reminder(hour = 12, minute = 0)
        assertEquals("12:00 PM", noonReminder.formattedTime())

        val eveningReminder = Reminder(hour = 20, minute = 30)
        assertEquals("8:30 PM", eveningReminder.formattedTime())

        val midnightReminder = Reminder(hour = 0, minute = 15)
        assertEquals("12:15 AM", midnightReminder.formattedTime())
    }

    @Test
    fun testNextTriggerWhenDisabledReturnsNull() {
        val disabledReminder = Reminder(isEnabled = false, hour = 9, minute = 0)
        assertNull(disabledReminder.getNextRegularTriggerMillis())
    }

    @Test
    fun testNextTriggerTodayIfFuture() {
        val ref = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reminder = Reminder(
            hour = 10,
            minute = 30,
            daysOfWeek = Reminder.ALL_DAYS_MASK
        )

        val nextTime = reminder.getNextRegularTriggerMillis(ref.timeInMillis)
        assertNotNull(nextTime)

        val resultCal = Calendar.getInstance().apply { timeInMillis = nextTime!! }
        assertEquals(ref.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(10, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun testNextTriggerTomorrowIfPastToday() {
        val ref = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reminder = Reminder(
            hour = 9,
            minute = 0,
            daysOfWeek = Reminder.ALL_DAYS_MASK
        )

        val nextTime = reminder.getNextRegularTriggerMillis(ref.timeInMillis)
        assertNotNull(nextTime)

        val resultCal = Calendar.getInstance().apply { timeInMillis = nextTime!! }
        val expectedDay = (ref.get(Calendar.DAY_OF_YEAR) + 1)
        assertEquals(expectedDay, resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(9, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun testSnoozeEffectiveTrigger() {
        val now = System.currentTimeMillis()
        val snoozeTime = now + 10 * 60 * 1000L // 10 mins from now

        val reminder = Reminder(
            hour = 8,
            minute = 0,
            snoozeUntilMillis = snoozeTime
        )

        val effectiveTrigger = reminder.getNextEffectiveTriggerMillis(now)
        assertEquals(snoozeTime, effectiveTrigger)
    }
}
