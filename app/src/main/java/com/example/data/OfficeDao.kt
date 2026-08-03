package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {
    @Query("SELECT * FROM office_documents ORDER BY lastModified DESC")
    fun getAllDocuments(): Flow<List<OfficeDocument>>

    @Query("SELECT * FROM office_documents WHERE type = :type ORDER BY lastModified DESC")
    fun getDocumentsByType(type: String): Flow<List<OfficeDocument>>

    @Query("SELECT * FROM office_documents WHERE id = :id")
    suspend fun getDocumentById(id: String): OfficeDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: OfficeDocument)

    @Delete
    suspend fun deleteDocument(document: OfficeDocument)

    @Query("DELETE FROM office_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}
