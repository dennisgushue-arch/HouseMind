package com.housemind.app.model

data class HouseItem(
    val id: String,
    val name: String,
    val category: String,
    val brand: String,
    val modelNumber: String,
    val serialNumber: String,
    val location: String,
    val filterPartNumber: String,
    val notes: String,
    val status: String,
    val maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val photoPath: String? = null
)