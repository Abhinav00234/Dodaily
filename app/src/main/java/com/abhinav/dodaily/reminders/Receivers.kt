package com.abhinav.dodaily.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abhinav.dodaily.App
import com.abhinav.dodaily.R
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the receiver alive. */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    App.get(context).scope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync(context) {
        val repo = App.repo(context)
        val today = repo.today()

        when (intent.getStringExtra(AlarmScheduler.EXTRA_TYPE)) {
            AlarmScheduler.TYPE_MORNING -> Notifications.showSummary(
                context,
                context.getString(R.string.morning_title, today.total),
                today.remaining,
            )
            AlarmScheduler.TYPE_NUDGE -> Notifications.showSummary(
                context,
                context.getString(R.string.nudge_title, today.remaining.size),
                today.remaining,
            )
            AlarmScheduler.TYPE_TASK -> {
                val id = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1)
                // Only remind if it's actually on today's list and not done yet.
                today.remaining.firstOrNull { it.id == id }?.let { Notifications.showTask(context, it) }
            }
            AlarmScheduler.TYPE_MIDNIGHT -> Unit // changed() below refreshes the widget
        }
        repo.changed() // re-arms the next alarms and refreshes the widget
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1)
        if (id < 0) return
        Notifications.cancel(context, Notifications.taskNotificationId(id))
        runAsync(context) { App.repo(context).setDone(id, true) }
    }

    companion object {
        const val ACTION_DONE = "com.abhinav.dodaily.MARK_DONE"
    }
}

/** Alarms are wiped on reboot, app update, and clock/timezone changes. Re-arm them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync(context) {
        App.repo(context).changed()
    }
}
