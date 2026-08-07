package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IncomeExpenseEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeExpenseEntryDao {
    @Query("SELECT * FROM income_expense_entries WHERE isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun getAllActive(): Flow<List<IncomeExpenseEntry>>

    @Query("SELECT * FROM income_expense_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getAllInTrash(): Flow<List<IncomeExpenseEntry>>

    @Query("SELECT * FROM income_expense_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): IncomeExpenseEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: IncomeExpenseEntry): Long

    @Update
    suspend fun update(entry: IncomeExpenseEntry)

    @Query("UPDATE income_expense_entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE income_expense_entries SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM income_expense_entries WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM income_expense_entries WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun purgeOldTrash(cutoff: Long)
}
