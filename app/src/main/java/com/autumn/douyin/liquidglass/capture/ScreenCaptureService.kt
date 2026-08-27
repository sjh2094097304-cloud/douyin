package com.autumn.douyin.liquidglass.capture

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null

    override fun onCreate() {
        super.onCreate()

        startForeground(
            10001,
            createNotification(),
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val resultCode =
            intent?.getIntExtra(
                "result_code",
                Activity.RESULT_CANCELED
            )
                ?: Activity.RESULT_CANCELED

        val data =
            intent?.getParcelableExtra<Intent>(
                "projection_data"
            )

        if (
            resultCode != Activity.RESULT_OK ||
            data == null
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        startProjection(
            resultCode,
            data
        )

        return START_STICKY
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}