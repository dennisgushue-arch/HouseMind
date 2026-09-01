package com.housemind.app.data

import android.content.Context
import android.util.Log
import com.housemind.app.model.HouseItem
import com.housemind.app.model.MaintenanceRecord
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalHouseItemStorage(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadOrSeed(): List<HouseItem> {
        if (!preferences.contains(KEY_INITIALIZED)) {
            val demoItems = createDemoItems()
            save(demoItems)
            return demoItems
        }

        val savedItems = preferences.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val itemsJson = JSONArray(savedItems)
            List(itemsJson.length()) { index -> itemsJson.getJSONObject(index).toHouseItem() }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to read saved HouseMind items.", exception)
            emptyList()
        }
    }

    fun save(items: List<HouseItem>) {
        val itemsJson = JSONArray()
        items.forEach { item -> itemsJson.put(item.toJson()) }

        preferences.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_ITEMS, itemsJson.toString())
            .apply()
    }

    private fun createDemoItems() = listOf(
        HouseItem(UUID.randomUUID().toString(), "Kitchen Refrigerator", "Refrigerator", "GE Profile", "", "", "Kitchen", "", "", "Everything looks good"),
        HouseItem(UUID.randomUUID().toString(), "Upstairs AC", "Air Conditioner", "Carrier", "", "", "Upstairs", "", "", "Filter due in 9 days"),
        HouseItem(UUID.randomUUID().toString(), "Water Heater", "Water Heater", "Rheem", "", "", "Garage", "", "", "No action needed")
    )

    private fun HouseItem.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("category", category)
        .put("brand", brand)
        .put("modelNumber", modelNumber)
        .put("serialNumber", serialNumber)
        .put("location", location)
        .put("filterPartNumber", filterPartNumber)
        .put("notes", notes)
        .put("status", status)
        .put("maintenanceRecords", JSONArray().apply {
            maintenanceRecords.forEach { record -> put(record.toJson()) }
        })
        .put("photoPath", photoPath)

    private fun JSONObject.toHouseItem() = HouseItem(
        id = getString("id"),
        name = getString("name"),
        category = getString("category"),
        brand = getString("brand"),
        modelNumber = getString("modelNumber"),
        serialNumber = getString("serialNumber"),
        location = getString("location"),
        filterPartNumber = getString("filterPartNumber"),
        notes = getString("notes"),
        status = getString("status"),
        maintenanceRecords = optJSONArray("maintenanceRecords")?.let { recordsJson ->
            List(recordsJson.length()) { index -> recordsJson.getJSONObject(index).toMaintenanceRecord() }
        } ?: emptyList(),
        photoPath = optString("photoPath").takeIf { it.isNotBlank() }
    )

    private fun MaintenanceRecord.toJson() = JSONObject()
        .put("id", id)
        .put("date", date)
        .put("serviceType", serviceType)
        .put("provider", provider)
        .put("cost", cost)
        .put("notes", notes)

    private fun JSONObject.toMaintenanceRecord() = MaintenanceRecord(
        id = getString("id"),
        date = getString("date"),
        serviceType = getString("serviceType"),
        provider = getString("provider"),
        cost = getString("cost"),
        notes = getString("notes")
    )

    private companion object {
        const val PREFERENCES_NAME = "housemind_items"
        const val KEY_INITIALIZED = "initialized"
        const val KEY_ITEMS = "items"
        const val TAG = "LocalHouseItemStorage"
    }
}