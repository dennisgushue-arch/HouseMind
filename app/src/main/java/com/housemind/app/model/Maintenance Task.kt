package com.housemind.app.model

data class MaintenanceTask(
    val id: String,
    val title: String,
    val lastCompletedDate: String,
    val intervalValue: Int,
    val intervalUnit: String,
    val reminderEnabled: Boolean = true
)