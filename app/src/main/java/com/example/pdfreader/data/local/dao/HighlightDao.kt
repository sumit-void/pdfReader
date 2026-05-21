package com.example.pdfreader.data.local.dao

import androidx.room.*
import com.example.pdfreader.data.local.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY pageNumber ASC, startIndex ASC")
    fun getHighlightsForBook(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND pageNumber = :page")
    fun getHighlightsForPage(bookId: Long, page: Int): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: Long)

    @Query("SELECT * FROM highlights WHERE id = :id LIMIT 1")
    suspend fun getHighlightById(id: Long): HighlightEntity?

    @Query("SELECT COUNT(*) FROM highlights WHERE bookId = :bookId")
    suspend fun getHighlightCount(bookId: Long): Int
}
