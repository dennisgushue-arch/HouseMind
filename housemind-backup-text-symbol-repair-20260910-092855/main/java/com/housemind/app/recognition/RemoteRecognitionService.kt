package com.housemind.app.recognition

import android.content.Context
import android.net.Uri
import android.util.Log
import com.housemind.app.data.LocalImageStorage
import com.housemind.app.model.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class RemoteRecognitionService(
    context: Context
) : RecognitionService {

    private val imageStorage =
        LocalImageStorage(
            context.applicationContext
        )

    override suspend fun analyze(
        imageUri: Uri
    ): RecognitionResult =
        withContext(Dispatchers.IO) {

            val imageDataUrl =
                imageStorage.createJpegDataUrl(
                    imageUri
                )
                    ?: throw RecognitionException(
                        "HouseMind couldn't prepare that photo. Try taking it again or choose it from Photos."
                    )

            val endpoint =
                "${HouseMindConfig.API_BASE_URL.trimEnd('/')}/api/analyze"

            val connection =
                URL(endpoint)
                    .openConnection()
                    as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 20_000
                connection.readTimeout = 90_000
                connection.doOutput = true
                connection.useCaches = false

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val requestBody =
                    JSONObject()
                        .put(
                            "imageDataUrl",
                            imageDataUrl
                        )
                        .toString()

                connection.outputStream
                    .writer(
                        Charsets.UTF_8
                    )
                    .use { output ->
                        output.write(
                            requestBody
                        )
                    }

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !in 200..299
                ) {
                    val errorText =
                        connection.errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                            .orEmpty()

                    val backendMessage =
                        runCatching {
                            JSONObject(
                                errorText
                            )
                                .optString(
                                    "error"
                                )
                        }
                            .getOrNull()
                            .orEmpty()

                    Log.e(
                        TAG,
                        "Recognition HTTP $responseCode: $errorText"
                    )

                    throw RecognitionException(
                        when {
                            responseCode == 413 ->
                                "That photo is too large. Try a closer photo of just the model label."

                            backendMessage.contains(
                                "not configured",
                                ignoreCase = true
                            ) ->
                                "HouseMind's AI backend is not configured correctly."

                            backendMessage.isNotBlank() ->
                                "HouseMind server: $backendMessage"

                            else ->
                                "HouseMind server returned error $responseCode."
                        }
                    )
                }

                val responseText =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                responseText.toRecognitionResult()
            } catch (
                exception: RecognitionException
            ) {
                throw exception
            } catch (
                exception: SocketTimeoutException
            ) {
                Log.e(
                    TAG,
                    "Recognition timed out.",
                    exception
                )

                throw RecognitionException(
                    "HouseMind timed out while analyzing the photo. Check your connection and try again.",
                    exception
                )
            } catch (
                exception: UnknownHostException
            ) {
                Log.e(
                    TAG,
                    "Could not resolve HouseMind backend.",
                    exception
                )

                throw RecognitionException(
                    "Your phone couldn't reach the HouseMind server. Check Wi-Fi or mobile data and try again.",
                    exception
                )
            } catch (
                exception: Exception
            ) {
                Log.e(
                    TAG,
                    "Recognition request failed.",
                    exception
                )

                throw RecognitionException(
                    "HouseMind couldn't send that photo: ${exception.javaClass.simpleName}. Try again.",
                    exception
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun String.toRecognitionResult():
        RecognitionResult {

        try {
            val json =
                JSONObject(this)

            val result =
                RecognitionResult(
                    itemName =
                        json.getString(
                            "itemName"
                        ),
                    category =
                        json.getString(
                            "category"
                        ),
                    brand =
                        json.getString(
                            "brand"
                        ),
                    modelNumber =
                        json.getString(
                            "modelNumber"
                        ),
                    serialNumber =
                        json.getString(
                            "serialNumber"
                        ),
                    locationSuggestion =
                        json.getString(
                            "locationSuggestion"
                        ),
                    filterPartNumber =
                        json.getString(
                            "filterPartNumber"
                        ),
                    notes =
                        json.getString(
                            "notes"
                        ),
                    confidence =
                        json.getString(
                            "confidence"
                        ),
                    recognizedText =
                        json.getString(
                            "recognizedText"
                        )
                )

            if (
                result.confidence !in
                setOf(
                    "high",
                    "medium",
                    "low"
                )
            ) {
                throw RecognitionException(
                    "HouseMind received an invalid recognition result."
                )
            }

            return result
        } catch (
            exception: RecognitionException
        ) {
            throw exception
        } catch (
            exception: Exception
        ) {
            Log.e(
                TAG,
                "Could not parse recognition response.",
                exception
            )

            throw RecognitionException(
                "HouseMind received a response it couldn't read. Please try again.",
                exception
            )
        }
    }

    private companion object {
        const val TAG =
            "HouseMindRecognition"
    }
}

class RecognitionException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
