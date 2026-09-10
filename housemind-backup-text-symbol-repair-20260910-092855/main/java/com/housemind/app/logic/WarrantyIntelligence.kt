package com.housemind.app.logic

import com.housemind.app.model.HouseItem
import com.housemind.app.model.SavedDocument
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class WarrantyState {
    None,
    Active,
    ExpiringSoon,
    Expired
}

data class WarrantySnapshot(
    val state: WarrantyState,
    val text: String?
)

object WarrantyIntelligence {

    fun snapshot(
        documents: List<SavedDocument>,
        today: LocalDate = LocalDate.now()
    ): WarrantySnapshot {

        val dated =
            documents.mapNotNull { document ->
                val date = document.warrantyExpirationDate
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@mapNotNull null
                document to date
            }

        if (dated.isEmpty()) {
            return WarrantySnapshot(WarrantyState.None, null)
        }

        val selected =
            dated.filter { !it.second.isBefore(today) }
                .minByOrNull { it.second }
                ?: dated.maxByOrNull { it.second }
                ?: return WarrantySnapshot(WarrantyState.None, null)

        val expiration = selected.second
        val days = ChronoUnit.DAYS.between(today, expiration)

        val state = when {
            days < 0 -> WarrantyState.Expired
            days <= 30 -> WarrantyState.ExpiringSoon
            else -> WarrantyState.Active
        }

        val text = when {
            days < 0 -> "Warranty expired ${formatDate(expiration)}"
            days == 0L -> "Warranty expires today"
            days == 1L -> "Warranty expires tomorrow"
            days in 2..30 -> "Warranty expires in $days days"
            else -> "Warranty expires ${formatDate(expiration)}"
        }

        return WarrantySnapshot(state, text)
    }

    fun needsAttention(
        documents: List<SavedDocument>
    ): Boolean =
        when (snapshot(documents).state) {
            WarrantyState.ExpiringSoon,
            WarrantyState.Expired -> true
            else -> false
        }

    fun answer(
        question: String,
        items: List<HouseItem>,
        loadDocuments: (String) -> List<SavedDocument>
    ): HouseMindAnswer? {

        val q = question.lowercase().trim()

        if ("warranty" !in q && "warranties" !in q) {
            return null
        }

        val matches = items.filter { item -> matchesItem(q, item) }

        if (matches.size > 1) {
            return HouseMindAnswer(
                "I found more than one matching item. Try asking about ${
                    matches.joinToString(" or ") { it.name }
                }."
            )
        }

        if (matches.size == 1) {
            val item = matches.single()
            val snapshot = snapshot(loadDocuments(item.id))

            return if (snapshot.state == WarrantyState.None) {
                HouseMindAnswer(
                    "I don't have a warranty expiration date saved for your ${item.name} yet.",
                    item.name
                )
            } else {
                HouseMindAnswer(
                    "Your ${item.name}: ${snapshot.text}.",
                    item.name
                )
            }
        }

        val summaries = items.mapNotNull { item ->
            snapshot(loadDocuments(item.id)).text?.let {
                "${item.name}: $it"
            }
        }

        return if (summaries.isEmpty()) {
            HouseMindAnswer(
                "You don't have any warranty expiration dates saved yet."
            )
        } else {
            HouseMindAnswer(
                "Here are your saved warranty dates:\n${
                    summaries.joinToString("\n") { "- $it" }
                }"
            )
        }
    }

    private fun matchesItem(
        question: String,
        item: HouseItem
    ): Boolean {

        val terms = listOf(
            item.name,
            item.category,
            item.brand,
            item.location
        ).map { it.lowercase().trim() }.filter { it.isNotBlank() }

        if (terms.any { it in question }) return true

        val itemText = terms.joinToString(" ")

        return when {
            "fridge" in question -> "refrigerator" in itemText
            "ac" in question || "air conditioner" in question || "hvac" in question ->
                "air conditioner" in itemText || "hvac" in itemText
            "heater" in question -> "water heater" in itemText
            else -> false
        }
    }

    private fun formatDate(date: LocalDate): String =
        date.format(
            DateTimeFormatter.ofPattern(
                "MMM d, yyyy",
                Locale.US
            )
        )
}
