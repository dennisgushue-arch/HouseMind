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
import com.housemind.app.data.LocalHouseItemStorage
import com.housemind.app.logic.MaintenanceScheduleCalculator
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MaintenanceReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        createNotificationChannel()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // The app will request notification permission in the UI later.
            return Result.success()
        }

        val items =
            LocalHouseItemStorage(applicationContext)
                .loadOrSeed()

        val today = LocalDate.now()

        items.forEach { item ->

            item.maintenanceTasks
                .filter { it.reminderEnabled }
                .forEach { task ->

                    val dueDate =
                        MaintenanceScheduleCalculator
                            .nextDueDate(task)
                            ?: return@forEach

                    val daysUntilDue =
                        ChronoUnit.DAYS.between(
                            today,
                            dueDate
                        )

                    val message =
                        reminderMessage(
                            itemName = item.name,
                            taskTitle = task.title,
                            daysUntilDue = daysUntilDue
                        )
                            ?: return@forEach

                    val reminderToken =
                        "${dueDate}:${daysUntilDue}"

                    if (
                        hasAlreadySent(
                            taskId = task.id,
                            reminderToken = reminderToken
                        )
                    ) {
                        return@forEach
                    }

                    showNotification(
                        itemName = item.name,
                        taskTitle = task.title,
                        message = message,
                        notificationId =
                            (
                                task.id +
                                    dueDate.toString() +
                                    daysUntilDue.toString()
                                ).hashCode() and Int.MAX_VALUE
                    )

                    markAsSent(
                        taskId = task.id,
                        reminderToken = reminderToken
                    )
                }
        }

        return Result.success()
    }

    private fun reminderMessage(
        itemName: String,
        taskTitle: String,
        daysUntilDue: Long
    ): String? {

        return when (daysUntilDue) {

            7L ->
                "$taskTitle for $itemName is due in 7 days."

            1L ->
                "$taskTitle for $itemName is due tomorrow."

            0L ->
                "$taskTitle for $itemName is due today."

            -1L ->
                "$taskTitle for $itemName is overdue."

            else ->
                null
        }
    }

    private fun showNotification(
        itemName: String,
        taskTitle: String,
        message: String,
        notificationId: Int
    ) {

        val launchIntent =
            Intent(
                applicationContext,
                MainActivity::class.java
            )

        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.ic_launcher_foreground
                )
                .setContentTitle(
                    "HouseMind"
                )
                .setContentText(
                    message
                )
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(message)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        NotificationManagerCompat
            .from(applicationContext)
            .notify(
                notificationId,
                notification
            )
    }

    private fun createNotificationChannel() {

        val notificationManager =
            applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {

                description =
                    "HouseMind home maintenance reminders"
            }

        notificationManager
            .createNotificationChannel(channel)
    }

    private fun hasAlreadySent(
        taskId: String,
        reminderToken: String
    ): Boolean {

        return reminderPreferences()
            .getString(
                taskId,
                null
            ) == reminderToken
    }

    private fun markAsSent(
        taskId: String,
        reminderToken: String
    ) {

        reminderPreferences()
            .edit()
            .putString(
                taskId,
                reminderToken
            )
            .apply()
    }

    private fun reminderPreferences() =
        applicationContext
            .getSharedPreferences(
                REMINDER_PREFERENCES,
                Context.MODE_PRIVATE
            )

    private companion object {

        const val CHANNEL_ID =
            "housemind_maintenance_reminders"

        const val CHANNEL_NAME =
            "Maintenance reminders"

        const val REMINDER_PREFERENCES =
            "housemind_reminder_history"
    }
}
