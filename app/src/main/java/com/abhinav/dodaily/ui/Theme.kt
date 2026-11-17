package com.abhinav.dodaily.ui

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.abhinav.dodaily.data.Task
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val Brand = Color(0xFF2E7D5B)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF7FD8A8))
        else -> lightColorScheme(primary = Brand)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

fun formatMinutes(context: Context, minutes: Int): String {
    val time = LocalTime.of(minutes / 60, minutes % 60)
    val pattern = if (DateFormat.is24HourFormat(context)) DateTimeFormatter.ofPattern("HH:mm")
    else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return time.format(pattern)
}

fun repeatLabel(task: Task): String = when {
    task.isOneTime -> "Once"
    task.daysMask == Task.ALL_DAYS -> "Every day"
    task.daysMask == Task.WEEKDAYS -> "Weekdays"
    task.daysMask == Task.WEEKENDS -> "Weekends"
    else -> DayOfWeek.entries
        .filter { task.daysMask and (1 shl (it.value - 1)) != 0 }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}
