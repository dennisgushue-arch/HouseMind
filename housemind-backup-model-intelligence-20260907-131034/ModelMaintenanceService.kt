package com.housemind.app.modelresearch

import com.housemind.app.model.HouseItem
import com.housemind.app.model.ModelMaintenanceRecommendation
import com.housemind.app.model.ModelMaintenanceResult
import com.housemind.app.recognition.HouseMindConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ModelMaintenanceService {

    suspend fun research(
        item: HouseItem
    ): ModelMaintenanceResult =
        withContext(
            Dispatchers.IO
        ) {
            if (
                item.brand.isBlank() ||
                item.modelNumber.isBlank()
            ) {
                throw ModelMaintenanceException(
                    "Brand and model number are required."
                )
            }

            if (
                HouseMindConfig.API_BASE_URL
                    .isBlank()
            ) {
                throw ModelMaintenanceException(
                    "HouseMind model research is not configured."
                )
            }

            val connection =
                (
                    URL(
                        "${
                            HouseMindConfig
                                .API_BASE_URL
                                .trimEnd('/')
                        }/api/model-maintenance"
                    )
                        .openConnection()
                    as HttpURLConnection
                )

            try {
                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    20_000

                connection.readTimeout =
                    90_000

                connection.doOutput =
                    true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                val requestBody =
                    JSONObject()
                        .put(
                            "brand",
                            item.brand
                        )
                        .put(
                            "modelNumber",
                            item.modelNumber
                        )
                        .put(
                            "category",
                            item.category
                        )

                connection.outputStream
                    .writer()
                    .use { output ->
                        output.write(
                            requestBody
                                .toString()
                        )
                    }

                if (
                    connection.responseCode
                    !in 200..299
                ) {
                    val errorText =
                        connection.errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                            .orEmpty()

                    throw ModelMaintenanceException(
                        errorMessageFrom(
                            errorText
                        )
                    )
                }

                val responseText =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                responseText
                    .toModelMaintenanceResult()
            } catch (
                exception:
                ModelMaintenanceException
            ) {
                throw exception
            } catch (
                exception:
                Exception
            ) {
                throw ModelMaintenanceException(
                    "HouseMind couldn't research that model. Please try again.",
                    exception
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun String
        .toModelMaintenanceResult():
        ModelMaintenanceResult {

        try {
            val json =
                JSONObject(this)

            val recommendationsJson =
                json.getJSONArray(
                    "recommendations"
                )

            val recommendations =
                List(
                    recommendationsJson
                        .length()
                ) { index ->

                    val item =
                        recommendationsJson
                            .getJSONObject(
                                index
                            )

                    ModelMaintenanceRecommendation(
                        name =
                            item.getString(
                                "name"
                            ),

                        kind =
                            item.getString(
                                "kind"
                            ),

                        partNumber =
                            item.getString(
                                "partNumber"
                            ),

                        maintenanceTitle =
                            item.getString(
                                "maintenanceTitle"
                            ),

                        hasInterval =
                            item.getBoolean(
                                "hasInterval"
                            ),

                        intervalValue =
                            item.getInt(
                                "intervalValue"
                            ),

                        intervalUnit =
                            item.getString(
                                "intervalUnit"
                            ),

                        instructions =
                            item.getString(
                                "instructions"
                            ),

                        sourceTitle =
                            item.getString(
                                "sourceTitle"
                            ),

                        sourceUrl =
                            item.getString(
                                "sourceUrl"
                            ),

                        verification =
                            item.getString(
                                "verification"
                            )
                    )
                }

            return ModelMaintenanceResult(
                brand =
                    json.getString(
                        "brand"
                    ),

                modelNumber =
                    json.getString(
                        "modelNumber"
                    ),

                exactModelMatched =
                    json.getBoolean(
                        "exactModelMatched"
                    ),

                summary =
                    json.getString(
                        "summary"
                    ),

                recommendations =
                    recommendations
            )
        } catch (
            exception:
            Exception
        ) {
            throw ModelMaintenanceException(
                "HouseMind couldn't read the model research result.",
                exception
            )
        }
    }

    private fun errorMessageFrom(
        responseText: String
    ): String =
        runCatching {
            JSONObject(
                responseText
            )
                .optString(
                    "error"
                )
                .takeIf {
                    it.isNotBlank()
                }
        }
            .getOrNull()
            ?: "HouseMind model research is temporarily unavailable."
}

class ModelMaintenanceException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
