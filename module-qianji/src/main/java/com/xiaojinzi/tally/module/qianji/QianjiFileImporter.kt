package com.xiaojinzi.tally.module.qianji

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

/** 现有 CSV/XLS 映射；导入在本机完成。poi 核心包不支持 XLSX。 */
object QianjiFileImporter {

    /** 兼容旧调用；始终在 IO 线程复制到独立缓存，结束或取消时删除。 */
    suspend fun import(
        context: Context,
        uri: Uri,
        targetBookId: String = QianjiBridge.DEFAULT_BOOK_ID,
        targetUserId: String = QianjiBridge.LOCAL_USER_ID,
    ): Int = withCachedDocument(context, uri) { file, name, mime ->
        import(context, file, name, mime, targetBookId, targetUserId)
    }

    suspend fun import(
        context: Context,
        source: File,
        displayName: String = source.name,
        mimeType: String = "",
        targetBookId: String = QianjiBridge.DEFAULT_BOOK_ID,
        targetUserId: String = QianjiBridge.LOCAL_USER_ID,
    ): Int = withContext(Dispatchers.IO) {
        val format = resolveFormat(source, displayName, mimeType)
        import(context, source, format, targetBookId, targetUserId)
    }

    internal suspend fun import(
        context: Context,
        source: File,
        format: ExternalImportPolicy.Format,
        targetBookId: String,
        targetUserId: String,
    ): Int = withContext(Dispatchers.IO) {
        require(targetBookId.isNotBlank() && targetUserId.isNotBlank()) { "请选择有效的目标账本" }
        val rows = readRows(source, format)
        currentCoroutineContext().ensureActive()
        val db = TallyDb.database
        val book = db.bookDao().getById(targetBookId) ?: throw IllegalArgumentException("目标账本不存在，请重新选择")
        require(book.userId == targetUserId) { "目标账本已变化，请重新选择" }
        if (rows.isEmpty()) return@withContext 0

        val header = rows.first().any { norm(it).let { n -> n == "金额" || n == "amount" || n == "money" } }
        val start = if (header) 1 else 0
        val idx = if (header) {
            rows.first().mapIndexed { i, s -> norm(s) to i }.toMap()
        } else emptyMap()
        val iTime = if (header) pick(idx, "时间", "日期", "date", "time") else 0
        val iType = if (header) pick(idx, "类型", "收支", "type") else 1
        val iMoney = if (header) pick(idx, "金额", "money", "amount") else 2
        val iCate = if (header) pick(idx, "分类", "类别", "category") else 3
        val iNote = if (header) pick(idx, "备注", "说明", "remark", "note") else 4

        val now = System.currentTimeMillis()
        val categories = db.categoryDao().getByBookId(targetBookId).filterNot { it.isDeleted }
        val bills = mutableListOf<BillDo>()

        for (r in start until rows.size) {
            currentCoroutineContext().ensureActive()
            val a = rows[r]
            val moneyText = col(a, iMoney).replace("¥", "").replace("￥", "").replace(",", "").trim()
            val raw = moneyText.toDoubleOrNull() ?: continue
            val money = abs(raw)
            if (!money.isFinite() || money <= 0 || money >= Long.MAX_VALUE / 100.0) continue
            val typeText = norm(col(a, iType))
            val income = typeText.contains("收入") || typeText.contains("income") || typeText == "1"
            val categoryName = col(a, iCate).trim()
            val categoryType = if (income) "income" else "spending"
            val categoryId = categories.firstOrNull { it.name == categoryName && it.type == categoryType }?.id
                ?: categories.firstOrNull { it.type == categoryType }?.id ?: continue
            val time = parseTime(col(a, iTime).trim()).let { if (it > 0) it else now }
            val amount = (money * 100.0).roundToLong()
            if (amount <= 0) continue
            bills += BillDo(
                id = QianjiBridge.scopedImportId("qjfile_" + UUID.randomUUID().toString(), targetBookId),
                userId = targetUserId,
                bookId = targetBookId,
                type = "normal",
                originBillId = null,
                time = time,
                categoryId = categoryId,
                accountId = null,
                transferTargetAccountId = null,
                amount = if (income) amount else -amount,
                note = col(a, iNote).trim().ifBlank { null },
                isNotCalculate = false,
                timeCreate = now,
                timeModify = now,
                isDeleted = false,
                isSync = false,
            )
        }
        if (bills.isNotEmpty()) db.withTransaction {
            val currentBook = db.bookDao().getById(targetBookId)
            require(currentBook?.userId == targetUserId) { "目标账本已变化，请重新选择" }
            currentCoroutineContext().ensureActive()
            db.billDao().insert(bills)
            currentCoroutineContext().ensureActive()
        }
        bills.size
    }

