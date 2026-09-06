package com.housemind.app.model

data class SavedDocument(
    val id: String,
    val title: String,
    val type: String,
    val fileName: String,
    val localPath: String,
    val mimeType: String,
    val notes: String,
    val addedDate: String
)
