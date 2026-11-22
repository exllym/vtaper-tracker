package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, rescheduling workout and weigh-in alarms")
            if (ReminderManager.isReminderEnabled(context)) {
                val (hour, minute) = ReminderManager.getReminderTime(context)
                ReminderManager.scheduleAllAlarms(context, hour, minute)
            }
            if (ReminderManager.isWeighInReminderEnabled(context)) {
                val day = ReminderManager.getWeighInReminderDay(context)
                val (wHour, wMinute) = ReminderManager.getWeighInReminderTime(context)
                ReminderManager.scheduleWeighInAlarm(context, day, wHour, wMinute)
            }
        }
    }
}
