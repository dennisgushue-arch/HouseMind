package com.housemind.app.model

data class RecognitionResult(
    val itemName: String,
    val category: String,
    val brand: String,
    val modelNumber: String,
    val serialNumber: String,
    val locationSuggestion: String,
    val filterPartNumber: String,
    val notes: String,
    val confidence: String,
    val recognizedText: String
)