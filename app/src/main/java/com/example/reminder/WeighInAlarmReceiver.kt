package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class WeighInAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showNotification(
            context = context,
            title = "Panggilan Guild: Waktunya Timbang Badan",
            content = "Timbang pagi ini sebelum makan, lalu catat di tab Petualangan.",
            notificationId = ReminderManager.WEIGH_IN_NOTIFICATION_ID
        )

        // Reschedule next occurrence one week later based on configured day/time (no drift)
        if (ReminderManager.isWeighInReminderEnabled(context)) {
            val day = ReminderManager.getWeighInReminderDay(context)
            val (hour, minute) = ReminderManager.getWeighInReminderTime(context)
            ReminderManager.scheduleWeighInAlarm(context, day, hour, minute)
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        content: String,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("WeighInAlarmReceiver", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        ReminderManager.createWeighInNotificationChannel(context)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderManager.WEIGH_IN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
