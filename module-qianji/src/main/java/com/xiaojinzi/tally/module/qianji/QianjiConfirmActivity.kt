package com.xiaojinzi.tally.module.qianji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortInlineField
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortStatusPill
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.AccountDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDo
import com.xiaojinzi.tally.module.qianji.autorecord.AutoRecordState
import com.xiaojinzi.tally.module.qianji.autorecord.PaymentNotifications
import com.xiaojinzi.tally.module.qianji.autorecord.PendingPayments
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal

/** 来自同一待确认队列，保存前必须核对真实账本和分类；不接受外部金额拼装入库。 */
class QianjiConfirmActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { PortTheme { Content() } } }
    }

    override fun onStart() {
        super.onStart()
        intent.getStringExtra(PaymentNotifications.EXTRA_PENDING_ID)?.let {
            AutoRecordState.confirmationStarted(it)
        }
    }

    override fun onStop() {
        intent.getStringExtra(PaymentNotifications.EXTRA_PENDING_ID)?.let {
            AutoRecordState.confirmationFinished(it)
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    @Composable
    private fun Content() {
        val id = intent.getStringExtra(PaymentNotifications.EXTRA_PENDING_ID).orEmpty()
        val pending = remember(id) { PendingPayments.find(this, id) }
        var error by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        var ready by remember { mutableStateOf(false) }
        var books by remember { mutableStateOf<List<BookDo>>(emptyList()) }
        var book by remember { mutableStateOf<BookDo?>(null) }
        var categories by remember { mutableStateOf<List<CategoryDo>>(emptyList()) }
        var accounts by remember { mutableStateOf<List<AccountDo>>(emptyList()) }
        var category by remember { mutableStateOf<CategoryDo?>(null) }
        var account by remember { mutableStateOf<AccountDo?>(null) }
        var selector by remember { mutableStateOf<String?>(null) }
        var income by remember { mutableStateOf(pending?.payment?.income ?: false) }
        var amount by remember { mutableStateOf(pending?.payment?.let { PaymentNotifications.money(it.amountFen) }.orEmpty()) }
        var note by remember { mutableStateOf(pending?.payment?.merchant.orEmpty()) }

        LaunchedEffect(id) {
            if (pending == null) return@LaunchedEffect
            try {
                check(withTimeoutOrNull(10_000) { AppServices.tallyDataSourceInitSpi.isInitStateOb.first { it } } == true) { "账本尚未准备好" }
                books = withContext(Dispatchers.IO) { TallyDb.database.bookDao().getAll() }
                val selected = AppServices.tallyDataSourceSpi.selectedBookStateOb.first()?.id
                book = books.find { it.id == selected } ?: books.firstOrNull()
                ready = true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "账本暂不可用，请先打开首页完成初始化。待确认记录已保留。"
            }
        }
        LaunchedEffect(book?.id, income) {
            val selected = book ?: return@LaunchedEffect
            category = null
            account = null
            try {
                val (availableCategories, availableAccounts) = withContext(Dispatchers.IO) {
                    TallyDb.database.categoryDao().getByBookId(selected.id)
                        .filter { !it.isDeleted && it.type == if (income) "income" else "spending" }
                        .sortedBy { it.sort } to
                            TallyDb.database.accountDao().getAll(selected.id).filterNot { it.isDeleted }
                }
                categories = availableCategories
                accounts = availableAccounts
                category = categories.firstOrNull { it.name == "其他" } ?: categories.firstOrNull()
                account = accounts.firstOrNull { it.isDefault }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { error = "读取分类或账户失败，请重试" }
        }
        PortPage(title = "核对账单", subtitle = if (pending?.synthetic == true) "自检记录 · 保存后会写入账本" else "确认后才会记入账本", onBack = { finish() }) {
            if (pending == null) {
                TransferStatus("这笔记录已处理或已过期")
                return@PortPage
            }
            if (!ready && error == null) TransferStatus("正在打开账本…", busy = true)
            error?.let { TransferStatus("暂时未能完成", it, error = true) }
            PortSectionTitle("账单信息")
            PortCard(contentPadding = PaddingValues(0.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(PortDesign.CardPadding),
                    horizontalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
                ) {
                    PortSecondaryButton(
                        text = if (!income) "✓ 支出" else "支出",
                        onClick = { income = false },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        color = if (!income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PortSecondaryButton(
                        text = if (income) "✓ 收入" else "收入",
                        onClick = { income = true },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        color = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PortDivider()
                PortInlineField(
                    label = "金额（元）",
                    value = amount,
                    onValueChange = { amount = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !busy,
                )
                PortDivider()
                PortInlineField(
                    label = "商户 / 备注",
                    value = note,
                    onValueChange = { note = it.take(500) },
                    singleLine = false,
                    enabled = !busy,
                )
            }
            PortSectionTitle("记入账本")
            PortCard(contentPadding = PaddingValues(0.dp)) {
                PortActionRow(title = "账本", subtitle = book?.name ?: "请选择", enabled = ready && !busy, onClick = { selector = "book" })
                PortDivider()
                PortActionRow(title = "分类", subtitle = category?.name ?: "请选择", enabled = ready && !busy, onClick = { selector = "category" })
                PortDivider()
                PortActionRow(title = "账户", subtitle = account?.name ?: "不关联账户", enabled = ready && !busy, onClick = { selector = "account" })
            }
            PortPrimaryButton(text = if (busy) "正在保存…" else "确认保存", modifier = Modifier.fillMaxWidth(), enabled = ready && book != null && category != null && !busy, onClick = {
                val selectedBook = book ?: return@PortPrimaryButton
                val selectedCategory = category ?: return@PortPrimaryButton
                val fen = runCatching { BigDecimal(amount.trim()).movePointRight(2).longValueExact() }.getOrNull()
                if (fen == null || fen <= 0 || fen > 100_000_000_000L) {
                    error = "请输入有效金额，最多两位小数"
                    return@PortPrimaryButton
                }
                busy = true
                error = null
                activityScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val database = TallyDb.database
                            database.withTransaction {
                                val billId = "auto_${pending.id}"
                                if (database.billDao().getById(billId) == null) {
                                    val latestBook = database.bookDao().getById(selectedBook.id)
                                        ?: throw IllegalStateException("账本已不存在")
                                    val latestCategory = database.categoryDao().getByIdAndBookId(selectedCategory.id, latestBook.id)
                                    require(latestCategory != null && !latestCategory.isDeleted) { "分类已不存在" }
                                    val selectedAccount = account?.let { database.accountDao().getAccountByIdAndBookId(it.id, latestBook.id) }
                                    require(selectedAccount?.isDeleted != true) { "账户已删除" }
                                    val now = System.currentTimeMillis()
                                    database.billDao().insert(BillDo(
                                        id = billId, userId = latestBook.userId, bookId = latestBook.id, type = "normal",
                                        originBillId = null, time = pending.payment.occurredAt, categoryId = latestCategory.id,
                                        accountId = selectedAccount?.id, transferTargetAccountId = null,
                                        amount = if (income) fen else -fen,
                                        note = (if (pending.synthetic) "[自检] " else "") + note,
                                        timeCreate = now, timeModify = now, isDeleted = false, isSync = false,
                                    ))
                                }
                            }
                        }
                        PendingPayments.remove(this@QianjiConfirmActivity, pending.id)
                        PaymentNotifications.cancel(this@QianjiConfirmActivity, pending.id)
                        AppServices.appWidgetSpi?.tryUpdateWidget()
                        finish()
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { error = "保存失败，记录已保留，请检查账本与分类后重试" }
                    finally { busy = false }
                }
            })
            PortSecondaryButton(text = "忽略这笔记录", onClick = {
                PendingPayments.remove(this@QianjiConfirmActivity, pending.id)
                PaymentNotifications.cancel(this@QianjiConfirmActivity, pending.id)
                finish()
            }, modifier = Modifier.fillMaxWidth(), enabled = !busy, destructive = true)
        }
        selector?.let { target ->
            AlertDialog(
                onDismissRequest = { selector = null },
                title = {
                    Text(
                        when (target) { "book" -> "选择账本"; "category" -> "选择分类"; else -> "选择账户" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                text = {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        when (target) {
                            "book" -> books.forEach { item ->
                                PortActionRow(
                                    title = item.name.orEmpty(),
                                    onClick = { book = item; selector = null },
                                    trailing = if (book?.id == item.id) ({ PortStatusPill("当前选择", emphasized = true) }) else null,
                                )
                            }
                            "category" -> categories.take(80).forEach { item ->
                                PortActionRow(
                                    title = item.name.orEmpty(),
                                    onClick = { category = item; selector = null },
                                    trailing = if (category?.id == item.id) ({ PortStatusPill("当前选择", emphasized = true) }) else null,
                                )
                            }
                            else -> {
                                PortActionRow(
                                    title = "不关联账户",
                                    onClick = { account = null; selector = null },
                                    trailing = if (account == null) ({ PortStatusPill("当前选择", emphasized = true) }) else null,
                                )
                                accounts.forEach { item ->
                                    PortActionRow(
                                        title = item.name.orEmpty(),
                                        onClick = { account = item; selector = null },
                                        trailing = if (account?.id == item.id) ({ PortStatusPill("当前选择", emphasized = true) }) else null,
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = { PortSecondaryButton(text = "取消", onClick = { selector = null }) },
            )
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
