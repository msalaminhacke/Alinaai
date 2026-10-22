package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Reminder(val id: String, val message: String, val triggerAt: Long)

/**
 * Time-based reminders. Stored in SharedPreferences, scheduled with AlarmManager,
 * and re-scheduled after reboot by ReminderBootReceiver.
 */
object ReminderManager {
    private const val PREFS = "reminder_prefs"
    private const val KEY = "reminders"
    private var appContext: Context? = null

    @Synchronized
    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun prefs() = appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): MutableList<Reminder> {
        val out = mutableListOf<Reminder>()
        val arr = try {
            JSONArray(appContext?.let { prefs().getString(KEY, "[]") } ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out += Reminder(o.optString("id"), o.optString("message"), o.optLong("triggerAt"))
        }
        return out
    }

    @Synchronized
    private fun save(list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("message", it.message).put("triggerAt", it.triggerAt))
        }
        prefs().edit().putString(KEY, arr.toString()).apply()
    }

    @Synchronized
    fun add(message: String, triggerAt: Long): Reminder {
        val ctx = appContext ?: error("ReminderManager not initialised")
        val reminder = Reminder(UUID.randomUUID().toString().take(8), message, triggerAt)
        save(load() + reminder)
        schedule(ctx, reminder)
        return reminder
    }

    @Synchronized
    fun cancel(id: String): Boolean {
        val ctx = appContext ?: return false
        val target = load().find { it.id == id } ?: return false
        save(load().filterNot { it.id == id })
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent(ctx, target))
        return true
    }

    @Synchronized
    fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    /** Called after boot. Reminders whose time already passed are dropped. */
    @Synchronized
    fun rescheduleAll(context: Context) {
        init(context)
        val now = System.currentTimeMillis()
        val (future, _) = load().partition { it.triggerAt > now }
        save(future)
        future.forEach { schedule(context, it) }
    }

    private fun schedule(context: Context, reminder: Reminder) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pendingIntent(context, reminder))
    }

    private fun pendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("id", reminder.id)
            .putExtra("message", reminder.message)
        return PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
