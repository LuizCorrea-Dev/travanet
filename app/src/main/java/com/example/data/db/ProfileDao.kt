package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM blocking_profiles ORDER BY createdAt ASC")
    fun observeProfilesWithSchedules(): Flow<List<ProfileWithSchedules>>

    @Transaction
    @Query("SELECT * FROM blocking_profiles ORDER BY createdAt ASC")
    suspend fun getProfilesWithSchedulesOnce(): List<ProfileWithSchedules>

    @Transaction
    @Query("SELECT * FROM blocking_profiles WHERE id = :profileId")
    fun observeProfileById(profileId: Long): Flow<ProfileWithSchedules?>

    @Transaction
    @Query("SELECT * FROM blocking_profiles WHERE id = :profileId")
    suspend fun getProfileByIdOnce(profileId: Long): ProfileWithSchedules?

    @Query("SELECT COUNT(*) FROM blocking_profiles")
    suspend fun getProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BlockingProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: BlockingProfileEntity)

    @Query("UPDATE blocking_profiles SET isEnabled = :enabled WHERE id = :profileId")
    suspend fun setProfileEnabled(profileId: Long, enabled: Boolean)

    @Query("DELETE FROM blocking_profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ScheduleRuleEntity): Long

    @Update
    suspend fun updateRule(rule: ScheduleRuleEntity)

    @Query("UPDATE schedule_rules SET isEnabled = :enabled WHERE id = :ruleId")
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)

    @Query("DELETE FROM schedule_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)
}
