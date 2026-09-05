package com.housemind.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.housemind.app.data.LocalDocumentStorage
import com.housemind.app.model.HouseItem
import com.housemind.app.model.SavedDocument
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DocumentsScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val storage = remember {
        LocalDocumentStorage(context.applicationContext)
    }

    var documents by remember(item.id) {
        mutableStateOf(storage.load(item.id))
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedName by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Manual") }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri.toString()
            selectedName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: "Selected document"
            if (title.isBlank()) {
                title = selectedName.substringBeforeLast('.')
            }
            error = null
        }
    }

    if (adding) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Add Document", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(item.name, fontSize = 18.sp)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Owner's manual") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(14.dp))
            Text("Document type", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                DocTypeButton("Manual", type == "Manual", Modifier.weight(1f)) { type = "Manual" }
                Spacer(Modifier.width(8.dp))
                DocTypeButton("Warranty", type == "Warranty", Modifier.weight(1f)) { type = "Warranty" }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                DocTypeButton("Receipt", type == "Receipt", Modifier.weight(1f)) { type = "Receipt" }
                Spacer(Modifier.width(8.dp))
                DocTypeButton("Other", type == "Other", Modifier.weight(1f)) { type = "Other" }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    picker.launch(arrayOf("application/pdf", "image/*", "text/plain"))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (selectedUri == null) "Choose File" else "Choose Different File")
            }

            if (selectedUri != null) {
                Spacer(Modifier.height(8.dp))
                Text("Selected: $selectedName")
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Warranty expiration, purchase location, or useful details") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val uriText = selectedUri
                    when {
                        title.isBlank() -> error = "Enter a title."
                        uriText == null -> error = "Choose a document first."
                        else -> {
                            val saved = storage.importDocument(
                                sourceUri = Uri.parse(uriText),
                                itemId = item.id,
                                title = title,
                                type = type,
                                notes = notes
                            )
                            if (saved == null) {
                                error = "HouseMind couldn't save that document. Try another file."
                            } else {
                                documents = documents + saved
                                storage.save(item.id, documents)
                                selectedUri = null
                                selectedName = ""
                                title = ""
                                type = "Manual"
                                notes = ""
                                error = null
                                adding = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Document")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    selectedUri = null
                    selectedName = ""
                    title = ""
                    notes = ""
                    error = null
                    adding = false
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Cancel")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(20.dp))
        Text("Documents", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(item.name, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Text("Keep manuals, warranties, receipts, and other important files with this item.")
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { adding = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Add Document")
        }

        Spacer(Modifier.height(24.dp))

        if (documents.isEmpty()) {
            Text("No documents saved yet.")
        } else {
            documents.sortedByDescending { it.addedDate }
                .forEachIndexed { index, document ->
                    DocumentCard(
                        document = document,
                        exists = storage.exists(document),
                        onOpen = { openDocument(context, document) },
                        onDelete = {
                            storage.deleteFile(document)
                            documents = documents.filterNot { it.id == document.id }
                            storage.save(item.id, documents)
                        }
                    )
                    if (index < documents.lastIndex) Spacer(Modifier.height(12.dp))
                }
        }
    }
}

@Composable
private fun DocumentCard(
    document: SavedDocument,
    exists: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmingDelete by rememberSaveable(document.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(document.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(document.type)
            Spacer(Modifier.height(4.dp))
            Text(document.fileName)
            Spacer(Modifier.height(4.dp))
            Text("Added ${formatDocumentDate(document.addedDate)}")

            if (document.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(document.notes)
            }

            if (!exists) {
                Spacer(Modifier.height(8.dp))
                Text("File is missing from local storage.")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOpen,
                enabled = exists,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Document")
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
                Text("Delete this saved document?", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Document")
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
private fun DocTypeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

private fun openDocument(
    context: android.content.Context,
    document: SavedDocument
) {
    val file = File(document.localPath)
    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, document.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        context.startActivity(Intent.createChooser(intent, "Open document"))
    }
}

private fun formatDocumentDate(value: String): String =
    runCatching {
        LocalDate.parse(value).format(
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
        )
    }.getOrDefault(value)
