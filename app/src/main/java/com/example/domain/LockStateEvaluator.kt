package com.example.domain

import com.example.data.db.ProfileWithSchedules
import com.example.data.db.ScheduleRuleEntity
import com.example.data.db.ScheduleRuleMode
import com.example.data.prefs.ParentalControlPrefs
import java.util.Calendar
import java.util.TimeZone

object DayMask {
    const val MONDAY = 1 shl 0
    const val TUESDAY = 1 shl 1
    const val WEDNESDAY = 1 shl 2
    const val THURSDAY = 1 shl 3
    const val FRIDAY = 1 shl 4
    const val SATURDAY = 1 shl 5
    const val SUNDAY = 1 shl 6

    const val WEEKDAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY
    const val WEEKENDS = SATURDAY or SUNDAY
    const val ALL_DAYS = WEEKDAYS or WEEKENDS

    val orderedBits = listOf(
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY,
    )

    val shortLabelsPt = listOf("S", "T", "Q", "Q", "S", "S", "D")
    val mediumLabelsPt = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    fun bitForCalendarDay(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
        Calendar.MONDAY -> MONDAY
        Calendar.TUESDAY -> TUESDAY
        Calendar.WEDNESDAY -> WEDNESDAY
        Calendar.THURSDAY -> THURSDAY
        Calendar.FRIDAY -> FRIDAY
        Calendar.SATURDAY -> SATURDAY
        Calendar.SUNDAY -> SUNDAY
        else -> MONDAY
    }

    fun yesterdayBitForCalendarDay(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
        Calendar.MONDAY -> SUNDAY
        Calendar.TUESDAY -> MONDAY
        Calendar.WEDNESDAY -> TUESDAY
        Calendar.THURSDAY -> WEDNESDAY
        Calendar.FRIDAY -> THURSDAY
        Calendar.SATURDAY -> FRIDAY
        Calendar.SUNDAY -> SATURDAY
        else -> SUNDAY
    }

    fun formatDaysSummary(daysMask: Int): String {
        return when (daysMask) {
            ALL_DAYS -> "Todos os dias"
            WEEKDAYS -> "Seg a Sex"
            WEEKENDS -> "Fins de semana"
            0 -> "Nenhum dia"
            else -> orderedBits.mapIndexedNotNull { index, bit ->
                if ((daysMask and bit) != 0) mediumLabelsPt[index] else null
            }.joinToString(", ")
        }
    }
}

sealed interface LockReason {
    data object Unlocked : LockReason
    data class TotalLock(
        val blockWifi: Boolean,
        val blockMobileData: Boolean,
    ) : LockReason

    data class QuickBlock(
        val endEpochMs: Long,
        val remainingMs: Long,
        val durationMinutes: Int,
        val blockWifi: Boolean,
        val blockMobileData: Boolean,
    ) : LockReason

    data class ScheduledBlock(
        val activeProfileNames: List<String>,
        val activeRuleLabels: List<String>,
    ) : LockReason
}

data class EffectiveLockState(
    val isBlocked: Boolean,
    val blockWifi: Boolean,
    val blockMobileData: Boolean,
    val reason: LockReason,
    val isScheduleBypassed: Boolean,
    val scheduleBypassRemainingMs: Long,
    val nextTransitionEpochMs: Long?,
    val nextScheduledLabel: String?,
)

object LockStateEvaluator {

    fun formatMinutesOfDay(minutes: Int): String {
        val clamped = ((minutes % 1440) + 1440) % 1440
        val h = clamped / 60
        val m = clamped % 60
        return "%02d:%02d".format(h, m)
    }

    fun evaluate(
        nowEpochMs: Long,
        prefs: ParentalControlPrefs,
        profiles: List<ProfileWithSchedules>,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): EffectiveLockState {
        val nextScheduledTransition = findNextScheduleTransition(nowEpochMs, profiles, timeZone)
        val bypassActive = prefs.scheduleBypassUntilEpochMs > nowEpochMs
        val bypassRemaining = if (bypassActive) prefs.scheduleBypassUntilEpochMs - nowEpochMs else 0L

        if (prefs.isTotalLockActive) {
            return EffectiveLockState(
                isBlocked = prefs.totalLockBlockWifi || prefs.totalLockBlockMobile,
                blockWifi = prefs.totalLockBlockWifi,
                blockMobileData = prefs.totalLockBlockMobile,
                reason = LockReason.TotalLock(
                    blockWifi = prefs.totalLockBlockWifi,
                    blockMobileData = prefs.totalLockBlockMobile,
                ),
                isScheduleBypassed = false,
                scheduleBypassRemainingMs = 0L,
                nextTransitionEpochMs = nextScheduledTransition?.first,
                nextScheduledLabel = nextScheduledTransition?.second,
            )
        }

        if (prefs.quickBlockEndEpochMs > nowEpochMs) {
            val remaining = prefs.quickBlockEndEpochMs - nowEpochMs
            val nextTransition = listOfNotNull(
                prefs.quickBlockEndEpochMs,
                nextScheduledTransition?.first,
            ).minOrNull()
            return EffectiveLockState(
                isBlocked = prefs.quickBlockWifi || prefs.quickBlockMobile,
                blockWifi = prefs.quickBlockWifi,
                blockMobileData = prefs.quickBlockMobile,
                reason = LockReason.QuickBlock(
                    endEpochMs = prefs.quickBlockEndEpochMs,
                    remainingMs = remaining,
                    durationMinutes = prefs.quickBlockDurationMinutes,
                    blockWifi = prefs.quickBlockWifi,
                    blockMobileData = prefs.quickBlockMobile,
                ),
                isScheduleBypassed = false,
                scheduleBypassRemainingMs = 0L,
                nextTransitionEpochMs = nextTransition,
                nextScheduledLabel = nextScheduledTransition?.second,
            )
        }

        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowEpochMs }
        val activeWifi = mutableListOf<String>()
        val activeMobile = mutableListOf<String>()
        val activeProfiles = mutableSetOf<String>()
        val activeRuleLabels = mutableListOf<String>()

