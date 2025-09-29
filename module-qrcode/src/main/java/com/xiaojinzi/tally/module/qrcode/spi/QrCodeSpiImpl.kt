package com.xiaojinzi.tally.module.qrcode.spi

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.component.anno.ServiceAnno
import com.xiaojinzi.component.error.RouterRuntimeException
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.component.impl.RouterRequest
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.tally.module.base.spi.QrCodeSpi
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

@RouterAnno(
    hostAndPath = "qrcode/scan",
)
fun qrcodeScan(request: RouterRequest): Intent {
    return (ActivityStack.topAlive as? FragmentActivity)?.run {
        val activityResultContract = ScanQRCode()
        activityResultContract.createIntent(
            app, null,
        )
    } ?: throw RouterRuntimeException()
}

@ServiceAnno(QrCodeSpi::class)
class QrCodeSpiImpl : QrCodeSpi {

    override suspend fun createQRCode(
        content: String,
        @ColorInt
        contentColor: Int,
        @ColorInt
        backgroundColor: Int,
        width: Int,
        height: Int,
    ): Bitmap {
        return withContext(context = Dispatchers.Default) {
            // 创建矩阵对象
            val bitMatrix: BitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                mapOf(
                    EncodeHintType.CHARACTER_SET to "utf-8",
                    EncodeHintType.MARGIN to 0,
                ),
            )
            // 创建像素数组, 并且赋值颜色
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        pixels[y * width + x] = contentColor
                    } else {
                        pixels[y * width + x] = backgroundColor
                    }
                }
            }
            createBitmap(width, height).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }
    }

    override suspend fun scanQrCode(): String {
        return (ActivityStack.topAlive as? FragmentActivity)?.run {
            val intent = Router
                .with(
                    context = this,
                )
                .hostAndPath(
                    hostAndPath = "qrcode/scan"
                )
                .requestCodeRandom()
                .resultCodeMatchAndIntentAwait(
                    expectedResultCode = Activity.RESULT_OK,
                )
            val qrResult = ScanQRCode().parseResult(
                Activity.RESULT_OK,
                intent,
            ) as? QRResult.QRSuccess ?: throw RuntimeException()
            qrResult.content.rawValue
        }?: throw RuntimeException()
    }

}