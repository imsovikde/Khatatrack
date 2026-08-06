package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

data class CategoryAggregate(
    val categoryTag: String,
    val totalAmount: Double,
    val count: Int
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY transactionDate DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE contactId = :contactId AND isDeleted = 0 ORDER BY transactionDate ASC, createdAt ASC")
    fun getTransactionsForContact(contactId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSync(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :timestamp WHERE contactId = :contactId")
    suspend fun softDeleteTransactionsForContact(contactId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTransaction(id: Long)

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = NULL WHERE contactId = :contactId")
    suspend fun restoreTransactionsForContact(contactId: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionPermanently(id: Long)

    @Query("DELETE FROM transactions WHERE contactId = :contactId")
    suspend fun deleteTransactionsForContact(contactId: Long)

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun getExpiredDeletedTransactionsSync(cutoffTimestamp: Long): List<Transaction>

    @Query("DELETE FROM transactions WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun purgeOldDeletedTransactions(cutoffTimestamp: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND (note LIKE '%' || :query || '%' OR paymentMode LIKE '%' || :query || '%' OR referenceNumber LIKE '%' || :query || '%')")
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE transactionDate >= :startDate AND transactionDate <= :endDate AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT categoryTag, SUM(amount) AS totalAmount, COUNT(*) AS count FROM transactions WHERE isDeleted = 0 AND transactionDate >= :startDate AND transactionDate <= :endDate GROUP BY categoryTag ORDER BY totalAmount DESC")
    fun getCategoryAggregates(startDate: Long, endDate: Long): Flow<List<CategoryAggregate>>
}
