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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.housemind.app.model.HouseItem
import com.housemind.app.model.MaintenanceTask
import com.housemind.app.model.ModelMaintenanceRecommendation
import com.housemind.app.model.ModelMaintenanceResult
import com.housemind.app.model.ReplacementPart
import com.housemind.app.modelresearch.ModelMaintenanceService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun PartsAndFiltersScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit,
    onUpdateItem: (HouseItem) -> Unit
) {
    val coroutineScope =
        rememberCoroutineScope()

    val modelService =
        remember {
            ModelMaintenanceService()
        }

    var addingPart by
        rememberSaveable {
            mutableStateOf(false)
        }

    var editingPartId by
        rememberSaveable {
            mutableStateOf<String?>(
                null
            )
        }

    var researching by
        rememberSaveable {
            mutableStateOf(false)
        }

    var researchError by
        rememberSaveable {
            mutableStateOf<String?>(
                null
            )
        }

    var researchResult by
        remember {
            mutableStateOf<ModelMaintenanceResult?>(
                null
            )
        }

    val editingPart =
        item.partsAndFilters
            .firstOrNull {
                it.id ==
                    editingPartId
            }

    if (
        editingPart != null
    ) {
        PartForm(
            existingPart =
                editingPart,

            onCancel = {
                editingPartId =
                    null
            },

            onSave = {
                updatedPart ->

                onUpdateItem(
                    item.copy(
                        partsAndFilters =
                            item.partsAndFilters
                                .map {
                                    savedPart ->

                                    if (
                                        savedPart.id ==
                                        updatedPart.id
                                    ) {
                                        updatedPart
                                    } else {
                                        savedPart
                                    }
                                }
                    )
                )

                editingPartId =
                    null
            }
        )

        return
    }

    if (
        addingPart
    ) {
        PartForm(
            existingPart =
                null,

            onCancel = {
                addingPart =
                    false
            },

            onSave = {
                newPart ->

                onUpdateItem(
                    item.copy(
                        partsAndFilters =
                            item.partsAndFilters +
                                newPart
                    )
                )

                addingPart =
                    false
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                contentPadding
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                20.dp
            )
    ) {
        OutlinedButton(
            onClick =
                onBack
        ) {
            Text(
                "Back"
            )
        }

        Spacer(
            Modifier.height(
                20.dp
            )
        )

        Text(
            text =
                "Parts & Filters",
            style =
                MaterialTheme
                    .typography
                    .displaySmall
        )

        Spacer(
            Modifier.height(
                4.dp
            )
        )

        Text(
            text =
                listOf(
                    item.brand,
                    item.modelNumber
                )
                    .filter {
                        it.isNotBlank()
                    }
                    .joinToString(
                        "  â€¢  "
                    )
                    .ifBlank {
                        item.name
                    },
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            Modifier.height(
                22.dp
            )
        )

        ModelResearchCard(
            item =
                item,

            researching =
                researching,

            result =
                researchResult,

            error =
                researchError,

            onResearch = {
                researchError =
                    null

                researchResult =
                    null

                researching =
                    true

                coroutineScope.launch {
                    runCatching {
                        modelService
                            .research(
                                item
                            )
                    }
                        .onSuccess {
                            result ->

                            researchResult =
                                result

                            researching =
                                false
                        }
                        .onFailure {
                            error ->

                            researchError =
                                error.message
                                    ?: "HouseMind couldn't research that model."

                            researching =
                                false
                        }
                }
            },

            onSavePart = {
                recommendation ->

                val updated =
                    addRecommendationPart(
                        item =
                            item,
                        recommendation =
                            recommendation
                    )

                onUpdateItem(
                    updated
                )
            },

            onStartSchedule = {
                recommendation ->

                val updated =
                    addRecommendationPartAndSchedule(
                        item =
                            item,
                        recommendation =
                            recommendation
                    )

                onUpdateItem(
                    updated
                )
            }
        )

        Spacer(
            Modifier.height(
                28.dp
            )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                text =
                    "Saved parts",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            OutlinedButton(
                onClick = {
                    addingPart =
                        true
                }
            ) {
                Text(
                    "Add manually"
                )
            }
        }

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        if (
            item.partsAndFilters
                .isEmpty()
        ) {
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                border =
                    BorderStroke(
                        1.dp,
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                    ),
                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                        )
            ) {
                Text(
                    text =
                        "No parts or filters saved yet.",
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        } else {
            item.partsAndFilters
                .forEachIndexed {
                    index,
                    part ->

                    PartCard(
                        part =
                            part,

                        onEdit = {
                            editingPartId =
                                part.id
                        },

                        onDelete = {
                            onUpdateItem(
                                item.copy(
                                    partsAndFilters =
                                        item.partsAndFilters
                                            .filterNot {
                                                it.id ==
                                                    part.id
                                            }
                                )
                            )
                        }
                    )

                    if (
                        index <
                        item.partsAndFilters
                            .lastIndex
                    ) {
                        Spacer(
                            Modifier.height(
                                12.dp
                            )
                        )
                    }
                }
        }
    }
}

@Composable
private fun ModelResearchCard(
    item: HouseItem,
    researching: Boolean,
    result: ModelMaintenanceResult?,
    error: String?,
    onResearch: () -> Unit,
    onSavePart: (ModelMaintenanceRecommendation) -> Unit,
    onStartSchedule: (ModelMaintenanceRecommendation) -> Unit
) {
    val canResearch =
        item.brand.isNotBlank() &&
            item.modelNumber.isNotBlank()

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .outlineVariant
            )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {
            Text(
                text =
                    "Model intelligence",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Spacer(
                Modifier.height(
                    5.dp
                )
            )

            Text(
                text =
                    if (
                        canResearch
                    ) {
                        "HouseMind can research this exact model for verified filters, consumables and manufacturer maintenance intervals."
                    } else {
                        "Add both brand and model number in Details before HouseMind can research model-specific parts."
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                Modifier.height(
                    16.dp
                )
            )

            Button(
                onClick =
                    onResearch,
                enabled =
                    canResearch &&
                        !researching,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        54.dp
                    ),
                shape =
                    RoundedCornerShape(
                        15.dp
                    )
            ) {
                if (
                    researching
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.height(
                                22.dp
                            ),
                        strokeWidth =
                            2.dp
                    )

                    Spacer(
                        Modifier.width(
                            10.dp
                        )
                    )

                    Text(
                        "Researching model..."
                    )
                } else {
                    Text(
                        if (
                            result == null
                        ) {
                            "Find parts for this model"
                        } else {
                            "Research again"
                        }
                    )
                }
            }

            error
                ?.let {
                    message ->

                    Spacer(
                        Modifier.height(
                            12.dp
                        )
                    )

                    Text(
                        text =
                            message,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }

            if (
                result != null
            ) {
                Spacer(
                    Modifier.height(
                        18.dp
                    )
                )

                Surface(
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    color =
                        if (
                            result.exactModelMatched
                        ) {
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        } else {
                            MaterialTheme
                                .colorScheme
                                .tertiaryContainer
                        }
                ) {
                    Text(
                        text =
                            if (
                                result.exactModelMatched
                            ) {
                                "Exact model evidence found"
                            } else {
                                "Exact model not verified"
                            },
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp,
                                vertical =
                                    8.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge
                    )
                }

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                Text(
                    text =
                        result.summary,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )

                if (
                    result.recommendations
                        .isEmpty()
                ) {
                    Text(
                        text =
                            "HouseMind did not find enough model-specific evidence to safely recommend a part or interval.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                } else {
                    result.recommendations
                        .forEachIndexed {
                            index,
                            recommendation ->

                            RecommendationCard(
                                recommendation =
                                    recommendation,

                                onSavePart = {
                                    onSavePart(
                                        recommendation
                                    )
                                },

                                onStartSchedule = {
                                    onStartSchedule(
                                        recommendation
                                    )
                                }
                            )

                            if (
                                index <
                                result.recommendations
                                    .lastIndex
                            ) {
                                Spacer(
                                    Modifier.height(
                                        12.dp
                                    )
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: ModelMaintenanceRecommendation,
    onSavePart: () -> Unit,
    onStartSchedule: () -> Unit
) {
    val uriHandler =
        LocalUriHandler.current

    val manufacturerVerified =
        recommendation.verification ==
            "manufacturer"

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {
            Text(
                text =
                    recommendation.name,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Spacer(
                Modifier.height(
                    6.dp
                )
            )

            Surface(
                shape =
                    RoundedCornerShape(
                        10.dp
                    ),
                color =
                    if (
                        manufacturerVerified
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .tertiaryContainer
                    }
            ) {
                Text(
                    text =
                        if (
                            manufacturerVerified
                        ) {
                            "Manufacturer verified"
                        } else {
                            "Secondary-source match"
                        },
                    modifier =
                        Modifier.padding(
                            horizontal =
                                10.dp,
                            vertical =
                                6.dp
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )
            }

            if (
                recommendation
                    .partNumber
                    .isNotBlank()
            ) {
                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                Text(
                    text =
                        "Part # ${recommendation.partNumber}",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (
                recommendation.hasInterval
            ) {
                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )

                Text(
                    text =
                        "Recommended interval: every ${recommendation.intervalValue} ${
                            recommendation.intervalUnit
                        }"
                )
            }

            if (
                recommendation.instructions
                    .isNotBlank()
            ) {
                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )

                Text(
                    text =
                        recommendation.instructions,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            if (
                recommendation.sourceTitle
                    .isNotBlank()
            ) {
                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                Text(
                    text =
                        "Source: ${recommendation.sourceTitle}",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )
            }

            if (
                recommendation.sourceUrl
                    .isNotBlank()
            ) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            uriHandler
                                .openUri(
                                    recommendation
                                        .sourceUrl
                                )
                        }
                    }
                ) {
                    Text(
                        "Open source"
                    )
                }
            }

            Spacer(
                Modifier.height(
                    12.dp
                )
            )

            OutlinedButton(
                onClick =
                    onSavePart,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "Save part"
                )
            }

            if (
                manufacturerVerified &&
                recommendation.hasInterval &&
                recommendation.intervalValue > 0
            ) {
                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                Button(
                    onClick =
                        onStartSchedule,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Save & start schedule today"
                    )
                }

                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )

                Text(
                    text =
                        "HouseMind will use today as the schedule starting point. If you know the actual last-change date, you can edit the maintenance schedule later.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

private fun addRecommendationPart(
    item: HouseItem,
    recommendation: ModelMaintenanceRecommendation
): HouseItem {
    val alreadySaved =
        item.partsAndFilters
            .any {
                saved ->

                recommendation
                    .partNumber
                    .isNotBlank() &&
                    saved.partNumber
                        .equals(
                            recommendation
                                .partNumber,
                            ignoreCase =
                                true
                        )
            }

    if (
        alreadySaved
    ) {
        return item
    }

    val newPart =
        ReplacementPart(
            id =
                UUID.randomUUID()
                    .toString(),

            name =
                recommendation.name,

            kind =
                recommendation.kind
                    .takeIf {
                        it in setOf(
                            "Filter",
                            "Part",
                            "Battery",
                            "Other"
                        )
                    }
                    ?: "Other",

            partNumber =
                recommendation
                    .partNumber,

            brand =
                item.brand,

            notes =
                buildString {
                    if (
                        recommendation
                            .instructions
                            .isNotBlank()
                    ) {
                        append(
                            recommendation
                                .instructions
                        )
                    }

                    if (
                        recommendation
                            .sourceTitle
                            .isNotBlank()
                    ) {
                        if (
                            isNotEmpty()
                        ) {
                            append(
                                "\n"
                            )
                        }

                        append(
                            "Source: "
                        )

                        append(
                            recommendation
                                .sourceTitle
                        )
                    }

                    if (
                        recommendation
                            .sourceUrl
                            .isNotBlank()
                    ) {
                        if (
                            isNotEmpty()
                        ) {
                            append(
                                "\n"
                            )
                        }

                        append(
                            recommendation
                                .sourceUrl
                        )
                    }
                }
        )

    return item.copy(
        partsAndFilters =
            item.partsAndFilters +
                newPart
    )
}

private fun addRecommendationPartAndSchedule(
    item: HouseItem,
    recommendation: ModelMaintenanceRecommendation
): HouseItem {
    val withPart =
        addRecommendationPart(
            item =
                item,
            recommendation =
                recommendation
        )

    if (
        !recommendation.hasInterval ||
        recommendation.intervalValue <= 0
    ) {
        return withPart
    }

    val title =
        recommendation
            .maintenanceTitle
            .ifBlank {
                "Replace ${recommendation.name}"
            }

    val existingTask =
        withPart
            .maintenanceTasks
            .any {
                it.title.equals(
                    title,
                    ignoreCase =
                        true
                )
            }

    if (
        existingTask
    ) {
        return withPart
    }

    val task =
        MaintenanceTask(
            id =
                UUID.randomUUID()
                    .toString(),

            title =
                title,

            lastCompletedDate =
                LocalDate.now()
                    .toString(),

            intervalValue =
                recommendation
                    .intervalValue,

            intervalUnit =
                recommendation
                    .intervalUnit,

            reminderEnabled =
                true
        )

    return withPart.copy(
        maintenanceTasks =
            withPart.maintenanceTasks +
                task
    )
}

@Composable
private fun PartCard(
    part: ReplacementPart,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmingDelete by
        rememberSaveable(
            part.id
        ) {
            mutableStateOf(
                false
            )
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme
                    .colorScheme
                    .outlineVariant
            )
    ) {
        Column(
            Modifier.padding(
                18.dp
            )
        ) {
            Text(
                text =
                    part.name,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Spacer(
                Modifier.height(
                    4.dp
                )
            )

            Text(
                text =
                    part.kind,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            if (
                part.partNumber
                    .isNotBlank()
            ) {
                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                Text(
                    text =
                        "Part # ${part.partNumber}",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (
                part.brand
                    .isNotBlank()
            ) {
                Spacer(
                    Modifier.height(
                        4.dp
                    )
                )

                Text(
                    text =
                        "Brand: ${part.brand}"
                )
            }

            if (
                part.notes
                    .isNotBlank()
            ) {
                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                Text(
                    text =
                        part.notes,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                Modifier.height(
                    16.dp
                )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick =
                        onEdit,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {
                    Text(
                        "Edit"
                    )
                }

                Spacer(
                    Modifier.width(
                        8.dp
                    )
                )

                OutlinedButton(
                    onClick = {
                        confirmingDelete =
                            true
                    },
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {
                    Text(
                        "Delete"
                    )
                }
            }

            if (
                confirmingDelete
            ) {
                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                Text(
                    text =
                        "Delete this saved part?",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                Button(
                    onClick =
                        onDelete,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Delete part"
                    )
                }

                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                OutlinedButton(
                    onClick = {
                        confirmingDelete =
                            false
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel"
                    )
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
    var name by
        rememberSaveable(
            existingPart?.id
        ) {
            mutableStateOf(
                existingPart?.name
                    ?: ""
            )
        }

    var kind by
        rememberSaveable(
            existingPart?.id
        ) {
            mutableStateOf(
                existingPart?.kind
                    ?: "Filter"
            )
        }

    var partNumber by
        rememberSaveable(
            existingPart?.id
        ) {
            mutableStateOf(
                existingPart?.partNumber
                    ?: ""
            )
        }

    var brand by
        rememberSaveable(
            existingPart?.id
        ) {
            mutableStateOf(
                existingPart?.brand
                    ?: ""
            )
        }

    var notes by
        rememberSaveable(
            existingPart?.id
        ) {
            mutableStateOf(
                existingPart?.notes
                    ?: ""
            )
        }

    var errorMessage by
        rememberSaveable {
            mutableStateOf<String?>(
                null
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                20.dp
            )
    ) {
        Text(
            text =
                if (
                    existingPart ==
                    null
                ) {
                    "Add Part or Filter"
                } else {
                    "Edit Part or Filter"
                },
            style =
                MaterialTheme
                    .typography
                    .displaySmall
        )

        Spacer(
            Modifier.height(
                20.dp
            )
        )

        OutlinedTextField(
            value =
                name,
            onValueChange = {
                name = it
            },
            label = {
                Text(
                    "Name"
                )
            },
            placeholder = {
                Text(
                    "Water filter"
                )
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine =
                true
        )

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        Text(
            text =
                "Type",
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(
                8.dp
            )
        )

        Row(
            Modifier.fillMaxWidth()
        ) {
            TypeButton(
                label =
                    "Filter",
                selected =
                    kind == "Filter",
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {
                kind =
                    "Filter"
            }

            Spacer(
                Modifier.width(
                    8.dp
                )
            )

            TypeButton(
                label =
                    "Part",
                selected =
                    kind == "Part",
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {
                kind =
                    "Part"
            }
        }

        Spacer(
            Modifier.height(
                8.dp
            )
        )

        Row(
            Modifier.fillMaxWidth()
        ) {
            TypeButton(
                label =
                    "Battery",
                selected =
                    kind == "Battery",
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {
                kind =
                    "Battery"
            }

            Spacer(
                Modifier.width(
                    8.dp
                )
            )

            TypeButton(
                label =
                    "Other",
                selected =
                    kind == "Other",
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {
                kind =
                    "Other"
            }
        }

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        OutlinedTextField(
            value =
                partNumber,
            onValueChange = {
                partNumber = it
            },
            label = {
                Text(
                    "Part number"
                )
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine =
                true
        )

        Spacer(
            Modifier.height(
                12.dp
            )
        )

        OutlinedTextField(
            value =
                brand,
            onValueChange = {
                brand = it
            },
            label = {
                Text(
                    "Brand"
                )
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine =
                true
        )

        Spacer(
            Modifier.height(
                12.dp
            )
        )

        OutlinedTextField(
            value =
                notes,
            onValueChange = {
                notes = it
            },
            label = {
                Text(
                    "Notes"
                )
            },
            modifier =
                Modifier.fillMaxWidth(),
            minLines =
                3
        )

        errorMessage
            ?.let {
                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )

                Text(
                    it
                )
            }

        Spacer(
            Modifier.height(
                20.dp
            )
        )

        Button(
            onClick = {
                if (
                    name.isBlank()
                ) {
                    errorMessage =
                        "Enter a name for this part or filter."
                } else {
                    onSave(
                        ReplacementPart(
                            id =
                                existingPart?.id
                                    ?: UUID.randomUUID()
                                        .toString(),

                            name =
                                name.trim(),

                            kind =
                                kind,

                            partNumber =
                                partNumber.trim(),

                            brand =
                                brand.trim(),

                            notes =
                                notes.trim()
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    56.dp
                ),
            shape =
                RoundedCornerShape(
                    14.dp
                )
        ) {
            Text(
                if (
                    existingPart ==
                    null
                ) {
                    "Save Part"
                } else {
                    "Save Changes"
                }
            )
        }

        Spacer(
            Modifier.height(
                10.dp
            )
        )

        OutlinedButton(
            onClick =
                onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    56.dp
                )
        ) {
            Text(
                "Cancel"
            )
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
    if (
        selected
    ) {
        Button(
            onClick =
                onClick,
            modifier =
                modifier
        ) {
            Text(
                label
            )
        }
    } else {
        OutlinedButton(
            onClick =
                onClick,
            modifier =
                modifier
        ) {
            Text(
                label
            )
        }
    }
}
