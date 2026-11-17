package com.abhinav.dodaily.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.abhinav.dodaily.data.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Every alarm is one-shot: when it fires, [AlarmReceiver] handles it and calls
 * [scheduleAll] again, which arms the next occurrence.
 */
object AlarmScheduler {

    const val TYPE_MORNING = "morning"
    const val TYPE_NUDGE = "nudge"
    const val TYPE_MIDNIGHT = "midnight"
    const val TYPE_TASK = "task"
    const val EXTRA_TYPE = "type"
    const val EXTRA_TASK_ID = "task_id"

    private const val RC_MORNING = 1
    private const val RC_NUDGE = 2
    private const val RC_MIDNIGHT = 3
    private const val RC_TASK_BASE = 1_000

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    fun scheduleAll(context: Context, tasks: List<Task>) {
        val prefs = ReminderPrefs.load(context)

        if (prefs.morningEnabled) set(context, RC_MORNING, TYPE_MORNING, null, nextAt(prefs.morningMinutes))
        else cancel(context, RC_MORNING, TYPE_MORNING, null)

        if (prefs.nudgeEnabled) set(context, RC_NUDGE, TYPE_NUDGE, null, nextAt(prefs.nudgeMinutes))
        else cancel(context, RC_NUDGE, TYPE_NUDGE, null)

        // Just after midnight: refresh the widget so it shows the new day.
        set(context, RC_MIDNIGHT, TYPE_MIDNIGHT, null, nextAt(0, plusSeconds = 5))

        for (task in tasks) {
            val minutes = task.reminderMinutes
            if (minutes != null) set(context, rcFor(task.id), TYPE_TASK, task.id, nextAt(minutes))
            else cancelTask(context, task.id)
        }
    }

    fun cancelTask(context: Context, taskId: Long) =
        cancel(context, rcFor(taskId), TYPE_TASK, taskId)

    private fun rcFor(taskId: Long) = RC_TASK_BASE + taskId.toInt()

    /** Next time the clock hits [minutes] after midnight, in epoch millis. */
    fun nextAt(minutes: Int, plusSeconds: Long = 0): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val time = LocalTime.of(minutes / 60, minutes % 60).plusSeconds(plusSeconds)
        var at = LocalDate.now(zone).atTime(time)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.atZone(zone).toInstant().toEpochMilli()
    }

    private fun intent(context: Context, rc: Int, type: String, taskId: Long?, flags: Int): PendingIntent? {
        val i = Intent(context, AlarmReceiver::class.java)
            .setAction("com.abhinav.dodaily.ALARM.$type.${taskId ?: 0}")
            .putExtra(EXTRA_TYPE, type)
        if (taskId != null) i.putExtra(EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(context, rc, i, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun set(context: Context, rc: Int, type: String, taskId: Long?, atMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = intent(context, rc, type, taskId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            // Without the exact-alarm permission Android may delay this a bit.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun cancel(context: Context, rc: Int, type: String, taskId: Long?) {
        val pi = intent(context, rc, type, taskId, PendingIntent.FLAG_NO_CREATE) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }
}
