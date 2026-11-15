# Glance finds placed widgets by their GlanceAppWidget class; merged classes render the wrong layout.
-keep class org.pilou.bettersony.widget.** extends androidx.glance.appwidget.GlanceAppWidget { *; }
