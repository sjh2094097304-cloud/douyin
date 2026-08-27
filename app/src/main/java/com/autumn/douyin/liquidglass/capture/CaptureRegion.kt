package com.autumn.douyin.liquidglass.capture

data class CaptureRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
        get() = (right - left).coerceAtLeast(1)

    val height: Int
        get() = (bottom - top).coerceAtLeast(1)
}