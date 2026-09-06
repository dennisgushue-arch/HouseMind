package com.housemind.app.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MaintenanceReminderScheduler {

    private const val UNIQUE_DAILY_WORK =
        "housemind_daily_maintenance_reminders"

    fun schedule(context: Context) {

        val workManager =
            WorkManager.getInstance(
                context.applicationContext
            )

        // Run one safe check now. The worker only notifies when a task
        // is at one of HouseMind's reminder thresholds.
        val immediateCheck =
            OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
                .build()

        workManager.enqueue(immediateCheck)

        // Keep checking approximately once per day.
        val dailyCheck =
            PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(
                24,
                TimeUnit.HOURS
            ).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_DAILY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyCheck
        )
    }
}
