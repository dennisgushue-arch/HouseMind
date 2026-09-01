package com.housemind.app.recognition

import android.content.Context
import android.net.Uri
import com.housemind.app.data.LocalImageStorage
import com.housemind.app.model.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RemoteRecognitionService(context: Context) : RecognitionService {

    private val imageStorage = LocalImageStorage(context.applicationContext)

    override suspend fun analyze(imageUri: Uri): RecognitionResult = withContext(Dispatchers.IO) {
        val imageDataUrl = imageStorage.createJpegDataUrl(imageUri)
            ?: throw RecognitionException("Couldn't prepare that photo.")
        val connection = (URL("${HouseMindConfig.API_BASE_URL.trimEnd('/')}/api/analyze")
            .openConnection() as HttpURLConnection)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.writer().use { output ->
                output.write(JSONObject().put("imageDataUrl", imageDataUrl).toString())
            }

            if (connection.responseCode !in 200..299) throw RecognitionException("Recognition is unavailable.")
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            responseText.toRecognitionResult()
        } catch (exception: RecognitionException) {
            throw exception
        } catch (exception: Exception) {
            throw RecognitionException("HouseMind couldn't analyze that photo. Please try again.", exception)
        } finally {
            connection.disconnect()
        }
    }

    private fun String.toRecognitionResult(): RecognitionResult = try {
        val json = JSONObject(this)
        RecognitionResult(
            itemName = json.getString("itemName"), category = json.getString("category"),
            brand = json.getString("brand"), modelNumber = json.getString("modelNumber"),
            serialNumber = json.getString("serialNumber"), locationSuggestion = json.getString("locationSuggestion"),
            filterPartNumber = json.getString("filterPartNumber"), notes = json.getString("notes"),
            confidence = json.getString("confidence"), recognizedText = json.getString("recognizedText")
        ).also { result ->
            if (result.confidence !in setOf("high", "medium", "low")) throw RecognitionException("Invalid recognition result.")
        }
    } catch (exception: RecognitionException) {
        throw exception
    } catch (exception: Exception) {
        throw RecognitionException("HouseMind couldn't read that result. Please try again.", exception)
    }
}

class RecognitionException(message: String, cause: Throwable? = null) : Exception(message, cause)