package com.housemind.app

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.housemind.app.model.HouseItem
import com.housemind.app.model.ReplacementPart
import java.util.UUID

@Composable
fun PartsAndFiltersScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit,
    onUpdateItem: (HouseItem) -> Unit
) {
    var addingPart by rememberSaveable { mutableStateOf(false) }
    var editingPartId by rememberSaveable { mutableStateOf<String?>(null) }

    val editingPart =
        item.partsAndFilters.firstOrNull { it.id == editingPartId }

    if (editingPart != null) {
        PartForm(
            existingPart = editingPart,
            onCancel = { editingPartId = null },
            onSave = { updatedPart ->
                onUpdateItem(
                    item.copy(
                        partsAndFilters =
                            item.partsAndFilters.map { savedPart ->
                                if (savedPart.id == updatedPart.id) {
                                    updatedPart
                                } else {
                                    savedPart
                                }
                            }
                    )
                )
                editingPartId = null
            }
        )
        return
    }

    if (addingPart) {
        PartForm(
            existingPart = null,
            onCancel = { addingPart = false },
            onSave = { newPart ->
                onUpdateItem(
                    item.copy(
                        partsAndFilters =
                            item.partsAndFilters + newPart
                    )
                )
                addingPart = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Parts & Filters",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(item.name, fontSize = 18.sp)

        Spacer(Modifier.height(12.dp))

        Text(
            "Save exact replacement numbers so you never have to search for them again."
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { addingPart = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Add Part or Filter")
        }

        Spacer(Modifier.height(24.dp))

        if (item.partsAndFilters.isEmpty()) {
            Text("No parts or filters saved yet.")
        } else {
            item.partsAndFilters.forEachIndexed { index, part ->
                PartCard(
                    part = part,
                    onEdit = { editingPartId = part.id },
                    onDelete = {
                        onUpdateItem(
                            item.copy(
                                partsAndFilters =
                                    item.partsAndFilters.filterNot {
                                        it.id == part.id
                                    }
                            )
                        )
                    }
                )

                if (index < item.partsAndFilters.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PartCard(
    part: ReplacementPart,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmingDelete by rememberSaveable(part.id) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                part.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))
            Text(part.kind)

            if (part.partNumber.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Part #: ${part.partNumber}",
                    fontWeight = FontWeight.Bold
                )
            }

            if (part.brand.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Brand: ${part.brand}")
            }

            if (part.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(part.notes)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                Text(
                    "Delete this saved part?",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Part")
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
private fun PartForm(
    existingPart: ReplacementPart?,
    onCancel: () -> Unit,
    onSave: (ReplacementPart) -> Unit
) {
    var name by rememberSaveable(existingPart?.id) {
        mutableStateOf(existingPart?.name ?: "")
    }
    var kind by rememberSaveable(existingPart?.id) {
        mutableStateOf(existingPart?.kind ?: "Filter")
    }
    var partNumber by rememberSaveable(existingPart?.id) {
        mutableStateOf(existingPart?.partNumber ?: "")
    }
    var brand by rememberSaveable(existingPart?.id) {
        mutableStateOf(existingPart?.brand ?: "")
    }
    var notes by rememberSaveable(existingPart?.id) {
        mutableStateOf(existingPart?.notes ?: "")
    }
    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            if (existingPart == null) {
                "Add Part or Filter"
            } else {
                "Edit Part or Filter"
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            placeholder = { Text("Water filter") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(14.dp))
        Text("Type", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            TypeButton(
                label = "Filter",
                selected = kind == "Filter",
                modifier = Modifier.weight(1f)
            ) { kind = "Filter" }

            Spacer(Modifier.width(8.dp))

            TypeButton(
                label = "Part",
                selected = kind == "Part",
                modifier = Modifier.weight(1f)
            ) { kind = "Part" }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            TypeButton(
                label = "Battery",
                selected = kind == "Battery",
                modifier = Modifier.weight(1f)
            ) { kind = "Battery" }

            Spacer(Modifier.width(8.dp))

            TypeButton(
                label = "Other",
                selected = kind == "Other",
                modifier = Modifier.weight(1f)
            ) { kind = "Other" }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = partNumber,
            onValueChange = { partNumber = it },
            label = { Text("Part number") },
            placeholder = { Text("XWFE") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Brand") },
            placeholder = { Text("GE") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            placeholder = {
                Text("Where I bought it, size, color, or other details")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (name.isBlank()) {
                    errorMessage =
                        "Enter a name for this part or filter."
                } else {
                    onSave(
                        ReplacementPart(
                            id =
                                existingPart?.id
                                    ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            kind = kind,
                            partNumber = partNumber.trim(),
                            brand = brand.trim(),
                            notes = notes.trim()
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (existingPart == null) {
                    "Save Part"
                } else {
                    "Save Changes"
                }
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun TypeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    }
}
