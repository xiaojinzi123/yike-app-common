package com.xiaojinzi.tally.module.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** Process-local Room subscriptions improve latency; WorkManager survives process death/reboot. */
internal object WidgetRefreshScheduler {
    private const val PERIODIC = "qianji.widgets.periodic.v2"
    private const val REFRESH = "qianji.widgets.refresh.v2"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val updateMutex = Mutex()
    private var observer: Job? = null

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(
            QianjiBudgetWidgetReceiver::class.java,
            AppDayStatisticsWidgetReceiver::class.java,
            AppMonthStatisticsWidgetReceiver::class.java,
        ).any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
    }

    @Synchronized
    @OptIn(FlowPreview::class)
    fun ensureScheduled(context: Context) {
        val app = context.applicationContext
        if (!hasWidgets(app)) {
            cancel(app)
            return
        }
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
        if (observer?.isActive != true) {
            // No service, wake lock or network. This subscription ends with the app process,
            // and is cancelled as soon as the last widget is removed.
            observer = scope.launch {
                AppWidgetDataSource.observe()
                    .debounce(250L)
                    .conflate()
                    .collect {
                        if (hasWidgets(app)) {
                            try {
                                updateWidgets(app)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                Log.w("WidgetRefresh", "Widget render deferred to next refresh")
                            }
                        } else {
                            cancel(app)
                        }
                    }
            }
        }
    }

    fun requestRefresh(context: Context) {
        val app = context.applicationContext
        ensureScheduled(app)
        if (hasWidgets(app)) {
            // KEEP coalesces requests while the running worker reads a fresh snapshot.
            WorkManager.getInstance(app).enqueueUniqueWork(
                REFRESH, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }

    @Synchronized
    fun cancel(context: Context) {
        observer?.cancel()
        observer = null
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(REFRESH)
    }

    suspend fun updateWidgets(context: Context) = updateMutex.withLock {
        // Only this package's three providers, preserving all existing appWidgetIds.
        QianjiBudgetWidget().updateAll(context)
        AppDayStatisticsWidget().updateAll(context)
        AppMonthStatisticsWidget().updateAll(context)
    }
}

/** Each execution reads one bounded local snapshot and returns; it never becomes foreground. */
class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!WidgetRefreshScheduler.hasWidgets(applicationContext)) return Result.success()
        return try {
            val completed = withTimeoutOrNull(20_000L) {
                // Each provider reads a fresh, bounded snapshot before provideContent.
                WidgetRefreshScheduler.updateWidgets(applicationContext)
                true
            } == true
            if (completed) Result.success() else retryOrFinish()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w("WidgetRefresh", "Widget refresh will retry with backoff")
            retryOrFinish()
        }
    }

    private fun retryOrFinish(): Result = if (runAttemptCount < 3) Result.retry() else Result.failure()
}
