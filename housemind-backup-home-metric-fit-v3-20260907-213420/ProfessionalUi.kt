package com.housemind.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.housemind.app.data.LocalDocumentStorage
import com.housemind.app.logic.MaintenanceScheduleCalculator
import com.housemind.app.logic.ReplacementForecastCalculator
import com.housemind.app.logic.WarrantyIntelligence
import com.housemind.app.logic.WarrantyState
import com.housemind.app.model.HouseItem
import java.io.File
import java.time.LocalDate

private enum class ProStatus {
    Critical,
    Attention,
    Planning,
    Healthy,
    Unscheduled
}

private data class ProItemState(
    val status: ProStatus,
    val label: String,
    val detail: String
)

@Composable
fun ProfessionalHomeScreen(
    contentPadding: PaddingValues,
    houseItems: List<HouseItem>,
    onScanSomething: () -> Unit,
    onItemClick: (HouseItem) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val documentStorage = remember(context) {
        LocalDocumentStorage(context.applicationContext)
    }

    val states =
        houseItems.associate { item ->
            item.id to professionalState(
                item = item,
                documentStorage = documentStorage
            )
        }

    val attentionCount =
        states.values.count {
            it.status == ProStatus.Critical || it.status == ProStatus.Attention
        }

    val planningCount =
        states.values.count {
            it.status == ProStatus.Planning
        }

    val healthyCount =
        states.values.count {
            it.status == ProStatus.Healthy
        }

    val sortedItems =
        houseItems.sortedWith(
            compareBy<HouseItem> {
                when (states[it.id]?.status ?: ProStatus.Unscheduled) {
                    ProStatus.Critical -> 0
                    ProStatus.Attention -> 1
                    ProStatus.Planning -> 2
                    ProStatus.Healthy -> 3
                    ProStatus.Unscheduled -> 4
                }
            }.thenBy { it.name }
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "HouseMind",
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Your home, organized and ahead of schedule.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = when (attentionCount) {
                        0 -> "Everything important is under control"
                        1 -> "1 item needs your attention"
                        else -> "$attentionCount items need your attention"
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        value = attentionCount.toString(),
                        label = "Attention"
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        value = planningCount.toString(),
                        label = "Planning"
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        value = healthyCount.toString(),
                        label = "On track"
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onScanSomething,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text("Add or scan a home item")
        }

        Spacer(Modifier.height(28.dp))

        SectionHeader(
            title = if (attentionCount > 0) "Priority" else "Your home",
            subtitle = if (attentionCount > 0) {
                "The items that deserve a look first."
            } else {
                "A clear view of every tracked appliance and system."
            }
        )

        Spacer(Modifier.height(14.dp))

        if (sortedItems.isEmpty()) {
            EmptyHomeCard(
                onScanSomething = onScanSomething
            )
        } else {
            sortedItems.forEachIndexed { index, item ->
                ProfessionalHomeItemCard(
                    item = item,
                    state = states[item.id] ?: ProItemState(
                        status = ProStatus.Unscheduled,
                        label = "Set up",
                        detail = "Add maintenance or model details to unlock smarter tracking."
                    ),
                    onClick = { onItemClick(item) }
                )

                if (index < sortedItems.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SummaryMetric(
    modifier: Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.96f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
@Composable
private fun EmptyHomeCard(
    onScanSomething: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = "Start building your home record",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Add an appliance or home system and HouseMind will organize maintenance, parts, documents, warranties, and replacement planning around it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onScanSomething
            ) {
                Text("Add first item")
            }
        }
    }
}

@Composable
private fun ProfessionalHomeItemCard(
    item: HouseItem,
    state: ProItemState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfessionalThumbnail(
                photoPath = item.photoPath
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusPill(state = state)
                }

                if (item.brand.isNotBlank() || item.modelNumber.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = listOf(item.brand, item.modelNumber)
                            .filter { it.isNotBlank() }
                            .joinToString("  â€¢  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(9.dp))

                Text(
                    text = state.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusPill(
    state: ProItemState
) {
    val background =
        when (state.status) {
            ProStatus.Critical -> MaterialTheme.colorScheme.errorContainer
            ProStatus.Attention,
            ProStatus.Planning -> MaterialTheme.colorScheme.tertiaryContainer
            ProStatus.Healthy -> MaterialTheme.colorScheme.primaryContainer
            ProStatus.Unscheduled -> MaterialTheme.colorScheme.surfaceVariant
        }

    val foreground =
        when (state.status) {
            ProStatus.Critical -> MaterialTheme.colorScheme.onErrorContainer
            ProStatus.Attention,
            ProStatus.Planning -> MaterialTheme.colorScheme.onTertiaryContainer
            ProStatus.Healthy -> MaterialTheme.colorScheme.onPrimaryContainer
            ProStatus.Unscheduled -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        shape = CircleShape,
        color = background,
        contentColor = foreground
    ) {
        Text(
            text = state.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ProfessionalThumbnail(
    photoPath: String?
) {
    val bitmap =
        remember(photoPath) {
            photoPath
                ?.takeIf { File(it).exists() }
                ?.let { BitmapFactory.decodeFile(it) }
        }

    Surface(
        modifier = Modifier.size(74.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun professionalState(
    item: HouseItem,
    documentStorage: LocalDocumentStorage,
    today: LocalDate = LocalDate.now()
): ProItemState {

    val overdueTask =
        item.maintenanceTasks.firstOrNull {
            MaintenanceScheduleCalculator.isOverdue(
                task = it,
                today = today
            )
        }

    if (overdueTask != null) {
        return ProItemState(
            status = ProStatus.Critical,
            label = "Overdue",
            detail = "${overdueTask.title} â€¢ ${MaintenanceScheduleCalculator.statusText(overdueTask)}"
        )
    }

    val dueSoonTask =
        item.maintenanceTasks.firstOrNull {
            MaintenanceScheduleCalculator.isDueSoon(
                task = it,
                today = today,
                withinDays = 30
            )
        }

    if (dueSoonTask != null) {
        return ProItemState(
            status = ProStatus.Attention,
            label = "Due soon",
            detail = "${dueSoonTask.title} â€¢ ${MaintenanceScheduleCalculator.formattedDueDate(dueSoonTask)}"
        )
    }

    val warranty =
        WarrantyIntelligence.snapshot(
            documentStorage.load(item.id)
        )

    if (
        warranty.state == WarrantyState.ExpiringSoon ||
        warranty.state == WarrantyState.Expired
    ) {
        return ProItemState(
            status = ProStatus.Attention,
            label = "Warranty",
            detail = warranty.text ?: "Warranty needs attention."
        )
    }

    val forecast =
        ReplacementForecastCalculator.forecast(
            item = item,
            today = today
        )

    if (forecast != null && !forecast.planningStartDate.isAfter(today)) {
        return ProItemState(
            status = ProStatus.Planning,
            label = "Planning",
            detail = "${forecast.statusText} â€¢ ${forecast.earliestReplacementDate.year}-${forecast.latestReplacementDate.year}"
        )
    }

    if (item.maintenanceTasks.isNotEmpty()) {
        val nextTask =
            item.maintenanceTasks
                .mapNotNull { task ->
                    MaintenanceScheduleCalculator.nextDueDate(task)?.let { task to it }
                }
                .minByOrNull { it.second }
                ?.first

        if (nextTask != null) {
            return ProItemState(
                status = ProStatus.Healthy,
                label = "On track",
                detail = "${nextTask.title} â€¢ ${MaintenanceScheduleCalculator.formattedDueDate(nextTask)}"
            )
        }
    }

    return ProItemState(
        status = ProStatus.Unscheduled,
        label = "Set up",
        detail = "Add maintenance or model details to unlock smarter tracking."
    )
}

@Composable
fun ProfessionalItemOverview(
    contentPadding: PaddingValues,
    item: HouseItem,
    onBack: () -> Unit,
    onMaintenance: () -> Unit,
    onParts: () -> Unit,
    onDocuments: () -> Unit,
    onReplacementForecast: () -> Unit,
    onDetails: () -> Unit,
    onEdit: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val documentStorage = remember(context) {
        LocalDocumentStorage(context.applicationContext)
    }

    val state =
        professionalState(
            item = item,
            documentStorage = documentStorage
        )

    val warranty =
        WarrantyIntelligence.snapshot(
            documentStorage.load(item.id)
        )

    val forecast =
        ReplacementForecastCalculator.forecast(item)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Back")
        }

        Spacer(Modifier.height(18.dp))

        val itemPhotoPath = item.photoPath

        if (itemPhotoPath != null && File(itemPhotoPath).exists()) {
            val bitmap = remember(itemPhotoPath) {
                BitmapFactory.decodeFile(itemPhotoPath)
            }

            if (bitmap != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.75f),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }

        Text(
            text = item.name,
            style = MaterialTheme.typography.displaySmall
        )

        if (item.brand.isNotBlank() || item.modelNumber.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(item.brand, item.modelNumber)
                    .filter { it.isNotBlank() }
                    .joinToString("  â€¢  "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Home status",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    StatusPill(state = state)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = state.detail,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniInfoCard(
                modifier = Modifier.weight(1f),
                label = "Warranty",
                value = warranty.text ?: "Not tracked"
            )

            MiniInfoCard(
                modifier = Modifier.weight(1f),
                label = "Replacement",
                value = forecast?.let {
                    "${it.earliestReplacementDate.year}-${it.latestReplacementDate.year}"
                } ?: "Not forecast"
            )
        }

        Spacer(Modifier.height(28.dp))

        SectionHeader(
            title = "Manage this item",
            subtitle = "Everything important for this appliance or home system."
        )

        Spacer(Modifier.height(12.dp))

        ProfessionalActionRow(
            icon = Icons.Outlined.Build,
            title = "Maintenance",
            subtitle = "Schedules, reminders and service history",
            onClick = onMaintenance
        )

        Spacer(Modifier.height(10.dp))

        ProfessionalActionRow(
            icon = Icons.Outlined.Search,
            title = "Parts & filters",
            subtitle = "Replacement parts and model-specific filters",
            onClick = onParts
        )

        Spacer(Modifier.height(10.dp))

        ProfessionalActionRow(
            icon = Icons.Outlined.Description,
            title = "Documents",
            subtitle = "Manuals, warranties and receipts",
            onClick = onDocuments
        )

        Spacer(Modifier.height(10.dp))

        ProfessionalActionRow(
            icon = Icons.Outlined.DateRange,
            title = "Replacement forecast",
            subtitle = "Age, lifespan and planning window",
            onClick = onReplacementForecast
        )

        Spacer(Modifier.height(10.dp))

        ProfessionalActionRow(
            icon = Icons.Outlined.Info,
            title = "Details",
            subtitle = "Brand, model, serial and location",
            onClick = onDetails
        )

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text("Edit item")
        }
    }
}

@Composable
private fun MiniInfoCard(
    modifier: Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfessionalActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfessionalBottomNavIcon(
    label: String
) {
    val icon =
        when (label) {
            "Home" -> Icons.Outlined.Home
            "Scan" -> Icons.Outlined.Add
            "Ask" -> Icons.Outlined.ChatBubbleOutline
            "Timeline" -> Icons.Outlined.DateRange
            else -> Icons.Outlined.Home
        }

    Icon(
        imageVector = icon,
        contentDescription = label
    )
}


