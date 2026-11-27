package com.xiaojinzi.tally.module.qianji

import android.content.Intent
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortStatusPill
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BookDo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@RouterAnno(hostAndPath = AppRouterConfig.QIANJI_IMPORT)
class QianjiImportActivity : ComponentActivity() {

    private var books by mutableStateOf<List<BookDo>>(emptyList())
    private var selectedBookId by mutableStateOf<String?>(null)
    private var loading by mutableStateOf(false)
    private var importing by mutableStateOf(false)
    private var choosingFile by mutableStateOf(false)
    private var choosingBook by mutableStateOf(false)
    private var loadError by mutableStateOf<String?>(null)
    private var importError by mutableStateOf<String?>(null)
    private var fileName by mutableStateOf<String?>(null)
    private var stage by mutableStateOf("正在读取文件…")
    private var result by mutableStateOf<CompletedImport?>(null)
    private var pendingTarget: ImportTarget? = null
    private var incomingUri by mutableStateOf<Uri?>(null)
    private var incomingMime by mutableStateOf("")
    private var preparingExternal by mutableStateOf(false)
    private var externalJob: Job? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        choosingFile = false
        val target = pendingTarget
        pendingTarget = null
        if (uri != null && target != null) importFile(uri, target)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        selectedBookId = savedInstanceState?.getString("selectedBookId")
            ?: intent.getStringExtra("bookId")?.takeIf { it.isNotBlank() }
        savedInstanceState?.getString("pendingBookId")?.let { id ->
            pendingTarget = ImportTarget(id, savedInstanceState.getString("pendingUserId").orEmpty(), savedInstanceState.getString("pendingBookName").orEmpty())
            choosingFile = true
        }
        fileName = savedInstanceState?.getString("fileName")
        if (savedInstanceState?.getBoolean("importing") == true) {
            importError = "上次导入已中断，请先查看本账本账单，再决定是否重新导入。"
        }
        savedInstanceState?.getString("resultBookId")?.let { id ->
            result = CompletedImport(
                target = ImportTarget(id, savedInstanceState.getString("resultUserId").orEmpty(), savedInstanceState.getString("resultBookName").orEmpty()),
                counts = QianjiBridge.ImportResult(savedInstanceState.getInt("billCount"), savedInstanceState.getInt("budgetCount"), savedInstanceState.getInt("labelCount")),
            )
        }
        setContent { AppTheme { PortTheme { ImportScreen() } } }
        if (savedInstanceState == null) {
            receiveExternal(intent)
        } else {
            incomingUri = savedInstanceState.getString("incomingUri")?.let(Uri::parse)?.takeIf { it.scheme == "content" }
            incomingMime = savedInstanceState.getString("incomingMime").orEmpty()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (importing || choosingFile || preparingExternal) {
            android.widget.Toast.makeText(this, "正在处理当前文件，请完成后重新打开新文件", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        setIntent(intent)
        receiveExternal(intent)
    }

    private fun receiveExternal(request: Intent) {
        if (request.action != Intent.ACTION_VIEW && request.action != Intent.ACTION_SEND) return
        externalJob?.cancel()
        incomingUri = null
        incomingMime = request.type.orEmpty()
        choosingBook = false
        result = null
        fileName = null
        importError = null
        try {
            val uri = if (request.action == Intent.ACTION_VIEW) request.data else {
                if (Build.VERSION.SDK_INT >= 33) request.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") request.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            } ?: request.clipData?.takeIf { it.itemCount == 1 }?.getItemAt(0)?.uri
            require(uri != null && uri.scheme == "content" && uri.authority != packageName && uri.authority != "$packageName.shizuku") {
                "请从文件管理器选择一份可读取的 CSV、XLS 或钱迹数据库"
            }
            incomingUri = uri
            preparingExternal = true
            externalJob = lifecycleScope.launch {
                try {
                    val name = withContext(Dispatchers.IO) {
                        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(0) else null
                        } ?: uri.lastPathSegment.orEmpty()
                    }
                    ExternalImportPolicy.validateMetadata(name, incomingMime)
                    fileName = name.take(240)
                    // The source grants access with its Intent; copying/parsing waits for confirmation.
                    incomingUri = uri
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    incomingUri = null
                    importError = failure.message ?: "无法读取文件信息，请从文件管理器重新打开或在应用内选择文件"
                } finally {
                    preparingExternal = false
                }
            }
        } catch (_: Exception) {
            importError = "文件请求无效，仅接受单个可读取的本地文档"
        }
    }

    override fun onResume() {
        super.onResume()
        if (!loading && !importing) loadBooks()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selectedBookId", selectedBookId)
        outState.putString("fileName", fileName)
        outState.putString("incomingUri", incomingUri?.toString())
        outState.putString("incomingMime", incomingMime)
        outState.putBoolean("importing", importing)
        pendingTarget?.let {
            outState.putString("pendingBookId", it.id)
            outState.putString("pendingUserId", it.userId)
            outState.putString("pendingBookName", it.name)
        }
        result?.let {
            outState.putString("resultBookId", it.target.id)
            outState.putString("resultUserId", it.target.userId)
            outState.putString("resultBookName", it.target.name)
            outState.putInt("billCount", it.counts.billCount)
            outState.putInt("budgetCount", it.counts.budgetCount)
            outState.putInt("labelCount", it.counts.labelLinkCount)
        }
        super.onSaveInstanceState(outState)
    }

    private fun loadBooks() {
        loading = true
        loadError = null
        lifecycleScope.launch {
            try {
                withTimeout(10_000L) { AppServices.tallyDataSourceInitSpi.isInitStateOb.first { it } }
                val localBooks = withContext(Dispatchers.IO) { TallyDb.database.bookDao().getAll() }
                val preferred = selectedBookId ?: AppServices.tallyDataSourceSpi.selectedBookStateOb.first()?.id
                books = localBooks
                selectedBookId = preferred?.takeIf { id -> localBooks.any { it.id == id } }
                    ?: localBooks.singleOrNull()?.id
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                loadError = failure.message ?: "本地账本读取失败"
            } finally {
                loading = false
            }
        }
    }

    private fun chooseFile(book: BookDo) {
        if (importing || choosingFile || loading) return
        importError = null
        result = null
        fileName = null
        pendingTarget = ImportTarget(book.id, book.userId, book.name.orEmpty().ifBlank { "未命名账本" })
        choosingFile = true
        try {
            // DB 备份常标记为 octet-stream；具体格式在读取后校验，不靠 MIME 放宽解析。
            filePicker.launch(arrayOf("*/*"))
        } catch (_: Exception) {
            choosingFile = false
            pendingTarget = null
            importError = "无法打开系统文件选择器，请确认设备有可用的文件管理应用。"
        }
    }

    private fun importFile(uri: Uri, target: ImportTarget) {
        if (importing) return
        importing = true
        importError = null
        stage = "正在读取文件…"
        lifecycleScope.launch {
            try {
                val counts = QianjiFileImporter.withCachedDocument(applicationContext, uri) { cached, name, mime ->
                    val header = cached.inputStream().use { input ->
                        val bytes = ByteArray(4096)
                        bytes.copyOf(input.read(bytes).coerceAtLeast(0))
                    }
                    val format = ExternalImportPolicy.validate(name, mime, header)
                    withContext(Dispatchers.Main) {
                        fileName = name
                        stage = "正在解析并导入到「${target.name}」…"
                    }
                    if (format == ExternalImportPolicy.Format.Sqlite) {
                        QianjiBridge.importQianjiDatabase(applicationContext, cached, target.id, target.userId)
                    } else {
                        QianjiBridge.ImportResult(
                            billCount = QianjiFileImporter.import(applicationContext, cached, format, target.id, target.userId),
                            budgetCount = 0,
                        )
                    }
                }
                incomingUri = null
                result = CompletedImport(target, counts)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SQLiteException) {
                importError = "无法读取或写入这份数据库，请确认是现有格式的钱迹原库，并检查本机可用空间。"
            } catch (failure: Exception) {
                importError = failure.message ?: "导入失败，请检查文件格式与读取权限后重试"
            } finally {
                importing = false
            }
        }
    }

    private fun viewBills(target: ImportTarget) {
        AppRouterCoreApi::class.routeApi().toBillListView(
            context = this,
            title = "${target.name}的账单".toStringItemDto(),
            question = TallyDataSourceSpi.Companion.BillQueryConditionDto(bookIdList = listOf(target.id)),
        )
    }

    @Composable
    private fun ImportScreen() {
        val book = books.firstOrNull { it.id == selectedBookId }
        val busy = loading || importing || choosingFile || preparingExternal
        PortPage(title = "导入账单", subtitle = "把已有记录带入所选账本", onBack = { finish() }) {
            PortSectionTitle("目标账本", subtitle = "先选择本地账本，再选择文件")
            if (loading) TransferStatus("正在读取账本…", busy = true)
            loadError?.let {
                TransferStatus("暂时无法读取账本", it, error = true, actionText = "重试", onAction = { loadBooks() })
            }
            if (!loading && loadError == null && books.isEmpty()) {
                TransferStatus("还没有本地账本", "请先在首页创建或选择账本，再导入已有记录。", actionText = "返回", onAction = { finish() })
            }
            if (books.isNotEmpty()) {
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    PortActionRow(
                        title = book?.name?.takeIf { it.isNotBlank() } ?: "选择目标账本",
                        subtitle = "导入内容将写入这个账本",
                        enabled = !busy && loadError == null,
                        onClick = { choosingBook = true },
                        trailing = if (book != null) ({ PortStatusPill("已选择", emphasized = true) }) else null,
                    )
                }
            }
            incomingUri?.let { uri ->
                PortSectionTitle("收到的文件")
                PortCard {
                    Text(fileName ?: "正在读取文件信息…", style = MaterialTheme.typography.titleMedium)
                    Text("确认目标账本后才会写入。重复导入 CSV / XLS 会新增账单。", style = MaterialTheme.typography.bodySmall)
                    PortPrimaryButton(
                        text = "确认导入这份文件",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = book != null && !busy && loadError == null,
                        onClick = { book?.let { importFile(uri, ImportTarget(it.id, it.userId, it.name.orEmpty())) } },
                    )
                    PortSecondaryButton(text = "取消接收", enabled = !busy, onClick = {
                        incomingUri = null
                        fileName = null
                        importError = null
                    })
                }
            }
            if (incomingUri == null) {
                PortSectionTitle("选择文件", subtitle = "支持现有可解析的 CSV、XLS 和钱迹 DB")
                PortCard {
                    Column(verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap)) {
                        Text("导入在本机完成", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("CSV / XLS 按时间、类型、金额、分类、备注等列读取。仅导入可解析的记录，完成后会显示实际数量。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("暂不支持 XLSX、加密数据库或压缩备份，请先转换为 CSV / XLS，或选择钱迹原始数据库。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PortPrimaryButton(
                            text = if (choosingFile) "等待选择文件…" else "选择文件并导入",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = book != null && !busy && loadError == null,
                            onClick = { book?.let { chooseFile(it) } },
                        )
                    }
                }
            }
            fileName?.let { Text("文件：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (importing) TransferStatus(stage, "关闭页面会取消尚未完成的任务，请等待结果后再返回。", busy = true)
            importError?.let { TransferStatus("导入未完成", it, error = true) }
            result?.let { completed ->
                val counts = completed.counts
                val empty = counts.billCount == 0 && counts.budgetCount == 0 && counts.labelLinkCount == 0
                TransferStatus(
                    title = if (empty) "未导入账单或预算" else "已导入到「${completed.target.name}」",
                    message = "${counts.billCount} 笔账单 · ${counts.budgetCount} 条预算 · ${counts.labelLinkCount} 条标签关联" +
                        if (empty) "\n未找到可写入的记录，请检查表头、金额和账本分类。" else "",
                    actionText = "查看本账本账单",
                    onAction = { viewBills(completed.target) },
                )
            }
            if (book != null && result == null && !busy) {
                PortSecondaryButton(text = "查看本账本账单", onClick = { viewBills(ImportTarget(book.id, book.userId, book.name.orEmpty().ifBlank { "未命名账本" })) })
            }
            Text("重复导入 CSV / XLS 会新增账单；钱迹 DB 在同一账本内按源记录 ID 更新。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (choosingBook) {
            AlertDialog(
                onDismissRequest = { choosingBook = false },
                shape = PortDesign.CardShape,
                title = { Text("选择目标账本", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(books, key = { it.id }) { candidate ->
                            PortActionRow(
                                title = candidate.name.orEmpty().ifBlank { "未命名账本" },
                                trailing = if (candidate.id == selectedBookId) ({ PortStatusPill("当前选择", emphasized = true) }) else null,
                                onClick = {
                                    selectedBookId = candidate.id
                                    choosingBook = false
                                    importError = null
                                    fileName = null
                                    result = null
                                },
                            )
                        }
                    }
                },
                confirmButton = { PortSecondaryButton(text = "取消", onClick = { choosingBook = false }) },
            )
        }
    }
}

private data class ImportTarget(val id: String, val userId: String, val name: String)
private data class CompletedImport(val target: ImportTarget, val counts: QianjiBridge.ImportResult)
