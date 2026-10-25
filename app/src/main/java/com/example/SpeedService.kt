package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * SpeedService runs as a foreground service maintaining a persistent low-priority
 * notification. It displays current playback speed and provides direct actions:
 * - "Show pill": Restores the floating pill if hidden and brings the app to focus.
 * - "Quit": Performs complete shutdown (stopping service, destroying webview, finishing tasks,
 *   and terminating process).
 */
class SpeedService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var currentSpeed: Float = SpeedPrefs.DEFAULT_SPEED

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_QUIT -> {
                performShutdown()
                return START_NOT_STICKY
            }

            ACTION_SHOW_PILL -> {
                // Restore pill visibility in preferences and notify MainActivity
                val prefs = SpeedPrefs(this)
                prefs.isPillVisible = true

                val showIntent = Intent(this, MainActivity::class.java).apply {
                    this.action = ACTION_SHOW_PILL
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(showIntent)

                // Refresh notification
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, prefs.speed)
                startForeground(NOTIFICATION_ID, buildNotification(currentSpeed))
            }

            ACTION_UPDATE_SPEED -> {
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, currentSpeed)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(currentSpeed))
            }

            else -> {
                // Normal start or foreground service initialization
                currentSpeed = intent?.getFloatExtra(EXTRA_SPEED, currentSpeed) ?: SpeedPrefs(this).speed
                startForeground(NOTIFICATION_ID, buildNotification(currentSpeed))
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(speed: Float): Notification {
        val speedText = String.format(Locale.US, "%.2fx", speed)

        // PendingIntent to launch / return to MainActivity
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Show Pill
        val showPillIntent = Intent(this, SpeedService::class.java).apply {
            action = ACTION_SHOW_PILL
            putExtra(EXTRA_SPEED, speed)
        }
        val showPillPendingIntent = PendingIntent.getService(
            this,
            1,
            showPillIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Quit App
        val quitIntent = Intent(this, SpeedService::class.java).apply {
            action = ACTION_QUIT
        }
        val quitPendingIntent = PendingIntent.getService(
            this,
            2,
            quitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_speed)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Playback speed: $speedText (Crunchyroll)")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_notification_speed, getString(R.string.action_show_pill), showPillPendingIntent)
            .addAction(R.drawable.ic_power_settings, getString(R.string.action_quit), quitPendingIntent)
            .build()
    }

    private fun performShutdown() {
        // 1. Remove foreground state and cancel notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()

        // 2. Trigger complete teardown of MainActivity and kill process
        MainActivity.requestFullShutdown(this)
    }

    override fun onDestroy() {
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "cr_speed_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.crspeed.action.START"
        const val ACTION_UPDATE_SPEED = "com.example.crspeed.action.UPDATE_SPEED"
        const val ACTION_SHOW_PILL = "com.example.crspeed.action.SHOW_PILL"
        const val ACTION_QUIT = "com.example.crspeed.action.QUIT"

        const val EXTRA_SPEED = "extra_speed"

        fun start(context: Context, speed: Float) {
            val intent = Intent(context, SpeedService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SPEED, speed)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateSpeed(context: Context, speed: Float) {
            val intent = Intent(context, SpeedService::class.java).apply {
                action = ACTION_UPDATE_SPEED
                putExtra(EXTRA_SPEED, speed)
            }
            context.startService(intent)
        }

        fun quit(context: Context) {
            val intent = Intent(context, SpeedService::class.java).apply {
                action = ACTION_QUIT
            }
            context.startService(intent)
        }
    }
}
