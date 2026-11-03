package com.proofapp.android.ui.cooking

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.proofapp.android.MainActivity
import com.proofapp.android.R

object TimerNotifications {
    const val CHANNEL_ID = "proof_timers"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Baking timers", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Lets you know when a step in cooking mode is done"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

/**
 * Timers are backed by AlarmManager so the "your dough is ready" notification still fires when
 * the app is in the background or the process has been killed during a four hour bulk ferment.
 */
class TimerAlarmScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun schedule(id: Int, triggerAtMillis: Long, title: String, text: String) {
        val pending = pendingIntent(id, title, text)
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    fun cancel(id: Int) {
        alarms.cancel(pendingIntent(id, "", ""))
    }

    private fun pendingIntent(id: Int, title: String, text: String): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java)
            .putExtra(TimerAlarmReceiver.EXTRA_ID, id)
            .putExtra(TimerAlarmReceiver.EXTRA_TITLE, title)
            .putExtra(TimerAlarmReceiver.EXTRA_TEXT, text)
        return PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerNotifications.show(
            context,
            id = intent.getIntExtra(EXTRA_ID, 0),
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Timer done",
            text = intent.getStringExtra(EXTRA_TEXT).orEmpty(),
        )
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
