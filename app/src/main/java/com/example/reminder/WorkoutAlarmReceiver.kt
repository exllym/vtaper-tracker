package com.example.reminder

import android.app.AlarmManager
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
import java.time.DayOfWeek

class WorkoutAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dayOfWeekName = intent.getStringExtra("day_of_week") ?: DayOfWeek.MONDAY.name
        val requestCode = intent.getIntExtra("request_code", 101)
        val dayOfWeek = try {
            DayOfWeek.valueOf(dayOfWeekName)
        } catch (_: Exception) {
            DayOfWeek.MONDAY
        }

        val bodyText = when (dayOfWeek) {
            DayOfWeek.MONDAY, DayOfWeek.THURSDAY -> "Quest Upper Body menanti di papan misi!"
            DayOfWeek.TUESDAY, DayOfWeek.FRIDAY -> "Quest Lower Body menanti di papan misi!"
            else -> "Quest Upper Body menanti di papan misi!"
        }

        // Post local notification
        showNotification(context, "Panggilan Guild: Waktunya Latihan!", bodyText, requestCode)

        // Reschedule next week's alarm for this day
        if (ReminderManager.isReminderEnabled(context)) {
            val (hour, minute) = ReminderManager.getReminderTime(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                ReminderManager.scheduleSingleDayAlarm(context, alarmManager, dayOfWeek, requestCode, hour, minute)
            }
        }
    }

    private fun showNotification(context: Context, title: String, content: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("WorkoutAlarmReceiver", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        ReminderManager.createNotificationChannel(context)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
