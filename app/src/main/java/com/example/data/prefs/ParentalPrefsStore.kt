package com.example.data.prefs

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.parentalDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "travanet_parental_prefs",
)

data class ParentalControlPrefs(
    val hasPinConfigured: Boolean = false,
    val onboardingShieldSeen: Boolean = false,
    val isTotalLockActive: Boolean = false,
    val totalLockBlockWifi: Boolean = true,
    val totalLockBlockMobile: Boolean = true,
    val quickBlockEndEpochMs: Long = 0L,
    val quickBlockDurationMinutes: Int = 0,
    val quickBlockWifi: Boolean = true,
    val quickBlockMobile: Boolean = true,
    val scheduleBypassUntilEpochMs: Long = 0L,
    val antiTamperPinRequired: Boolean = true,
    val defaultsSeeded: Boolean = false,
)

class ParentalPrefsStore(private val context: Context) {
    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val ONBOARDING_SHIELD_SEEN = booleanPreferencesKey("onboarding_shield_seen")
        val TOTAL_LOCK_ACTIVE = booleanPreferencesKey("total_lock_active")
        val TOTAL_LOCK_WIFI = booleanPreferencesKey("total_lock_wifi")
        val TOTAL_LOCK_MOBILE = booleanPreferencesKey("total_lock_mobile")
        val QUICK_BLOCK_END_MS = longPreferencesKey("quick_block_end_ms")
        val QUICK_BLOCK_DURATION_MIN = intPreferencesKey("quick_block_duration_min")
        val QUICK_BLOCK_WIFI = booleanPreferencesKey("quick_block_wifi")
        val QUICK_BLOCK_MOBILE = booleanPreferencesKey("quick_block_mobile")
        val SCHEDULE_BYPASS_UNTIL_MS = longPreferencesKey("schedule_bypass_until_ms")
        val ANTI_TAMPER_PIN_REQUIRED = booleanPreferencesKey("anti_tamper_pin_required")
        val DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")
    }

    val prefsFlow: Flow<ParentalControlPrefs> = context.parentalDataStore.data.map { prefs ->
        ParentalControlPrefs(
            hasPinConfigured = !prefs[Keys.PIN_HASH].isNullOrEmpty(),
            onboardingShieldSeen = prefs[Keys.ONBOARDING_SHIELD_SEEN] ?: false,
            isTotalLockActive = prefs[Keys.TOTAL_LOCK_ACTIVE] ?: false,
            totalLockBlockWifi = prefs[Keys.TOTAL_LOCK_WIFI] ?: true,
            totalLockBlockMobile = prefs[Keys.TOTAL_LOCK_MOBILE] ?: true,
            quickBlockEndEpochMs = prefs[Keys.QUICK_BLOCK_END_MS] ?: 0L,
            quickBlockDurationMinutes = prefs[Keys.QUICK_BLOCK_DURATION_MIN] ?: 0,
            quickBlockWifi = prefs[Keys.QUICK_BLOCK_WIFI] ?: true,
            quickBlockMobile = prefs[Keys.QUICK_BLOCK_MOBILE] ?: true,
            scheduleBypassUntilEpochMs = prefs[Keys.SCHEDULE_BYPASS_UNTIL_MS] ?: 0L,
            antiTamperPinRequired = prefs[Keys.ANTI_TAMPER_PIN_REQUIRED] ?: true,
            defaultsSeeded = prefs[Keys.DEFAULTS_SEEDED] ?: false,
        )
    }

    suspend fun getPrefsOnce(): ParentalControlPrefs = prefsFlow.first()

    suspend fun setPin(pin: String) {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
        }
    }

    suspend fun markOnboardingShieldSeen() {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_SHIELD_SEEN] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.parentalDataStore.data.first()
        val storedHash = prefs[Keys.PIN_HASH] ?: return false
        val storedSalt = prefs[Keys.PIN_SALT] ?: return false
        return MessageDigest.isEqual(
            hashPin(pin, storedSalt).toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8),
        )
    }

    suspend fun setTotalLock(active: Boolean, blockWifi: Boolean = true, blockMobile: Boolean = true) {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.TOTAL_LOCK_ACTIVE] = active
            prefs[Keys.TOTAL_LOCK_WIFI] = blockWifi
            prefs[Keys.TOTAL_LOCK_MOBILE] = blockMobile
        }
    }

    suspend fun startQuickBlock(
        endEpochMs: Long,
        durationMinutes: Int,
        blockWifi: Boolean = true,
        blockMobile: Boolean = true,
    ) {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.QUICK_BLOCK_END_MS] = endEpochMs
            prefs[Keys.QUICK_BLOCK_DURATION_MIN] = durationMinutes
            prefs[Keys.QUICK_BLOCK_WIFI] = blockWifi
            prefs[Keys.QUICK_BLOCK_MOBILE] = blockMobile
        }
    }

    suspend fun clearQuickBlock() {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.QUICK_BLOCK_END_MS] = 0L
            prefs[Keys.QUICK_BLOCK_DURATION_MIN] = 0
        }
    }

    suspend fun unlockAllImmediate(scheduleBypassUntilEpochMs: Long) {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.TOTAL_LOCK_ACTIVE] = false
            prefs[Keys.QUICK_BLOCK_END_MS] = 0L
            prefs[Keys.QUICK_BLOCK_DURATION_MIN] = 0
            prefs[Keys.SCHEDULE_BYPASS_UNTIL_MS] = scheduleBypassUntilEpochMs
        }
    }

    suspend fun clearScheduleBypass() {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.SCHEDULE_BYPASS_UNTIL_MS] = 0L
        }
    }

    suspend fun setAntiTamperPinRequired(required: Boolean) {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.ANTI_TAMPER_PIN_REQUIRED] = required
        }
    }

    suspend fun markDefaultsSeeded() {
        context.parentalDataStore.edit { prefs ->
            prefs[Keys.DEFAULTS_SEEDED] = true
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin".toByteArray(Charsets.UTF_8)
        val hashed = digest.digest(input)
        return Base64.encodeToString(hashed, Base64.NO_WRAP)
    }
}
