package com.housemind.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.housemind.app.model.SavedDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

class LocalDocumentStorage(
    private val context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun load(itemId: String): List<SavedDocument> {
        val raw = preferences.getString(itemId, null)
            ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                array.getJSONObject(index).toSavedDocument()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to read saved documents.", exception)
            emptyList()
        }
    }

    fun importDocument(
        sourceUri: Uri,
        itemId: String,
        title: String,
        type: String,
        notes: String
    ): SavedDocument? {
        return try {
            val resolver = context.contentResolver
            val originalName = displayName(sourceUri) ?: "document"
            val safeExtension = originalName
                .substringAfterLast('.', "")
                .replace(Regex("[^A-Za-z0-9]"), "")
                .takeIf { it.isNotBlank() }

            val storedName = buildString {
                append(UUID.randomUUID().toString())
                if (safeExtension != null) {
                    append('.')
                    append(safeExtension)
                }
            }

            val directory = File(
                context.filesDir,
                "housemind_documents/$itemId"
            ).apply { mkdirs() }

            val destination = File(directory, storedName)

            resolver.openInputStream(sourceUri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            SavedDocument(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                type = type,
                fileName = originalName,
                localPath = destination.absolutePath,
                mimeType = resolver.getType(sourceUri)
                    ?: "application/octet-stream",
                notes = notes.trim(),
                addedDate = LocalDate.now().toString()
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to save document.", exception)
            null
        }
    }

    fun save(itemId: String, documents: List<SavedDocument>) {
        val array = JSONArray()
        documents.forEach { array.put(it.toJson()) }

        preferences.edit()
            .putString(itemId, array.toString())
            .apply()
    }

    fun deleteFile(document: SavedDocument) {
        runCatching { File(document.localPath).delete() }
            .onFailure { Log.e(TAG, "Unable to delete document file.", it) }
    }

    fun exists(document: SavedDocument): Boolean =
        document.localPath.isNotBlank() && File(document.localPath).exists()

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }

    private fun SavedDocument.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("type", type)
        .put("fileName", fileName)
        .put("localPath", localPath)
        .put("mimeType", mimeType)
        .put("notes", notes)
        .put("addedDate", addedDate)

    private fun JSONObject.toSavedDocument() = SavedDocument(
        id = optString("id", UUID.randomUUID().toString()),
        title = optString("title", "Document"),
        type = optString("type", "Other"),
        fileName = optString("fileName", ""),
        localPath = optString("localPath", ""),
        mimeType = optString("mimeType", "application/octet-stream"),
        notes = optString("notes", ""),
        addedDate = optString("addedDate", "")
    )

    private companion object {
        const val PREFERENCES_NAME = "housemind_documents"
        const val TAG = "LocalDocumentStorage"
    }
}
