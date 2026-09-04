package com.housemind.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.housemind.app.logic.MaintenanceScheduleCalculator
import com.housemind.app.logic.MaintenanceSuggestionEngine
import com.housemind.app.model.HouseItem
import com.housemind.app.model.MaintenanceTask
import java.time.LocalDate
import java.util.UUID

@Composable
fun MaintenanceTaskSection(
    item: HouseItem,
    onAddTask: (MaintenanceTask) -> Unit,
    onMarkDone: (MaintenanceTask) -> Unit
) {

    var addingTask by rememberSaveable {
        mutableStateOf(false)
    }

    var initialTitle by rememberSaveable {
        mutableStateOf("")
    }

    var initialIntervalValue by rememberSaveable {
        mutableStateOf("3")
    }

    var initialIntervalUnit by rememberSaveable {
        mutableStateOf("months")
    }

    if (addingTask) {

        AddMaintenanceTaskForm(
            initialTitle = initialTitle,
            initialIntervalValue = initialIntervalValue,
            initialIntervalUnit = initialIntervalUnit,
            onCancel = {
                addingTask = false
            },
            onSave = { task ->

                onAddTask(task)

                addingTask = false
            }
        )

        return
    }

    val suggestions =
        MaintenanceSuggestionEngine
            .suggestionsFor(item)
            .filterNot { suggestion ->
                item.maintenanceTasks.any { task ->
                    task.title.equals(
                        suggestion.title,
                        ignoreCase = true
                    )
                }
            }

    Column {

        Text(
            text = "Maintenance Schedule",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Track what needs to be done and when."
        )

        if (suggestions.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Suggested for this item",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Typical guidance only. Confirm the interval with your owner's manual or manufacturer."
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            suggestions.forEachIndexed { index, suggestion ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = suggestion.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Suggested every ${suggestion.intervalValue} ${suggestion.intervalUnit}"
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = suggestion.reason
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                initialTitle =
                                    suggestion.title

                                initialIntervalValue =
                                    suggestion.intervalValue
                                        .toString()

                                initialIntervalUnit =
                                    suggestion.intervalUnit

                                addingTask = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = "Use Suggestion"
                            )
                        }
                    }
                }

                if (index < suggestions.lastIndex) {

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
                initialTitle = ""
                initialIntervalValue = "3"
                initialIntervalUnit = "months"
                addingTask = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text = "Add Maintenance Task"
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (item.maintenanceTasks.isEmpty()) {

            Text(
                text = "No maintenance scheduled yet."
            )

        } else {

            item.maintenanceTasks
                .forEachIndexed { index, task ->

                    MaintenanceTaskCard(
                        task = task,
                        onMarkDone = {
                            onMarkDone(
                                task.copy(
                                    lastCompletedDate =
                                        LocalDate.now()
                                            .toString()
                                )
                            )
                        }
                    )

                    if (
                        index <
                        item.maintenanceTasks.lastIndex
                    ) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }
                }
        }
    }
}

@Composable
private fun MaintenanceTaskCard(
    task: MaintenanceTask,
    onMarkDone: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = task.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    MaintenanceScheduleCalculator
                        .statusText(task),
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Next due: ${
                        MaintenanceScheduleCalculator
                            .formattedDueDate(task)
                    }"
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Repeat every ${task.intervalValue} ${task.intervalUnit}"
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    if (task.reminderEnabled) {
                        "Reminder on"
                    } else {
                        "Reminder off"
                    }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onMarkDone,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Mark Done"
                )
            }
        }
    }
}

@Composable
private fun AddMaintenanceTaskForm(
    initialTitle: String,
    initialIntervalValue: String,
    initialIntervalUnit: String,
    onCancel: () -> Unit,
    onSave: (MaintenanceTask) -> Unit
) {

    var title by rememberSaveable(initialTitle) {
        mutableStateOf(initialTitle)
    }

    var lastCompletedDate by rememberSaveable {
        mutableStateOf(
            LocalDate.now().toString()
        )
    }

    var intervalValue by rememberSaveable(
        initialIntervalValue
    ) {
        mutableStateOf(initialIntervalValue)
    }

    var intervalUnit by rememberSaveable(
        initialIntervalUnit
    ) {
        mutableStateOf(initialIntervalUnit)
    }

    var reminderEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    Column {

        Text(
            text = "Add Maintenance Task",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Confirm when you last completed this task. HouseMind will calculate the next due date."
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
            },
            label = {
                Text("Task")
            },
            placeholder = {
                Text("Replace water filter")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = lastCompletedDate,
            onValueChange = {
                lastCompletedDate = it
            },
            label = {
                Text("Last completed")
            },
            supportingText = {
                Text("Use YYYY-MM-DD")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = intervalValue,
            onValueChange = {
                intervalValue = it
            },
            label = {
                Text("Repeat every")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "Interval",
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            UnitButton(
                label = "Days",
                value = "days",
                selectedValue = intervalUnit,
                modifier = Modifier.weight(1f)
            ) {
                intervalUnit = it
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            UnitButton(
                label = "Weeks",
                value = "weeks",
                selectedValue = intervalUnit,
                modifier = Modifier.weight(1f)
            ) {
                intervalUnit = it
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            UnitButton(
                label = "Months",
                value = "months",
                selectedValue = intervalUnit,
                modifier = Modifier.weight(1f)
            ) {
                intervalUnit = it
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            UnitButton(
                label = "Years",
                value = "years",
                selectedValue = intervalUnit,
                modifier = Modifier.weight(1f)
            ) {
                intervalUnit = it
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Reminder",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Remind me when this is due"
                )
            }

            Switch(
                checked = reminderEnabled,
                onCheckedChange = {
                    reminderEnabled = it
                }
            )
        }

        errorMessage?.let {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = it
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                val interval =
                    intervalValue.toIntOrNull()

                val validDate =
                    runCatching {
                        LocalDate.parse(
                            lastCompletedDate
                        )
                    }.isSuccess

                when {

                    title.isBlank() -> {

                        errorMessage =
                            "Enter a maintenance task."
                    }

                    interval == null ||
                        interval <= 0 -> {

                        errorMessage =
                            "Enter a valid repeat interval."
                    }

                    !validDate -> {

                        errorMessage =
                            "Enter the date as YYYY-MM-DD."
                    }

                    else -> {

                        onSave(
                            MaintenanceTask(
                                id =
                                    UUID.randomUUID()
                                        .toString(),

                                title =
                                    title.trim(),

                                lastCompletedDate =
                                    lastCompletedDate,

                                intervalValue =
                                    interval,

                                intervalUnit =
                                    intervalUnit,

                                reminderEnabled =
                                    reminderEnabled
                            )
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text = "Save Schedule"
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {

            Text(
                text = "Cancel"
            )
        }
    }
}

@Composable
private fun UnitButton(
    label: String,
    value: String,
    selectedValue: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {

    if (selectedValue == value) {

        Button(
            onClick = {
                onSelected(value)
            },
            modifier = modifier
        ) {

            Text(label)
        }

    } else {

        OutlinedButton(
            onClick = {
                onSelected(value)
            },
            modifier = modifier
        ) {

            Text(label)
        }
    }
}
