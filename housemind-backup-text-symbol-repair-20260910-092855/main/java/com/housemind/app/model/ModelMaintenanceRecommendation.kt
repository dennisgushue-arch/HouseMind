package com.housemind.app.model

data class ModelMaintenanceRecommendation(
    val name: String,
    val kind: String,
    val partNumber: String,
    val maintenanceTitle: String,
    val hasInterval: Boolean,
    val intervalValue: Int,
    val intervalUnit: String,
    val instructions: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val verification: String
)

data class ModelMaintenanceResult(
    val brand: String,
    val modelNumber: String,
    val exactModelMatched: Boolean,
    val summary: String,
    val recommendations: List<ModelMaintenanceRecommendation>
)
