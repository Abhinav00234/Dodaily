@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.abhinav.dodaily.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.abhinav.dodaily.App
import com.abhinav.dodaily.data.Task
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Small floating editor. Opened from the widget's "+" or a task title, and from the app.
 * Widgets can't take keyboard input, so this is the quickest way to type a task.
 */
class EditTaskActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        setContent { AppTheme { EditTaskSheet(taskId = id, onClose = { finish() }) } }
    }

    companion object {
        private const val EXTRA_ID = "task_id"

        fun intent(context: Context, taskId: Long?): Intent =
            Intent(context, EditTaskActivity::class.java).apply {
                // Unique data URI so each task gets its own PendingIntent from the widget.
                data = Uri.parse("dodaily://task/${taskId ?: "new"}")
                if (taskId != null) putExtra(EXTRA_ID, taskId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}

private enum class RepeatMode(val label: String) { DAILY("Daily"), WEEKDAYS("Weekdays"), CUSTOM("Pick days"), ONCE("Once") }

@Composable
private fun EditTaskSheet(taskId: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { App.repo(context) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val today = remember { LocalDate.now().toEpochDay() }

    var loaded by remember { mutableStateOf<Task?>(null) }
    var title by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(RepeatMode.DAILY) }
    var mask by remember { mutableIntStateOf(Task.ALL_DAYS) }
    var onceDay by remember { mutableLongStateOf(today) }
    var reminder by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(taskId) {
        if (taskId != 0L) {
            val t = repo.getTask(taskId)
            if (t == null) { onClose(); return@LaunchedEffect }
            loaded = t
            title = t.title
            reminder = t.reminderMinutes
            when {
                t.oneTimeDay != null -> { mode = RepeatMode.ONCE; onceDay = t.oneTimeDay }
                t.daysMask == Task.ALL_DAYS -> mode = RepeatMode.DAILY
                t.daysMask == Task.WEEKDAYS -> mode = RepeatMode.WEEKDAYS
                else -> { mode = RepeatMode.CUSTOM; mask = t.daysMask }
            }
        } else {
            focus.requestFocus()
            keyboard?.show()
        }
    }

    fun save() {
        if (title.isBlank()) return
        val base = loaded ?: Task(title = "")
        val task = base.copy(
            title = title.trim(),
            daysMask = when (mode) {
                RepeatMode.DAILY -> Task.ALL_DAYS
                RepeatMode.WEEKDAYS -> Task.WEEKDAYS
                RepeatMode.CUSTOM -> if (mask == 0) Task.ALL_DAYS else mask
                RepeatMode.ONCE -> Task.ALL_DAYS
            },
            oneTimeDay = if (mode == RepeatMode.ONCE) onceDay else null,
            reminderMinutes = reminder,
        )
        scope.launch { repo.save(task); onClose() }
    }

    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClose)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (loaded == null) "New task" else "Edit task",
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Gym, Study SDEV, Call mom") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )

                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatMode.entries.forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                    }
                }
                if (mode == RepeatMode.CUSTOM) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { d ->
                            val bit = 1 shl (d.value - 1)
                            FilterChip(
                                selected = mask and bit != 0,
                                onClick = { mask = mask xor bit },
                                label = { Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                            )
                        }
                    }
                }
                if (mode == RepeatMode.ONCE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = onceDay == today, onClick = { onceDay = today }, label = { Text("Today") })
                        FilterChip(selected = onceDay == today + 1, onClick = { onceDay = today + 1 }, label = { Text("Tomorrow") })
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reminder", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val start = reminder ?: (9 * 60)
                        TimePickerDialog(
                            context,
                            { _, h, m -> reminder = h * 60 + m },
                            start / 60, start % 60,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    }) { Text(reminder?.let { formatMinutes(context, it) } ?: "Add") }
                    if (reminder != null) TextButton(onClick = { reminder = null }) { Text("Clear") }
                }

                val existing = loaded
                if (existing != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { scope.launch { repo.delete(existing.id); onClose() } }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        if (existing.isOneTime) {
                            TextButton(onClick = { scope.launch { repo.moveToTomorrow(existing.id); onClose() } }) {
                                Text("Move to tomorrow")
                            }
                        } else {
                            TextButton(onClick = { scope.launch { repo.skipToday(existing.id); onClose() } }) {
                                Text("Skip today")
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Cancel") }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { save() }, enabled = title.isNotBlank()) { Text("Save") }
                }
            }
        }
    }
}
