package com.xiaojinzi.tally.module.qianji

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import com.xiaojinzi.tally.module.datasource.storage.db.tally.dao.BillDo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

/** 本地 CSV/XLS/XLSX 导入：写进 yike Room，不经过服务器。 */
object QianjiFileImporter {

    suspend fun import(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val name = resolveName(context, uri)
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val rows = context.contentResolver.openInputStream(uri)?.use { input ->
            when {
                name.endsWith(".csv") || name.endsWith(".txt") || mime.contains("csv") || mime.contains("text/plain") ->
                    readCsv(input)
                name.endsWith(".xls") || name.endsWith(".xlsx") || mime.contains("excel") || mime.contains("spreadsheetml") ->
                    readWorkbook(input)
                else -> throw IllegalArgumentException("仅支持 CSV / XLS / XLSX")
            }
        } ?: throw IllegalArgumentException("无法读取文件")
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
        val categories = TallyDb.database.categoryDao().getByBookId(QianjiBridge.DEFAULT_BOOK_ID)
        val fallback = categories.firstOrNull { it.type == "spending" }?.id
        val bills = mutableListOf<BillDo>()

        for (r in start until rows.size) {
            val a = rows[r]
            val moneyText = col(a, iMoney).replace("¥", "").replace("￥", "").replace(",", "").trim()
            val raw = moneyText.toDoubleOrNull() ?: continue
            val money = abs(raw)
            if (money <= 0) continue
            val typeText = norm(col(a, iType))
            val income = typeText.contains("收入") || typeText.contains("income") || typeText == "1"
            val categoryName = col(a, iCate).trim()
            val categoryId = categories.firstOrNull { it.name == categoryName }?.id ?: fallback ?: continue
            val time = parseTime(col(a, iTime).trim()).let { if (it > 0) it else now }
            val amount = (money * 100.0).roundToLong()
            bills += BillDo(
                id = "qjfile_" + UUID.randomUUID().toString(),
                userId = QianjiBridge.LOCAL_USER_ID,
                bookId = QianjiBridge.DEFAULT_BOOK_ID,
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
        if (bills.isNotEmpty()) TallyDb.database.billDao().insert(bills)
        bills.size
    }

    private fun readCsv(input: java.io.InputStream): List<List<String>> {
        val r = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val out = mutableListOf<List<String>>()
        while (true) {
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

    private fun readWorkbook(input: java.io.InputStream): List<List<String>> {
        val wb = WorkbookFactory.create(input)
        val sheet = wb.getSheetAt(0)
        val rows = mutableListOf<List<String>>()
        for (row in sheet) {
            val cells = mutableListOf<String>()
            for (i in 0 until row.lastCellNum) cells += cellToString(row.getCell(i))
            rows += cells
        }
        wb.close()
        return rows
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
        var s = uri.lastPathSegment.orEmpty().lowercase(Locale.ROOT)
        if (!s.contains(".")) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (i >= 0) c.getString(i)?.let { s = it.lowercase(Locale.ROOT) }
                    }
                }
            } catch (e: Exception) { }
        }
        return s
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
