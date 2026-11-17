package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "blocking_profiles")
data class BlockingProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconKey: String = "shield",
    val blockWifi: Boolean = true,
    val blockMobileData: Boolean = true,
    val isEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ScheduleRuleMode(val key: String) {
    BLOCK_WINDOW("BLOCK_WINDOW"),
    ALLOW_WINDOW("ALLOW_WINDOW");

    companion object {
        fun fromKey(key: String): ScheduleRuleMode =
            entries.firstOrNull { it.key == key } ?: BLOCK_WINDOW
    }
}

@Entity(
    tableName = "schedule_rules",
    foreignKeys = [
        ForeignKey(
            entity = BlockingProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("profileId")],
)
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val label: String,
    val ruleMode: String = ScheduleRuleMode.BLOCK_WINDOW.key,
    val daysMask: Int = 0b1111111,
    val startMinutes: Int,
    val endMinutes: Int,
    val isEnabled: Boolean = true,
)

data class ProfileWithSchedules(
    @Embedded val profile: BlockingProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId",
    )
    val schedules: List<ScheduleRuleEntity>,
)
