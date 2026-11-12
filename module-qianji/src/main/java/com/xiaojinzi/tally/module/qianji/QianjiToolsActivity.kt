package com.xiaojinzi.tally.module.qianji

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * 钱迹工具页：把 B 方案的三件事收在一个入口里。
 *
 * 1. 导入钱迹原库（qianjiapp / *.db / 备份文件）
 * 2. 导入账单文件（CSV / XLS / XLSX）
 * 3. 打开系统无障碍设置，启用「钱迹自动记账」
 */
@RouterAnno(hostAndPath = AppRouterConfig.QIANJI_TOOLS)
class QianjiToolsActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF2F3F7.toInt())
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })

        root.addView(TextView(this).apply {
            text = "钱迹工具"
            textSize = 22f
            setTextColor(0xFF111114.toInt())
        })
        root.addView(TextView(this).apply {
            text = "数据只写到本机 yike 数据库，不上传任何服务器。"
            textSize = 13f
            setTextColor(0xFF8A8A8E.toInt())
            setPadding(0, dp(8), 0, dp(20))
        })

        root.addView(action("导入钱迹数据库", "选择 qianjiapp 或钱迹备份 .db 文件") {
            pickFile(REQ_DB, listOf("*/*"))
        })
        root.addView(action("导入账单文件", "支持 CSV / XLS / XLSX，表头按列名识别") {
            pickFile(
                REQ_FILE,
                listOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "text/plain",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                )
            )
        })
        root.addView(action("预算", "总预算 + 分类预算，按 yyyy-MM 记录（钱迹口径）") {
            Router.with(context = this)
                .hostAndPath(hostAndPath = AppRouterConfig.QIANJI_BUDGET)
                .forward()
        })
        root.addView(action("启用自动记账", "打开系统无障碍设置，找到「钱迹自动记账」") {
            openAccessibilitySettings()
        })

        status = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(0xFF34C759.toInt())
            setPadding(0, dp(24), 0, 0)
        }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "返回"
            textSize = 15f
            setTextColor(0xFF0A84FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(32), 0, 0)
            setOnClickListener { finish() }
        })
    }

    private fun action(title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10)
            layoutParams = lp
            addView(TextView(this@QianjiToolsActivity).apply {
                text = title
                textSize = 16f
                setTextColor(0xFF111114.toInt())
            })
            addView(TextView(this@QianjiToolsActivity).apply {
                text = subtitle
                textSize = 12f
                setTextColor(0xFF8A8A8E.toInt())
                setPadding(0, dp(4), 0, 0)
            })
            setOnClickListener { onClick() }
        }
    }

    private fun pickFile(requestCode: Int, mimeTypes: List<String>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            toast("没有可用的文件选择器")
        }
    }

    @Deprecated("依赖 startActivityForResult 以保持零额外依赖")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_DB -> importDatabase(uri)
            REQ_FILE -> importBills(uri)
        }
    }

    private fun importDatabase(uri: android.net.Uri) {
        setStatus("正在导入钱迹数据库…")
        scope.launch {
            runCatching {
                val cached = copyToCache(uri, "qianji_import.db")
                QianjiBridge.importQianjiDatabase(this@QianjiToolsActivity, cached)
            }.onSuccess { count ->
                setStatus("导入完成：$count 笔账单")
            }.onFailure { e ->
                setStatus("导入失败：${e.message}")
            }
        }
    }

    private fun importBills(uri: android.net.Uri) {
        setStatus("正在导入账单文件…")
        scope.launch {
            runCatching {
                QianjiFileImporter.import(this@QianjiToolsActivity, uri)
            }.onSuccess { count ->
                setStatus("导入完成：$count 笔账单")
            }.onFailure { e ->
                setStatus("导入失败：${e.message}")
            }
        }
    }

    private fun copyToCache(uri: android.net.Uri, name: String): File {
        val out = File(cacheDir, name)
        contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("无法读取所选文件")
        return out
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            toast("请在列表中找到「钱迹自动记账」并启用")
        } catch (e: Exception) {
            toast("无法打开无障碍设置")
        }
    }

    private fun setStatus(text: String) {
        status?.text = text
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_DB = 1001
        private const val REQ_FILE = 1002
    }
}
