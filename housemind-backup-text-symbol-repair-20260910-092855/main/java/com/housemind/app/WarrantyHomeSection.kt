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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.housemind.app.data.LocalDocumentStorage
import com.housemind.app.logic.WarrantyIntelligence
import com.housemind.app.model.HouseItem

@Composable
fun WarrantyHomeSection(
    houseItems: List<HouseItem>,
    onItemClick: (HouseItem) -> Unit
) {
    val context = LocalContext.current
    val storage = remember {
        LocalDocumentStorage(context.applicationContext)
    }

    val alerts = houseItems.mapNotNull { item ->
        val snapshot = WarrantyIntelligence.snapshot(
            storage.load(item.id)
        )

        if (WarrantyIntelligence.needsAttention(storage.load(item.id))) {
            item to snapshot
        } else {
            null
        }
    }

    if (alerts.isEmpty()) return

    Spacer(Modifier.height(30.dp))

    Text(
        text = "Warranty attention",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = "Deadlines worth knowing before coverage ends."
    )

    Spacer(Modifier.height(12.dp))

    alerts.forEachIndexed { index, (item, snapshot) ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(item) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = item.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = snapshot.text ?: "Warranty needs attention",
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text("Open Documents to review the warranty.")
            }
        }

        if (index < alerts.lastIndex) {
            Spacer(Modifier.height(12.dp))
        }
    }
}
