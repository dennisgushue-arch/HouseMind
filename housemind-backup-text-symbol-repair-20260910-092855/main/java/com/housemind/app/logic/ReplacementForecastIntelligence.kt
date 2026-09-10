package com.housemind.app.logic

import com.housemind.app.model.HouseItem
import java.time.LocalDate

object ReplacementForecastIntelligence {

    fun answer(
        question: String,
        items: List<HouseItem>,
        today: LocalDate = LocalDate.now()
    ): HouseMindAnswer? {

        val q = question.lowercase().trim()

        val isAgeQuestion =
            containsAny(
                q,
                "how old",
                "age of",
                "what age"
            )

        val isReplacementQuestion =
            containsAny(
                q,
                "when should i replace",
                "when do i replace",
                "when should we replace",
                "replacement window",
                "replace my",
                "replace the"
            )

        val isBudgetQuestion =
            containsAny(
                q,
                "what should i start budgeting for",
                "what should i budget for",
                "what needs replacing",
                "what should i replace",
                "replacement planning",
                "replacement forecast"
            )

        if (
            !isAgeQuestion &&
            !isReplacementQuestion &&
            !isBudgetQuestion
        ) {
            return null
        }

        if (isBudgetQuestion) {
            return budgetAnswer(
                items = items,
                today = today
            )
        }

        val matches =
            items.filter {
                matchesItem(
                    question = q,
                    item = it
                )
            }

        if (matches.isEmpty()) {
            return HouseMindAnswer(
                "I couldn't tell which home item you mean. Try naming the appliance or system."
            )
        }

        if (matches.size > 1) {
            return HouseMindAnswer(
                "I found more than one matching item. Try asking about ${
                    matches.joinToString(" or ") { it.name }
                }."
            )
        }

        val item =
            matches.single()

        val forecast =
            ReplacementForecastCalculator
                .forecast(
                    item = item,
                    today = today
                )
                ?: return HouseMindAnswer(
                    "I don't have enough replacement-forecast information saved for your ${item.name} yet. Open Replacement Forecast and add its installed or purchased date.",
                    item.name
                )

        return if (isAgeQuestion) {
            HouseMindAnswer(
                "Your ${item.name} is about ${forecast.ageText} old based on the installed or purchased date saved in HouseMind.",
                item.name
            )
        } else {
            HouseMindAnswer(
                "For planning, your ${item.name} has an estimated replacement window of ${
                    ReplacementForecastCalculator.formatDate(
                        forecast.earliestReplacementDate
                    )
                } through ${
                    ReplacementForecastCalculator.formatDate(
                        forecast.latestReplacementDate
                    )
                }. ${forecast.statusText}.",
                item.name
            )
        }
    }

    private fun budgetAnswer(
        items: List<HouseItem>,
        today: LocalDate
    ): HouseMindAnswer {

        val forecasts =
            items.mapNotNull { item ->
                ReplacementForecastCalculator
                    .forecast(
                        item = item,
                        today = today
                    )
                    ?.let {
                        item to it
                    }
            }

        if (forecasts.isEmpty()) {
            return HouseMindAnswer(
                "You don't have any replacement forecasts saved yet."
            )
        }

        val shouldPlanNow =
            forecasts
                .filter { (_, forecast) ->
                    !forecast.planningStartDate.isAfter(
                        today
                    )
                }
                .sortedBy { (_, forecast) ->
                    forecast.earliestReplacementDate
                }

        if (shouldPlanNow.isNotEmpty()) {
            val lines =
                shouldPlanNow
                    .take(5)
                    .joinToString("\n") { (item, forecast) ->
                        "- ${item.name}: ${forecast.statusText}. Estimated window ${
                            forecast.earliestReplacementDate.year
                        }-${forecast.latestReplacementDate.year}."
                    }

            return HouseMindAnswer(
                "These are the items HouseMind says are worth planning or budgeting for now:\n$lines"
            )
        }

        val next =
            forecasts
                .sortedBy { (_, forecast) ->
                    forecast.planningStartDate
                }
                .take(3)

        val lines =
            next.joinToString("\n") { (item, forecast) ->
                "- ${item.name}: start planning around ${
                    ReplacementForecastCalculator.formatDate(
                        forecast.planningStartDate
                    )
                }."
            }

        return HouseMindAnswer(
            "Nothing with a saved forecast has reached its planning date yet. The next items to plan for are:\n$lines"
        )
    }

    private fun matchesItem(
        question: String,
        item: HouseItem
    ): Boolean {

        val terms =
            listOf(
                item.name,
                item.category,
                item.brand,
                item.location
            )
                .map {
                    it.lowercase().trim()
                }
                .filter {
                    it.isNotBlank()
                }

        if (
            terms.any {
                it in question
            }
        ) {
            return true
        }

        val itemText =
            terms.joinToString(" ")

        return when {
            "fridge" in question ->
                "refrigerator" in itemText

            "ac" in question ||
                "air conditioner" in question ||
                "hvac" in question ->
                "air conditioner" in itemText ||
                    "hvac" in itemText

            "heater" in question ->
                "water heater" in itemText

            else ->
                false
        }
    }

    private fun containsAny(
        text: String,
        vararg values: String
    ): Boolean =
        values.any {
            it in text
        }
}
