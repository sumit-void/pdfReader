package com.example.pdfreader.data.local.dao

import androidx.room.*
import com.example.pdfreader.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE startTime >= :since ORDER BY startTime DESC")
    fun getSessionsSince(since: Long): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ReadingSessionEntity)

    @Query("DELETE FROM reading_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT SUM(endTime - startTime) FROM reading_sessions WHERE bookId = :bookId AND endTime > 0")
    suspend fun getTotalReadingTimeForBook(bookId: Long): Long?

    @Query("SELECT SUM(pagesRead) FROM reading_sessions")
    suspend fun getTotalPagesRead(): Int?

    @Query("SELECT SUM(endTime - startTime) FROM reading_sessions WHERE endTime > 0")
    suspend fun getTotalReadingTime(): Long?

    @Query("SELECT COUNT(DISTINCT date(startTime / 1000, 'unixepoch', 'localtime')) FROM reading_sessions WHERE startTime >= :since")
    suspend fun getReadingDaysSince(since: Long): Int

    @Query("SELECT * FROM reading_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ReadingSessionEntity?
}
