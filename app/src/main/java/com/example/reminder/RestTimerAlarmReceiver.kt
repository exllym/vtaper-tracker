package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
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
import com.example.util.VibrationSoundHelper

class RestTimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val exerciseIndex = intent.getIntExtra("exercise_index", 0)
        val exerciseName = intent.getStringExtra("exercise_name") ?: "Latihan"

        // Clean up from RestTimerStore since it has fired
        RestTimerStore.clearTimer(context, exerciseIndex)

        // Trigger vibration and sound
        VibrationSoundHelper.playTimerFinished(context)

        // Show heads-up notification
        showNotification(context, exerciseIndex, exerciseName)
    }

    private fun showNotification(context: Context, exerciseIndex: Int, exerciseName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("RestTimerAlarmReceiver", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2000 + exerciseIndex,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Istirahat Selesai!")
            .setContentText("Waktu istirahat $exerciseName telah habis. Lanjut set berikutnya!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID_BASE + exerciseIndex, notification)
    }

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        private const val NOTIFICATION_ID_BASE = 5000

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Rest Timer",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi saat waktu istirahat antar set selesai"
                    enableVibration(true)
                    enableLights(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun scheduleAlarm(context: Context, exerciseIndex: Int, exerciseName: String, triggerAtMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, RestTimerAlarmReceiver::class.java).apply {
                putExtra("exercise_index", exerciseIndex)
                putExtra("exercise_name", exerciseName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1000 + exerciseIndex,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.e("RestTimerAlarmReceiver", "Failed to schedule exact alarm for rest timer", e)
            }
        }

        fun cancelAlarm(context: Context, exerciseIndex: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, RestTimerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1000 + exerciseIndex,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        fun cancelNotification(context: Context, exerciseIndex: Int) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID_BASE + exerciseIndex)
        }

        fun cancelAlarmAndNotification(context: Context, exerciseIndex: Int) {
            cancelAlarm(context, exerciseIndex)
            cancelNotification(context, exerciseIndex)
        }
    }
}
