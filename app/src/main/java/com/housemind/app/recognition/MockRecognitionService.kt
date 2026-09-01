package com.housemind.app.recognition

import android.net.Uri
import com.housemind.app.model.RecognitionResult

// Development fallback only. This service does not analyze the supplied image.
class MockRecognitionService : RecognitionService {
    override suspend fun analyze(imageUri: Uri) = RecognitionResult(
        itemName = "Kitchen Refrigerator",
        category = "Refrigerator",
        brand = "GE Profile",
        modelNumber = "PFE28KYNFS",
        serialNumber = "GS742913",
        locationSuggestion = "Kitchen",
        filterPartNumber = "RPWFE",
        notes = "Replace water filter approximately every 6 months.",
        confidence = "medium",
        recognizedText = "Development mock result"
    )
}