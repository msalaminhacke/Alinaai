package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-schedules saved reminders after the phone reboots. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderManager.rescheduleAll(context)
        }
    }
}
