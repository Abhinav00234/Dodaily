package com.abhinav.dodaily.reminders

import android.content.Context

/** Daily summary settings. Times are minutes after midnight. */
data class ReminderPrefs(
    val morningEnabled: Boolean = true,
    val morningMinutes: Int = 8 * 60,
    val nudgeEnabled: Boolean = true,
    val nudgeMinutes: Int = 20 * 60,
) {
    companion object {
        private const val FILE = "reminder_prefs"

        fun load(context: Context): ReminderPrefs {
            val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            val d = ReminderPrefs()
            return ReminderPrefs(
                morningEnabled = p.getBoolean("morning_enabled", d.morningEnabled),
                morningMinutes = p.getInt("morning_minutes", d.morningMinutes),
                nudgeEnabled = p.getBoolean("nudge_enabled", d.nudgeEnabled),
                nudgeMinutes = p.getInt("nudge_minutes", d.nudgeMinutes),
            )
        }

        fun save(context: Context, prefs: ReminderPrefs) {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putBoolean("morning_enabled", prefs.morningEnabled)
                .putInt("morning_minutes", prefs.morningMinutes)
                .putBoolean("nudge_enabled", prefs.nudgeEnabled)
                .putInt("nudge_minutes", prefs.nudgeMinutes)
                .apply()
        }
    }
}
