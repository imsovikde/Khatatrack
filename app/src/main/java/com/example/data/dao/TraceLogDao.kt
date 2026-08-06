package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TraceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TraceLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrace(trace: TraceLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraces(traces: List<TraceLog>)

    @Query("SELECT * FROM trace_logs ORDER BY timestamp DESC")
    fun getAllTraces(): Flow<List<TraceLog>>

    @Query("SELECT * FROM trace_logs WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    fun getTracesForEntity(entityType: String, entityId: Long): Flow<List<TraceLog>>

    @Query("DELETE FROM trace_logs")
    suspend fun clearAllTraces()
}
