package com.xiaojinzi.tally.module.qianji

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDao
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BudgetDo
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.CategoryDo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * 预算页：移植钱迹的「总预算 + 分类预算，按月」逻辑。
 *
 * 数据写在本机 Room 的 budget 表（flag=1 总预算，flag=2 分类预算，typeInfo=yyyy-MM），
 * 支出统计直接聚合 bill 表，不经过服务器。
 */
@RouterAnno(hostAndPath = AppRouterConfig.QIANJI_BUDGET)
class BudgetActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.CHINA)
    private val month = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private lateinit var root: LinearLayout
    private var categoryCache: List<CategoryDo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF2F3F7.toInt())
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
        render()
        loadData()
    }

    // ---------------- 数据 ----------------

    private fun monthKey(): String = monthFormat.format(month.time)

    private fun monthRange(): Pair<Long, Long> {
        val start = month.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        val end = start.clone() as Calendar
        end.add(Calendar.MONTH, 1)
        return start.timeInMillis to end.timeInMillis
    }

    private fun loadData() {
        val key = monthKey()
        val (start, end) = monthRange()
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val db = TallyDb.database
                    val bookId = QianjiBridge.DEFAULT_BOOK_ID
                    val total = db.budgetDao().getTotal(bookId, key)
                    val categories = db.budgetDao().getCategoryBudgets(bookId, key)
                    val expense = abs(db.billDao().sumExpense(bookId, start, end))
                    val categoryRows = categories.map { budget ->
                        val name = db.categoryDao().getById(budget.categoryId.orEmpty())?.name
                            ?: "已删除分类"
                        val spent = abs(
                            db.billDao().sumExpenseByCategory(
                                bookId = bookId,
                                categoryId = budget.categoryId.orEmpty(),
                                start = start,
                                end = end,
                            )
                        )
                        Triple(name, budget.amount, spent)
                    }
                    Quad(total?.amount ?: 0L, expense, categoryRows, db.categoryDao().getByBookId(bookId))
                }
            }
            result.onSuccess { (totalAmount, spent, categoryRows, allCategories) ->
                categoryCache = allCategories
                render(totalAmount, spent, categoryRows)
            }.onFailure {
                render(error = it.message)
            }
        }
    }

    private data class Quad<A, B, C, D>(
        val a: A, val b: B, val c: C, val d: D,
    )

    // ---------------- 渲染 ----------------

    private fun render(
        totalBudget: Long? = null,
        totalSpent: Long? = null,
        categoryRows: List<Triple<String, Long, Long>> = emptyList(),
        error: String? = null,
    ) {
        root.removeAllViews()

        // 顶部：月份切换
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@BudgetActivity).apply {
                text = "←"
                textSize = 20f
                setTextColor(0xFF111114.toInt())
                setPadding(0, dp(4), dp(16), dp(16))
                setOnClickListener { finish() }
            })
            addView(TextView(this@BudgetActivity).apply {
                text = "‹"
                textSize = 22f
                setTextColor(0xFF0A84FF.toInt())
                setPadding(dp(12), 0, dp(12), dp(12))
                setOnClickListener { shiftMonth(-1) }
            })
            addView(TextView(this@BudgetActivity).apply {
                text = "${month.get(Calendar.YEAR)}年${month.get(Calendar.MONTH) + 1}月"
                textSize = 18f
                setTextColor(0xFF111114.toInt())
                setPadding(dp(8), 0, dp(8), dp(12))
            })
            addView(TextView(this@BudgetActivity).apply {
                text = "›"
                textSize = 22f
                setTextColor(0xFF0A84FF.toInt())
                setPadding(dp(12), 0, dp(12), dp(12))
                setOnClickListener { shiftMonth(1) }
            })
        })

        if (error != null) {
            root.addView(TextView(this).apply {
                text = "读取失败：$error"
                textSize = 14f
                setTextColor(0xFFFF3B30.toInt())
            })
            return
        }

        // 总预算卡片
        val budget = totalBudget ?: 0L
        val spent = totalSpent ?: 0L
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(TextView(this@BudgetActivity).apply {
                text = if (budget > 0) "本月预算 ${yuan(budget)}" else "本月还未设置预算"
                textSize = 17f
                setTextColor(0xFF111114.toInt())
            })
            addView(TextView(this@BudgetActivity).apply {
                text = "已支出 ${yuan(spent)}" + if (budget > 0) " / ${yuan(budget)}" else ""
                textSize = 14f
                setTextColor(if (budget > 0 && spent > budget) 0xFFFF3B30.toInt() else 0xFF8A8A8E.toInt())
                setPadding(0, dp(6), 0, dp(12))
            })
            if (budget > 0) {
                addView(ProgressBar(
                    this@BudgetActivity,
                    null,
                    android.R.attr.progressBarStyleHorizontal,
                ).apply {
                    max = 1000
                    progress = ((spent.toDouble() / budget) * 1000).toInt().coerceIn(0, 1000)
                })
            }
            addView(TextView(this@BudgetActivity).apply {
                text = "设置总预算"
                textSize = 14f
                setTextColor(0xFF0A84FF.toInt())
                gravity = Gravity.END
                setPadding(0, dp(14), 0, 0)
                setOnClickListener {
                    askAmount("本月总预算", budget) { cents ->
                        saveBudget(BudgetDao.FLAG_TOTAL, null, cents)
                    }
                }
            })
        }, lp(topMargin = 0))

        // 分类预算
        root.addView(TextView(this).apply {
            text = "分类预算"
            textSize = 15f
            setTextColor(0xFF8A8A8E.toInt())
            setPadding(dp(4), dp(22), 0, dp(10))
        })

        if (categoryRows.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "还没有分类预算"
                textSize = 13f
                setTextColor(0xFFB0B0B5.toInt())
                setPadding(dp(4), 0, 0, dp(8))
            })
        } else {
            categoryRows.forEach { (name, amount, used) ->
                root.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    addView(TextView(this@BudgetActivity).apply {
                        text = name
                        textSize = 15f
                        setTextColor(0xFF111114.toInt())
                    })
                    addView(TextView(this@BudgetActivity).apply {
                        text = "${yuan(used)} / ${yuan(amount)}"
                        textSize = 13f
                        setTextColor(if (used > amount) 0xFFFF3B30.toInt() else 0xFF8A8A8E.toInt())
                        setPadding(0, dp(4), 0, 0)
                    })
                }, lp(topMargin = 8))
            }
        }

        root.addView(TextView(this).apply {
            text = "＋ 添加分类预算"
            textSize = 14f
            setTextColor(0xFF0A84FF.toInt())
            setPadding(dp(4), dp(16), 0, 0)
            setOnClickListener { pickCategory() }
        })
    }

    private fun lp(topMargin: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = dp(topMargin)
        }
    }

    // ---------------- 交互 ----------------

    private fun shiftMonth(delta: Int) {
        month.add(Calendar.MONTH, delta)
        render()
        loadData()
    }

    private fun pickCategory() {
        val spending = categoryCache.filter { it.type == "spending" }
        if (spending.isEmpty()) {
            toast("还没有支出分类")
            return
        }
        val names = spending.map { it.name.orEmpty() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择分类")
            .setItems(names) { _, which ->
                val category = spending[which]
                askAmount("${category.name} 本月预算", 0L) { cents ->
                    saveBudget(BudgetDao.FLAG_CATEGORY, category.id, cents)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun askAmount(title: String, initial: Long, onOk: (Long) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "金额（元）"
            if (initial > 0) setText(yuanPlain(initial))
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val yuanValue = input.text.toString().trim().toDoubleOrNull()
                if (yuanValue == null || yuanValue < 0) {
                    toast("金额不正确")
                    return@setPositiveButton
                }
                onOk((yuanValue * 100.0).roundToLong())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveBudget(flag: Int, categoryId: String?, amountCent: Long) {
        val key = monthKey()
        val id = "qjb_${QianjiBridge.DEFAULT_BOOK_ID}_${key}_${flag}_${categoryId ?: "total"}"
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    TallyDb.database.budgetDao().insert(
                        BudgetDo(
                            id = id,
                            userId = QianjiBridge.LOCAL_USER_ID,
                            bookId = QianjiBridge.DEFAULT_BOOK_ID,
                            flag = flag,
                            categoryId = categoryId,
                            typeInfo = key,
                            amount = amountCent,
                            timeCreate = now,
                            timeModify = now,
                        )
                    )
                }
            }.onSuccess {
                toast("已保存")
                render()
                loadData()
            }.onFailure {
                toast("保存失败：${it.message}")
            }
        }
    }

    // ---------------- 工具 ----------------

    private fun yuan(cents: Long): String = "¥" + yuanPlain(cents)

    private fun yuanPlain(cents: Long): String {
        val value = cents / 100.0
        return if (max(value, -value) >= 1_000_000_000) {
            String.format(Locale.CHINA, "%.0f", value)
        } else {
            String.format(Locale.CHINA, "%.2f", value)
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
