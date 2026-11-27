package com.xiaojinzi.tally.module.qianji

import java.util.Locale

/** File names and MIME types are hints; validate the cached bytes before the importer writes. */
internal object ExternalImportPolicy {
    enum class Format { Csv, Xls, Sqlite }

    fun validateMetadata(name: String, mime: String) {
        val lowerName = name.lowercase(Locale.ROOT)
        val lowerMime = mime.substringBefore(';').trim().lowercase(Locale.ROOT)
        require(!lowerName.endsWith(".xlsx") && !lowerMime.contains("spreadsheetml")) {
            "暂不支持 XLSX，请先转换为 CSV 或 XLS"
        }
        val extension = lowerName.substringAfterLast('.', "")
        require(extension in setOf("", "csv", "txt", "xls", "db", "sqlite", "sqlite3") || lowerName == "qianjiapp") {
            "仅支持 CSV、XLS 和未加密的钱迹数据库"
        }
    }

    fun validate(name: String, mime: String, header: ByteArray): Format {
        validateMetadata(name, mime)
        val lowerName = name.lowercase(Locale.ROOT)
        val lowerMime = mime.substringBefore(';').trim().lowercase(Locale.ROOT)
        val sqlite = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        val ole = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())
        fun startsWith(prefix: ByteArray) = header.size >= prefix.size && prefix.indices.all { header[it] == prefix[it] }
        val extension = lowerName.substringAfterLast('.', "")
        require(extension in setOf("", "csv", "txt", "xls", "db", "sqlite", "sqlite3") || lowerName == "qianjiapp") {
            "仅支持 CSV、XLS 和未加密的钱迹数据库"
        }
        if (startsWith(sqlite)) return Format.Sqlite
        require(extension !in setOf("db", "sqlite", "sqlite3") && lowerName != "qianjiapp") {
            "这份文件不是未加密的 SQLite 数据库"
        }
        if (startsWith(ole)) return Format.Xls
        require(extension != "xls") { "这份文件不是可读取的 XLS 表格" }
        require(header.isNotEmpty() && header.none { it == 0.toByte() } && !startsWith(byteArrayOf(0x50, 0x4B))) {
            "文件内容不是可读取的 CSV、XLS 或钱迹数据库"
        }
        require(extension in setOf("csv", "txt") || lowerMime in setOf("text/csv", "application/csv", "text/comma-separated-values", "text/plain")) {
            "无法确定文件格式，请选择 CSV、XLS 或钱迹数据库"
        }
        val text = header.toString(Charsets.UTF_8).removePrefix("\uFEFF").trimStart()
        require(!text.startsWith("<") && text.contains(',')) { "CSV 文件需要包含以逗号分隔的账单列" }
        return Format.Csv
    }
}