        if (!bypassActive) {
            for (item in profiles) {
                if (!item.profile.isEnabled) continue
                val matchingRules = item.schedules.filter { rule ->
                    rule.isEnabled && isRuleBlockingAt(rule, cal)
                }
                if (matchingRules.isNotEmpty() && (item.profile.blockWifi || item.profile.blockMobileData)) {
                    activeProfiles.add(item.profile.name)
                    matchingRules.forEach { r ->
                        activeRuleLabels.add(r.label.ifBlank { item.profile.name })
                    }
                    if (item.profile.blockWifi) activeWifi.add(item.profile.name)
                    if (item.profile.blockMobileData) activeMobile.add(item.profile.name)
                }
            }
        }

        val blockWifi = activeWifi.isNotEmpty()
        val blockMobile = activeMobile.isNotEmpty()
        val isBlocked = blockWifi || blockMobile

        val nextWake = listOfNotNull(
            if (bypassActive) prefs.scheduleBypassUntilEpochMs else null,
            nextScheduledTransition?.first,
        ).minOrNull()

        return EffectiveLockState(
            isBlocked = isBlocked,
            blockWifi = blockWifi,
            blockMobileData = blockMobile,
            reason = if (isBlocked) {
                LockReason.ScheduledBlock(
                    activeProfileNames = activeProfiles.toList(),
                    activeRuleLabels = activeRuleLabels.distinct(),
                )
            } else {
                LockReason.Unlocked
            },
            isScheduleBypassed = bypassActive,
            scheduleBypassRemainingMs = bypassRemaining,
            nextTransitionEpochMs = nextWake,
            nextScheduledLabel = nextScheduledTransition?.second,
        )
    }

    fun isRuleBlockingAt(rule: ScheduleRuleEntity, cal: Calendar): Boolean {
        val inWindow = isInsideWindow(rule, cal)
        return when (ScheduleRuleMode.fromKey(rule.ruleMode)) {
            ScheduleRuleMode.BLOCK_WINDOW -> inWindow
            ScheduleRuleMode.ALLOW_WINDOW -> !inWindow
        }
    }

    fun isInsideWindow(rule: ScheduleRuleEntity, cal: Calendar): Boolean {
        if (rule.daysMask == 0) return false
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val todayBit = DayMask.bitForCalendarDay(dayOfWeek)
        val yesterdayBit = DayMask.yesterdayBitForCalendarDay(dayOfWeek)
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val start = ((rule.startMinutes % 1440) + 1440) % 1440
        val end = ((rule.endMinutes % 1440) + 1440) % 1440

        return when {
            start == end -> (rule.daysMask and todayBit) != 0
            start < end -> {
                (rule.daysMask and todayBit) != 0 && minuteOfDay in start until end
            }
            else -> {
                val activeFromToday = (rule.daysMask and todayBit) != 0 && minuteOfDay >= start
                val activeFromYesterday = (rule.daysMask and yesterdayBit) != 0 && minuteOfDay < end
                activeFromToday || activeFromYesterday
            }
        }
    }

    private fun findNextScheduleTransition(
        nowEpochMs: Long,
        profiles: List<ProfileWithSchedules>,
        timeZone: TimeZone,
    ): Pair<Long, String>? {
        val enabledRules = profiles.filter { it.profile.isEnabled }
            .flatMap { profileItem ->
                profileItem.schedules.filter { it.isEnabled }.map { profileItem.profile to it }
            }
        if (enabledRules.isEmpty()) return null

        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Collect candidate minute boundaries across next 7 days
        var earliestEpoch: Long? = null
        var earliestLabel: String? = null

        val baseDayStart = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (dayOffset in 0..7) {
            val dayCal = (baseDayStart.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            for ((profile, rule) in enabledRules) {
                val boundaries = listOf(rule.startMinutes, rule.endMinutes).distinct()
                for (minuteBoundary in boundaries) {
                    val candidate = (dayCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, minuteBoundary / 60)
                        set(Calendar.MINUTE, minuteBoundary % 60)
                    }
                    val candidateMs = candidate.timeInMillis
                    if (candidateMs <= nowEpochMs) continue

                    val beforeCal = (candidate.clone() as Calendar).apply {
                        add(Calendar.MINUTE, -1)
                    }
                    val wasBlocking = isRuleBlockingAt(rule, beforeCal)
                    val willBlock = isRuleBlockingAt(rule, candidate)
                    if (wasBlocking != willBlock) {
                        if (earliestEpoch == null || candidateMs < earliestEpoch) {
                            earliestEpoch = candidateMs
                            val timeStr = formatMinutesOfDay(minuteBoundary)
                            val dayPrefix = when (dayOffset) {
                                0 -> "hoje"
                                1 -> "amanhã"
                                else -> DayMask.mediumLabelsPt[
                                    DayMask.orderedBits.indexOf(
                                        DayMask.bitForCalendarDay(candidate.get(Calendar.DAY_OF_WEEK)),
                                    ).coerceAtLeast(0),
                                ]
                            }
                            earliestLabel = if (willBlock) {
                                "Próximo bloqueio ($dayPrefix às $timeStr) • ${profile.name}"
                            } else {
                                "Liberação prevista ($dayPrefix às $timeStr) • ${profile.name}"
                            }
                        }
                    }
                }
            }
        }

        return if (earliestEpoch != null && earliestLabel != null) {
            earliestEpoch to earliestLabel
        } else {
            null
        }
    }
}
