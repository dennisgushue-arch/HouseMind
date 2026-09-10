
package com.housemind.app.logic

import com.housemind.app.model.MaintenanceTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object MaintenanceScheduleCalculator {

    fun nextDueDate(task: MaintenanceTask): LocalDate? {

        if (task.lastCompletedDate.isBlank()) {
            return null
        }

        val lastCompleted = runCatching {
            LocalDate.parse(task.lastCompletedDate)
        }.getOrNull() ?: return null

        if (task.intervalValue <= 0) {
            return null
        }

        return when (task.intervalUnit.lowercase()) {

            "day",
            "days" ->
                lastCompleted.plusDays(task.intervalValue.toLong())

            "week",
            "weeks" ->
                lastCompleted.plusWeeks(task.intervalValue.toLong())

            "month",
            "months" ->
                lastCompleted.plusMonths(task.intervalValue.toLong())

            "year",
            "years" ->
                lastCompleted.plusYears(task.intervalValue.toLong())

            else ->
                null
        }
    }

    fun statusText(
        task: MaintenanceTask,
        today: LocalDate = LocalDate.now()
    ): String {

        val dueDate = nextDueDate(task)
            ?: return "Schedule not complete"

        val days = ChronoUnit.DAYS.between(
            today,
            dueDate
        )

        return when {

            days > 1 ->
                "Due in $days days"

            days == 1L ->
                "Due tomorrow"

            days == 0L ->
                "Due today"

            days == -1L ->
                "Overdue by 1 day"

            else ->
                "Overdue by ${-days} days"
        }
    }

    fun formattedDueDate(
        task: MaintenanceTask
    ): String {

        val dueDate = nextDueDate(task)
            ?: return "Not scheduled"

        return dueDate.format(
            DateTimeFormatter.ofPattern(
                "MMM d, yyyy",
                Locale.US
            )
        )
    }

    fun isOverdue(
        task: MaintenanceTask,
        today: LocalDate = LocalDate.now()
    ): Boolean {

        val dueDate = nextDueDate(task)
            ?: return false

        return dueDate.isBefore(today)
    }

    fun isDueSoon(
        task: MaintenanceTask,
        today: LocalDate = LocalDate.now(),
        withinDays: Long = 30
    ): Boolean {

        val dueDate = nextDueDate(task)
            ?: return false

        val days = ChronoUnit.DAYS.between(
            today,
            dueDate
        )

        return days in 0..withinDays
    }
}