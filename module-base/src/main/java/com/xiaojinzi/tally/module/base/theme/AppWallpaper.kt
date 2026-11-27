package com.xiaojinzi.tally.module.base.theme

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.AtomicFile
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** 仅在根部壁纸已成功解码并绘制时为 true，页面据此让出背景，卡片仍使用实底。 */
val LocalWallpaperAvailable = compositionLocalOf { false }
private val LocalWallpaperRootView = compositionLocalOf<View?> { null }

@Composable
fun appPageBackgroundColor(): Color = if (
    LocalWallpaperAvailable.current && LocalWallpaperRootView.current === LocalView.current.rootView
) {
    Color.Transparent
} else {
    MaterialTheme.colorScheme.background
}

@Composable
internal fun wallpaperScrimColor(): Color {
    val background = MaterialTheme.colorScheme.background
    return background.copy(alpha = if (background.luminance() < 0.5f) 0.78f else 0.82f)
}

@Composable
internal fun AppWallpaperHost(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val rootView = LocalView.current.rootView
    val state by AppWallpaperStore.state.collectAsState()
    LaunchedEffect(context) { AppWallpaperStore.ensureLoaded(context) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        state.bitmap?.let { bitmap ->
            Image(
                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.matchParentSize().background(wallpaperScrimColor()))
        }
        CompositionLocalProvider(
            LocalWallpaperAvailable provides (state.bitmap != null),
            LocalWallpaperRootView provides rootView,
        ) {
            content()
        }
    }
}

internal data class AppWallpaperState(
    val loaded: Boolean = false,
    val bitmap: Bitmap? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

/** 应用私有的单张壁纸；所有文件操作和解码均在 IO 上执行，关闭设置页不会中断保存。 */
internal object AppWallpaperStore {
    private const val MAX_SOURCE_BYTES = 32L * 1024 * 1024
    private const val MAX_SOURCE_EDGE = 65_536
    private const val MAX_SOURCE_PIXELS = 200_000_000L
    private const val MAX_DECODE_EDGE = 2_048
    private const val MAX_DECODE_PIXELS = 3_145_728L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val changing = AtomicBoolean(false)
    private val mutableState = MutableStateFlow(AppWallpaperState())
    val state = mutableState.asStateFlow()

    private fun wallpaperFile(context: Context) = AtomicFile(
        File(context.noBackupFilesDir, "app-wallpaper.png"),
    )

    fun ensureLoaded(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            mutex.withLock {
                if (mutableState.value.loaded) return@withLock
                val file = wallpaperFile(appContext)
                try {
                    // openRead 同时恢复未完成的原子写入；不存在文件时正常回退主题实底。
                    val exists = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
                    val bitmap = if (exists) {
                        file.openRead().use { }
                        decodeBounded(file.baseFile)
                    } else null
                    mutableState.update { it.copy(loaded = true, bitmap = bitmap) }
                } catch (_: Exception) {
                    mutableState.update {
                        it.copy(loaded = true, error = "壁纸无法读取，请重新选择图片或清除壁纸")
                    }
                } catch (_: OutOfMemoryError) {
                    mutableState.update {
                        it.copy(loaded = true, error = "内存不足，暂时使用主题背景，请选择较小的图片")
                    }
                }
            }
        }
    }

    fun select(context: Context, uri: Uri) = change(context) { appContext ->
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            throw WallpaperException("无法读取这张图片，请从系统图片选择器重新选择")
        }
        val source = File.createTempFile("app-wallpaper-", ".image", appContext.cacheDir)
        var bitmap: Bitmap? = null
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(source).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_SOURCE_BYTES) {
                            throw WallpaperException("图片文件超过 32 MB，请选择较小的图片")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: throw WallpaperException("图片暂时无法读取，请下载到本机后重试")
            val decoded = decodeBounded(source)
            bitmap = decoded
            val target = wallpaperFile(appContext)
            val output = target.startWrite()
            try {
                // 只保存有界、方向已修正的像素，不保存外部 URI 或原图元数据。
                if (!decoded.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw IOException("Unable to save wallpaper")
                }
                target.finishWrite(output)
            } catch (error: Throwable) {
                target.failWrite(output)
                throw error
            }
            mutableState.update { it.copy(loaded = true, bitmap = decoded, error = null) }
            bitmap = null // 已发布的位图可能仍在绘制，不主动 recycle。
        } finally {
            bitmap?.recycle()
            source.delete()
        }
    }

    fun clear(context: Context) = change(context) { appContext ->
        val file = wallpaperFile(appContext)
        file.delete()
        if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
            throw WallpaperException("壁纸清除失败，请稍后重试")
        }
        mutableState.update { it.copy(loaded = true, bitmap = null, error = null) }
    }

    private fun change(context: Context, action: (Context) -> Unit) {
        if (!changing.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        mutableState.update { it.copy(busy = true, error = null) }
        scope.launch {
            try {
                mutex.withLock { action(appContext) }
            } catch (error: WallpaperException) {
                mutableState.update { it.copy(error = error.message) }
            } catch (_: OutOfMemoryError) {
                mutableState.update { it.copy(error = "内存不足，请选择较小的图片；原壁纸已保留") }
            } catch (_: Exception) {
                mutableState.update { it.copy(error = "壁纸保存失败，请检查图片后重试；原壁纸已保留") }
            } finally {
                changing.set(false)
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private fun decodeBounded(file: File): Bitmap {
        if (file.length() !in 1..MAX_SOURCE_BYTES) {
            throw WallpaperException("图片为空或文件过大，请重新选择")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) throw WallpaperException("不支持这张图片，请选择其他图片")
        if (width > MAX_SOURCE_EDGE || height > MAX_SOURCE_EDGE || width.toLong() * height > MAX_SOURCE_PIXELS) {
            throw WallpaperException("图片尺寸过大，请缩小后重试")
        }
        var sample = 1
        while (
            (width.toLong() + sample - 1) / sample > MAX_DECODE_EDGE ||
            (height.toLong() + sample - 1) / sample > MAX_DECODE_EDGE ||
            ((width.toLong() + sample - 1) / sample) * ((height.toLong() + sample - 1) / sample) > MAX_DECODE_PIXELS
        ) {
            sample *= 2
        }
        val bitmap = BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            },
        ) ?: throw WallpaperException("图片已损坏或格式不受支持，请选择其他图片")
        try {
            if (bitmap.width > MAX_DECODE_EDGE || bitmap.height > MAX_DECODE_EDGE || bitmap.width.toLong() * bitmap.height > MAX_DECODE_PIXELS) {
                throw WallpaperException("图片尺寸过大，请缩小后重试")
            }
            val orientation = try {
                ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (_: IOException) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                }
            }
            if (matrix.isIdentity) return bitmap
            val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (oriented !== bitmap) bitmap.recycle()
            return oriented
        } catch (error: Throwable) {
            bitmap.recycle()
            throw error
        }
    }

    private class WallpaperException(message: String) : IOException(message)
}
