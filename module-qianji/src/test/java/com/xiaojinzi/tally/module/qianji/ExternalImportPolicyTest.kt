package com.xiaojinzi.tally.module.qianji

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ExternalImportPolicyTest {
    private val csv = "日期,金额\r\n2026-01-01,12.50\r\n".toByteArray(Charsets.UTF_8)
    private val sqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    private val oleHeader = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(),
        0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(),
    )

    @Test
    fun acceptsCsvAndTxtExtensionsWithoutRelyingOnMime() {
        for (name in listOf("bills.csv", "bills.CSV", "bills.txt", "bills.TXT")) {
            assertEquals(
                name,
                ExternalImportPolicy.Format.Csv,
                ExternalImportPolicy.validate(name, "application/octet-stream", csv),
            )
        }
    }

    @Test
    fun acceptsUtf8BomBeforeCsvColumns() {
        val header = "\uFEFF \t日期,金额\r\n2026-01-01,12.50\r\n".toByteArray(Charsets.UTF_8)
        assertEquals(
            ExternalImportPolicy.Format.Csv,
            ExternalImportPolicy.validate("bills.csv", "text/csv", header),
        )
    }

    @Test
    fun rejectsTextWithoutCommaSeparatedColumns() {
        for (text in listOf("日期\t金额", "日期;金额", " \t\r\n", "\uFEFF")) {
            assertRejected("CSV without commas: $text") {
                ExternalImportPolicy.validate("bills.csv", "text/csv", text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    @Test
    fun acceptsExtensionlessCsvForSupportedMimeTypesWithCaseAndParameters() {
        for (mime in listOf(
            " TEXT/CSV ; Charset=UTF-8",
            "APPLICATION/CSV;charset=utf-8",
            "Text/Comma-Separated-Values; header=present",
            "  tExT/pLaIn ; charset=UTF-8; name=bills.csv",
        )) {
            assertEquals(
                mime,
                ExternalImportPolicy.Format.Csv,
                ExternalImportPolicy.validate("download", mime, csv),
            )
        }
    }

    @Test
    fun ignoresXlsxWordsInsideCsvMimeParameters() {
        assertEquals(
            ExternalImportPolicy.Format.Csv,
            ExternalImportPolicy.validate(
                "download",
                "TEXT/CSV; name=spreadsheetml.xlsx; charset=UTF-8",
                csv,
            ),
        )
    }

    @Test
    fun rejectsExtensionlessCsvWithoutAnExactSupportedMime() {
        for (mime in listOf(
            "",
            "application/octet-stream",
            "text/csv-extra",
            "application/octet-stream; type=text/csv",
        )) {
            assertRejected("extensionless CSV with MIME $mime") {
                ExternalImportPolicy.validate("download", mime, csv)
            }
        }
    }

    @Test
    fun acceptsCompleteSqliteHeaderForDatabaseNames() {
        for (name in listOf("bills.db", "bills.SQLITE", "bills.sqlite3", "qianjiapp", "QIANJIAPP")) {
            assertEquals(
                name,
                ExternalImportPolicy.Format.Sqlite,
                ExternalImportPolicy.validate(name, "application/octet-stream", sqliteHeader),
            )
        }
    }

    @Test
    fun sqliteHeaderTakesPriorityOverCsvAndXlsHints() {
        for (name in listOf("bills.csv", "bills.xls", "download")) {
            assertEquals(
                name,
                ExternalImportPolicy.Format.Sqlite,
                ExternalImportPolicy.validate(name, "text/csv", sqliteHeader),
            )
        }
    }

    @Test
    fun rejectsSqliteHeaderWithoutItsExactTerminatingNul() {
        val headers = listOf(
            sqliteHeader.copyOf(sqliteHeader.size - 1),
            sqliteHeader.copyOf().apply { this[lastIndex] = 1 },
        )
        for ((index, header) in headers.withIndex()) {
            assertRejected("invalid SQLite header $index") {
                ExternalImportPolicy.validate("bills.db", "application/octet-stream", header)
            }
        }
    }

    @Test
    fun rejectsDatabaseNamesWithoutSqliteHeader() {
        for (name in listOf("bills.db", "bills.sqlite", "bills.SQLITE3", "qianjiapp", "QIANJIAPP")) {
            for ((index, header) in listOf(csv, oleHeader, byteArrayOf()).withIndex()) {
                assertRejected("$name without SQLite header, sample $index") {
                    ExternalImportPolicy.validate(name, "text/csv", header)
                }
            }
        }
    }

    @Test
    fun recognizesOleHeaderAsXlsRegardlessOfCsvHints() {
        for (name in listOf("bills.xls", "bills.XLS", "bills.csv", "download")) {
            assertEquals(
                name,
                ExternalImportPolicy.Format.Xls,
                ExternalImportPolicy.validate(name, "text/csv", oleHeader),
            )
        }
    }

    @Test
    fun rejectsTruncatedAndCorruptOleHeaders() {
        val headers = listOf(
            oleHeader.copyOf(1),
            oleHeader.copyOf(oleHeader.size - 1),
            oleHeader.copyOf().apply { this[lastIndex] = 0 },
        )
        for ((index, header) in headers.withIndex()) {
            assertRejected("invalid OLE header $index") {
                ExternalImportPolicy.validate("bills.xls", "application/vnd.ms-excel", header)
            }
        }
    }

    @Test
    fun rejectsXlsExtensionWhenContentIsCsv() {
        assertRejected("CSV disguised as XLS") {
            ExternalImportPolicy.validate("bills.XLS", "application/vnd.ms-excel", csv)
        }
    }

    @Test
    fun rejectsXlsxExtensionInMetadataAndContentValidation() {
        for (name in listOf("bills.xlsx", "bills.XLSX")) {
            assertRejected("XLSX metadata: $name") {
                ExternalImportPolicy.validateMetadata(name, "text/csv")
            }
            for ((index, header) in listOf(csv, sqliteHeader, oleHeader).withIndex()) {
                assertRejected("XLSX content: $name, sample $index") {
                    ExternalImportPolicy.validate(name, "text/csv", header)
                }
            }
        }
    }

    @Test
    fun rejectsXlsxMimeWithCaseWhitespaceAndParameters() {
        for (mime in listOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            " Application/Vnd.OpenXmlFormats-Officedocument.SpreadsheetML.Sheet ; charset=UTF-8",
        )) {
            assertRejected("XLSX MIME metadata: $mime") {
                ExternalImportPolicy.validateMetadata("bills.csv", mime)
            }
            assertRejected("XLSX MIME content: $mime") {
                ExternalImportPolicy.validate("bills.csv", mime, csv)
            }
        }
    }

    @Test
    fun rejectsZipSignaturesEvenWhenFollowedByCsvColumns() {
        val signatures = listOf(
            byteArrayOf(0x50, 0x4B, 0x03, 0x04),
            byteArrayOf(0x50, 0x4B, 0x05, 0x06),
            byteArrayOf(0x50, 0x4B, 0x07, 0x08),
        )
        for ((index, signature) in signatures.withIndex()) {
            assertRejected("ZIP signature $index disguised as CSV") {
                ExternalImportPolicy.validate("bills.csv", "text/csv", signature + csv)
            }
        }
    }

    @Test
    fun rejectsHtmlWithCommasIncludingBomAndLeadingWhitespace() {
        for (text in listOf(
            "<html><body>date,amount</body></html>",
            " \r\n\t<!DOCTYPE html><html><body>date,amount</body></html>",
            "\uFEFF \t<html><body>date,amount</body></html>",
        )) {
            assertRejected("HTML disguised as CSV: $text") {
                ExternalImportPolicy.validate("bills.csv", "text/csv", text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    @Test
    fun rejectsEmptyHeader() {
        assertRejected("empty CSV") {
            ExternalImportPolicy.validate("bills.csv", "text/csv", byteArrayOf())
        }
    }

    @Test
    fun rejectsNulAtBeginningMiddleAndEndOfOtherwiseValidCsv() {
        for (offset in listOf(0, csv.size / 2, csv.size)) {
            val header = csv.copyOfRange(0, offset) + byteArrayOf(0) + csv.copyOfRange(offset, csv.size)
            assertRejected("NUL at CSV byte $offset") {
                ExternalImportPolicy.validate("bills.csv", "text/csv", header)
            }
        }
    }

    @Test
    fun rejectsUnknownExtensionsEvenWithRecognizedContentAndCsvMime() {
        for (name in listOf("bills.pdf", "bills.zip", "bills.csv.exe")) {
            assertRejected("unknown extension metadata: $name") {
                ExternalImportPolicy.validateMetadata(name, "text/csv")
            }
            for ((index, header) in listOf(csv, sqliteHeader, oleHeader).withIndex()) {
                assertRejected("unknown extension content: $name, sample $index") {
                    ExternalImportPolicy.validate(name, "text/csv", header)
                }
            }
        }
    }

    private fun assertRejected(description: String, action: () -> Unit) {
        try {
            action()
        } catch (_: IllegalArgumentException) {
            return
        }
        fail("Expected IllegalArgumentException: $description")
    }
}
