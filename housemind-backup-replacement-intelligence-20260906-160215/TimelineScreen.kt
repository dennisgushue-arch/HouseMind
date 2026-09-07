package com.housemind.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.housemind.app.data.LocalDocumentStorage
import com.housemind.app.logic.MaintenanceScheduleCalculator
import com.housemind.app.model.HouseItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class TimelineEventKind {
    ScheduledMaintenance,
    MaintenanceHistory,
    Warranty
}

private data class TimelineEvent(
    val date: LocalDate,
    val itemId: String,
    val itemName: String,
    val title: String,
    val detail: String,
    val kind: TimelineEventKind
)

@Composable
fun TimelineScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    houseItems: List<HouseItem>,
    onItemClick: (HouseItem) -> Unit
) {
    val context = LocalContext.current

    val documentStorage =
        remember(context) {
            LocalDocumentStorage(
                context.applicationContext
            )
        }

    val events =
        buildTimelineEvents(
            houseItems = houseItems,
            documentStorage = documentStorage
        )

    val today = LocalDate.now()

    val overdue =
        events.filter {
            it.kind == TimelineEventKind.ScheduledMaintenance &&
                it.date.isBefore(today)
        }
            .sortedByDescending {
                it.date
            }

    val upcoming =
        events.filter {
            (
                it.kind == TimelineEventKind.ScheduledMaintenance ||
                    it.kind == TimelineEventKind.Warranty
                ) &&
                !it.date.isBefore(today)
        }
            .sortedBy {
                it.date
            }

    val history =
        events.filter {
            it.kind == TimelineEventKind.MaintenanceHistory ||
                (
                    it.kind == TimelineEventKind.Warranty &&
                        it.date.isBefore(today)
                    )
        }
            .sortedByDescending {
                it.date
            }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp)
    ) {
        Text(
            text = "Timeline",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "What happened in your home and what is coming next.",
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (
            overdue.isEmpty() &&
            upcoming.isEmpty() &&
            history.isEmpty()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Your timeline is empty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Maintenance schedules, service records, and warranty dates will appear here automatically."
                    )
                }
            }

            return
        }

        if (overdue.isNotEmpty()) {
            TimelineSection(
                title = "Overdue",
                subtitle = "These scheduled tasks are past their due date.",
                events = overdue,
                houseItems = houseItems,
                onItemClick = onItemClick
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )
        }

        if (upcoming.isNotEmpty()) {
            TimelineSection(
                title = "Coming up",
                subtitle = "Maintenance and warranty deadlines ahead.",
                events = upcoming,
                houseItems = houseItems,
                onItemClick = onItemClick
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )
        }

        if (history.isNotEmpty()) {
            TimelineSection(
                title = "History",
                subtitle = "Completed maintenance and past warranty dates.",
                events = history,
                houseItems = houseItems,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun TimelineSection(
    title: String,
    subtitle: String,
    events: List<TimelineEvent>,
    houseItems: List<HouseItem>,
    onItemClick: (HouseItem) -> Unit
) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(4.dp)
    )

    Text(
        text = subtitle
    )

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    events.forEachIndexed { index, event ->
        val item =
            houseItems.firstOrNull {
                it.id == event.itemId
            }

        TimelineEventCard(
            event = event,
            onClick = {
                if (item != null) {
                    onItemClick(item)
                }
            }
        )

        if (index < events.lastIndex) {
            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }
    }
}

@Composable
private fun TimelineEventCard(
    event: TimelineEvent,
    onClick: () -> Unit
) {
    val today = LocalDate.now()

    val containerColor =
        when {
            event.kind == TimelineEventKind.ScheduledMaintenance &&
                event.date.isBefore(today) ->
                MaterialTheme.colorScheme.errorContainer

            event.kind == TimelineEventKind.Warranty &&
                !event.date.isBefore(today) ->
                MaterialTheme.colorScheme.tertiaryContainer

            event.kind == TimelineEventKind.ScheduledMaintenance ->
                MaterialTheme.colorScheme.primaryContainer

            else ->
                MaterialTheme.colorScheme.surfaceVariant
        }

    val contentColor =
        when {
            event.kind == TimelineEventKind.ScheduledMaintenance &&
                event.date.isBefore(today) ->
                MaterialTheme.colorScheme.onErrorContainer

            event.kind == TimelineEventKind.Warranty &&
                !event.date.isBefore(today) ->
                MaterialTheme.colorScheme.onTertiaryContainer

            event.kind == TimelineEventKind.ScheduledMaintenance ->
                MaterialTheme.colorScheme.onPrimaryContainer

            else ->
                MaterialTheme.colorScheme.onSurfaceVariant
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = formatTimelineDate(
                    event.date
                ),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = event.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = event.itemName,
                fontWeight = FontWeight.Medium
            )

            if (event.detail.isNotBlank()) {
                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = event.detail
                )
            }
        }
    }
}

private fun buildTimelineEvents(
    houseItems: List<HouseItem>,
    documentStorage: LocalDocumentStorage
): List<TimelineEvent> {
    val events =
        mutableListOf<TimelineEvent>()

    houseItems.forEach { item ->

        item.maintenanceTasks.forEach { task ->
            val dueDate =
                MaintenanceScheduleCalculator
                    .nextDueDate(task)

            if (dueDate != null) {
                events +=
                    TimelineEvent(
                        date = dueDate,
                        itemId = item.id,
                        itemName = item.name,
                        title = task.title,
                        detail =
                            MaintenanceScheduleCalculator
                                .statusText(task),
                        kind =
                            TimelineEventKind
                                .ScheduledMaintenance
                    )
            }
        }

        item.maintenanceRecords.forEach { record ->
            val recordDate =
                runCatching {
                    LocalDate.parse(
                        record.date
                    )
                }
                    .getOrNull()
                    ?: return@forEach

            val detailParts =
                buildList {
                    if (
                        record.provider.isNotBlank()
                    ) {
                        add(
                            "Provider: ${record.provider}"
                        )
                    }

                    if (
                        record.cost.isNotBlank()
                    ) {
                        add(
                            "Cost: ${record.cost}"
                        )
                    }

                    if (
                        record.notes.isNotBlank()
                    ) {
                        add(
                            record.notes
                        )
                    }
                }

            events +=
                TimelineEvent(
                    date = recordDate,
                    itemId = item.id,
                    itemName = item.name,
                    title =
                        record.serviceType
                            .ifBlank {
                                "Maintenance completed"
                            },
                    detail =
                        detailParts
                            .joinToString(
                                " Â· "
                            ),
                    kind =
                        TimelineEventKind
                            .MaintenanceHistory
                )
        }

        documentStorage
            .load(
                item.id
            )
            .forEach { document ->
                val expiration =
                    document
                        .warrantyExpirationDate
                        ?.let {
                            runCatching {
                                LocalDate.parse(
                                    it
                                )
                            }
                                .getOrNull()
                        }
                        ?: return@forEach

                events +=
                    TimelineEvent(
                        date = expiration,
                        itemId = item.id,
                        itemName = item.name,
                        title =
                            document.title
                                .ifBlank {
                                    "Warranty"
                                },
                        detail =
                            "Warranty expiration",
                        kind =
                            TimelineEventKind
                                .Warranty
                    )
            }
    }

    return events
}

private fun formatTimelineDate(
    date: LocalDate
): String =
    date.format(
        DateTimeFormatter.ofPattern(
            "MMM d, yyyy",
            Locale.US
        )
    )
