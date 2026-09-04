package com.housemind.app

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.housemind.app.data.LocalHouseItemStorage
import com.housemind.app.data.LocalImageStorage
import com.housemind.app.data.CameraPhoto
import com.housemind.app.data.CameraPhotoManager
import com.housemind.app.logic.HouseMindQueryEngine
import com.housemind.app.logic.MaintenanceScheduleCalculator
import com.housemind.app.model.HouseItem
import com.housemind.app.model.MaintenanceRecord
import com.housemind.app.model.MaintenanceTask
import com.housemind.app.model.RecognitionResult
import com.housemind.app.recognition.HouseMindConfig
import com.housemind.app.recognition.MockRecognitionService
import com.housemind.app.recognition.RemoteRecognitionService
import com.housemind.app.ui.theme.HouseMindTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            HouseMindTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    HouseMindApp()
                }
            }
        }
    }
}

@Composable
fun HouseMindApp() {

    var currentTab by rememberSaveable { mutableStateOf(HomeMindTab.Home) }
    var selectedImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var scanState by rememberSaveable { mutableStateOf(ScanState.ChoosingPhoto) }
    var recognitionResult by remember { mutableStateOf<RecognitionResult?>(null) }
    var recognitionError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val storage = remember { LocalHouseItemStorage(context.applicationContext) }
    val imageStorage = remember { LocalImageStorage(context.applicationContext) }
    val recognitionService = remember {
        if (HouseMindConfig.API_BASE_URL.isBlank()) MockRecognitionService()
        else RemoteRecognitionService(context.applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()
    val cameraPhotoManager = remember { CameraPhotoManager(context.applicationContext) }
    var temporaryCameraPhoto by remember { mutableStateOf<CameraPhoto?>(null) }
    var cameraError by rememberSaveable { mutableStateOf<String?>(null) }
    val houseItems = remember {
        mutableStateListOf<HouseItem>().apply { addAll(storage.loadOrSeed()) }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        cameraPhotoManager.deleteTemporaryPhoto(temporaryCameraPhoto)
        temporaryCameraPhoto = null
        selectedImageUri = uri?.toString()
        recognitionResult = null
        recognitionError = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { wasPhotoTaken ->
        val cameraPhoto = temporaryCameraPhoto
        if (wasPhotoTaken && cameraPhoto != null && cameraPhoto.file.length() > 0) {
            selectedImageUri = cameraPhoto.uri.toString()
            cameraError = null
            recognitionResult = null
            recognitionError = null
        } else {
            cameraPhotoManager.deleteTemporaryPhoto(cameraPhoto)
            temporaryCameraPhoto = null
            if (cameraPhoto != null) cameraError = "Couldn't take that photo. Please try again."
        }
    }

    Scaffold(bottomBar = {
        NavigationBar {
            HomeMindTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentTab == tab,
                    onClick = {
                        currentTab = tab
                        if (tab == HomeMindTab.Home) selectedItemId = null
                    },
                    icon = { Text(tab.icon) },
                    label = { Text(tab.label) }
                )
            }
        }
    }) { innerPadding ->
        when (currentTab) {
            HomeMindTab.Home -> {
                val selectedItem = houseItems.firstOrNull { it.id == selectedItemId }
                if (selectedItem == null) {
                    HomeScreen(
                        contentPadding = innerPadding,
                        houseItems = houseItems,
                        onScanSomething = { currentTab = HomeMindTab.Scan },
                        onItemClick = { selectedItemId = it.id }
                    )
                } else {
                    ItemDetailScreen(
                        contentPadding = innerPadding,
                        item = selectedItem,
                        onBack = { selectedItemId = null },
                        onUpdateItem = { updatedItem ->
                            val itemIndex = houseItems.indexOfFirst { it.id == updatedItem.id }
                            if (itemIndex >= 0) {
                                houseItems[itemIndex] = updatedItem
                                storage.save(houseItems)
                            }
                        }
                    )
                }
            }
            HomeMindTab.Scan -> ScanScreen(
                contentPadding = innerPadding,
                selectedImageUri = selectedImageUri,
                scanState = scanState,
                onChoosePhoto = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onTakePhoto = {
                    cameraError = null
                    cameraPhotoManager.deleteTemporaryPhoto(temporaryCameraPhoto)
                    val cameraPhoto = cameraPhotoManager.createTemporaryPhoto()
                    if (cameraPhoto == null) {
                        cameraError = "Couldn't take that photo. Please try again."
                    } else {
                        temporaryCameraPhoto = cameraPhoto
                        runCatching { cameraLauncher.launch(cameraPhoto.uri) }
                            .onFailure {
                                cameraPhotoManager.deleteTemporaryPhoto(cameraPhoto)
                                temporaryCameraPhoto = null
                                cameraError = "Couldn't take that photo. Please try again."
                            }
                    }
                },
                onRemovePhoto = {
                    cameraPhotoManager.deleteTemporaryPhoto(temporaryCameraPhoto)
                    temporaryCameraPhoto = null
                    selectedImageUri = null
                    recognitionResult = null
                    recognitionError = null
                },
                cameraError = cameraError,
                recognitionError = recognitionError,
                recognitionResult = recognitionResult,
                onAnalyzeItem = {
                    val imageUri = selectedImageUri ?: return@ScanScreen
                    recognitionError = null
                    scanState = ScanState.Analyzing
                    coroutineScope.launch {
                        runCatching { recognitionService.analyze(Uri.parse(imageUri)) }
                            .onSuccess {
                                recognitionResult = it
                                scanState = ScanState.Result
                            }
                            .onFailure {
                                recognitionError = "HouseMind couldn't analyze that photo. Please try again."
                                scanState = ScanState.ChoosingPhoto
                            }
                    }
                },
                onBackToScan = {
                    recognitionResult = null
                    scanState = ScanState.ChoosingPhoto
                },
                onAddToHome = { item ->
                    val itemWithPhoto = selectedImageUri?.let { uri ->
                        item.copy(photoPath = imageStorage.saveImage(Uri.parse(uri), item.id))
                    } ?: item
                    houseItems.add(itemWithPhoto)
                    storage.save(houseItems)
                    cameraPhotoManager.deleteTemporaryPhoto(temporaryCameraPhoto)
                    temporaryCameraPhoto = null
                    selectedImageUri = null
                    scanState = ScanState.ChoosingPhoto
                    currentTab = HomeMindTab.Home
                }
            )
            HomeMindTab.Ask -> AskScreen(innerPadding, houseItems)
        }
    }
}

private enum class HomeMindTab(val label: String, val icon: String) {
    Home("Home", "⌂"),
    Scan("Scan", "+"),
    Ask("Ask", "?")
}

private enum class ScanState {
    ChoosingPhoto,
    Analyzing,
    Result
}

@Composable
private fun HomeScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    houseItems: List<HouseItem>,
    onScanSomething: () -> Unit,
    onItemClick: (HouseItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("HouseMind", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Everything about your home, remembered.", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onScanSomething,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Scan Something") }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Your Home", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        houseItems.forEachIndexed { index, item ->
            HouseItemCard(item.name, item.brand, maintenanceStatusFor(item), item.photoPath) { onItemClick(item) }
            if (index < houseItems.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private enum class ItemDetailSection {
    Overview,
    Maintenance,
    PartsAndFilters,
    Documents,
    Details,
    Edit
}

@Composable
private fun ItemDetailScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit,
    onUpdateItem: (HouseItem) -> Unit
) {
    var section by rememberSaveable { mutableStateOf(ItemDetailSection.Overview) }

    when (section) {
        ItemDetailSection.Overview -> ItemDetailOverview(
            contentPadding = contentPadding,
            item = item,
            onBack = onBack,
            onEdit = { section = ItemDetailSection.Edit },
            onSectionSelected = { section = it }
        )
        ItemDetailSection.Details -> ItemDetailsContent(
            contentPadding = contentPadding,
            item = item,
            onBack = { section = ItemDetailSection.Overview }
        )
        ItemDetailSection.Edit -> EditItemScreen(
            contentPadding = contentPadding,
            item = item,
            onCancel = { section = ItemDetailSection.Overview },
            onSave = {
                onUpdateItem(it)
                section = ItemDetailSection.Overview
            }
        )
        ItemDetailSection.Maintenance -> MaintenanceScreen(
            contentPadding = contentPadding,
            item = item,
            onBack = {
                section = ItemDetailSection.Overview
            },
            onSaveRecord = { record ->
                onUpdateItem(
                    item.copy(
                        maintenanceRecords =
                            item.maintenanceRecords + record
                    )
                )
            },
            onAddTask = { task ->
                onUpdateItem(
                    item.copy(
                        maintenanceTasks =
                            item.maintenanceTasks + task
                    )
                )
            },
            onMarkTaskDone = { updatedTask ->

                val updatedTasks =
                    item.maintenanceTasks.map { task ->

                        if (task.id == updatedTask.id) {
                            updatedTask
                        } else {
                            task
                        }
                    }

                val completionRecord =
                    MaintenanceRecord(
                        id = UUID.randomUUID().toString(),
                        date = LocalDate.now().toString(),
                        serviceType = updatedTask.title,
                        provider = "",
                        cost = "",
                        notes = "Completed from maintenance schedule"
                    )

                onUpdateItem(
                    item.copy(
                        maintenanceTasks = updatedTasks,
                        maintenanceRecords =
                            item.maintenanceRecords + completionRecord
                    )
                )
            }
        )
        ItemDetailSection.PartsAndFilters -> PlaceholderSection(
            contentPadding, "Parts & Filters", "No parts or filters saved yet."
        ) { section = ItemDetailSection.Overview }
        ItemDetailSection.Documents -> PlaceholderSection(
            contentPadding, "Documents", "No documents saved yet."
        ) { section = ItemDetailSection.Overview }
    }
}

@Composable
private fun ItemDetailOverview(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSectionSelected: (ItemDetailSection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(modifier = Modifier.height(20.dp))
        item.photoPath?.let { photoPath ->
            StoredImagePreview(
                photoPath = photoPath,
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(item.name, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(item.brand, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(maintenanceStatusFor(item), modifier = Modifier.padding(18.dp), fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Last serviced", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.maintenanceRecords.maxByOrNull { it.date }?.let { formatMaintenanceDate(it.date) } ?: "No service history yet")
        Spacer(modifier = Modifier.height(28.dp))
        Text("Next up", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // Uses the saved maintenance schedule to calculate the next task.
        Text(nextActionFor(item))
        Spacer(modifier = Modifier.height(28.dp))
        ItemSectionRow("Maintenance") { onSectionSelected(ItemDetailSection.Maintenance) }
        Spacer(modifier = Modifier.height(10.dp))
        ItemSectionRow("Parts & Filters") { onSectionSelected(ItemDetailSection.PartsAndFilters) }
        Spacer(modifier = Modifier.height(10.dp))
        ItemSectionRow("Documents") { onSectionSelected(ItemDetailSection.Documents) }
        Spacer(modifier = Modifier.height(10.dp))
        ItemSectionRow("Details") { onSectionSelected(ItemDetailSection.Details) }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Edit")
        }
    }
}

private fun nextMaintenanceTask(item: HouseItem) =
    item.maintenanceTasks
        .mapNotNull { task ->
            MaintenanceScheduleCalculator
                .nextDueDate(task)
                ?.let { dueDate -> task to dueDate }
        }
        .minByOrNull { (_, dueDate) -> dueDate }
        ?.first

private fun maintenanceStatusFor(item: HouseItem): String {

    val nextTask = nextMaintenanceTask(item)
        ?: return "No maintenance scheduled yet"

    return "${nextTask.title}: ${
        MaintenanceScheduleCalculator.statusText(nextTask)
    }"
}

private fun nextActionFor(item: HouseItem): String {

    val nextTask = nextMaintenanceTask(item)
        ?: return "No maintenance scheduled yet"

    return "${nextTask.title} — ${
        MaintenanceScheduleCalculator.statusText(nextTask)
    } · Next due ${
        MaintenanceScheduleCalculator.formattedDueDate(nextTask)
    }"
}

@Composable
private fun MaintenanceScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit,
    onSaveRecord: (MaintenanceRecord) -> Unit,
    onAddTask: (MaintenanceTask) -> Unit,
    onMarkTaskDone: (MaintenanceTask) -> Unit
) {

    var addingRecord by rememberSaveable {
        mutableStateOf(false)
    }

    if (addingRecord) {

        MaintenanceForm(
            contentPadding = contentPadding,
            itemName = item.name,
            onCancel = {
                addingRecord = false
            },
            onSave = {

                onSaveRecord(it)

                addingRecord = false
            }
        )

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
            text = "Maintenance",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = item.name,
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        MaintenanceTaskSection(
            tasks = item.maintenanceTasks,
            onAddTask = onAddTask,
            onMarkDone = onMarkTaskDone
        )

        Spacer(
            modifier = Modifier.height(36.dp)
        )

        Text(
            text = "Maintenance History",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Keep a record of repairs and service."
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedButton(
            onClick = {
                addingRecord = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text = "Add Maintenance Record"
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (item.maintenanceRecords.isEmpty()) {

            Text(
                text = "No maintenance history yet."
            )

        } else {

            item.maintenanceRecords
                .sortedByDescending {
                    it.date
                }
                .forEachIndexed { index, record ->

                    MaintenanceRecordCard(
                        record
                    )

                    if (
                        index <
                        item.maintenanceRecords.lastIndex
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
private fun MaintenanceForm(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    itemName: String,
    onCancel: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit
) {
    var serviceType by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var provider by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Add Maintenance", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(itemName, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(20.dp))
        RecognitionField("Service Type", serviceType) { serviceType = it }
        RecognitionField("Date", date) { date = it }
        RecognitionField("Provider / Company", provider) { provider = it }
        RecognitionField("Cost", cost) { cost = it }
        RecognitionField("Notes", notes, minLines = 3) { notes = it }
        Button(
            onClick = {
                onSave(MaintenanceRecord(UUID.randomUUID().toString(), date, serviceType, provider, cost, notes))
            },
            enabled = serviceType.isNotBlank() && date.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Save Maintenance") }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Cancel")
        }
    }
}

@Composable
private fun MaintenanceRecordCard(record: MaintenanceRecord) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(record.serviceType, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatMaintenanceDate(record.date))
            if (record.provider.isNotBlank()) Text(record.provider)
            if (record.cost.isNotBlank()) Text(record.cost)
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(record.notes)
            }
        }
    }
}

private fun formatMaintenanceDate(date: String): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
}.getOrDefault(date)

@Composable
private fun ItemSectionRow(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = "$label  >",
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            fontSize = 18.sp
        )
    }
}

@Composable
private fun ItemDetailsContent(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Details", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        DetailValue("Category", item.category)
        DetailValue("Brand", item.brand)
        DetailValue("Model", item.modelNumber)
        DetailValue("Serial", item.serialNumber)
        DetailValue("Location", item.location)
        DetailValue("Filter", item.filterPartNumber)
        DetailValue("Notes", item.notes)
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    if (value.isNotBlank()) {
        Text(label, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value)
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PlaceholderSection(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(20.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(modifier = Modifier.height(20.dp))
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(message)
    }
}

@Composable
private fun EditItemScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    item: HouseItem,
    onCancel: () -> Unit,
    onSave: (HouseItem) -> Unit
) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var category by rememberSaveable(item.id) { mutableStateOf(item.category) }
    var brand by rememberSaveable(item.id) { mutableStateOf(item.brand) }
    var modelNumber by rememberSaveable(item.id) { mutableStateOf(item.modelNumber) }
    var serialNumber by rememberSaveable(item.id) { mutableStateOf(item.serialNumber) }
    var location by rememberSaveable(item.id) { mutableStateOf(item.location) }
    var filterPartNumber by rememberSaveable(item.id) { mutableStateOf(item.filterPartNumber) }
    var notes by rememberSaveable(item.id) { mutableStateOf(item.notes) }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Edit item", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        RecognitionField("Item Name", name) { name = it }
        RecognitionField("Category", category) { category = it }
        RecognitionField("Brand", brand) { brand = it }
        RecognitionField("Model Number", modelNumber) { modelNumber = it }
        RecognitionField("Serial Number", serialNumber) { serialNumber = it }
        RecognitionField("Room / Location", location) { location = it }
        RecognitionField("Filter / Part Number", filterPartNumber) { filterPartNumber = it }
        RecognitionField("Notes", notes, minLines = 3) { notes = it }
        Button(
            onClick = {
                onSave(item.copy(
                    name = name, category = category, brand = brand, modelNumber = modelNumber,
                    serialNumber = serialNumber, location = location, filterPartNumber = filterPartNumber,
                    notes = notes
                ))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Save Changes") }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Cancel")
        }
    }
}

@Composable
private fun ScanScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    selectedImageUri: String?,
    scanState: ScanState,
    onChoosePhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    cameraError: String?,
    recognitionError: String?,
    onAnalyzeItem: () -> Unit,
    onBackToScan: () -> Unit,
    onAddToHome: (HouseItem) -> Unit,
    recognitionResult: RecognitionResult? = null
) {
    if (scanState == ScanState.Analyzing) {
        AnalyzingScreen(contentPadding)
        return
    }

    if (scanState == ScanState.Result && selectedImageUri != null && recognitionResult != null) {
        RecognitionResultScreen(
            contentPadding = contentPadding,
            imageUri = selectedImageUri,
            recognitionResult = recognitionResult,
            onBack = onBackToScan,
            onAddToHome = onAddToHome
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Scan", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Point HouseMind at something in your home.", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp)
        ) { Text("Take Photo") }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onChoosePhoto,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp)
        ) { Text("Choose From Photos") }
        cameraError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(error)
        }
        recognitionError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(error)
        }

        selectedImageUri?.let { uri ->
            Spacer(modifier = Modifier.height(28.dp))
            SelectedImagePreview(uriString = uri)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onRemovePhoto, modifier = Modifier.fillMaxWidth()) {
                Text("Remove Photo")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAnalyzeItem, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Analyze Item")
            }
        }
    }
}

