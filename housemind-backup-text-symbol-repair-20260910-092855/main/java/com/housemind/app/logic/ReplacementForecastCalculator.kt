package com.housemind.app.logic

import com.housemind.app.model.HouseItem
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

data class LifespanRange(val minYears: Int, val maxYears: Int)

data class ReplacementForecast(
    val installedDate: LocalDate,
    val lifespan: LifespanRange,
    val earliestReplacementDate: LocalDate,
    val latestReplacementDate: LocalDate,
    val planningStartDate: LocalDate,
    val ageText: String,
    val statusText: String
)

object ReplacementForecastCalculator {

    fun suggestedLifespan(category: String, name: String = ""): LifespanRange? {
        val text = "$category $name".lowercase()
        return when {
            "water heater" in text -> LifespanRange(8, 12)
            "air conditioner" in text || "central air" in text || "hvac" in text -> LifespanRange(10, 15)
            "heat pump" in text -> LifespanRange(10, 15)
            "furnace" in text -> LifespanRange(15, 20)
            "refrigerator" in text || "fridge" in text -> LifespanRange(10, 15)
            "dishwasher" in text -> LifespanRange(8, 12)
            "washing machine" in text || "washer" in text -> LifespanRange(8, 12)
            "clothes dryer" in text || "dryer" in text -> LifespanRange(10, 13)
            "range" in text || "oven" in text || "stove" in text -> LifespanRange(13, 17)
            "microwave" in text -> LifespanRange(7, 10)
            "garbage disposal" in text || "disposal" in text -> LifespanRange(8, 12)
            "garage door opener" in text -> LifespanRange(10, 15)
            "sump pump" in text -> LifespanRange(7, 10)
            else -> null
        }
    }

    fun lifespanFor(item: HouseItem): LifespanRange? {
        val min = item.lifespanMinYears
        val max = item.lifespanMaxYears
        return if (min != null && max != null && min > 0 && max >= min) {
            LifespanRange(min, max)
        } else {
            suggestedLifespan(item.category, item.name)
        }
    }

    fun forecast(item: HouseItem, today: LocalDate = LocalDate.now()): ReplacementForecast? {
        val installed = item.installedDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return null

        val lifespan = lifespanFor(item) ?: return null
        val earliest = installed.plusYears(lifespan.minYears.toLong())
        val latest = installed.plusYears(lifespan.maxYears.toLong())
        val planningStart = earliest.minusYears(1)

        val status = when {
            today.isAfter(latest) -> "Past the general planning window"
            !today.isBefore(earliest) -> "Within the general replacement window"
            !today.isBefore(planningStart) -> "Start planning for replacement"
            else -> "No near-term replacement planning needed"
        }

        val period = if (installed.isAfter(today)) Period.ZERO else Period.between(installed, today)
        val ageText = when {
            installed.isAfter(today) -> "Date is in the future"
            period.years > 0 && period.months > 0 -> "${period.years} yr ${period.months} mo"
            period.years > 0 -> "${period.years} yr"
            period.months > 0 -> "${period.months} mo"
            else -> "${period.days} days"
        }

        return ReplacementForecast(
            installedDate = installed,
            lifespan = lifespan,
            earliestReplacementDate = earliest,
            latestReplacementDate = latest,
            planningStartDate = planningStart,
            ageText = ageText,
            statusText = status
        )
    }

    fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
}
