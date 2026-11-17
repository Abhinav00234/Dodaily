package com.abhinav.dodaily.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A task on the list.
 *
 * Repeating tasks use [daysMask]: bit 0 = Monday ... bit 6 = Sunday.
 * One-time tasks set [oneTimeDay] (epoch day) and carry over until done.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val daysMask: Int = ALL_DAYS,
    val oneTimeDay: Long? = null,
    /** Minutes after midnight for a per-task reminder, or null for none. */
    val reminderMinutes: Int? = null,
    val sortOrder: Int = 0,
    val createdDay: Long = LocalDate.now().toEpochDay(),
) {
    val isOneTime: Boolean get() = oneTimeDay != null

    companion object {
        const val ALL_DAYS = 0b1111111
        const val WEEKDAYS = 0b0011111
        const val WEEKENDS = 0b1100000
    }
}

/** One row per task per day it was ticked off (or skipped). */
@Entity(
    tableName = "completions",
    primaryKeys = ["taskId", "day"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class Completion(
    val taskId: Long,
    val day: Long,
    /** True when the user chose "Skip today" instead of doing it. */
    val skipped: Boolean = false,
)
