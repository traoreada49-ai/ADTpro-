package com.example

import com.example.data.local.entity.AppSettingsEntity
import com.example.util.ThemeScheduleManager
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ThemeScheduleTest {

    @Test
    fun appSettingsEntity_darkScheduleDefaults_areCorrect() {
        val settings = AppSettingsEntity()
        assertEquals("DARK", settings.themeMode)
        assertEquals("SUNSET_TO_SUNRISE", settings.darkScheduleType)
        assertEquals(20, settings.darkScheduleStartHour)
        assertEquals(0, settings.darkScheduleStartMinute)
        assertEquals(7, settings.darkScheduleEndHour)
        assertEquals(0, settings.darkScheduleEndMinute)
    }

    @Test
    fun isNightTime_overnightWindow_evaluatesCorrectly() {
        // Standard overnight window: 20:00 to 07:00
        val startH = 20
        val startM = 0
        val endH = 7
        val endM = 0

        // Day hours (outside night window)
        assertFalse("12:00 should be daytime", ThemeScheduleManager.isNightTime(12, 0, startH, startM, endH, endM))
        assertFalse("19:59 should be daytime", ThemeScheduleManager.isNightTime(19, 59, startH, startM, endH, endM))
        assertFalse("07:00 should be daytime", ThemeScheduleManager.isNightTime(7, 0, startH, startM, endH, endM))
        assertFalse("07:01 should be daytime", ThemeScheduleManager.isNightTime(7, 1, startH, startM, endH, endM))

        // Night hours (inside night window)
        assertTrue("20:00 should be night", ThemeScheduleManager.isNightTime(20, 0, startH, startM, endH, endM))
        assertTrue("20:01 should be night", ThemeScheduleManager.isNightTime(20, 1, startH, startM, endH, endM))
        assertTrue("23:45 should be night", ThemeScheduleManager.isNightTime(23, 45, startH, startM, endH, endM))
        assertTrue("00:00 should be night", ThemeScheduleManager.isNightTime(0, 0, startH, startM, endH, endM))
        assertTrue("03:30 should be night", ThemeScheduleManager.isNightTime(3, 30, startH, startM, endH, endM))
        assertTrue("06:59 should be night", ThemeScheduleManager.isNightTime(6, 59, startH, startM, endH, endM))
    }

    @Test
    fun isNightTime_intraDayWindow_evaluatesCorrectly() {
        // Day window: 10:00 to 18:00
        val startH = 10
        val startM = 0
        val endH = 18
        val endM = 0

        assertFalse("09:59 should be outside", ThemeScheduleManager.isNightTime(9, 59, startH, startM, endH, endM))
        assertTrue("10:00 should be inside", ThemeScheduleManager.isNightTime(10, 0, startH, startM, endH, endM))
        assertTrue("14:30 should be inside", ThemeScheduleManager.isNightTime(14, 30, startH, startM, endH, endM))
        assertFalse("18:00 should be outside", ThemeScheduleManager.isNightTime(18, 0, startH, startM, endH, endM))
        assertFalse("22:00 should be outside", ThemeScheduleManager.isNightTime(22, 0, startH, startM, endH, endM))
    }

    @Test
    fun shouldUseDarkTheme_handlesAllModes() {
        // Manual DARK mode
        assertTrue(ThemeScheduleManager.shouldUseDarkTheme("DARK", systemInDarkTheme = false))
        assertTrue(ThemeScheduleManager.shouldUseDarkTheme("DARK", systemInDarkTheme = true))

        // Manual LIGHT mode
        assertFalse(ThemeScheduleManager.shouldUseDarkTheme("LIGHT", systemInDarkTheme = false))
        assertFalse(ThemeScheduleManager.shouldUseDarkTheme("LIGHT", systemInDarkTheme = true))

        // SYSTEM mode
        assertTrue(ThemeScheduleManager.shouldUseDarkTheme("SYSTEM", systemInDarkTheme = true))
        assertFalse(ThemeScheduleManager.shouldUseDarkTheme("SYSTEM", systemInDarkTheme = false))

        // AUTO_SCHEDULE mode at 22:00 (should be dark)
        val nightCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(ThemeScheduleManager.shouldUseDarkTheme("AUTO_SCHEDULE", calendar = nightCal))

        // AUTO_SCHEDULE mode at 14:00 (should be light)
        val dayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ThemeScheduleManager.shouldUseDarkTheme("AUTO_SCHEDULE", calendar = dayCal))
    }

    @Test
    fun computeTransitionInfo_reportsExpectedNextSwitch() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
        }
        val info = ThemeScheduleManager.computeTransitionInfo(
            scheduleType = ThemeScheduleManager.SCHEDULE_SUNSET_TO_SUNRISE,
            startHour = 20,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            calendar = cal
        )

        assertTrue(info.isCurrentlyDark)
        assertEquals(7, info.nextSwitchHour)
        assertEquals(0, info.nextSwitchMinute)
        assertFalse("Next switch should be to light mode", info.nextModeIsDark)
        assertEquals(9, info.hoursRemaining) // 22:00 -> 07:00 is 9 hours
        assertEquals(0, info.minutesRemaining)
        assertTrue(info.statusSummary.contains("Mode sombre actif"))
    }

    @Test
    fun presetSchedules_areValidAndNonEmpty() {
        val presets = ThemeScheduleManager.PRESET_SCHEDULES
        assertTrue(presets.isNotEmpty())
        for (preset in presets) {
            assertTrue(preset.startHour in 0..23)
            assertTrue(preset.startMinute in 0..59)
            assertTrue(preset.endHour in 0..23)
            assertTrue(preset.endMinute in 0..59)
            assertTrue(preset.label.isNotBlank())
        }
    }
}
