package com.housemind.app.logic

import com.housemind.app.model.HouseItem
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HouseMindAnswer(
    val answerText: String,
    val sourceLabel: String? = null
)

object HouseMindQueryEngine {

    fun answer(question: String, items: List<HouseItem>): HouseMindAnswer {
        val normalizedQuestion = question.lowercase().trim()

        if (isListQuestion(normalizedQuestion)) {
            return listItems(items)
        }
        if (isAttentionQuestion(normalizedQuestion)) {
            return attentionAnswer(items)
        }

        val matches = findMatchingItems(normalizedQuestion, items)
        if (matches.isEmpty()) return unsupportedAnswer()
        if (matches.size > 1) {
            return HouseMindAnswer(
                "I found more than one matching item. Try asking about ${matches.joinToString(" or ") { it.name }}."
            )
        }

        val item = matches.single()
        return when {
            containsAny(normalizedQuestion, "filter", "part number", "part") -> filterAnswer(item)
            containsAny(normalizedQuestion, "how much", "spent", "cost", "costs") -> costAnswer(item)
            containsAny(normalizedQuestion, "who", "serviced", "repaired", "repair") -> providerAnswer(item)
            containsAny(normalizedQuestion, "when", "last service", "worked on") -> lastServiceAnswer(item)
            containsAny(normalizedQuestion, "model") -> detailAnswer(item, "model", item.modelNumber)
            containsAny(normalizedQuestion, "serial") -> detailAnswer(item, "serial number", item.serialNumber)
            containsAny(normalizedQuestion, "where", "location") -> detailAnswer(item, "location", item.location)
            containsAny(normalizedQuestion, "brand", "make") -> detailAnswer(item, "brand", item.brand)
            else -> unsupportedAnswer()
        }
    }

    private fun findMatchingItems(question: String, items: List<HouseItem>): List<HouseItem> {
        val searchTerms = buildList {
            add(question)
            if ("fridge" in question) add("refrigerator")
            if ("ac" in question || "air conditioner" in question || "hvac" in question) {
                add("air conditioner")
                add("hvac")
            }
            if ("heater" in question) add("water heater")
        }

        return items.filter { item ->
            val itemText = listOf(item.name, item.category, item.brand, item.location)
                .joinToString(" ").lowercase()
            searchTerms.any { term -> itemText.contains(term) || term.contains(item.category.lowercase()) }
        }
    }

    private fun filterAnswer(item: HouseItem) = if (item.filterPartNumber.isBlank()) {
        HouseMindAnswer("I don't have a filter or part number saved for your ${item.name} yet.", item.name)
    } else {
        HouseMindAnswer("Your ${item.name} uses ${item.filterPartNumber}.", item.name)
    }

    private fun lastServiceAnswer(item: HouseItem): HouseMindAnswer {
        val record = item.maintenanceRecords.maxByOrNull { it.date }
            ?: return HouseMindAnswer("I don't have any service history for your ${item.name} yet.", item.name)
        val providerText = if (record.provider.isBlank()) "" else " by ${record.provider}"
        return HouseMindAnswer(
            "Your ${item.name} was last serviced on ${formatDate(record.date)}$providerText for ${record.serviceType}.",
            item.name
        )
    }

    private fun providerAnswer(item: HouseItem): HouseMindAnswer {
        val record = item.maintenanceRecords.sortedByDescending { it.date }
            .firstOrNull { it.provider.isNotBlank() }
            ?: return if (item.maintenanceRecords.isEmpty()) {
                HouseMindAnswer("I don't have any service history for your ${item.name} yet.", item.name)
            } else {
                HouseMindAnswer("I have a service record for your ${item.name}, but no provider was saved.", item.name)
            }
        return HouseMindAnswer(
            "${record.provider} last serviced your ${item.name} on ${formatDate(record.date)}.",
            item.name
        )
    }

    private fun costAnswer(item: HouseItem): HouseMindAnswer {
        val costs = item.maintenanceRecords.mapNotNull { parseCost(it.cost) }
        if (costs.isEmpty()) {
            return HouseMindAnswer("I don't have any maintenance costs saved for your ${item.name} yet.", item.name)
        }
        val total = costs.fold(BigDecimal.ZERO, BigDecimal::add)
        return HouseMindAnswer(
            "You've recorded $${"%.2f".format(Locale.US, total)} in maintenance costs for your ${item.name} across ${costs.size} records.",
            item.name
        )
    }

    private fun parseCost(value: String): BigDecimal? {
        val normalized = value.trim().removePrefix("$")
        return normalized.takeIf { it.matches(Regex("\\d+(\\.\\d{1,2})?")) }?.toBigDecimalOrNull()
    }

    private fun detailAnswer(item: HouseItem, fieldName: String, value: String): HouseMindAnswer {
        return if (value.isBlank()) {
            HouseMindAnswer("I don't have the $fieldName saved for your ${item.name} yet.", item.name)
        } else {
            val description = when (fieldName) {
                "model" -> "is model"
                "serial number" -> "has serial number"
                "location" -> "is located in"
                else -> "is made by"
            }
            HouseMindAnswer("Your ${item.name} $description $value.", item.name)
        }
    }

    private fun attentionAnswer(items: List<HouseItem>): HouseMindAnswer {
        val healthyStatuses = setOf("everything looks good", "no action needed", "recently added")
        val itemsNeedingAttention = items.filter { it.status.lowercase() !in healthyStatuses }
        return if (itemsNeedingAttention.isEmpty()) {
            HouseMindAnswer("Nothing in your saved home currently shows an upcoming issue.")
        } else {
            HouseMindAnswer(itemsNeedingAttention.joinToString("\n") { "${it.name} needs attention: ${it.status}." })
        }
    }

    private fun listItems(items: List<HouseItem>) = if (items.isEmpty()) {
        HouseMindAnswer("You don't have any items saved in HouseMind yet.")
    } else {
        HouseMindAnswer("You currently have ${items.size} items saved:\n${items.joinToString("\n") { "- ${it.name}" }}")
    }

    private fun isListQuestion(question: String) = containsAny(question, "what appliances", "what items", "saved in my home", "in housemind")

    private fun isAttentionQuestion(question: String) = containsAny(question, "needs attention", "anything need attention")

    private fun unsupportedAnswer() = HouseMindAnswer(
        "I don't know that yet. Try asking about an item's model, filter, location, service history, maintenance costs, or status."
    )

    private fun containsAny(text: String, vararg values: String) = values.any { it in text }

    private fun formatDate(date: String): String = runCatching {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    }.getOrDefault(date)
}