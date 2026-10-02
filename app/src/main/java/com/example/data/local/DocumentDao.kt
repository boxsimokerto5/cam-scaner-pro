package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdTimestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE category = :category ORDER BY createdTimestamp DESC")
    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>>

    @Query(
        """
        SELECT * FROM documents 
        WHERE title LIKE '%' || :query || '%' 
           OR ocrText LIKE '%' || :query || '%' 
        ORDER BY createdTimestamp DESC
        """
    )
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query(
        """
        SELECT * FROM documents 
        WHERE category = :category 
          AND (title LIKE '%' || :query || '%' OR ocrText LIKE '%' || :query || '%')
        ORDER BY createdTimestamp DESC
        """
    )
    fun searchDocumentsWithCategory(query: String, category: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)
}
