package com.housemind.app.recognition

import android.net.Uri
import com.housemind.app.model.RecognitionResult

interface RecognitionService {
    suspend fun analyze(imageUri: Uri): RecognitionResult
}