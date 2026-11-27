package com.xiaojinzi.tally.module.qianji

import kotlinx.coroutines.runBlocking
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class QianjiFileImporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun parsesExtensionlessXlsWithGenericMime() = runBlocking {
        val source = xlsFile()
        val format = QianjiFileImporter.resolveFormat(source, "download", "application/octet-stream")

        assertEquals(ExternalImportPolicy.Format.Xls, format)
        assertEquals(expectedRows, QianjiFileImporter.readRows(source, format))
    }

    @Test
    fun parsesXlsDespiteCsvNameAndMimeHints() = runBlocking {
        val source = xlsFile()
        for (name in listOf("bills.xls", "bills.csv")) {
            val format = QianjiFileImporter.resolveFormat(source, name, "text/csv")

            assertEquals(ExternalImportPolicy.Format.Xls, format)
            assertEquals(expectedRows, QianjiFileImporter.readRows(source, format))
        }
    }

    @Test
    fun parsesExtensionlessCsvWithMimeAliasesAndParameters() = runBlocking {
        val source = csvFile()
        for (mime in listOf(
            "Text/Comma-Separated-Values; header=present",
            " TEXT/CSV ; charset=UTF-8",
            "text/plain; charset=UTF-8",
        )) {
            val format = QianjiFileImporter.resolveFormat(source, "download", mime)

            assertEquals(ExternalImportPolicy.Format.Csv, format)
            assertEquals(expectedRows, QianjiFileImporter.readRows(source, format))
        }
    }

    @Test
    fun parsesCsvWhenMimeParameterMentionsXlsx() = runBlocking {
        val source = csvFile()
        val format = QianjiFileImporter.resolveFormat(
            source,
            "download",
            "TEXT/CSV; name=spreadsheetml.xlsx; charset=UTF-8",
        )

        assertEquals(ExternalImportPolicy.Format.Csv, format)
        assertEquals(expectedRows, QianjiFileImporter.readRows(source, format))
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesSqliteInSpreadsheetParser() {
        val source = temporaryFolder.newFile().apply {
            writeText("SQLite format 3\u0000", Charsets.US_ASCII)
        }
        runBlocking {
            QianjiFileImporter.readRows(source, ExternalImportPolicy.Format.Sqlite)
        }
    }

    private val expectedRows = listOf(
        listOf("日期", "金额"),
        listOf("2026-01-01", "12.5"),
    )

    private fun csvFile(): File = temporaryFolder.newFile().apply {
        writeText("\uFEFF日期,金额\r\n2026-01-01,12.5\r\n", Charsets.UTF_8)
    }

    private fun xlsFile(): File = temporaryFolder.newFile().apply {
        HSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("账单")
            expectedRows.forEachIndexed { rowIndex, values ->
                val row = sheet.createRow(rowIndex)
                values.forEachIndexed { column, value -> row.createCell(column).setCellValue(value) }
            }
            outputStream().use { workbook.write(it) }
        }
    }
}
