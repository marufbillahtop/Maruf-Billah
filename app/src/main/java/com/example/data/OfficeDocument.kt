package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_documents")
data class OfficeDocument(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "DOC", "SHEET", "SLIDE", "HSC"
    val content: String, // JSON representation of content
    val lastModified: Long = System.currentTimeMillis()
)
