package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PaymentModeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentModeDao {
    @Query("SELECT * FROM payment_modes WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getAllPaymentModes(): Flow<List<PaymentModeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMode(mode: PaymentModeItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentModes(modes: List<PaymentModeItem>)

    @Update
    suspend fun updatePaymentMode(mode: PaymentModeItem)

    @Query("UPDATE payment_modes SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE payment_modes SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("DELETE FROM payment_modes WHERE id = :id")
    suspend fun deletePermanently(id: Long)
}
