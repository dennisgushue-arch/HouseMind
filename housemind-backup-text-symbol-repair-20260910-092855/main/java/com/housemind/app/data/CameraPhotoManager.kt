package com.housemind.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

data class CameraPhoto(val file: File, val uri: Uri)

class CameraPhotoManager(private val context: Context) {

    fun createTemporaryPhoto(): CameraPhoto? = try {
        val directory = File(context.cacheDir, CAMERA_DIRECTORY).apply { mkdirs() }
        val file = File(directory, "scan_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        CameraPhoto(file, uri)
    } catch (exception: Exception) {
        Log.e(TAG, "Unable to create temporary camera photo.", exception)
        null
    }

    fun deleteTemporaryPhoto(photo: CameraPhoto?) {
        if (photo != null && photo.file.exists() && !photo.file.delete()) {
            Log.w(TAG, "Unable to delete temporary camera photo.")
        }
    }

    private companion object {
        const val CAMERA_DIRECTORY = "scan_photos"
        const val TAG = "CameraPhotoManager"
    }
}