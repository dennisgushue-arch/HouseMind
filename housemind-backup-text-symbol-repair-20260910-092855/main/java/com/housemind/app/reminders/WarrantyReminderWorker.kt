package com.housemind.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.housemind.app.MainActivity
import com.housemind.app.R
import com.housemind.app.data.LocalDocumentStorage
import com.housemind.app.data.LocalHouseItemStorage
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WarrantyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        createChannel()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val items = LocalHouseItemStorage(applicationContext).loadOrSeed()
        val documents = LocalDocumentStorage(applicationContext)
        val today = LocalDate.now()

        items.forEach { item ->
            documents.load(item.id).forEach { document ->
                val expiration = document.warrantyExpirationDate
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@forEach

                val days = ChronoUnit.DAYS.between(today, expiration)
                val message = when (days) {
                    30L -> "${document.title} for ${item.name} expires in 30 days."
                    7L -> "${document.title} for ${item.name} expires in 7 days."
                    1L -> "${document.title} for ${item.name} expires tomorrow."
                    0L -> "${document.title} for ${item.name} expires today."
                    else -> null
                } ?: return@forEach

                val token = "${expiration}:$days"
                val key = "warranty_${document.id}"

                if (alreadySent(key, token)) return@forEach

                showNotification(
                    message = message,
                    notificationId = (key + token).hashCode() and Int.MAX_VALUE
                )

                markSent(key, token)
            }
        }

        return Result.success()
    }

    private fun showNotification(
        message: String,
        notificationId: Int
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("HouseMind Warranty")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)
    }

    private fun createChannel() {
        val manager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Warranty reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "HouseMind warranty expiration reminders"
            }
        )
    }

    private fun alreadySent(key: String, token: String): Boolean =
        preferences().getString(key, null) == token

    private fun markSent(key: String, token: String) {
        preferences().edit().putString(key, token).apply()
    }

    private fun preferences() =
        applicationContext.getSharedPreferences(
            "housemind_warranty_reminder_history",
            Context.MODE_PRIVATE
        )

    private companion object {
        const val CHANNEL_ID = "housemind_warranty_reminders"
    }
}
