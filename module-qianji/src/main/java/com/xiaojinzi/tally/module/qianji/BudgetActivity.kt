package com.xiaojinzi.tally.module.qianji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortCardExpand
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortInlineField
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/** 按目标账本和月份读写现有 Room v4 budget 表，不创建或切换全局账本。 */
@RouterAnno(hostAndPath = AppRouterConfig.QIANJI_BUDGET)
class BudgetActivity : ComponentActivity() {

    private var bookId: String? = null
    private var month by mutableStateOf(YearMonth.now())
    private var data by mutableStateOf<BudgetSnapshot?>(null)
    private var busy by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)
    private var notice by mutableStateOf<String?>(null)
    private var editor by mutableStateOf<BudgetEditor?>(null)
    private var deleting by mutableStateOf<BudgetDo?>(null)
    private var pickCategory by mutableStateOf(false)
    private var pickMonth by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bookId = (savedInstanceState?.getString(EXTRA_BOOK_ID) ?: intent.getStringExtra(EXTRA_BOOK_ID))
            ?.takeIf { it.isNotBlank() }
        val requestedMonth = savedInstanceState?.getString(EXTRA_MONTH) ?: intent.getStringExtra(EXTRA_MONTH)
        month = parseMonth(requestedMonth) ?: YearMonth.now()
        if (requestedMonth != null && parseMonth(requestedMonth) == null) {
            notice = "月份格式应为 yyyy-MM，已显示本月。"
        }
        setContent {
            AppTheme {
                PortTheme {
                    BudgetScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!busy) loadBudget()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_BOOK_ID, bookId)
        outState.putString(EXTRA_MONTH, month.toString())
        super.onSaveInstanceState(outState)
    }

    private fun loadBudget() {
        if (busy) return
        busy = true
        error = null
        val requestedMonth = month
        data = null
        lifecycleScope.launch {
            try {
                val id = bookId ?: AppServices.tallyDataSourceSpi.selectedBookStateOb.first()?.id
                if (id == null) return@launch
                bookId = id
                data = withContext(Dispatchers.IO) {
                    val db = TallyDb.database
                    val book = db.bookDao().getById(id) ?: error("账本不存在，请返回选择账本")
                    val start = requestedMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val end = requestedMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val key = requestedMonth.toString()
                    val categories = db.categoryDao().getByBookId(id)
                    BudgetSnapshot(
                        book = book,
                        total = db.budgetDao().getTotal(id, key),
                        spent = abs(db.billDao().sumExpense(id, start, end)),
                        categories = categories.filter { !it.isDeleted && it.type == "spending" },
                        rows = db.budgetDao().getCategoryBudgets(id, key).map { budget ->
                            val category = categories.firstOrNull { it.id == budget.categoryId && !it.isDeleted }
                            BudgetRow(
                                budget = budget,
                                name = category?.name?.takeIf { it.isNotBlank() } ?: "已删除分类",
                                spent = abs(db.billDao().sumExpenseByCategory(id, budget.categoryId.orEmpty(), start, end)),
                            )
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "预算读取失败，请重试"
            } finally {
                busy = false
            }
        }
    }

    private fun saveBudget(draft: BudgetEditor, amount: Long) {
        val snapshot = data ?: return
        if (busy) return
        val key = month.toString()
        busy = true
        error = null
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val db = TallyDb.database
                    val book = db.bookDao().getById(snapshot.book.id) ?: error("账本已不存在")
                    if (draft.categoryId != null) {
                        val category = db.categoryDao().getByIdAndBookId(draft.categoryId, book.id)
                        require(category != null && !category.isDeleted && category.type == "spending") {
                            "分类已不可用，请重新选择"
                        }
                    }
                    val now = System.currentTimeMillis()
                    db.budgetDao().saveMonthBudget(
                        BudgetDo(
                            id = draft.existing?.id ?: QianjiBridge.budgetId(key, draft.flag, draft.categoryId, book.id),
                            userId = book.userId,
                            bookId = book.id,
                            flag = draft.flag,
                            categoryId = draft.categoryId,
                            typeInfo = key,
                            amount = amount,
                            timeCreate = draft.existing?.timeCreate ?: now,
                            timeModify = now,
                        ),
                    )
                }
                editor = null
                notice = "预算已保存"
                refreshWidgets()
                busy = false
                loadBudget()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                busy = false
                error = failure.message ?: "预算保存失败，请重试"
            }
        }
    }

    private fun deleteBudget(budget: BudgetDo) {
        if (busy) return
        busy = true
        error = null
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    TallyDb.database.budgetDao().deleteMonthBudget(
                        bookId = budget.bookId,
                        typeInfo = budget.typeInfo,
                        flag = budget.flag,
                        categoryId = budget.categoryId,
                    )
                }
                deleting = null
                notice = if (budget.flag == BudgetDao.FLAG_TOTAL) "总预算已移除" else "分类预算已移除"
                refreshWidgets()
                busy = false
                loadBudget()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                busy = false
                error = failure.message ?: "删除失败，请重试"
            }
        }
    }

    @Composable
    private fun BudgetScreen() {
        val snapshot = data
        PortPage(
            title = "预算",
            subtitle = snapshot?.book?.name?.takeIf { it.isNotBlank() } ?: "按月安排支出",
            onBack = { finish() },
        ) {
            PortCard(contentPadding = PaddingValues(PortDesign.SmallGap)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
                ) {
                    IconButton(enabled = !busy && month > YearMonth.of(1, 1), onClick = { changeMonth(month.minusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上个月")
                    }
                    PortSecondaryButton(
                        text = "${month.year}年${month.monthValue}月",
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = { pickMonth = true },
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(enabled = !busy && month < YearMonth.of(9999, 12), onClick = { changeMonth(month.plusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下个月")
                    }
                }
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            error?.let {
                TransferStatus(title = "暂时未能完成", message = it, error = true,
                    actionText = if (!busy) "重新加载" else null, onAction = { editor = null; deleting = null; loadBudget() })
            }
            if (busy) TransferStatus(title = "正在处理预算…", busy = true)
            if (!busy && snapshot == null && error == null) {
                TransferStatus("尚未选择账本", "请先在首页选择一个本地账本，再设置预算。", actionText = "返回", onAction = { finish() })
            }
            if (snapshot != null) {
                PortCard {
                    val total = snapshot.total?.amount ?: 0L
                    Column(verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap)) {
                        Text(
                            if (total > 0) "本月总预算" else "本月尚未设置总预算",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(if (total > 0) yuan(total) else "—", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
                        Column {
                            Text("已支出 ${yuan(snapshot.spent)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PortCardExpand(visible = total > 0) {
                                if (total > 0) {
                                    Column(
                                        modifier = Modifier.padding(top = PortDesign.ItemGap),
                                        verticalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
                                    ) {
                                        BudgetProgress(snapshot.spent, total)
                                        Text(
                                            if (snapshot.spent > total) "已超出 ${yuan(snapshot.spent - total)}" else "剩余 ${yuan(total - snapshot.spent)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (snapshot.spent > total) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                        PortPrimaryButton(
                            text = if (snapshot.total == null) "设置总预算" else "编辑总预算",
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { error = null; editor = BudgetEditor("本月总预算", BudgetDao.FLAG_TOTAL, null, snapshot.total) },
                        )
                        snapshot.total?.let { totalBudget ->
                            PortSecondaryButton(
                                text = "移除总预算",
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                destructive = true,
                                onClick = { error = null; deleting = totalBudget },
                            )
                        }
                    }
                }
                PortSectionTitle("分类预算", subtitle = "按分类单独设置额度，不自动分配总预算")
                if (snapshot.rows.isEmpty()) {
                    TransferStatus("还没有分类预算", "可为餐饮、交通等支出分类设置每月额度。")
                }
                snapshot.rows.forEach { row ->
                    PortCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap)) {
                            Text(row.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "已支出 ${yuan(row.spent)} / 预算 ${yuan(row.budget.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (row.budget.amount > 0) BudgetProgress(row.spent, row.budget.amount)
                            Column {
                                Text(
                                    if (row.spent > row.budget.amount) "已超出 ${yuan(row.spent - row.budget.amount)}"
                                    else "剩余 ${yuan(row.budget.amount - row.spent)}",
                                    modifier = Modifier.padding(bottom = PortDesign.ItemGap),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (row.spent > row.budget.amount) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(PortDesign.SmallGap)) {
                                    PortSecondaryButton(text = "编辑", enabled = !busy && snapshot.categories.any { it.id == row.budget.categoryId }, onClick = {
                                        error = null
                                        editor = BudgetEditor(row.name, BudgetDao.FLAG_CATEGORY, row.budget.categoryId, row.budget)
                                    })
                                    PortSecondaryButton(text = "删除", enabled = !busy, onClick = { error = null; deleting = row.budget }, destructive = true)
                                }
                            }
                        }
                    }
                }
                val available = snapshot.categories.filter { category -> snapshot.rows.none { it.budget.categoryId == category.id } }
                PortPrimaryButton(
                    text = "添加分类预算",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && available.isNotEmpty(),
                    onClick = { pickCategory = true },
                )
                if (available.isEmpty()) {
                    Text(
                        if (snapshot.categories.isEmpty()) "本账本暂无支出分类，请先添加分类。" else "所有支出分类都已设置预算，可在上方编辑。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("支出按本账本所选月份内的未删除支出账单统计。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (pickMonth) MonthDialog(month, onDismiss = { pickMonth = false }) {
            pickMonth = false
            changeMonth(it)
        }
        if (pickCategory && snapshot != null) {
            AlertDialog(
                onDismissRequest = { pickCategory = false },
                shape = PortDesign.CardShape,
                title = { Text("选择支出分类", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(snapshot.categories.filter { category -> snapshot.rows.none { it.budget.categoryId == category.id } }, key = { it.id }) { category ->
                            PortActionRow(title = category.name.orEmpty().ifBlank { "未命名分类" }, onClick = {
                                pickCategory = false
                                error = null
                                editor = BudgetEditor(category.name.orEmpty(), BudgetDao.FLAG_CATEGORY, category.id, null)
                            })
                        }
                    }
                },
                confirmButton = { PortSecondaryButton(text = "取消", onClick = { pickCategory = false }) },
            )
        }
        editor?.let { draft ->
            BudgetAmountDialog(draft, busy, error, onDismiss = { if (!busy) editor = null }, onSave = { saveBudget(draft, it) })
        }
        deleting?.let { budget ->
            AlertDialog(
                onDismissRequest = { if (!busy) deleting = null },
                shape = PortDesign.CardShape,
                title = { Text(if (budget.flag == BudgetDao.FLAG_TOTAL) "移除总预算？" else "移除分类预算？", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text(
                        error ?: if (budget.flag == BudgetDao.FLAG_TOTAL) "只移除 ${budget.typeInfo} 的总额度，分类预算和已有账单会保留。"
                        else "只移除 ${budget.typeInfo} 的这个分类额度，总预算和已有账单会保留。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                confirmButton = { PortSecondaryButton(text = if (busy) "正在移除…" else "移除", enabled = !busy, onClick = { deleteBudget(budget) }, destructive = true) },
                dismissButton = { PortSecondaryButton(text = "取消", enabled = !busy, onClick = { deleting = null }) },
            )
        }
    }

    private fun refreshWidgets() {
        runCatching { AppServices.appWidgetSpi?.tryUpdateWidget() }
            .onFailure { notice = "${notice.orEmpty()}，桌面小部件稍后刷新" }
    }

    private fun changeMonth(value: YearMonth) {
        month = value
        notice = null
        loadBudget()
    }

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
        const val EXTRA_MONTH = "month"
    }
}

private data class BudgetSnapshot(val book: BookDo, val total: BudgetDo?, val spent: Long, val categories: List<CategoryDo>, val rows: List<BudgetRow>)
private data class BudgetRow(val budget: BudgetDo, val name: String, val spent: Long)
private data class BudgetEditor(val title: String, val flag: Int, val categoryId: String?, val existing: BudgetDo?)

@Composable
private fun BudgetProgress(spent: Long, amount: Long) {
    LinearProgressIndicator(
        progress = { (spent.toDouble() / amount).coerceIn(0.0, 1.0).toFloat() },
        modifier = Modifier.fillMaxWidth().height(PortDesign.ProgressHeight),
        color = if (spent > amount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun BudgetAmountDialog(draft: BudgetEditor, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var value by remember(draft) { mutableStateOf(draft.existing?.amount?.let { BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty()) }
    val cents = remember(value) {
        if (value.matches(Regex("\\d+(\\.\\d{1,2})?"))) {
            runCatching { BigDecimal(value).movePointRight(2).longValueExact().takeIf { it > 0 } }.getOrNull()
        } else null
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = PortDesign.CardShape,
        title = { Text(draft.title.ifBlank { "分类预算" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                PortInlineField(
                    value = value,
                    onValueChange = { value = it },
                    enabled = !busy,
                    label = "预算金额（元）",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = value.isNotBlank() && cents == null,
                )
                PortDivider()
                Text(
                    "输入大于 0 的金额，最多两位小数",
                    modifier = Modifier.padding(horizontal = PortDesign.CardPadding, vertical = 14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PortCardExpand(visible = error != null) {
                    error?.let {
                        Text(it, modifier = Modifier.padding(horizontal = PortDesign.CardPadding), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { PortPrimaryButton(text = if (busy) "正在保存…" else "保存", enabled = !busy && cents != null, onClick = { cents?.let(onSave) }) },
        dismissButton = { PortSecondaryButton(text = "取消", enabled = !busy, onClick = onDismiss) },
    )
}

@Composable
private fun MonthDialog(initial: YearMonth, onDismiss: () -> Unit, onSelect: (YearMonth) -> Unit) {
    var value by remember { mutableStateOf(initial.toString()) }
    val parsed = parseMonth(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PortDesign.CardShape,
        title = { Text("选择月份", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                PortInlineField(
                    value = value,
                    onValueChange = { value = it },
                    label = "月份（yyyy-MM）",
                    hint = "2026-11",
                    isError = parsed == null,
                )
                PortDivider()
                Text(
                    "也可使用页面两侧箭头逐月切换",
                    modifier = Modifier.padding(horizontal = PortDesign.CardPadding, vertical = 14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { PortPrimaryButton(text = "确定", enabled = parsed != null, onClick = { parsed?.let(onSelect) }) },
        dismissButton = { PortSecondaryButton(text = "取消", onClick = onDismiss) },
    )
}

private fun parseMonth(value: String?): YearMonth? {
    if (value == null || !value.matches(Regex("\\d{4}-\\d{2}"))) return null
    return runCatching { YearMonth.parse(value).takeIf { it.year in 1..9999 } }.getOrNull()
}

private fun yuan(cents: Long): String = "¥${BigDecimal.valueOf(cents, 2).toPlainString()}"