@Composable
private fun AnalyzingScreen(contentPadding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(20.dp)
    ) {
        Text("Looking at your item...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
private fun RecognitionResultScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    imageUri: String,
    recognitionResult: RecognitionResult,
    onBack: () -> Unit,
    onAddToHome: (HouseItem) -> Unit
) {
    var name by remember(recognitionResult) { mutableStateOf(recognitionResult.itemName) }
    var category by remember(recognitionResult) { mutableStateOf(recognitionResult.category) }
    var brand by remember(recognitionResult) { mutableStateOf(recognitionResult.brand) }
    var modelNumber by remember(recognitionResult) { mutableStateOf(recognitionResult.modelNumber) }
    var serialNumber by remember(recognitionResult) { mutableStateOf(recognitionResult.serialNumber) }
    var location by remember(recognitionResult) { mutableStateOf(recognitionResult.locationSuggestion) }
    var filterPartNumber by remember(recognitionResult) { mutableStateOf(recognitionResult.filterPartNumber) }
    var notes by remember(recognitionResult) { mutableStateOf(recognitionResult.notes) }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(modifier = Modifier.height(20.dp))
        Text("We found it", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Check the details before adding this to your home.", fontSize = 16.sp)
        if (recognitionResult.confidence == "low" && name.isBlank() && category.isBlank() && brand.isBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("We couldn't read enough from that photo. Try taking a closer picture of the model label.")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack) { Text("Try Another Photo") }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SelectedImagePreview(uriString = imageUri)
        Spacer(modifier = Modifier.height(24.dp))
        RecognitionField("Item Name", name) { name = it }
        RecognitionField("Category", category) { category = it }
        RecognitionField("Brand", brand) { brand = it }
        RecognitionField("Model Number", modelNumber) { modelNumber = it }
        RecognitionField("Serial Number", serialNumber) { serialNumber = it }
        RecognitionField("Room / Location", location) { location = it }
        RecognitionField("Filter / Part Number", filterPartNumber) { filterPartNumber = it }
        RecognitionField("Notes", notes, minLines = 3) { notes = it }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                onAddToHome(
                    HouseItem(
                        id = UUID.randomUUID().toString(), name = name, category = category, brand = brand,
                        modelNumber = modelNumber, serialNumber = serialNumber, location = location,
                        filterPartNumber = filterPartNumber, notes = notes, status = "Recently added"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Add to My Home") }
    }
}

@Composable
private fun RecognitionField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun AskScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    houseItems: List<HouseItem>
) {
    var question by rememberSaveable { mutableStateOf("") }
    var answerText by rememberSaveable { mutableStateOf<String?>(null) }
    var sourceLabel by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Ask HouseMind", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Ask anything about your home.", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = question,
            onValueChange = {
                question = it
                answerText = null
                sourceLabel = null
            },
            placeholder = { Text("What filter does my refrigerator use?") },
            modifier = Modifier.fillMaxWidth(), minLines = 3
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val answer = HouseMindQueryEngine.answer(question.trim(), houseItems)
                answerText = answer.answerText
                sourceLabel = answer.sourceLabel
            },
            enabled = question.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Ask") }
        answerText?.let { response ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(response)
            sourceLabel?.let { source ->
                Spacer(modifier = Modifier.height(8.dp))
                Text("Source: $source", fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("Try asking", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        SuggestedQuestion("What filter does my refrigerator use?") { question = it }
        Spacer(modifier = Modifier.height(8.dp))
        SuggestedQuestion("When was my AC last serviced?") { question = it }
        Spacer(modifier = Modifier.height(8.dp))
        SuggestedQuestion("What needs attention?") { question = it }
        Spacer(modifier = Modifier.height(8.dp))
        SuggestedQuestion("How much have I spent on my AC?") { question = it }
    }
}

@Composable
private fun SuggestedQuestion(question: String, onClick: (String) -> Unit) {
    OutlinedButton(onClick = { onClick(question) }, modifier = Modifier.fillMaxWidth()) {
        Text(question)
    }
}

@Composable
fun SelectedImagePreview(
    uriString: String
) {

    val context = LocalContext.current

    val bitmap = remember(uriString) {

        try {

            context.contentResolver
                .openInputStream(
                    Uri.parse(uriString)
                )
                ?.use { inputStream ->

                    BitmapFactory.decodeStream(
                        inputStream
                    )
                }

        } catch (exception: Exception) {

            null
        }
    }

    if (bitmap != null) {

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Selected appliance photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(
                    RoundedCornerShape(16.dp)
                ),
            contentScale = ContentScale.Crop
        )

    } else {

        Text(
            text = "Unable to display photo."
        )
    }
}

@Composable
fun HouseItemCard(
    name: String,
    brand: String,
    status: String,
    photoPath: String?,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(modifier = Modifier.padding(16.dp)) {
            if (photoPath != null) {
                StoredImagePreview(
                    photoPath = photoPath,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Text("Home", modifier = Modifier.size(64.dp).padding(top = 20.dp))
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = brand)
                Text(text = status)
            }
        }
    }
}

@Composable
private fun StoredImagePreview(photoPath: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(photoPath) {
        LocalImageStorage(context.applicationContext).loadImage(photoPath)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Saved item photo",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
fun HouseMindPreview() {

    HouseMindTheme {

        HouseMindApp()
    }
}
