@file:OptIn(ExperimentalMaterial3Api::class)

package com.abhinav.dodaily

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.abhinav.dodaily.data.Task
import com.abhinav.dodaily.logic.TodayState
import com.abhinav.dodaily.reminders.AlarmScheduler
import com.abhinav.dodaily.reminders.ReminderPrefs
import com.abhinav.dodaily.ui.AppTheme
import com.abhinav.dodaily.ui.EditTaskActivity
import com.abhinav.dodaily.ui.formatMinutes
import com.abhinav.dodaily.ui.repeatLabel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppTheme { HomeScreen() } }
    }
}

@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val repo = remember { App.repo(context) }
    val scope = rememberCoroutineScope()
    val todayFlow = remember { repo.observeToday() }
    val tasksFlow = remember { repo.observeTasks() }
    val today by todayFlow.collectAsState(TodayState(LocalDate.now(), emptyList(), 0))
    val tasks by tasksFlow.collectAsState(emptyList())

    // Bumped on every resume so permission banners update after visiting Settings.
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resumeTick++ }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("DoDaily") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { context.startActivity(EditTaskActivity.intent(context, null)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add task") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { PermissionBanners(resumeTick) }
            item { TodayHeader(today) }
            items(today.items, key = { "today-${it.task.id}" }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { context.startActivity(EditTaskActivity.intent(context, item.task.id)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { checked -> scope.launch { repo.setDone(item.task.id, checked) } },
                    )
                    Text(
                        item.task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("All tasks", style = MaterialTheme.typography.titleMedium)
            }
            if (tasks.isEmpty()) {
                item { Text("No tasks yet. Tap “Add task” to start your list.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(tasks, key = { "all-${it.id}" }) { task -> TaskCard(task) }
            item { ReminderSettings() }
        }
    }
}

@Composable
private fun TodayHeader(today: TodayState) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (today.streak > 0) Text("🔥 ${today.streak} day streak", style = MaterialTheme.typography.labelLarge)
        }
        if (today.total > 0) {
            Text("${today.doneCount} of ${today.total} done", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { today.doneCount.toFloat() / today.total },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text("Nothing due today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskCard(task: Task) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable { context.startActivity(EditTaskActivity.intent(context, task.id)) },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge)
            val reminder = task.reminderMinutes?.let { "  ·  ⏰ ${formatMinutes(context, it)}" } ?: ""
            Text(
                repeatLabel(task) + reminder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionBanners(@Suppress("UNUSED_PARAMETER") resumeTick: Int) {
    val context = LocalContext.current
    var notifGranted by remember(resumeTick) { mutableStateOf(hasNotificationPermission(context)) }
    val exactOk = remember(resumeTick) { AlarmScheduler.canScheduleExact(context) }
    val batteryOk = remember(resumeTick) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!notifGranted) Banner("Turn on notifications so DoDaily can remind you.", "Allow") {
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Banner("Allow exact alarms so reminders arrive right on time.", "Allow") {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                )
            }
        }
        if (!batteryOk) Banner("Some phones (Samsung, Xiaomi…) kill reminders to save battery. Set DoDaily to “Unrestricted”.", "Settings") {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}

private fun hasNotificationPermission(context: Context) =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun ReminderSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prefs by remember { mutableStateOf(ReminderPrefs.load(context)) }

    fun update(p: ReminderPrefs) {
        prefs = p
        ReminderPrefs.save(context, p)
        scope.launch { App.repo(context).changed() }
    }

    fun pick(current: Int, onPicked: (Int) -> Unit) {
        val t = LocalTime.of(current / 60, current % 60)
        TimePickerDialog(context, { _, h, m -> onPicked(h * 60 + m) }, t.hour, t.minute, DateFormat.is24HourFormat(context)).show()
    }

    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(Modifier.padding(bottom = 8.dp))
        Text("Daily reminders", style = MaterialTheme.typography.titleMedium)
        SettingRow(
            label = "Morning list",
            time = formatMinutes(context, prefs.morningMinutes),
            enabled = prefs.morningEnabled,
            onToggle = { update(prefs.copy(morningEnabled = it)) },
            onTime = { pick(prefs.morningMinutes) { m -> update(prefs.copy(morningMinutes = m)) } },
        )
        SettingRow(
            label = "Evening nudge (if unfinished)",
            time = formatMinutes(context, prefs.nudgeMinutes),
            enabled = prefs.nudgeEnabled,
            onToggle = { update(prefs.copy(nudgeEnabled = it)) },
            onTime = { pick(prefs.nudgeMinutes) { m -> update(prefs.copy(nudgeMinutes = m)) } },
        )
    }
}

@Composable
private fun SettingRow(label: String, time: String, enabled: Boolean, onToggle: (Boolean) -> Unit, onTime: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = onTime, enabled = enabled) { Text(time) }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
