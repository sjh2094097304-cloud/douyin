package com.autumn.douyin.liquidglass.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

class MediaProjectionController(
    private val context: Context
) {

    private val manager =
        context.getSystemService(
            MediaProjectionManager::class.java
        )

    fun createPermissionIntent(): Intent {
        return manager.createScreenCaptureIntent()
    }

    fun getManager(): MediaProjectionManager {
        return manager
    }

    companion object {
        const val REQUEST_CODE = 9527
    }
}