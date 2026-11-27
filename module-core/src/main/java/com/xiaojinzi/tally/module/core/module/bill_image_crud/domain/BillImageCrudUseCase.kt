package com.xiaojinzi.tally.module.core.module.bill_image_crud.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.Keep
import androidx.annotation.UiContext
import com.xiaojinzi.module.common.base.support.CommonServices
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.support.ktx.copyFileTo
import com.xiaojinzi.support.ktx.extension
import com.xiaojinzi.support.ktx.getActivity
import com.xiaojinzi.support.ktx.newUUid
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

@Keep
data class BillImageCrudImageUseCaseDto(
    val uid: String = newUUid(),
    // 已保存的图片地址；新增图片使用应用私有目录中的 file URI。
    val url: String?,
    val localUri: Uri?,
    val fileName: String? = null,
)

sealed class BillImageCrudIntent {

    data class ParameterInit(
        @UiContext val imageUrlList: List<String>,
    ) : BillImageCrudIntent()

    data class Submit(
        @UiContext val context: Context,
    ) : BillImageCrudIntent()

    data class ImageSelect(
        @UiContext val context: Context,
    ) : BillImageCrudIntent()

    data class ImageDelete(
        val uid: String,
    ) : BillImageCrudIntent()

}

@ViewModelLayer
interface BillImageCrudUseCase : BusinessMVIUseCase {

    companion object {
        const val TAG = "BillImageCrudUseCase"
        const val IMAGE_COUNT = 3
    }

    /**
     * 图片的列表
     */
    @StateHotObservable
    val imageListStateOb: Flow<List<BillImageCrudImageUseCaseDto>>

}

@ViewModelLayer
class BillImageCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillImageCrudUseCase {

    override val imageListStateOb = MutableSharedStateFlow<List<BillImageCrudImageUseCaseDto>>(
        initValue = emptyList(),
    )

    @IntentProcess
    private suspend fun parameterInit(intent: BillImageCrudIntent.ParameterInit) {
        imageListStateOb.emit(
            value = intent.imageUrlList.map {
                BillImageCrudImageUseCaseDto(
                    url = it,
                    localUri = null,
                )
            }
        )
    }

    @IntentProcess
    @BusinessMVIUseCase.ErrorIgnore
    private suspend fun imageSelect(intent: BillImageCrudIntent.ImageSelect) {

        val currentImageList = imageListStateOb.first()

        val selectUriList = AppServices
            .imagePickerSpi
            ?.selectImage(
                context = intent.context,
                count = BillImageCrudUseCase.IMAGE_COUNT - currentImageList.size,
            )

        imageListStateOb.emit(
            value = (currentImageList + (selectUriList?.map {
                BillImageCrudImageUseCaseDto(
                    url = null,
                    localUri = it.uri,
                    fileName = it.fileName,
                )
            } ?: emptyList())).take(
                n = BillImageCrudUseCase.IMAGE_COUNT,
            )
        )

    }

    @IntentProcess
    private suspend fun imageDelete(intent: BillImageCrudIntent.ImageDelete) {
        val currentImageList = imageListStateOb.first()
        imageListStateOb.emit(
            value = currentImageList.filter {
                it.uid != intent.uid
            }
        )
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: BillImageCrudIntent.Submit) {
        val currentImageList = imageListStateOb.first()
        val resultImageList = withContext(Dispatchers.IO) {
            val imageFolder = File(app.filesDir, "bill_images")
            check(imageFolder.isDirectory || imageFolder.mkdirs()) {
                "无法创建本地图片目录"
            }
            val tempFolder = File(app.cacheDir, "billImageTemp/${newUUid()}")
            check(tempFolder.mkdirs()) { "无法创建图片临时目录" }
            val savedFiles = mutableListOf<File>()
            try {
                currentImageList.map { item ->
                    if (!item.url.isNullOrEmpty()) {
                        item
                    } else {
                        val localUri = requireNotNull(item.localUri) { "图片不存在" }
                        require(localUri.scheme in setOf("content", "file", "android.resource")) {
                            "请选择本地图片"
                        }
                        val extension = item.fileName?.extension.orEmpty()
                            .lowercase()
                            .takeIf { it.matches(Regex("[a-z0-9]{1,10}")) }
                            ?: "img"
                        val sourceFile = localUri.copyFileTo(
                            destFile = File(tempFolder, "${newUUid()}.$extension"),
                        )
                        check(sourceFile.isFile && sourceFile.length() > 0L) { "无法读取图片" }
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(sourceFile.path, options)
                        check(options.outWidth > 0 && options.outHeight > 0) { "图片格式不支持" }
                        val imageFile = if (options.outWidth > 2000) {
                            val compressedFile = File(tempFolder, "${newUUid()}.jpg")
                            try {
                                CommonServices.ffmpegSpi?.executeCommand(
                                    command = "ffmpeg -i ${sourceFile.path} -vf scale=1600:-1 ${compressedFile.path}",
                                )
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Exception) {
                                // 压缩不可用时保留原图。
                            }
                            val compressedOptions = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeFile(compressedFile.path, compressedOptions)
                            compressedFile.takeIf {
                                it.isFile && it.length() > 0L && compressedOptions.outWidth > 0
                            } ?: sourceFile
                        } else {
                            sourceFile
                        }
                        val savedFile = File(imageFolder, "${newUUid()}.${imageFile.extension}")
                        savedFiles.add(savedFile)
                        imageFile.copyTo(target = savedFile)
                        item.copy(
                            url = Uri.fromFile(savedFile).toString(),
                            localUri = null,
                        )
                    }
                }
            } catch (exception: Exception) {
                savedFiles.forEach { it.delete() }
                throw exception
            } finally {
                tempFolder.deleteRecursively()
            }
        }
        imageListStateOb.emit(value = resultImageList)
        val resultUrlList = resultImageList.mapNotNull { it.url.orNull() }
        LogSupport.d(
            tag = BillImageCrudUseCase.TAG,
            content = "resultUrlList = ${resultUrlList.joinToString()}",
        )
        intent.context.getActivity()?.apply {
            this.setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    this.putStringArrayListExtra(
                        "data", ArrayList(resultUrlList)
                    )
                }
            )
            this.finish()
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}