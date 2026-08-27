package com.autumn.douyin.liquidglass.capture

import android.graphics.Bitmap
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread

class ImageReaderFrameSource(
    private val width: Int,
    private val height: Int
) {

    private val thread =
        HandlerThread("LiquidGlassCapture").apply {
            start()
        }

    private val handler =
        android.os.Handler(
            thread.looper
        )

    private val reader =
        ImageReader.newInstance(
            width,
            height,
            android.graphics.PixelFormat.RGBA_8888,
            2
        )

    fun surface() =
        reader.surface

    fun start(
        onFrame: (Bitmap) -> Unit
    ) {

        reader.setOnImageAvailableListener(
            { imageReader ->

                val image =
                    imageReader.acquireLatestImage()
                        ?: return@setOnImageAvailableListener

                try {
                    val plane =
                        image.planes[0]

                    val buffer =
                        plane.buffer

                    val pixelStride =
                        plane.pixelStride

                    val rowStride =
                        plane.rowStride

                    val rowPadding =
                        rowStride -
                            pixelStride * width

                    val bitmapWidth =
                        width +
                            rowPadding / pixelStride

                    val bitmap =
                        Bitmap.createBitmap(
                            bitmapWidth,
                            height,
                            Bitmap.Config.ARGB_8888
                        )

                    bitmap.copyPixelsFromBuffer(
                        buffer
                    )

                    onFrame(bitmap)

                } finally {
                    image.close()
                }

            },
            handler
        )
    }

    fun stop() {
        reader.close()
        thread.quitSafely()
    }
}