package org.pilou.bettersony.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent

/** All widgets share one live [WidgetStore] state; each only differs by its layout. */
abstract class HeadsetWidget(private val content: @Composable (WidgetData) -> Unit) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (!WidgetStore.loaded) WidgetStore.load(context)
        provideContent {
            val data by WidgetStore.state.collectAsState()
            content(data)
        }
    }
}

class RingWidget : HeadsetWidget({ RingWidgetContent(it) })
class BigNumberWidget : HeadsetWidget({ BigNumberWidgetContent(it) })
class ModeBarWidget : HeadsetWidget({ ModeBarWidgetContent(it) })
class DashboardWidget : HeadsetWidget({ DashboardWidgetContent(it) })
class MiniBatteryWidget : HeadsetWidget({ MiniBatteryWidgetContent(it) })
class MiniModesWidget : HeadsetWidget({ MiniModesWidgetContent(it) })
class PowerWidget : HeadsetWidget({ PowerWidgetContent(it) })

val ALL_WIDGETS: List<GlanceAppWidget> = listOf(
    RingWidget(), BigNumberWidget(), ModeBarWidget(), DashboardWidget(), MiniBatteryWidget(), MiniModesWidget(), PowerWidget(),
)

class RingWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = RingWidget() }
class BigNumberWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = BigNumberWidget() }
class ModeBarWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ModeBarWidget() }
class DashboardWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DashboardWidget() }
class MiniBatteryWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MiniBatteryWidget() }
class MiniModesWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MiniModesWidget() }
class PowerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = PowerWidget() }
