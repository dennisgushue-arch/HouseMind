package com.housemind.app.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MaintenanceReminderScheduler {

    private const val UNIQUE_DAILY_MAINTENANCE_WORK =
        "housemind_daily_maintenance_reminders"

    private const val UNIQUE_DAILY_WARRANTY_WORK =
        "housemind_daily_warranty_reminders"

    fun schedule(context: Context) {

        val workManager =
            WorkManager.getInstance(
                context.applicationContext
            )

        workManager.enqueue(
            OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
                .build()
        )

        workManager.enqueue(
            OneTimeWorkRequestBuilder<WarrantyReminderWorker>()
                .build()
        )

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_DAILY_MAINTENANCE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(
                24,
                TimeUnit.HOURS
            ).build()
        )

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_DAILY_WARRANTY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WarrantyReminderWorker>(
                24,
                TimeUnit.HOURS
            ).build()
        )
    }
}
