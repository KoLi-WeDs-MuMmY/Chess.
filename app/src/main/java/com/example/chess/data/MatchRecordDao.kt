package com.example.chess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchRecordDao {
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC, id DESC")
    fun getAllMatches(): Flow<List<MatchRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecordEntity): Long

    @Query("DELETE FROM match_records WHERE id = :id")
    suspend fun deleteMatchById(id: Long)

    @Query("DELETE FROM match_records")
    suspend fun deleteAllMatches()
}
