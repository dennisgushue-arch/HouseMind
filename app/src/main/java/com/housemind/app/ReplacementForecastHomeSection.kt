package com.housemind.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.housemind.app.logic.ReplacementForecastCalculator
import com.housemind.app.model.HouseItem
import java.time.LocalDate

@Composable
fun ReplacementForecastHomeSection(
    houseItems: List<HouseItem>,
    onItemClick: (HouseItem) -> Unit
) {

    val today =
        LocalDate.now()

    val alerts =
        houseItems
            .mapNotNull { item ->
                ReplacementForecastCalculator
                    .forecast(
                        item = item,
                        today = today
                    )
                    ?.takeIf { forecast ->
                        !forecast.planningStartDate
                            .isAfter(
                                today.plusYears(2)
                            )
                    }
                    ?.let {
                        item to it
                    }
            }
            .sortedBy { (_, forecast) ->
                forecast.planningStartDate
            }

    if (alerts.isEmpty()) {
        return
    }

    Spacer(
        modifier =
            Modifier.height(
                30.dp
            )
    )

    Text(
        text =
            "Replacement planning",
        fontSize =
            22.sp,
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        modifier =
            Modifier.height(
                6.dp
            )
    )

    Text(
        text =
            "Items approaching a useful budgeting or replacement-planning window."
    )

    Spacer(
        modifier =
            Modifier.height(
                12.dp
            )
    )

    alerts
        .take(5)
        .forEachIndexed {
            index,
            pair ->

            val item =
                pair.first

            val forecast =
                pair.second

            val needsPlanningNow =
                !forecast.planningStartDate
                    .isAfter(today)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemClick(
                            item
                        )
                    },
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                if (
                                    needsPlanningNow
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .tertiaryContainer
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                },
                            contentColor =
                                if (
                                    needsPlanningNow
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .onTertiaryContainer
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                }
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
                            item.name,
                        fontSize =
                            19.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            if (
                                needsPlanningNow
                            ) {
                                forecast.statusText
                            } else {
                                "Planning date is approaching"
                            },
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            "Age: ${forecast.ageText}"
                    )

                    Text(
                        text =
                            "Estimated window: ${
                                forecast.earliestReplacementDate.year
                            }-${forecast.latestReplacementDate.year}"
                    )

                    Text(
                        text =
                            "Start planning: ${
                                ReplacementForecastCalculator
                                    .formatDate(
                                        forecast.planningStartDate
                                    )
                            }"
                    )
                }
            }

            if (
                index <
                alerts
                    .take(5)
                    .lastIndex
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )
            }
        }
}
