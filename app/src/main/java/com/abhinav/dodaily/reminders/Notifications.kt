package com.abhinav.dodaily.reminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.abhinav.dodaily.MainActivity
import com.abhinav.dodaily.R
import com.abhinav.dodaily.data.Task

object Notifications {
    private const val CHANNEL = "reminders"
    private const val ID_SUMMARY = 1
    private const val ID_TASK_BASE = 10_000

    fun taskNotificationId(taskId: Long) = ID_TASK_BASE + taskId.toInt()

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Morning "here's your day" or evening "still left" summary. */
    fun showSummary(context: Context, title: String, tasks: List<Task>) {
        if (tasks.isEmpty()) return
        val style = NotificationCompat.InboxStyle()
        tasks.take(7).forEach { style.addLine("• ${it.title}") }
        if (tasks.size > 7) style.setSummaryText("+${tasks.size - 7} more")

        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(tasks.joinToString(", ") { it.title })
            .setStyle(style)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        post(context, ID_SUMMARY, n)
    }

    /** Single-task reminder with a "Mark done" button. */
    fun showTask(context: Context, task: Task) {
        val done = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_DONE)
                .putExtra(AlarmScheduler.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(context.getString(R.string.task_reminder_text))
            .setContentIntent(openApp(context))
            .addAction(0, context.getString(R.string.mark_done), done)
            .setAutoCancel(true)
            .build()
        post(context, taskNotificationId(task.id), n)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    @SuppressLint("MissingPermission") // checked via areNotificationsEnabled()
    private fun post(context: Context, id: Int, n: android.app.Notification) {
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) nm.notify(id, n)
    }
}
