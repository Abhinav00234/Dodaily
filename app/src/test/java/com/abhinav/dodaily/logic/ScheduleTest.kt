package com.abhinav.dodaily.logic

import com.abhinav.dodaily.data.Completion
import com.abhinav.dodaily.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleTest {
    // Monday, Nov 16 2026
    private val mon = LocalDate.of(2026, 11, 16)
    private val start = mon.minusDays(30).toEpochDay()

    private fun task(id: Long, mask: Int = Task.ALL_DAYS, once: Long? = null) =
        Task(id = id, title = "t$id", daysMask = mask, oneTimeDay = once, createdDay = start)

    @Test fun dailyTaskIsDueEveryDay() {
        val t = task(1)
        (0L..6L).forEach { assertTrue(Schedule.isDue(t, mon.plusDays(it), emptyList())) }
    }

    @Test fun weekdayTaskSkipsWeekend() {
        val t = task(1, Task.WEEKDAYS)
        assertTrue(Schedule.isDue(t, mon, emptyList()))
        assertFalse(Schedule.isDue(t, mon.plusDays(5), emptyList())) // Saturday
    }

    @Test fun notDueBeforeCreated() {
        val t = task(1).copy(createdDay = mon.toEpochDay())
        assertFalse(Schedule.isDue(t, mon.minusDays(1), emptyList()))
    }

    @Test fun skippedTodayHidesTask() {
        val t = task(1)
        val c = listOf(Completion(1, mon.toEpochDay(), skipped = true))
        assertFalse(Schedule.isDue(t, mon, c))
        assertTrue(Schedule.isDue(t, mon.plusDays(1), c))
    }

    @Test fun oneTimeCarriesOverUntilDone() {
        val t = task(1, once = mon.toEpochDay())
        assertFalse(Schedule.isDue(t, mon.minusDays(1), emptyList()))
        assertTrue(Schedule.isDue(t, mon.plusDays(2), emptyList()))
        val done = listOf(Completion(1, mon.plusDays(1).toEpochDay()))
        assertTrue(Schedule.isDue(t, mon.plusDays(1), done)) // shows as done that day
        assertFalse(Schedule.isDue(t, mon.plusDays(2), done)) // gone after
    }

    @Test fun itemsSortUnfinishedFirst() {
        val tasks = listOf(task(1), task(2))
        val items = Schedule.itemsFor(mon, tasks, listOf(Completion(1, mon.toEpochDay())))
        assertEquals(listOf(2L, 1L), items.map { it.task.id })
        assertTrue(items[1].done)
    }

    @Test fun streakCountsBackUntilMiss() {
        val tasks = listOf(task(1))
        val c = (1L..3L).map { Completion(1, mon.minusDays(it).toEpochDay()) }
        assertEquals(3, Schedule.streak(mon, tasks, c)) // today not done yet: still 3
        assertEquals(4, Schedule.streak(mon, tasks, c + Completion(1, mon.toEpochDay())))
    }

    @Test fun daysWithNothingDueDontBreakStreak() {
        val tasks = listOf(task(1, Task.WEEKDAYS))
        // Fri done, Sat/Sun nothing due, Mon today done
        val fri = mon.minusDays(3).toEpochDay()
        val thu = mon.minusDays(4).toEpochDay()
        val c = listOf(Completion(1, thu), Completion(1, fri), Completion(1, mon.toEpochDay()))
        assertEquals(3, Schedule.streak(mon, tasks, c))
    }
}
