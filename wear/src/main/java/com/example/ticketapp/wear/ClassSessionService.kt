package com.example.ticketapp.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.wear.ongoing.OngoingActivity

/** Keeps a deliberately started class accessible; performs no background work or wake locks. */
class ClassSessionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).getBoolean(SESSION_ACTIVE, false) || !canNotify(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Class session", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps Ticket Toss available until End Session."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        })
        val touchIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_ticket_shortcut)
            .setContentTitle("Ticket Toss · class active")
            .setContentText("Tap to return. End Session in the app to finish.")
            .setContentIntent(touchIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        OngoingActivity.Builder(this, NOTIFICATION_ID, notification)
            .setStaticIcon(R.drawable.ic_ticket_shortcut)
            .setTouchIntent(touchIntent)
            .build().apply(this)
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification.build(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        private const val CHANNEL = "class_session"
        fun canNotify(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
}
