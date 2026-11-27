package com.xiaojinzi.tally.module.base.support

import android.content.Context
import com.xiaojinzi.module.common.base.spi.SPSpi
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.tally.lib.res.model.exception.CommonBusinessException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本地账本的稳定归属标识，只用于延续已有数据库和数据行，不代表账号或登录状态。
 */
object LocalLedgerIdentity {

    private const val OWNER_ID_KEY = "localLedgerOwnerId"
    private const val LEGACY_OWNER_ID_KEY = "latestUserId"
    private const val DEFAULT_OWNER_ID = "opensourceUserId"
    private const val DATABASE_PREFIX = "tally_"
    private val databaseSidecarSuffixes = listOf("-wal", "-shm", "-journal")
    private val accountKeys = listOf("userToken", "userInfo", "userVipInfo")
    private val identityMutex = Mutex()
    private val resolvedOwnerIdStateOb = MutableSharedFlow<String>(replay = 1)

    /**
     * 不设置默认初始值；旧偏好读取和本地身份持久化完成后才发出第一个值。
     * 加载失败向收集方抛出异常，允许启动页提示并重试。
     */
    val ownerIdStateOb: Flow<String> = flow {
        requiredId()
        emitAll(resolvedOwnerIdStateOb)
    }

    suspend fun requiredId(): String = identityMutex.withLock {
        resolvedOwnerIdStateOb.replayCache.firstOrNull()?.let {
            return@withLock it
        }
        val ownerId = withContext(Dispatchers.IO) {
            loadAndMigrateOwnerId()
        }
        resolvedOwnerIdStateOb.emit(ownerId)
        ownerId
    }

    private fun loadAndMigrateOwnerId(): String {
        // 与旧 spPersistence 使用相同的 SP_DEFAULT 文件和未加前缀的键。
        val preferences = app.getSharedPreferences(
            SPSpi.DEFAULT_FILE_NAME,
            Context.MODE_PRIVATE,
        )
        val localOwnerId = preferences.getString(OWNER_ID_KEY, null)
            ?.takeIf { it.isNotBlank() }
        val latestUserId = preferences.getString(LEGACY_OWNER_ID_KEY, null)
            ?.takeIf { it.isNotBlank() }
        val primaryFiles = readLedgerDatabaseFiles()
        val savedOwnerId = localOwnerId ?: latestUserId
        val ownerId = savedOwnerId ?: recoverOwnerIdFromDatabaseNames(primaryFiles)
        if (ownerId.any { it == '/' || it == '\\' || it == '\u0000' }) {
            throw CommonBusinessException(message = "本地账本标识异常，已保留原数据，请先备份后检查")
        }
        if (savedOwnerId != null &&
            primaryFiles.none { it.name == "$DATABASE_PREFIX$ownerId" } &&
            primaryFiles.any { it.name.startsWith(DATABASE_PREFIX) }
        ) {
            throw CommonBusinessException(message = "原账本库已缺失，但检测到其他旧账本，已保留全部数据，请先备份后确认要恢复的账本")
        }
        // 先确保身份落盘；即使后续清理中断，下次也仍能找到相同账本。
        if (!preferences.edit().putString(OWNER_ID_KEY, ownerId).commit()) {
            throw CommonBusinessException(message = "无法保存本地账本标识，请检查存储空间后重试")
        }
        if (accountKeys.any { preferences.contains(it) }) {
            val editor = preferences.edit()
            accountKeys.forEach { editor.remove(it) }
            if (!editor.commit()) {
                throw CommonBusinessException(message = "无法清理旧账号信息，请检查存储空间后重试")
            }
        }
        return ownerId
    }

    private fun readLedgerDatabaseFiles(): List<File> {
        val databaseDirectory = app.getDatabasePath("tally").parentFile
            ?: throw CommonBusinessException(message = "无法读取本地账本目录，请重试")
        val databaseFiles = if (databaseDirectory.exists()) {
            databaseDirectory.listFiles()?.filter { it.isFile }
                ?: throw CommonBusinessException(message = "无法读取本地账本目录，请重试")
        } else {
            emptyList()
        }
        val ledgerFiles = databaseFiles.filter {
            it.name == "tally" || it.name.startsWith(DATABASE_PREFIX) ||
                databaseSidecarSuffixes.any { suffix -> it.name == "tally$suffix" }
        }
        val primaryFiles = ledgerFiles.filter { file ->
            databaseSidecarSuffixes.none { file.name.endsWith(it) }
        }
        // WAL/SHM 可能包含尚未归并的数据，主文件缺失时不能创建空库覆盖现场。
        val hasOrphanSidecar = ledgerFiles.any { file ->
            val suffix = databaseSidecarSuffixes.firstOrNull { file.name.endsWith(it) }
            suffix != null && primaryFiles.none { it.name == file.name.removeSuffix(suffix) }
        }
        if (hasOrphanSidecar) {
            throw CommonBusinessException(message = "检测到账本恢复文件但缺少主数据库，已保留全部文件，请先备份后恢复")
        }
        return primaryFiles
    }

    private fun recoverOwnerIdFromDatabaseNames(primaryFiles: List<File>): String {
        if (primaryFiles.size > 1) {
            throw CommonBusinessException(message = "检测到多个旧账本库，无法确定原账本，已保留全部数据，请先备份后确认要恢复的账本")
        }
        val databaseName = primaryFiles.singleOrNull()?.name ?: return DEFAULT_OWNER_ID
        // 无后缀的旧 tally 由数据源原地打开，本地身份不重命名或删除任何文件。
        if (databaseName == "tally") {
            return DEFAULT_OWNER_ID
        }
        return databaseName.removePrefix(DATABASE_PREFIX).takeIf { it.isNotBlank() }
            ?: throw CommonBusinessException(message = "旧账本文件名异常，已保留原数据，请先备份后检查")
    }

}
