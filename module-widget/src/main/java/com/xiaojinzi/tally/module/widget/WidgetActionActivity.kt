package com.xiaojinzi.tally.module.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import androidx.lifecycle.lifecycleScope
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.support.init.BootView
import com.xiaojinzi.support.ktx.await
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.YearMonth

/** Glance uses an explicit Activity PendingIntent. No broadcast starts an Activity. */
@BootView
class WidgetActionActivity : ComponentActivity() {

    private var busy by mutableStateOf(true)
    private var message by mutableStateOf("正在打开本地账本…")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                PortTheme {
                    PortPage(title = "打开账本", onBack = { finish() }) {
                        PortCard {
                            Column(verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap)) {
                                if (busy) CircularProgressIndicator()
                                Text(message, style = MaterialTheme.typography.bodyLarge)
                                if (!busy) {
                                    PortPrimaryButton("重试", onClick = { openDestination() })
                                    TextButton(onClick = { openLauncher() }) { Text("打开应用") }
                                }
                            }
                        }
                    }
                }
            }
        }
        openDestination()
    }

    private fun openDestination() {
        busy = true
        message = "正在打开本地账本…"
        lifecycleScope.launch {
            try {
                val destination = intent.getStringExtra(EXTRA_DESTINATION)
                if (destination == OPEN_APP) {
                    WidgetRefreshScheduler.requestRefresh(applicationContext)
                    openLauncher()
                    return@launch
                }
                require(destination == BUDGET || destination == ADD_BILL)
                // BaseApplication restores the local database; never obtain the data source first.
                val ready = withTimeoutOrNull(10_000L) {
                    AppServices.tallyDataSourceInitSpi.isInitStateOb.first { it }
                } == true
                if (!ready) {
                    message = "本地账本尚未就绪。请打开应用完成初始化，再返回小部件。"
                    return@launch
                }
                val source = AppServices.tallyDataSourceSpi
                val requestedId = intent.getStringExtra(EXTRA_BOOK_ID)?.takeIf { it.isNotBlank() }
                val book = withTimeoutOrNull(5_000L) {
                    if (requestedId == null) source.selectedBookStateOb.first()
                    else source.getBookById(requestedId)
                }
                if (book == null) {
                    message = "请先在应用中选择一个可用账本，再使用小部件。"
                    return@launch
                }
                WidgetRefreshScheduler.ensureScheduled(applicationContext)
                if (destination == BUDGET) {
                    val month = intent.getStringExtra(EXTRA_MONTH)
                        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                        ?: YearMonth.now()
                    Router.with(context = this@WidgetActionActivity)
                        .hostAndPath(hostAndPath = AppRouterConfig.QIANJI_BUDGET)
                        .putString("bookId", book.id)
                        .putString("month", month.toString())
                        .await()
                } else {
                    Router.with(context = this@WidgetActionActivity)
                        .hostAndPath(hostAndPath = AppRouterConfig.CORE_BILL_CURD)
                        .putString("initBookId", book.id)
                        .await()
                }
                finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "暂时无法打开页面，请重试或打开应用。"
            } finally {
                busy = false
            }
        }
    }

    private fun openLauncher() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                ?: error("No launcher")
            startActivity(launchIntent)
            finish()
        } catch (_: Exception) {
            busy = false
            message = "暂时无法打开应用，请从桌面应用图标进入。"
        }
    }

    companion object {
        internal const val BUDGET = "budget"
        internal const val ADD_BILL = "add_bill"
        internal const val OPEN_APP = "open_app"
        private const val EXTRA_DESTINATION = "widgetDestination"
        private const val EXTRA_BOOK_ID = "bookId"
        private const val EXTRA_MONTH = "month"

        internal fun action(
            context: Context,
            destination: String,
            snapshot: WidgetSnapshot,
        ): Action = actionStartActivity(
            Intent(context, WidgetActionActivity::class.java).apply {
                // Extras alone do not distinguish PendingIntents.
                data = Uri.Builder().scheme("qianji-widget").authority("local")
                    .appendPath(destination)
                    .appendQueryParameter("book", snapshot.bookId.orEmpty())
                    .appendQueryParameter("month", snapshot.month)
                    .build()
                putExtra(EXTRA_DESTINATION, destination)
                putExtra(EXTRA_BOOK_ID, snapshot.bookId)
                putExtra(EXTRA_MONTH, snapshot.month)
            },
        )
    }
}
