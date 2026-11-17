package com.abhinav.dodaily.logic

import com.abhinav.dodaily.data.Completion
import com.abhinav.dodaily.data.Task
import java.time.LocalDate

data class DayItem(val task: Task, val done: Boolean)

data class TodayState(
    val date: LocalDate,
    val items: List<DayItem>,
    val streak: Int,
) {
    val doneCount: Int get() = items.count { it.done }
    val total: Int get() = items.size
    val remaining: List<Task> get() = items.filterNot { it.done }.map { it.task }
}

/** Pure scheduling rules. No Android code here, so it's easy to unit test. */
object Schedule {

    fun dayBit(date: LocalDate): Int = 1 shl (date.dayOfWeek.value - 1)

    private class Index(completions: List<Completion>) {
        val done = HashMap<Long, MutableSet<Long>>()
        val skipped = HashMap<Long, MutableSet<Long>>()

        init {
            for (c in completions) {
                val map = if (c.skipped) skipped else done
                map.getOrPut(c.taskId) { HashSet() }.add(c.day)
            }
        }

        fun isDone(taskId: Long, day: Long) = done[taskId]?.contains(day) == true
        fun isSkipped(taskId: Long, day: Long) = skipped[taskId]?.contains(day) == true
        fun doneBefore(taskId: Long, day: Long) = done[taskId]?.any { it < day } == true
    }

    private fun isDue(task: Task, date: LocalDate, index: Index): Boolean {
        val day = date.toEpochDay()
        val oneTime = task.oneTimeDay
        return if (oneTime != null) {
            // Shows on its day and carries over until it's done.
            day >= oneTime && !index.doneBefore(task.id, day)
        } else {
            day >= task.createdDay &&
                (task.daysMask and dayBit(date)) != 0 &&
                !index.isSkipped(task.id, day)
        }
    }

    fun isDue(task: Task, date: LocalDate, completions: List<Completion>): Boolean =
        isDue(task, date, Index(completions))

    fun itemsFor(date: LocalDate, tasks: List<Task>, completions: List<Completion>): List<DayItem> {
        val index = Index(completions)
        return itemsFor(date, tasks, index)
    }

    private fun itemsFor(date: LocalDate, tasks: List<Task>, index: Index): List<DayItem> {
        val day = date.toEpochDay()
        return tasks.filter { isDue(it, date, index) }
            .map { DayItem(it, index.isDone(it.id, day)) }
            .sortedBy { it.done } // unfinished first, stable keeps user order
    }

    /**
     * Consecutive days where everything due was done.
     * Today counts only once it's complete; days with nothing due don't break the streak.
     */
    fun streak(today: LocalDate, tasks: List<Task>, completions: List<Completion>): Int {
        if (tasks.isEmpty()) return 0
        val index = Index(completions)
        val earliest = tasks.minOf { it.oneTimeDay ?: it.createdDay }
        var count = 0

        val todayItems = itemsFor(today, tasks, index)
        if (todayItems.isNotEmpty() && todayItems.all { it.done }) count++

        var date = today.minusDays(1)
        while (date.toEpochDay() >= earliest && count < 3650) {
            val items = itemsFor(date, tasks, index)
            if (items.isNotEmpty()) {
                if (items.all { it.done }) count++ else break
            }
            date = date.minusDays(1)
        }
        return count
    }

    fun today(date: LocalDate, tasks: List<Task>, completions: List<Completion>) =
        TodayState(date, itemsFor(date, tasks, completions), streak(date, tasks, completions))
}
