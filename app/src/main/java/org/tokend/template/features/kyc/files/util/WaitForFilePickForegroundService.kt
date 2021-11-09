package org.tokend.template.features.kyc.files.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.tokend.template.R

/**
 * A foreground service that does nothing but keeps the app from being killed
 * while user is browsing the explorer.
 */
class WaitForFilePickForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.waiting_for_file_progress))
            .setProgress(0, 0, true)
            .setSmallIcon(R.drawable.ic_file_download_outline)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        NotificationManagerCompat.from(this)
            .cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.waiting_for_file_progress),
                NotificationManager.IMPORTANCE_LOW
            )
            NotificationManagerCompat.from(this)
                .createNotificationChannel(serviceChannel)
        }
    }

    private companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "WaitForFilePickForegroundService"
    }
}