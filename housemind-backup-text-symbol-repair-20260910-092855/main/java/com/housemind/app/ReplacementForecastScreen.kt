package com.housemind.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.housemind.app.logic.ReplacementForecastCalculator
import com.housemind.app.model.HouseItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacementForecastScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit,
    onUpdateItem: (HouseItem) -> Unit
) {
    val suggested = ReplacementForecastCalculator.suggestedLifespan(item.category, item.name)

    var installedDate by rememberSaveable(item.id) { mutableStateOf(item.installedDate ?: "") }
    var minYears by rememberSaveable(item.id) {
        mutableStateOf((item.lifespanMinYears ?: suggested?.minYears)?.toString() ?: "")
    }
    var maxYears by rememberSaveable(item.id) {
        mutableStateOf((item.lifespanMaxYears ?: suggested?.maxYears)?.toString() ?: "")
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val previewItem = item.copy(
        installedDate = installedDate.takeIf { it.isNotBlank() },
        lifespanMinYears = minYears.toIntOrNull(),
        lifespanMaxYears = maxYears.toIntOrNull()
    )
    val forecast = ReplacementForecastCalculator.forecast(previewItem)

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(20.dp))

        Text("Replacement Forecast", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(item.name, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Text("Estimate when replacement may become worth budgeting for based on age and a planning lifespan range.")
        Spacer(Modifier.height(22.dp))

        Text("Installed or purchased", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            val label = installedDate
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { ReplacementForecastCalculator.formatDate(it) }
                ?: "Choose date"
            Text(label)
        }

        if (showDatePicker) {
            val initialMillis = installedDate
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
                ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            installedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toString()
                        }
                        showDatePicker = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Planning lifespan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        Text(
            if (suggested != null) {
                "General starting estimate for this category: ${suggested.minYears}-${suggested.maxYears} years. You can change it."
            } else {
                "No default lifespan range is available for this category. Enter the range you want HouseMind to use."
            }
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = minYears,
            onValueChange = { minYears = it.filter(Char::isDigit) },
            label = { Text("Minimum years") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = maxYears,
            onValueChange = { maxYears = it.filter(Char::isDigit) },
            label = { Text("Maximum years") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(20.dp))

        if (forecast != null) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(forecast.statusText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    ForecastValue("Current age", forecast.ageText)
                    ForecastValue("Planning lifespan", "${forecast.lifespan.minYears}-${forecast.lifespan.maxYears} years")
                    ForecastValue(
                        "Estimated replacement window",
                        "${ReplacementForecastCalculator.formatDate(forecast.earliestReplacementDate)} - " +
                            ReplacementForecastCalculator.formatDate(forecast.latestReplacementDate)
                    )
                    ForecastValue(
                        "Start planning by",
                        ReplacementForecastCalculator.formatDate(forecast.planningStartDate)
                    )
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(
                    "Choose an installed date and enter a valid lifespan range to see the forecast.",
                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Planning estimate only. Actual service life varies by model, usage, maintenance, installation, environment, and manufacturer guidance.")

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val min = minYears.toIntOrNull()
                val max = maxYears.toIntOrNull()
                val date = installedDate
                    .takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                when {
                    date == null -> errorMessage = "Choose the installed or purchased date."
                    date.isAfter(LocalDate.now()) -> errorMessage = "The installed date cannot be in the future."
                    min == null || min <= 0 -> errorMessage = "Enter a valid minimum lifespan."
                    max == null || max < min -> errorMessage = "Maximum lifespan must be at least the minimum."
                    else -> {
                        onUpdateItem(
                            item.copy(
                                installedDate = date.toString(),
                                lifespanMinYears = min,
                                lifespanMaxYears = max
                            )
                        )
                        errorMessage = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Save Forecast") }

        if (item.installedDate != null || item.lifespanMinYears != null || item.lifespanMaxYears != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    installedDate = ""
                    minYears = suggested?.minYears?.toString() ?: ""
                    maxYears = suggested?.maxYears?.toString() ?: ""
                    onUpdateItem(
                        item.copy(
                            installedDate = null,
                            lifespanMinYears = null,
                            lifespanMaxYears = null
                        )
                    )
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Clear Forecast") }
        }
    }
}

@Composable
private fun ForecastValue(label: String, value: String) {
    Text(label, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(3.dp))
    Text(value)
    Spacer(Modifier.height(12.dp))
}
