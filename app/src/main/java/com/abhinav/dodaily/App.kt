package com.abhinav.dodaily

import android.app.Application
import android.content.Context
import com.abhinav.dodaily.data.AppDatabase
import com.abhinav.dodaily.data.TaskRepository
import com.abhinav.dodaily.reminders.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    /** App-wide scope for fire-and-forget work (alarm re-arming, widget refresh). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: TaskRepository by lazy {
        TaskRepository(this, AppDatabase.create(this).taskDao())
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        scope.launch { repository.changed() }
    }

    companion object {
        fun get(context: Context): App = context.applicationContext as App
        fun repo(context: Context): TaskRepository = get(context).repository
    }
}
