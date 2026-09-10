package com.housemind.app.logic

import com.housemind.app.model.HouseItem

data class MaintenanceSuggestion(
    val title: String,
    val intervalValue: Int,
    val intervalUnit: String,
    val reason: String
)

object MaintenanceSuggestionEngine {

    fun suggestionsFor(item: HouseItem): List<MaintenanceSuggestion> {

        val description =
            listOf(
                item.name,
                item.category,
                item.brand
            )
                .joinToString(" ")
                .lowercase()

        return when {

            description.contains("refrigerator") ||
                description.contains("fridge") -> listOf(

                MaintenanceSuggestion(
                    title = "Replace water filter",
                    intervalValue = 6,
                    intervalUnit = "months",
                    reason = "A common replacement interval for many refrigerator water filters."
                ),

                MaintenanceSuggestion(
                    title = "Clean condenser coils",
                    intervalValue = 12,
                    intervalUnit = "months",
                    reason = "Periodic coil cleaning can help the refrigerator run efficiently."
                )
            )

            description.contains("air conditioner") ||
                description.contains("hvac") ||
                description.contains("heat pump") ||
                description.contains("furnace") ||
                description.contains(" a/c") ||
                description.contains(" ac ") -> listOf(

                MaintenanceSuggestion(
                    title = "Replace air filter",
                    intervalValue = 3,
                    intervalUnit = "months",
                    reason = "A common starting interval for residential HVAC filters."
                ),

                MaintenanceSuggestion(
                    title = "Professional HVAC service",
                    intervalValue = 12,
                    intervalUnit = "months",
                    reason = "Annual professional inspection and service is a common maintenance schedule."
                )
            )

            description.contains("water heater") -> listOf(

                MaintenanceSuggestion(
                    title = "Flush water heater",
                    intervalValue = 12,
                    intervalUnit = "months",
                    reason = "Periodic flushing may help reduce sediment buildup. Follow the manufacturer's instructions."
                )
            )

            description.contains("dishwasher") -> listOf(

                MaintenanceSuggestion(
                    title = "Clean dishwasher filter",
                    intervalValue = 1,
                    intervalUnit = "months",
                    reason = "Regular filter cleaning can help maintain washing performance."
                ),

                MaintenanceSuggestion(
                    title = "Run dishwasher cleaning cycle",
                    intervalValue = 1,
                    intervalUnit = "months",
                    reason = "A periodic cleaning cycle can help reduce residue and odor."
                )
            )

            description.contains("washing machine") ||
                description.contains("washer") -> listOf(

                MaintenanceSuggestion(
                    title = "Clean washing machine",
                    intervalValue = 1,
                    intervalUnit = "months",
                    reason = "A regular cleaning cycle can help reduce residue and odor."
                ),

                MaintenanceSuggestion(
                    title = "Inspect washer hoses",
                    intervalValue = 12,
                    intervalUnit = "months",
                    reason = "Periodic inspection can help catch visible wear, bulges, or leaks."
                )
            )

            description.contains("dryer") -> listOf(

                MaintenanceSuggestion(
                    title = "Inspect and clean dryer vent",
                    intervalValue = 12,
                    intervalUnit = "months",
                    reason = "Periodic vent inspection and cleaning can help airflow and reduce lint buildup."
                )
            )

            description.contains("smoke detector") ||
                description.contains("smoke alarm") ||
                description.contains("carbon monoxide") ||
                description.contains("co detector") -> listOf(

                MaintenanceSuggestion(
                    title = "Test alarm",
                    intervalValue = 1,
                    intervalUnit = "months",
                    reason = "Regular testing helps confirm the alarm is operating."
                ),

                MaintenanceSuggestion(
                    title = "Check replaceable batteries",
                    intervalValue = 12,
                    intervalUnit = "months",
                    reason = "For alarms with replaceable batteries, periodic battery checks are useful."
                )
            )

            description.contains("sump pump") -> listOf(

                MaintenanceSuggestion(
                    title = "Test sump pump",
                    intervalValue = 3,
                    intervalUnit = "months",
                    reason = "Periodic testing can help confirm the pump starts and drains normally."
                )
            )

            description.contains("garage door") -> listOf(

                MaintenanceSuggestion(
                    title = "Test garage door safety reverse",
                    intervalValue = 1,
                    intervalUnit = "months",
                    reason = "Periodic safety-reverse testing helps confirm the opener's safety feature works."
                )
            )

            else -> emptyList()
        }
    }
}
