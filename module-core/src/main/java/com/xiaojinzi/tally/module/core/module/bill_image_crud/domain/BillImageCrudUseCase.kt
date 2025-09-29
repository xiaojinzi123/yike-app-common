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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

@Keep
data class BillImageCrudImageUseCaseDto(
    val uid: String = newUUid(),
    // 远程的地址
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
        val tempFolder = File(
            app.cacheDir,
            "billImageTemp",
        ).apply {
            this.deleteRecursively()
            this.mkdirs()
        }
        val resultUrlList = currentImageList
            .map { item ->
                if (item.url.isNullOrEmpty()) {
                    item.localUri!!.copyFileTo(
                        destFile = File(
                            tempFolder,
                            "${newUUid()}.${item.fileName?.extension.orEmpty()}"
                        )
                    ).let { targetFile ->
                        val options = BitmapFactory.Options().apply {
                            this.inJustDecodeBounds = true
                        }
                        // 获取目标图片的宽高
                        BitmapFactory.decodeFile(
                            targetFile.path, options,
                        )
                        val (imageWidth, imageHeight) = options.outWidth to options.outHeight
                        LogSupport.d(
                            tag = BillImageCrudUseCase.TAG,
                            content = "imageWidth = $imageWidth, imageHeight = $imageHeight",
                        )
                        // 如果需要压缩, 就压缩一下
                        if (imageWidth > 2000) {
                            val targetFileAfterCompressed = File(
                                targetFile.parentFile,
                                "_${targetFile.name}",
                            )
                            CommonServices
                                .ffmpegSpi
                                ?.executeCommand(
                                    command = "ffmpeg -i ${targetFile.path} -vf scale=1600:-1 ${targetFileAfterCompressed.path}",
                                )
                            targetFileAfterCompressed
                        } else {
                            targetFile
                        }.let { targetFileAfterCompressed ->
                            // 进行上传
                            val targetUrl = CommonServices
                                .aliOssSpi
                                ?.simpleUpload(
                                    file = targetFileAfterCompressed,
                                )
                            item.copy(
                                url = targetUrl,
                            )
                        }
                    }
                } else {
                    item
                }
            }
            .mapNotNull { it.url.orNull() }
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