package com.example.domain

import com.example.data.db.BlockingProfileEntity
import com.example.data.db.ProfileWithSchedules
import com.example.data.db.ScheduleRuleEntity
import com.example.data.db.ScheduleRuleMode
import com.example.data.prefs.ParentalControlPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LockStateEvaluatorTest {

    private val utcZone: TimeZone = TimeZone.getTimeZone("UTC")

    private fun epochFor(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(utcZone).apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `total lock overrides schedules and blocks configured networks`() {
        val now = epochFor(2026, Calendar.NOVEMBER, 16, 14, 0)
        val state = LockStateEvaluator.evaluate(
            nowEpochMs = now,
            prefs = ParentalControlPrefs(
                isTotalLockActive = true,
                totalLockBlockWifi = true,
                totalLockBlockMobile = false,
            ),
            profiles = emptyList(),
            timeZone = utcZone,
        )

        assertTrue(state.isBlocked)
        assertTrue(state.blockWifi)
        assertFalse(state.blockMobileData)
        assertTrue(state.reason is LockReason.TotalLock)
    }

    @Test
    fun `quick block expires after endEpochMs`() {
        val now = epochFor(2026, Calendar.NOVEMBER, 16, 15, 0)
        val activeState = LockStateEvaluator.evaluate(
            nowEpochMs = now,
            prefs = ParentalControlPrefs(
                quickBlockEndEpochMs = now + 15 * 60_000L,
                quickBlockDurationMinutes = 30,
                quickBlockWifi = true,
                quickBlockMobile = true,
            ),
            profiles = emptyList(),
            timeZone = utcZone,
        )
        assertTrue(activeState.isBlocked)
        assertTrue(activeState.reason is LockReason.QuickBlock)

        val expiredState = LockStateEvaluator.evaluate(
            nowEpochMs = now + 16 * 60_000L,
            prefs = ParentalControlPrefs(
                quickBlockEndEpochMs = now + 15 * 60_000L,
                quickBlockDurationMinutes = 30,
                quickBlockWifi = true,
                quickBlockMobile = true,
            ),
            profiles = emptyList(),
            timeZone = utcZone,
        )
        assertFalse(expiredState.isBlocked)
        assertEquals(LockReason.Unlocked, expiredState.reason)
    }

    @Test
    fun `overnight block window triggers late night and early next morning`() {
        val profile = BlockingProfileEntity(
            id = 1L,
            name = "Hora de Dormir",
            blockWifi = true,
            blockMobileData = true,
            isEnabled = true,
        )
        // Monday..Friday 22:00 -> 07:00
        val rule = ScheduleRuleEntity(
            id = 10L,
            profileId = 1L,
            label = "Noite",
            ruleMode = ScheduleRuleMode.BLOCK_WINDOW.key,
            daysMask = DayMask.WEEKDAYS,
            startMinutes = 22 * 60,
            endMinutes = 7 * 60,
            isEnabled = true,
        )
        val profiles = listOf(ProfileWithSchedules(profile, listOf(rule)))

        // Monday Nov 16, 2026 at 23:15 -> blocked
        val mondayNight = epochFor(2026, Calendar.NOVEMBER, 16, 23, 15)
        assertTrue(
            LockStateEvaluator.evaluate(mondayNight, ParentalControlPrefs(), profiles, utcZone).isBlocked,
        )

        // Tuesday Nov 17, 2026 at 06:30 -> blocked from Monday's overnight rule
        val tuesdayEarly = epochFor(2026, Calendar.NOVEMBER, 17, 6, 30)
        assertTrue(
            LockStateEvaluator.evaluate(tuesdayEarly, ParentalControlPrefs(), profiles, utcZone).isBlocked,
        )

        // Tuesday Nov 17, 2026 at 12:00 -> unlocked
        val tuesdayNoon = epochFor(2026, Calendar.NOVEMBER, 17, 12, 0)
        assertFalse(
            LockStateEvaluator.evaluate(tuesdayNoon, ParentalControlPrefs(), profiles, utcZone).isBlocked,
        )
    }

    @Test
    fun `allow window blocks outside permitted usage hours`() {
        val profile = BlockingProfileEntity(
            id = 2L,
            name = "Estudos",
            blockWifi = true,
            blockMobileData = true,
            isEnabled = true,
        )
        // Allowed 17:00..19:00 every day
        val rule = ScheduleRuleEntity(
            id = 20L,
            profileId = 2L,
            label = "Liberado à tarde",
            ruleMode = ScheduleRuleMode.ALLOW_WINDOW.key,
            daysMask = DayMask.ALL_DAYS,
            startMinutes = 17 * 60,
            endMinutes = 19 * 60,
            isEnabled = true,
        )
        val profiles = listOf(ProfileWithSchedules(profile, listOf(rule)))

        val insideAllowed = epochFor(2026, Calendar.NOVEMBER, 16, 18, 0)
        assertFalse(
            LockStateEvaluator.evaluate(insideAllowed, ParentalControlPrefs(), profiles, utcZone).isBlocked,
        )

        val outsideAllowed = epochFor(2026, Calendar.NOVEMBER, 16, 20, 0)
        assertTrue(
            LockStateEvaluator.evaluate(outsideAllowed, ParentalControlPrefs(), profiles, utcZone).isBlocked,
        )
    }

    @Test
    fun `schedule bypass pauses scheduled blocks until pause timer expires`() {
        val profile = BlockingProfileEntity(
            id = 3L,
            name = "Bloqueio Diário",
            blockWifi = true,
            blockMobileData = true,
            isEnabled = true,
        )
        val rule = ScheduleRuleEntity(
            id = 30L,
            profileId = 3L,
            label = "Tarde",
            ruleMode = ScheduleRuleMode.BLOCK_WINDOW.key,
            daysMask = DayMask.ALL_DAYS,
            startMinutes = 14 * 60,
            endMinutes = 18 * 60,
            isEnabled = true,
        )
        val profiles = listOf(ProfileWithSchedules(profile, listOf(rule)))
        val now = epochFor(2026, Calendar.NOVEMBER, 16, 15, 0)

        val paused = LockStateEvaluator.evaluate(
            nowEpochMs = now,
            prefs = ParentalControlPrefs(scheduleBypassUntilEpochMs = now + 30 * 60_000L),
            profiles = profiles,
            timeZone = utcZone,
        )
        assertFalse(paused.isBlocked)
        assertTrue(paused.isScheduleBypassed)

        val resumed = LockStateEvaluator.evaluate(
            nowEpochMs = now + 31 * 60_000L,
            prefs = ParentalControlPrefs(scheduleBypassUntilEpochMs = now + 30 * 60_000L),
            profiles = profiles,
            timeZone = utcZone,
        )
        assertTrue(resumed.isBlocked)
        assertFalse(resumed.isScheduleBypassed)
    }
}
