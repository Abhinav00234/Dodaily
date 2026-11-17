package com.abhinav.dodaily.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.abhinav.dodaily.logic.Schedule
import com.abhinav.dodaily.logic.TodayState
import com.abhinav.dodaily.reminders.AlarmScheduler
import com.abhinav.dodaily.widget.TodayWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class TaskRepository(private val context: Context, private val dao: TaskDao) {

    fun observeTasks(): Flow<List<Task>> = dao.observeTasks()

    fun observeToday(): Flow<TodayState> =
        combine(dao.observeTasks(), dao.observeCompletions()) { tasks, completions ->
            Schedule.today(LocalDate.now(), tasks, completions)
        }

    suspend fun today(): TodayState =
        Schedule.today(LocalDate.now(), dao.getTasks(), dao.getCompletions())

    suspend fun getTask(id: Long): Task? = dao.getTask(id)

    suspend fun save(task: Task): Long {
        val id = if (task.id == 0L) {
            dao.insert(task.copy(sortOrder = (dao.maxSortOrder() ?: 0) + 1))
        } else {
            dao.update(task)
            task.id
        }
        changed()
        return id
    }

    suspend fun delete(id: Long) {
        AlarmScheduler.cancelTask(context, id)
        dao.delete(id)
        changed()
    }

    suspend fun setDone(id: Long, done: Boolean, day: Long = todayDay()) {
        if (done) dao.upsertCompletion(Completion(id, day)) else dao.deleteCompletion(id, day)
        changed()
    }

    suspend fun toggle(id: Long) {
        val day = todayDay()
        val done = dao.getCompletions().any { it.taskId == id && it.day == day && !it.skipped }
        setDone(id, !done, day)
    }

    /** Repeating task: hide it for today only. */
    suspend fun skipToday(id: Long) {
        dao.upsertCompletion(Completion(id, todayDay(), skipped = true))
        changed()
    }

    /** One-time task: push it to tomorrow. */
    suspend fun moveToTomorrow(id: Long) {
        val task = dao.getTask(id) ?: return
        dao.update(task.copy(oneTimeDay = todayDay() + 1))
        changed()
    }

    /** Refresh every widget and re-arm alarms after any change. */
    suspend fun changed() {
        TodayWidget().updateAll(context)
        AlarmScheduler.scheduleAll(context, dao.getTasks())
    }

    private fun todayDay() = LocalDate.now().toEpochDay()
}