    /** SAF 名称/类型查询、复制和解析都在 IO 上；取消不会吞掉，也不会遗留同名缓存。 */
    internal suspend fun <T> withCachedDocument(
        context: Context,
        uri: Uri,
        block: suspend (File, String, String) -> T,
    ): T = withContext(Dispatchers.IO) {
        val name = resolveName(context, uri)
        val mime = context.contentResolver.getType(uri).orEmpty()
        ExternalImportPolicy.validateMetadata(name, mime)
        val cached = File.createTempFile("qianji-import-", ".tmp", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                cached.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val size = input.read(buffer)
                        if (size < 0) break
                        output.write(buffer, 0, size)
                    }
                }
            } ?: throw IllegalArgumentException("无法读取所选文件，请重新选择")
            require(cached.length() > 0) { "所选文件为空" }
            currentCoroutineContext().ensureActive()
            block(cached, name, mime)
        } finally {
            cached.delete()
            listOf("-journal", "-wal", "-shm").forEach { File(cached.absolutePath + it).delete() }
        }
    }

    internal fun resolveFormat(source: File, displayName: String, mimeType: String): ExternalImportPolicy.Format {
        val header = source.inputStream().use { input ->
            val bytes = ByteArray(4096)
            bytes.copyOf(input.read(bytes).coerceAtLeast(0))
        }
        return ExternalImportPolicy.validate(displayName, mimeType, header)
    }

    internal suspend fun readRows(source: File, format: ExternalImportPolicy.Format): List<List<String>> =
        source.inputStream().use { input ->
            when (format) {
                ExternalImportPolicy.Format.Csv -> readCsv(input)
                ExternalImportPolicy.Format.Xls -> readWorkbook(input)
                ExternalImportPolicy.Format.Sqlite -> throw IllegalArgumentException("数据库请使用钱迹 DB 导入")
            }
        }

    private suspend fun readCsv(input: java.io.InputStream): List<List<String>> {
        val r = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val out = mutableListOf<List<String>>()
        while (true) {
            currentCoroutineContext().ensureActive()
            val line = r.readLine() ?: break
            if (line.isBlank()) continue
            out += parseCsvLine(line.removePrefix("\uFEFF"))
        }
        return out
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cell = StringBuilder()
        var quote = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quote && i + 1 < line.length && line[i + 1] == '"' -> { cell.append('"'); i++ }
                ch == '"' -> quote = !quote
                ch == ',' && !quote -> { out += cell.toString(); cell.setLength(0) }
                else -> cell.append(ch)
            }
            i++
        }
        out += cell.toString()
        return out
    }

    private suspend fun readWorkbook(input: java.io.InputStream): List<List<String>> =
        WorkbookFactory.create(input).use { wb ->
            val rows = mutableListOf<List<String>>()
            if (wb.numberOfSheets > 0) for (row in wb.getSheetAt(0)) {
                currentCoroutineContext().ensureActive()
                val cells = mutableListOf<String>()
                for (i in 0 until row.lastCellNum) cells += cellToString(row.getCell(i))
                rows += cells
            }
            rows
        }

    private fun cellToString(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(cell.dateCellValue)
                } else {
                    val d = cell.numericCellValue
                    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                else -> cell.toString()
            }
        } catch (e: Exception) { cell.toString() }
    }

    private fun resolveName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (column >= 0) cursor.getString(column)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // 部分 DocumentProvider 不提供 DISPLAY_NAME，继续按 MIME / URI 判断。
        }
        return uri.lastPathSegment.orEmpty()
    }

    private fun norm(s: String) = s.trim().lowercase(Locale.ROOT).replace(" ", "")
    private fun pick(map: Map<String, Int>, vararg keys: String) = keys.firstNotNullOfOrNull { map[norm(it)] } ?: -1
    private fun col(a: List<String>, i: Int) = if (i in a.indices) a[i] else ""

    private fun parseTime(s: String): Long {
        if (s.isBlank()) return 0
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm", "yyyy-MM-dd", "yyyy/MM/dd"
        )
        for (p in patterns) {
            try {
                SimpleDateFormat(p, Locale.CHINA).parse(s)?.let { return it.time }
            } catch (e: Exception) { }
        }
        val v = s.toDoubleOrNull() ?: return 0
        return when {
            v > 1e12 -> (v / 1000).toLong() * 1000
            v > 1e9 -> v.toLong() * 1000
            v > 20000 && v < 80000 -> ((v - 25569) * 86400).toLong() * 1000
            else -> 0
        }
    }
}
