package org.pilou.bettersony.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.pilou.bettersony.widget.WidgetStore

/**
 * Short expedited work that must finish even with the app closed: restoring settings when
 * the headphones connect, and turning them off from a widget or the Quick Settings tile.
 */
class HeadsetJob : JobService() {
    enum class Task(val id: Int) { ON_CONNECT(43), POWER_OFF(44) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val task = Task.entries.firstOrNull { it.name == params.extras.getString(KEY_TASK) } ?: return false
        work = scope.launch {
            runCatching { run(task) }.onFailure { Log.w(TAG, "$task failed", it) }
            jobFinished(params, false)
        }
        return true
    }

    private suspend fun run(task: Task) {
        val app = applicationContext
        when (task) {
            Task.ON_CONNECT -> Headsets.onHeadsetConnected(app)
            Task.POWER_OFF -> {
                val ok = Headsets.powerOff(app)
                Log.i(TAG, "power off sent: $ok")
                WidgetStore.patch(app) { it.copy(turningOff = false) }
                WidgetStore.refresh(app)
            }
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BetterSony"
        private const val KEY_TASK = "task"

        fun schedule(context: Context, task: Task) {
            val result = context.getSystemService(JobScheduler::class.java).schedule(
                JobInfo.Builder(task.id, ComponentName(context, HeadsetJob::class.java))
                    .setExpedited(true)
                    .setExtras(PersistableBundle().apply { putString(KEY_TASK, task.name) })
                    .build(),
            )
            Log.i(TAG, "$task scheduled: ${result == JobScheduler.RESULT_SUCCESS}")
        }
    }
}
