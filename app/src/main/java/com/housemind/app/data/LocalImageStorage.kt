package com.housemind.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class LocalImageStorage(private val context: Context) {

    fun saveImage(sourceUri: Uri, itemId: String): String? {
        return try {
            val bitmap = decodeResizedImage(sourceUri) ?: return null

            val photoDirectory = File(context.filesDir, PHOTO_DIRECTORY).apply { mkdirs() }
            val outputFile = File(photoDirectory, "$itemId.jpg")
            FileOutputStream(outputFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            bitmap.recycle()
            outputFile.absolutePath
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to save item photo.", exception)
            null
        }
    }

    fun loadImage(photoPath: String): Bitmap? = try {
        BitmapFactory.decodeFile(photoPath)
    } catch (exception: Exception) {
        Log.e(TAG, "Unable to load item photo.", exception)
        null
    }

    fun createJpegDataUrl(sourceUri: Uri): String? = try {
        val bitmap = decodeResizedImage(sourceUri) ?: return null
        val bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
        bitmap.recycle()
        "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    } catch (exception: Exception) {
        Log.e(TAG, "Unable to prepare item photo for recognition.", exception)
        null
    }

    private fun decodeResizedImage(sourceUri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth > MAX_IMAGE_DIMENSION || sampledHeight > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }
        return sampleSize
    }

    private companion object {
        const val PHOTO_DIRECTORY = "housemind_photos"
        const val MAX_IMAGE_DIMENSION = 1600
        const val JPEG_QUALITY = 85
        const val TAG = "LocalImageStorage"
    }
}