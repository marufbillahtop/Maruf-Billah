package com.example.data

import kotlinx.coroutines.flow.Flow

class OfficeRepository(private val officeDao: OfficeDao) {
    val allDocuments: Flow<List<OfficeDocument>> = officeDao.getAllDocuments()

    fun getDocumentsByType(type: String): Flow<List<OfficeDocument>> {
        return officeDao.getDocumentsByType(type)
    }

    suspend fun getDocumentById(id: String): OfficeDocument? {
        return officeDao.getDocumentById(id)
    }

    suspend fun insertDocument(document: OfficeDocument) {
        officeDao.insertDocument(document)
    }

    suspend fun deleteDocument(document: OfficeDocument) {
        officeDao.deleteDocument(document)
    }

    suspend fun deleteDocumentById(id: String) {
        officeDao.deleteDocumentById(id)
    }
}
