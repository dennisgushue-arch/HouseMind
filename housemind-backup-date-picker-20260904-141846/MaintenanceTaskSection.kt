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
    onUpdateTask: (MaintenanceTask) -> Unit,
    onDeleteTask: (MaintenanceTask) -> Unit,
    onMarkDone: (MaintenanceTask) -> Unit
) {
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialTitle by rememberSaveable { mutableStateOf("") }
    var initialIntervalValue by rememberSaveable { mutableStateOf("3") }
    var initialIntervalUnit by rememberSaveable { mutableStateOf("months") }

    val editingTask = item.maintenanceTasks.firstOrNull { it.id == editingTaskId }

    if (editingTask != null) {
        MaintenanceTaskForm(
            existingTask = editingTask,
            initialTitle = editingTask.title,
            initialIntervalValue = editingTask.intervalValue.toString(),
            initialIntervalUnit = editingTask.intervalUnit,
            onCancel = { editingTaskId = null },
            onSave = { updatedTask ->
                onUpdateTask(updatedTask)
                editingTaskId = null
            }
        )
        return
    }

    if (addingTask) {
        MaintenanceTaskForm(
            existingTask = null,
            initialTitle = initialTitle,
            initialIntervalValue = initialIntervalValue,
            initialIntervalUnit = initialIntervalUnit,
            onCancel = { addingTask = false },
            onSave = { task ->
                onAddTask(task)
                addingTask = false
            }
        )
        return
    }

    val suggestions = MaintenanceSuggestionEngine
        .suggestionsFor(item)
        .filterNot { suggestion ->
            item.maintenanceTasks.any { task ->
                task.title.equals(suggestion.title, ignoreCase = true)
            }
        }

    Column {
        Text("Maintenance Schedule", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Track what needs to be done and when.")

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Suggested for this item", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Typical guidance only. Confirm the interval with your owner's manual or manufacturer.")
            Spacer(Modifier.height(12.dp))

            suggestions.forEachIndexed { index, suggestion ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(suggestion.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Suggested every ${suggestion.intervalValue} ${suggestion.intervalUnit}")
                        Spacer(Modifier.height(6.dp))
                        Text(suggestion.reason)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                initialTitle = suggestion.title
                                initialIntervalValue = suggestion.intervalValue.toString()
                                initialIntervalUnit = suggestion.intervalUnit
                                addingTask = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Suggestion")
                        }
                    }
                }
                if (index < suggestions.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                initialTitle = ""
                initialIntervalValue = "3"
                initialIntervalUnit = "months"
                addingTask = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Add Maintenance Task")
        }

        Spacer(Modifier.height(20.dp))

        if (item.maintenanceTasks.isEmpty()) {
            Text("No maintenance scheduled yet.")
        } else {
            item.maintenanceTasks.forEachIndexed { index, task ->
                MaintenanceTaskCard(
                    task = task,
                    onMarkDone = {
                        onMarkDone(task.copy(lastCompletedDate = LocalDate.now().toString()))
                    },
                    onEdit = { editingTaskId = task.id },
                    onDelete = { onDeleteTask(task) }
                )
                if (index < item.maintenanceTasks.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MaintenanceTaskCard(
    task: MaintenanceTask,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmingDelete by rememberSaveable(task.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(task.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(MaintenanceScheduleCalculator.statusText(task), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("Next due: ${MaintenanceScheduleCalculator.formattedDueDate(task)}")
            Spacer(Modifier.height(4.dp))
            Text("Repeat every ${task.intervalValue} ${task.intervalUnit}")
            Spacer(Modifier.height(4.dp))
            Text(if (task.reminderEnabled) "Reminder on" else "Reminder paused")
            Spacer(Modifier.height(16.dp))

            Button(onClick = onMarkDone, modifier = Modifier.fillMaxWidth()) {
                Text("Mark Done")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit")
            }

            Spacer(Modifier.height(8.dp))

            if (!confirmingDelete) {
                OutlinedButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }
            } else {
                Text("Delete this maintenance schedule?", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Schedule")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { confirmingDelete = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun MaintenanceTaskForm(
    existingTask: MaintenanceTask?,
    initialTitle: String,
    initialIntervalValue: String,
    initialIntervalUnit: String,
    onCancel: () -> Unit,
    onSave: (MaintenanceTask) -> Unit
) {
    var title by rememberSaveable(existingTask?.id, initialTitle) {
        mutableStateOf(existingTask?.title ?: initialTitle)
    }
    var lastCompletedDate by rememberSaveable(existingTask?.id) {
        mutableStateOf(existingTask?.lastCompletedDate ?: LocalDate.now().toString())
    }
    var intervalValue by rememberSaveable(existingTask?.id, initialIntervalValue) {
        mutableStateOf(existingTask?.intervalValue?.toString() ?: initialIntervalValue)
    }
    var intervalUnit by rememberSaveable(existingTask?.id, initialIntervalUnit) {
        mutableStateOf(existingTask?.intervalUnit ?: initialIntervalUnit)
    }
    var reminderEnabled by rememberSaveable(existingTask?.id) {
        mutableStateOf(existingTask?.reminderEnabled ?: true)
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column {
        Text(
            if (existingTask == null) "Add Maintenance Task" else "Edit Maintenance Task",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (existingTask == null) {
                "Confirm when you last completed this task. HouseMind will calculate the next due date."
            } else {
                "Update the schedule, last-completed date, or reminder setting."
            }
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task") },
            placeholder = { Text("Replace water filter") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = lastCompletedDate,
            onValueChange = { lastCompletedDate = it },
            label = { Text("Last completed") },
            supportingText = { Text("Use YYYY-MM-DD") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = intervalValue,
            onValueChange = { intervalValue = it },
            label = { Text("Repeat every") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        Text("Interval", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            UnitButton("Days", "days", intervalUnit, Modifier.weight(1f)) { intervalUnit = it }
            Spacer(Modifier.width(8.dp))
            UnitButton("Weeks", "weeks", intervalUnit, Modifier.weight(1f)) { intervalUnit = it }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            UnitButton("Months", "months", intervalUnit, Modifier.weight(1f)) { intervalUnit = it }
            Spacer(Modifier.width(8.dp))
            UnitButton("Years", "years", intervalUnit, Modifier.weight(1f)) { intervalUnit = it }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Reminder", fontWeight = FontWeight.Bold)
                Text(
                    if (reminderEnabled) {
                        "HouseMind will remind you when this is due"
                    } else {
                        "Reminders are paused for this task"
                    }
                )
            }
            Switch(
                checked = reminderEnabled,
                onCheckedChange = { reminderEnabled = it }
            )
        }

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val interval = intervalValue.toIntOrNull()
                val validDate = runCatching { LocalDate.parse(lastCompletedDate) }.isSuccess

                when {
                    title.isBlank() -> errorMessage = "Enter a maintenance task."
                    interval == null || interval <= 0 ->
                        errorMessage = "Enter a valid repeat interval."
                    !validDate -> errorMessage = "Enter the date as YYYY-MM-DD."
                    else -> onSave(
                        MaintenanceTask(
                            id = existingTask?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            lastCompletedDate = lastCompletedDate,
                            intervalValue = interval,
                            intervalUnit = intervalUnit,
                            reminderEnabled = reminderEnabled
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (existingTask == null) "Save Schedule" else "Save Changes")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Cancel")
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
        Button(onClick = { onSelected(value) }, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = { onSelected(value) }, modifier = modifier) { Text(label) }
    }
}
