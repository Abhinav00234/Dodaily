package com.abhinav.dodaily.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.abhinav.dodaily.App
import com.abhinav.dodaily.MainActivity
import com.abhinav.dodaily.logic.DayItem
import com.abhinav.dodaily.logic.TodayState
import com.abhinav.dodaily.ui.EditTaskActivity

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = App.repo(context)
        val initial = repo.today()
        provideContent {
            // Room emits on every change, so the widget live-updates while it's active.
            val flow = remember { repo.observeToday() }
            val state by flow.collectAsState(initial)
            GlanceTheme { Content(context, state) }
        }
    }

    @Composable
    private fun Content(context: Context, state: TodayState) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(12.dp),
        ) {
            Header(context, state)
            Spacer(GlanceModifier.height(6.dp))
            if (state.items.isEmpty()) {
                Text(
                    text = "Nothing due today. Tap + to add a task.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                    modifier = GlanceModifier.padding(top = 8.dp),
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    items(state.items, itemId = { it.task.id }) { item -> TaskRow(context, item) }
                }
            }
        }
    }

    @Composable
    private fun Header(context: Context, state: TodayState) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = GlanceModifier.defaultWeight()
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Text(
                    text = "Today",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                val streak = if (state.streak > 0) "  ·  🔥 ${state.streak}" else ""
                Text(
                    text = if (state.total == 0) "All clear$streak" else "${state.doneCount}/${state.total} done$streak",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(20.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(actionStartActivity(EditTaskActivity.intent(context, null))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }

    @Composable
    private fun TaskRow(context: Context, item: DayItem) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckBox(
                checked = item.done,
                onCheckedChange = actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(TASK_ID to item.task.id),
                ),
            )
            Text(
                text = item.task.title,
                maxLines = 2,
                style = TextStyle(
                    color = if (item.done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                ),
                // Tap the title to edit, delete, skip, or move to tomorrow.
                modifier = GlanceModifier.defaultWeight()
                    .padding(start = 4.dp, top = 6.dp, bottom = 6.dp)
                    .clickable(actionStartActivity(EditTaskActivity.intent(context, item.task.id))),
            )
        }
    }

    companion object {
        val TASK_ID = ActionParameters.Key<Long>("task_id")
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TodayWidget.TASK_ID] ?: return
        App.repo(context).toggle(id)
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
