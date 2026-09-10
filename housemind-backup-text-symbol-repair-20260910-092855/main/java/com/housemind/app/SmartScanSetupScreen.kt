package com.housemind.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.housemind.app.model.HouseItem
import com.housemind.app.model.MaintenanceTask
import com.housemind.app.model.ModelMaintenanceRecommendation
import com.housemind.app.model.ModelMaintenanceResult
import com.housemind.app.model.RecognitionResult
import com.housemind.app.model.ReplacementPart
import com.housemind.app.modelresearch.ModelMaintenanceService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun SmartScanSetupScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    imageUri: String,
    recognitionResult: RecognitionResult,
    onBack: () -> Unit,
    onAddToHome: (HouseItem) -> Unit
) {
    var name by rememberSaveable(recognitionResult) { mutableStateOf(recognitionResult.itemName) }
    var category by rememberSaveable(recognitionResult) { mutableStateOf(recognitionResult.category) }
    var brand by rememberSaveable(recognitionResult) { mutableStateOf(recognitionResult.brand) }
    var modelNumber by rememberSaveable(recognitionResult) { mutableStateOf(recognitionResult.modelNumber) }
    var serialNumber by rememberSaveable(recognitionResult) { mutableStateOf(recognitionResult.serialNumber) }
    var location by rememberSaveable(recognitionResult) { mutableStateOf(recognitionResult.locationSuggestion) }
    var notes by rememberSaveable(recognitionResult) { mutableStateOf(recognitionResult.notes) }

    var researching by rememberSaveable { mutableStateOf(false) }
    var autoTried by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ModelMaintenanceResult?>(null) }

    val service = remember { ModelMaintenanceService() }
    val scope = rememberCoroutineScope()

    fun baseItem() = HouseItem(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        category = category.trim(),
        brand = brand.trim(),
        modelNumber = modelNumber.trim(),
        serialNumber = serialNumber.trim(),
        location = location.trim(),
        filterPartNumber = recognitionResult.filterPartNumber.trim(),
        notes = notes.trim(),
        status = "Recently added"
    )

    fun research() {
        if (brand.isBlank() || modelNumber.isBlank()) {
            error = "Enter both brand and model number to research exact parts."
            return
        }

        error = null
        result = null
        researching = true

        scope.launch {
            runCatching { service.research(baseItem()) }
                .onSuccess {
                    result = it
                    researching = false
                }
                .onFailure {
                    error = it.message ?: "HouseMind couldn't research that model."
                    researching = false
                }
        }
    }

    LaunchedEffect(recognitionResult) {
        if (!autoTried && brand.isNotBlank() && modelNumber.isNotBlank()) {
            autoTried = true
            research()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }

        Spacer(Modifier.height(18.dp))

        Text("Smart setup", style = MaterialTheme.typography.headlineMedium)
        Text(
            "HouseMind can identify the exact model, find verified parts, and build the recurring schedule.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))
        SelectedImagePreview(imageUri)
        Spacer(Modifier.height(20.dp))

        SmartField("Item name", name) { name = it }
        SmartField("Category", category) { category = it }
        SmartField("Brand", brand) {
            brand = it
            result = null
            error = null
        }
        SmartField("Model number", modelNumber) {
            modelNumber = it
            result = null
            error = null
        }
        SmartField("Serial number", serialNumber) { serialNumber = it }
        SmartField("Room / location", location) { location = it }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Model intelligence", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))

                if (brand.isBlank() || modelNumber.isBlank()) {
                    Text(
                        "Add the exact brand and model number above. HouseMind will not guess compatibility.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "$brand â€¢ $modelNumber",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    if (researching) {
                        Row {
                            CircularProgressIndicator(
                                modifier = Modifier.height(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Verifying exact model...")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { research() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (result == null) "Research this model" else "Research again")
                        }
                    }
                }

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                result?.let { modelResult ->
                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (modelResult.exactModelMatched) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            if (modelResult.exactModelMatched) "Exact model verified"
                            else "Exact model not verified",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val verified = modelResult.recommendations.filter {
                        it.verification == "manufacturer"
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (verified.isEmpty()) "No manufacturer-verified items found."
                        else "${verified.size} manufacturer-verified item${if (verified.size == 1) "" else "s"} found",
                        style = MaterialTheme.typography.titleMedium
                    )

                    verified.take(6).forEach { recommendation ->
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(recommendation.name, fontWeight = FontWeight.SemiBold)
                                if (recommendation.partNumber.isNotBlank()) {
                                    Text("Part # ${recommendation.partNumber}")
                                }
                                if (recommendation.hasInterval) {
                                    Text(
                                        "Every ${recommendation.intervalValue} ${recommendation.intervalUnit}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (recommendation.sourceTitle.isNotBlank()) {
                                    Text(
                                        recommendation.sourceTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        val verified = result?.recommendations.orEmpty().filter {
            it.verification == "manufacturer"
        }

        if (result?.exactModelMatched == true && verified.isNotEmpty()) {
            val schedules = verified.count { it.hasInterval && it.intervalValue > 0 }

            Button(
                onClick = {
                    onAddToHome(
                        smartSetupItem(
                            base = baseItem(),
                            recommendations = verified,
                            startSchedules = true
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Set up my ${name.ifBlank { "item" }}")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                if (schedules > 0) {
                    "Saves verified parts and creates $schedules recurring schedule${if (schedules == 1) "" else "s"}. Tracking starts today; edit the true last-change date later if you know it."
                } else {
                    "Saves the manufacturer-verified parts HouseMind found."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    onAddToHome(
                        smartSetupItem(
                            base = baseItem(),
                            recommendations = verified,
                            startSchedules = false
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add without starting schedules")
            }
        } else {
            Button(
                onClick = { onAddToHome(baseItem()) },
                enabled = name.isNotBlank() || brand.isNotBlank() || modelNumber.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add to my home")
            }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SmartField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
    Spacer(Modifier.height(10.dp))
}

private fun smartSetupItem(
    base: HouseItem,
    recommendations: List<ModelMaintenanceRecommendation>,
    startSchedules: Boolean
): HouseItem {
    val verified = recommendations.filter {
        it.verification == "manufacturer"
    }

    val parts = verified
        .filter { it.partNumber.isNotBlank() || it.kind != "Maintenance" }
        .distinctBy {
            if (it.partNumber.isNotBlank()) it.partNumber.lowercase()
            else it.name.lowercase()
        }
        .map {
            ReplacementPart(
                id = UUID.randomUUID().toString(),
                name = it.name,
                kind = it.kind.takeIf { kind ->
                    kind in setOf("Filter", "Part", "Battery", "Other")
                } ?: "Other",
                partNumber = it.partNumber,
                brand = base.brand,
                notes = buildString {
                    if (it.instructions.isNotBlank()) append(it.instructions)
                    if (it.sourceTitle.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append("Source: ${it.sourceTitle}")
                    }
                    if (it.sourceUrl.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(it.sourceUrl)
                    }
                }
            )
        }

    val tasks = if (startSchedules) {
        verified
            .filter { it.hasInterval && it.intervalValue > 0 }
            .distinctBy {
                it.maintenanceTitle.ifBlank { "Replace ${it.name}" }.lowercase()
            }
            .map {
                MaintenanceTask(
                    id = UUID.randomUUID().toString(),
                    title = it.maintenanceTitle.ifBlank { "Replace ${it.name}" },
                    lastCompletedDate = LocalDate.now().toString(),
                    intervalValue = it.intervalValue,
                    intervalUnit = it.intervalUnit,
                    reminderEnabled = true
                )
            }
    } else {
        emptyList()
    }

    val filterNumber = verified.firstOrNull {
        it.kind == "Filter" && it.partNumber.isNotBlank()
    }?.partNumber.orEmpty()

    return base.copy(
        filterPartNumber = base.filterPartNumber.ifBlank { filterNumber },
        partsAndFilters = (base.partsAndFilters + parts).distinctBy {
            if (it.partNumber.isNotBlank()) it.partNumber.lowercase()
            else it.name.lowercase()
        },
        maintenanceTasks = (base.maintenanceTasks + tasks).distinctBy {
            it.title.lowercase()
        }
    )
}
