package com.xiaojinzi.tally.module.qianji

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 自动记账确认页：极简代码 UI，不使用钱迹浮窗资源。 */
class QianjiConfirmActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val amount = intent.getDoubleExtra(QianjiAutoRecordService.EXTRA_AMOUNT, 0.0)
        val income = intent.getBooleanExtra(QianjiAutoRecordService.EXTRA_INCOME, false)
        val note = intent.getStringExtra(QianjiAutoRecordService.EXTRA_NOTE).orEmpty()
        val time = intent.getLongExtra(QianjiAutoRecordService.EXTRA_TIME, System.currentTimeMillis())
        if (amount <= 0) { finish(); return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF2F3F7.toInt())
        }
        setContentView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        root.addView(TextView(this).apply {
            text = "自动记一笔"
            textSize = 18f
            setTextColor(0xFF111114.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = (if (income) "+" else "-") + String.format("%.2f", amount)
            textSize = 32f
            setTextColor(if (income) 0xFF34C759.toInt() else 0xFFFF3B30.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 12)
        })
        if (note.isNotBlank()) root.addView(TextView(this).apply {
            text = note
            textSize = 14f
            setTextColor(0xFF8A8A8E.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "保存"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0A84FF.toInt())
            setPadding(72, 24, 72, 24)
            setOnClickListener {
                scope.launch {
                    QianjiBridge.insertBill(amount, income, "其他", note, time)
                    Toast.makeText(this@QianjiConfirmActivity, "已保存", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        })
        root.addView(TextView(this).apply {
            text = "忽略"
            textSize = 14f
            setTextColor(0xFF8A8A8E.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
            setOnClickListener { finish() }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // SupervisorJob 不取消全局，仅避免页面销毁后回调泄漏
    }
}
