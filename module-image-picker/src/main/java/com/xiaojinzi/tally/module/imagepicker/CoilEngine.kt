package com.xiaojinzi.tally.module.imagepicker

import android.content.Context
import android.net.Uri
import java.io.File
import android.widget.ImageView
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.luck.picture.lib.engine.ImageEngine
import com.luck.picture.lib.utils.ActivityCompatHelper

/**
 * @author：luck
 * @date：2022/2/14 3:00 下午
 * @describe：CoilEngine
 */
object CoilEngine : ImageEngine {
    private fun localImage(url: String?): Any? {
        val value = url?.takeIf { it.isNotBlank() } ?: return null
        val uri = Uri.parse(value)
        return when (uri.scheme?.lowercase()) {
            "content", "file", "android.resource" -> uri
            null -> File(value).takeIf { it.isAbsolute }
            else -> null
        }
    }

    override fun loadImage(context: Context, url: String, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        val target = ImageRequest.Builder(context)
            .data(localImage(url))
            .target(imageView)
            .build()
        context.imageLoader.enqueue(target)
    }

    override fun loadImage(
        context: Context?,
        imageView: ImageView?,
        url: String?,
        maxWidth: Int,
        maxHeight: Int
    ) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        context?.let {
            val builder = ImageRequest.Builder(it)
            if (maxWidth > 0 && maxHeight > 0) {
                builder.size(maxWidth, maxHeight)
            }
            imageView?.let { v -> builder.data(localImage(url)).target(v) }
            val request = builder.build();
            context.imageLoader.enqueue(request)
        }
    }

    override fun loadAlbumCover(context: Context, url: String, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        val target = ImageRequest.Builder(context)
            .data(localImage(url))
            .transformations(RoundedCornersTransformation(8F))
            .size(180, 180)
            .placeholder(com.xiaojinzi.tally.lib.res.R.drawable.res_placeholder1)
            .target(imageView)
            .build()
        context.imageLoader.enqueue(target)
    }

    override fun loadGridImage(context: Context, url: String, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        val target = ImageRequest.Builder(context)
            .data(localImage(url))
            .size(270, 270)
            .placeholder(com.xiaojinzi.tally.lib.res.R.drawable.res_placeholder1)
            .target(imageView)
            .build()
        context.imageLoader.enqueue(target)
    }


    override fun pauseRequests(context: Context?) {

    }

    override fun resumeRequests(context: Context?) {

    }
}