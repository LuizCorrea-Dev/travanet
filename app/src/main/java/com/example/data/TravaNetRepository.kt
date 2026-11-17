package com.example.data

import android.content.Context
import com.example.data.db.BlockingProfileEntity
import com.example.data.db.ProfileWithSchedules
import com.example.data.db.ScheduleRuleEntity
import com.example.data.db.ScheduleRuleMode
import com.example.data.db.TravaNetDatabase
import com.example.data.prefs.ParentalControlPrefs
import com.example.data.prefs.ParentalPrefsStore
import com.example.domain.DayMask
import com.example.domain.EffectiveLockState
import com.example.domain.LockStateEvaluator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class TravaNetRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = TravaNetDatabase.getInstance(appContext)
    private val profileDao = database.profileDao()
    private val prefsStore = ParentalPrefsStore(appContext)

    val profilesFlow: Flow<List<ProfileWithSchedules>> = profileDao.observeProfilesWithSchedules()
    val prefsFlow: Flow<ParentalControlPrefs> = prefsStore.prefsFlow

    fun observeEffectiveLockState(tickerFlow: Flow<Long>): Flow<EffectiveLockState> {
        return combine(prefsFlow, profilesFlow, tickerFlow) { prefs, profiles, nowMs ->
            LockStateEvaluator.evaluate(nowMs, prefs, profiles)
        }
    }

    suspend fun evaluateCurrentLockState(nowMs: Long = System.currentTimeMillis()): EffectiveLockState {
        val prefs = prefsStore.getPrefsOnce()
        val profiles = profileDao.getProfilesWithSchedulesOnce()
        return LockStateEvaluator.evaluate(nowMs, prefs, profiles)
    }

    suspend fun ensureDefaultProfilesSeeded() {
        val prefs = prefsStore.getPrefsOnce()
        if (prefs.defaultsSeeded) return
        if (profileDao.getProfileCount() == 0) {
            val sleepProfileId = profileDao.insertProfile(
                BlockingProfileEntity(
                    name = "Hora de Dormir",
                    description = "Bloqueia Wi-Fi e dados móveis durante a noite",
                    iconKey = "bedtime",
                    blockWifi = true,
                    blockMobileData = true,
                    isEnabled = false,
                ),
            )
            profileDao.insertRule(
                ScheduleRuleEntity(
                    profileId = sleepProfileId,
                    label = "Noite • Dias úteis",
                    ruleMode = ScheduleRuleMode.BLOCK_WINDOW.key,
                    daysMask = DayMask.WEEKDAYS,
                    startMinutes = 22 * 60,
                    endMinutes = 7 * 60,
                    isEnabled = true,
                ),
            )

            val studyProfileId = profileDao.insertProfile(
                BlockingProfileEntity(
                    name = "Foco nos Estudos",
                    description = "Uso permitido apenas no fim da tarde",
                    iconKey = "school",
                    blockWifi = true,
                    blockMobileData = true,
                    isEnabled = false,
                ),
            )
            profileDao.insertRule(
                ScheduleRuleEntity(
                    profileId = studyProfileId,
                    label = "Uso liberado à tarde",
                    ruleMode = ScheduleRuleMode.ALLOW_WINDOW.key,
                    daysMask = DayMask.WEEKDAYS,
                    startMinutes = 17 * 60,
                    endMinutes = 20 * 60,
                    isEnabled = true,
                ),
            )
        }
        prefsStore.markDefaultsSeeded()
    }

    suspend fun setPin(pin: String) = prefsStore.setPin(pin)

    suspend fun markOnboardingShieldSeen() = prefsStore.markOnboardingShieldSeen()

    suspend fun verifyPin(pin: String): Boolean = prefsStore.verifyPin(pin)

    suspend fun setTotalLock(active: Boolean, blockWifi: Boolean = true, blockMobile: Boolean = true) {
        prefsStore.setTotalLock(active, blockWifi, blockMobile)
    }

    suspend fun startQuickBlock(
        durationMinutes: Int,
        blockWifi: Boolean = true,
        blockMobile: Boolean = true,
    ) {
        val endMs = System.currentTimeMillis() + durationMinutes * 60_000L
        prefsStore.startQuickBlock(endMs, durationMinutes, blockWifi, blockMobile)
    }

    suspend fun pauseProtection(durationMinutes: Int) {
        val pauseUntil = System.currentTimeMillis() + durationMinutes * 60_000L
        prefsStore.unlockAllImmediate(pauseUntil)
    }

    suspend fun unlockImmediate(bypassHours: Int = 2) {
        val now = System.currentTimeMillis()
        val profiles = profileDao.getProfilesWithSchedulesOnce()
        val prefs = prefsStore.getPrefsOnce()
        val currentlyScheduled = LockStateEvaluator.evaluate(
            nowEpochMs = now,
            prefs = prefs.copy(isTotalLockActive = false, quickBlockEndEpochMs = 0L, scheduleBypassUntilEpochMs = 0L),
            profiles = profiles,
        )
        val bypassUntil = if (currentlyScheduled.isBlocked) {
            now + bypassHours * 3_600_000L
        } else {
            0L
        }
        prefsStore.unlockAllImmediate(bypassUntil)
    }

    suspend fun clearScheduleBypass() = prefsStore.clearScheduleBypass()

    suspend fun setAntiTamperPinRequired(required: Boolean) =
        prefsStore.setAntiTamperPinRequired(required)

    suspend fun getProfileById(profileId: Long): ProfileWithSchedules? =
        profileDao.getProfileByIdOnce(profileId)

    fun observeProfileById(profileId: Long): Flow<ProfileWithSchedules?> =
        profileDao.observeProfileById(profileId)

    suspend fun createOrUpdateProfile(profile: BlockingProfileEntity): Long {
        return if (profile.id == 0L) {
            profileDao.insertProfile(profile)
        } else {
            profileDao.updateProfile(profile)
            profile.id
        }
    }

    suspend fun setProfileEnabled(profileId: Long, enabled: Boolean) {
        if (enabled) {
            prefsStore.clearScheduleBypass()
        }
        profileDao.setProfileEnabled(profileId, enabled)
    }

    suspend fun deleteProfile(profileId: Long) = profileDao.deleteProfileById(profileId)

    suspend fun saveRule(rule: ScheduleRuleEntity): Long {
        return if (rule.id == 0L) {
            profileDao.insertRule(rule)
        } else {
            profileDao.updateRule(rule)
            rule.id
        }
    }

    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        if (enabled) {
            prefsStore.clearScheduleBypass()
        }
        profileDao.setRuleEnabled(ruleId, enabled)
    }

    suspend fun deleteRule(ruleId: Long) = profileDao.deleteRuleById(ruleId)

    suspend fun getPrefsOnce(): ParentalControlPrefs = prefsFlow.first()
}
