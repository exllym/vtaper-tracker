package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderManager {
    const val CHANNEL_ID = "workout_reminder_channel"
    const val WEIGH_IN_CHANNEL_ID = "weigh_in_channel"
    const val WEIGH_IN_CHANNEL_NAME = "Pengingat Timbang"
    const val WEIGH_IN_REQUEST_CODE = 7001
    const val WEIGH_IN_NOTIFICATION_ID = 8001

    private const val PREFS_NAME = "workout_reminder_prefs"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"
    private const val KEY_ENABLED = "reminder_enabled"

    private const val KEY_WEIGH_IN_ENABLED = "weigh_in_reminder_enabled"
    private const val KEY_WEIGH_IN_DAY = "weigh_in_reminder_day"
    private const val KEY_WEIGH_IN_HOUR = "weigh_in_reminder_hour"
    private const val KEY_WEIGH_IN_MINUTE = "weigh_in_reminder_minute"

    val TRAINING_DAYS = listOf(
        DayOfWeek.MONDAY to 101,
        DayOfWeek.TUESDAY to 102,
        DayOfWeek.THURSDAY to 104,
        DayOfWeek.FRIDAY to 105
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat jadwal latihan bulking"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createWeighInNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WEIGH_IN_CHANNEL_ID,
                WEIGH_IN_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat timbang badan mingguan"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getReminderTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(KEY_HOUR, 16)
        val minute = prefs.getInt(KEY_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun isReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            val (hour, minute) = getReminderTime(context)
            scheduleAllAlarms(context, hour, minute)
        } else {
            cancelAllAlarms(context)
        }
    }

    fun saveReminderTime(context: Context, hour: Int, minute: Int, enabled: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        if (enabled) {
            scheduleAllAlarms(context, hour, minute)
        } else {
            cancelAllAlarms(context)
        }
    }

    fun isWeighInReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WEIGH_IN_ENABLED, true)
    }

    fun getWeighInReminderDay(context: Context): DayOfWeek {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dayValue = prefs.getInt(KEY_WEIGH_IN_DAY, DayOfWeek.SUNDAY.value)
        return try {
            DayOfWeek.of(dayValue.coerceIn(1, 7))
        } catch (_: Exception) {
            DayOfWeek.SUNDAY
        }
    }

    fun getWeighInReminderTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(KEY_WEIGH_IN_HOUR, 7)
        val minute = prefs.getInt(KEY_WEIGH_IN_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun setWeighInReminderEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WEIGH_IN_ENABLED, enabled).apply()
        if (enabled) {
            val day = getWeighInReminderDay(context)
            val (hour, minute) = getWeighInReminderTime(context)
            scheduleWeighInAlarm(context, day, hour, minute)
        } else {
            cancelWeighInAlarm(context)
        }
    }

    fun saveWeighInReminderSettings(
        context: Context,
        enabled: Boolean,
        dayOfWeek: DayOfWeek,
        hour: Int,
        minute: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_WEIGH_IN_ENABLED, enabled)
            .putInt(KEY_WEIGH_IN_DAY, dayOfWeek.value)
            .putInt(KEY_WEIGH_IN_HOUR, hour)
            .putInt(KEY_WEIGH_IN_MINUTE, minute)
            .apply()

        if (enabled) {
            scheduleWeighInAlarm(context, dayOfWeek, hour, minute)
        } else {
            cancelWeighInAlarm(context)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e3: Exception) {
                        Log.e("ReminderManager", "Cannot open battery settings", e3)
                    }
                }
            }
        }
    }

    fun scheduleAllAlarms(context: Context, hour: Int, minute: Int) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for ((dayOfWeek, requestCode) in TRAINING_DAYS) {
            scheduleSingleDayAlarm(context, alarmManager, dayOfWeek, requestCode, hour, minute)
        }
    }

    fun scheduleSingleDayAlarm(
        context: Context,
        alarmManager: AlarmManager,
        dayOfWeek: DayOfWeek,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(hour, minute, 0)
        var targetDate = LocalDate.now()

        // Find next target date matching dayOfWeek
        while (targetDate.dayOfWeek != dayOfWeek) {
            targetDate = targetDate.plusDays(1)
        }

        var targetDateTime = LocalDateTime.of(targetDate, targetTime)
        if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
            // Already passed today, move to next week
            targetDateTime = targetDateTime.plusWeeks(1)
        }

        val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
            putExtra("day_of_week", dayOfWeek.name)
            putExtra("request_code", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d("ReminderManager", "Scheduled alarm for $dayOfWeek at $targetDateTime (epoch: $triggerMillis)")
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule alarm for $dayOfWeek", e)
        }
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for ((_, requestCode) in TRAINING_DAYS) {
            val intent = Intent(context, WorkoutAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun scheduleWeighInAlarm(
        context: Context,
        dayOfWeek: DayOfWeek = getWeighInReminderDay(context),
        hour: Int = getWeighInReminderTime(context).first,
        minute: Int = getWeighInReminderTime(context).second
    ) {
        createWeighInNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(hour, minute, 0)
        var targetDate = LocalDate.now()

        // Find next target date matching configured dayOfWeek
        while (targetDate.dayOfWeek != dayOfWeek) {
            targetDate = targetDate.plusDays(1)
        }

        var targetDateTime = LocalDateTime.of(targetDate, targetTime)
        if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
            // Already passed this week's configured day/time, schedule for next week
            targetDateTime = targetDateTime.plusWeeks(1)
        }

        val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, WeighInAlarmReceiver::class.java).apply {
            putExtra("day_of_week", dayOfWeek.name)
            putExtra("request_code", WEIGH_IN_REQUEST_CODE)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WEIGH_IN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d("ReminderManager", "Scheduled weigh-in alarm for $dayOfWeek at $targetDateTime (epoch: $triggerMillis)")
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule weigh-in alarm for $dayOfWeek", e)
        }
    }

    fun cancelWeighInAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WeighInAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WEIGH_IN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
