package com.housemind.app.model

data class MaintenanceRecord(
    val id: String,
    val date: String,
    val serviceType: String,
    val provider: String,
    val cost: String,
    val notes: String
)