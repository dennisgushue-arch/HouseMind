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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val storage =
        remember {

            LocalDocumentStorage(
                context.applicationContext
            )
        }

    var documents by remember(item.id) {

        mutableStateOf(
            storage.load(
                item.id
            )
        )
    }

    var adding by rememberSaveable {
        mutableStateOf(false)
    }

    var editingDocumentId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedName by rememberSaveable {
        mutableStateOf("")
    }

    var title by rememberSaveable {
        mutableStateOf("")
    }

    var type by rememberSaveable {
        mutableStateOf("Manual")
    }

    var notes by rememberSaveable {
        mutableStateOf("")
    }

    var warrantyExpirationDate by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var showWarrantyDatePicker by rememberSaveable {
        mutableStateOf(false)
    }

    var error by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val editingDocument =
        documents.firstOrNull {
            it.id ==
                editingDocumentId
        }

    fun clearForm() {

        selectedUri = null
        selectedName = ""
        title = ""
        type = "Manual"
        notes = ""
        warrantyExpirationDate = null
        showWarrantyDatePicker = false
        error = null
        editingDocumentId = null
        adding = false
    }

    fun beginEdit(
        document: SavedDocument
    ) {

        selectedUri = null
        selectedName =
            document.fileName

        title =
            document.title

        type =
            document.type

        notes =
            document.notes

        warrantyExpirationDate =
            document.warrantyExpirationDate

        error =
            null

        editingDocumentId =
            document.id

        adding =
            true
    }

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .OpenDocument()
        ) { uri ->

            if (
                uri != null
            ) {

                selectedUri =
                    uri.toString()

                selectedName =
                    uri.lastPathSegment
                        ?.substringAfterLast('/')
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Selected document"

                if (
                    title.isBlank()
                ) {

                    title =
                        selectedName
                            .substringBeforeLast('.')
                }

                error =
                    null
            }
        }

    if (adding) {

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
                text =
                    if (
                        editingDocument == null
                    ) {
                        "Add Document"
                    } else {
                        "Edit Document"
                    },
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = item.name,
                fontSize = 18.sp
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                label = {
                    Text("Title")
                },
                placeholder = {
                    Text(
                        "Owner's manual"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = "Document type",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                DocTypeButton(
                    label = "Manual",
                    selected =
                        type == "Manual",
                    modifier =
                        Modifier.weight(1f)
                ) {
                    type = "Manual"
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                DocTypeButton(
                    label = "Warranty",
                    selected =
                        type == "Warranty",
                    modifier =
                        Modifier.weight(1f)
                ) {
                    type = "Warranty"
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                DocTypeButton(
                    label = "Receipt",
                    selected =
                        type == "Receipt",
                    modifier =
                        Modifier.weight(1f)
                ) {
                    type = "Receipt"
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                DocTypeButton(
                    label = "Other",
                    selected =
                        type == "Other",
                    modifier =
                        Modifier.weight(1f)
                ) {
                    type = "Other"
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            if (
                editingDocument == null
            ) {

                OutlinedButton(
                    onClick = {

                        picker.launch(
                            arrayOf(
                                "application/pdf",
                                "image/*",
                                "text/plain"
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                ) {

                    Text(
                        text =
                            if (
                                selectedUri == null
                            ) {
                                "Choose File"
                            } else {
                                "Choose Different File"
                            }
                    )
                }

                if (
                    selectedUri != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Selected: $selectedName"
                    )
                }

            } else {

                Text(
                    text =
                        "File: ${editingDocument.fileName}"
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Warranty expiration",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = {
                    showWarrantyDatePicker =
                        true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape =
                    RoundedCornerShape(
                        14.dp
                    )
            ) {

                Text(
                    text =
                        warrantyExpirationDate
                            ?.let {
                                formatDocumentDate(
                                    it
                                )
                            }
                            ?: "No expiration date"
                )
            }

            if (
                warrantyExpirationDate != null
            ) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                TextButton(
                    onClick = {
                        warrantyExpirationDate =
                            null
                    }
                ) {

                    Text(
                        text =
                            "Remove expiration date"
                    )
                }
            }

            if (
                showWarrantyDatePicker
            ) {

                val initialMillis =
                    warrantyExpirationDate
                        ?.let {
                            dateToUtcMillis(
                                it
                            )
                        }
                        ?: LocalDate
                            .now()
                            .plusYears(1)
                            .atStartOfDay(
                                ZoneOffset.UTC
                            )
                            .toInstant()
                            .toEpochMilli()

                val datePickerState =
                    rememberDatePickerState(
                        initialSelectedDateMillis =
                            initialMillis
                    )

                DatePickerDialog(
                    onDismissRequest = {
                        showWarrantyDatePicker =
                            false
                    },
                    confirmButton = {

                        TextButton(
                            onClick = {

                                datePickerState
                                    .selectedDateMillis
                                    ?.let { millis ->

                                        warrantyExpirationDate =
                                            utcMillisToDate(
                                                millis
                                            )
                                    }

                                showWarrantyDatePicker =
                                    false
                            }
                        ) {

                            Text("Save")
                        }
                    },
                    dismissButton = {

                        TextButton(
                            onClick = {
                                showWarrantyDatePicker =
                                    false
                            }
                        ) {

                            Text("Cancel")
                        }
                    }
                ) {

                    DatePicker(
                        state =
                            datePickerState
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                label = {
                    Text("Notes")
                },
                placeholder = {
                    Text(
                        "Purchase location, coverage details, or useful notes"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            error?.let {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(it)
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = {

                    when {

                        title.isBlank() -> {

                            error =
                                "Enter a title."
                        }

                        editingDocument == null &&
                            selectedUri == null -> {

                            error =
                                "Choose a document first."
                        }

                        editingDocument != null -> {

                            val updated =
                                editingDocument.copy(
                                    title =
                                        title.trim(),

                                    type =
                                        type,

                                    notes =
                                        notes.trim(),

                                    warrantyExpirationDate =
                                        warrantyExpirationDate
                                )

                            documents =
                                documents.map {

                                    if (
                                        it.id ==
                                        updated.id
                                    ) {
                                        updated
                                    } else {
                                        it
                                    }
                                }

                            storage.save(
                                item.id,
                                documents
                            )

                            clearForm()
                        }

                        else -> {

                            val uriText =
                                selectedUri
                                    ?: return@Button

                            val saved =
                                storage
                                    .importDocument(
                                        sourceUri =
                                            Uri.parse(
                                                uriText
                                            ),

                                        itemId =
                                            item.id,

                                        title =
                                            title,

                                        type =
                                            type,

                                        notes =
                                            notes,

                                        warrantyExpirationDate =
                                            warrantyExpirationDate
                                    )

                            if (
                                saved == null
                            ) {

                                error =
                                    "HouseMind couldn't save that document. Try another file."

                            } else {

                                documents =
                                    documents +
                                        saved

                                storage.save(
                                    item.id,
                                    documents
                                )

                                clearForm()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape =
                    RoundedCornerShape(
                        14.dp
                    )
            ) {

                Text(
                    text =
                        if (
                            editingDocument == null
                        ) {
                            "Save Document"
                        } else {
                            "Save Changes"
                        }
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick = {
                    clearForm()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp)
    ) {

        OutlinedButton(
            onClick = onBack
        ) {

            Text("Back")
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Documents",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = item.name,
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text =
                "Keep manuals, warranties, receipts, and other important files with this item."
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                clearForm()
                adding = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape =
                RoundedCornerShape(
                    14.dp
                )
        ) {

            Text("Add Document")
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (
            documents.isEmpty()
        ) {

            Text(
                text =
                    "No documents saved yet."
            )

        } else {

            documents
                .sortedByDescending {
                    it.addedDate
                }
                .forEachIndexed {
                    index,
                    document ->

                    DocumentCard(
                        document =
                            document,

                        exists =
                            storage.exists(
                                document
                            ),

                        onOpen = {
                            openDocument(
                                context,
                                document
                            )
                        },

                        onEdit = {
                            beginEdit(
                                document
                            )
                        },

                        onDelete = {

                            storage.deleteFile(
                                document
                            )

                            documents =
                                documents.filterNot {
                                    it.id ==
                                        document.id
                                }

                            storage.save(
                                item.id,
                                documents
                            )
                        }
                    )

                    if (
                        index <
                        documents.lastIndex
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )
                    }
                }
        }
    }
}

@Composable
private fun DocumentCard(
    document: SavedDocument,
    exists: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    var confirmingDelete by rememberSaveable(
        document.id
    ) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            )
    ) {

        Column(
            modifier = Modifier.padding(
                18.dp
            )
        ) {

            Text(
                text =
                    document.title,
                fontSize = 19.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    document.type
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    document.fileName
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "Added ${
                        formatDocumentDate(
                            document.addedDate
                        )
                    }"
            )

            document
                .warrantyExpirationDate
                ?.let { expiration ->

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            warrantyStatusText(
                                expiration
                            ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

            if (
                document.notes.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        document.notes
                )
            }

            if (!exists) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "File is missing from local storage."
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Button(
                onClick = onOpen,
                enabled = exists,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text =
                        "Open Document"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = onEdit,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Edit")
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            if (!confirmingDelete) {

                OutlinedButton(
                    onClick = {

                        confirmingDelete =
                            true
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("Delete")
                }

            } else {

                Text(
                    text =
                        "Delete this saved document?",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Button(
                    onClick = onDelete,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            "Delete Document"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedButton(
                    onClick = {

                        confirmingDelete =
                            false
                    },
                    modifier =
                        Modifier.fillMaxWidth()
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

private fun openDocument(
    context: android.content.Context,
    document: SavedDocument
) {

    val file =
        File(
            document.localPath
        )

    if (!file.exists()) {

        return
    }

    val uri =
        FileProvider
            .getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

    val intent =
        Intent(
            Intent.ACTION_VIEW
        ).apply {

            setDataAndType(
                uri,
                document.mimeType
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    runCatching {

        context.startActivity(
            Intent.createChooser(
                intent,
                "Open document"
            )
        )
    }
}

private fun warrantyStatusText(
    value: String,
    today: LocalDate = LocalDate.now()
): String {

    val expiration =
        runCatching {
            LocalDate.parse(value)
        }
            .getOrNull()
            ?: return "Warranty expires $value"

    val days =
        ChronoUnit.DAYS
            .between(
                today,
                expiration
            )

    return when {

        days < 0 ->
            "Warranty expired ${
                formatDocumentDate(
                    value
                )
            }"

        days == 0L ->
            "Warranty expires today"

        days <= 30 ->
            "Warranty expires in $days ${
                if (days == 1L) {
                    "day"
                } else {
                    "days"
                }
            }"

        else ->
            "Warranty expires ${
                formatDocumentDate(
                    value
                )
            }"
    }
}

private fun formatDocumentDate(
    value: String
): String =

    runCatching {

        LocalDate
            .parse(value)
            .format(
                DateTimeFormatter
                    .ofPattern(
                        "MMM d, yyyy",
                        Locale.US
                    )
            )
    }
        .getOrDefault(
            value
        )

private fun dateToUtcMillis(
    value: String
): Long? =

    runCatching {

        LocalDate
            .parse(value)
            .atStartOfDay(
                ZoneOffset.UTC
            )
            .toInstant()
            .toEpochMilli()
    }
        .getOrNull()

private fun utcMillisToDate(
    millis: Long
): String =

    Instant
        .ofEpochMilli(
            millis
        )
        .atZone(
            ZoneOffset.UTC
        )
        .toLocalDate()
        .toString()
