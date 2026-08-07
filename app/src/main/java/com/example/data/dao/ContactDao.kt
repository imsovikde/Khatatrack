package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactById(id: Long): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactByIdSync(id: Long): Contact?

    @Query("SELECT SUM(CASE WHEN type = 'YOU_GOT' THEN amount ELSE -amount END) FROM transactions WHERE contactId = :contactId AND isDeleted = 0")
    suspend fun getContactNetBalanceSync(contactId: Long): Double?

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsSync(): List<Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Update
    suspend fun updateContact(contact: Contact)

    @Query("UPDATE contacts SET isArchived = 1 WHERE id = :id")
    suspend fun archiveContact(id: Long)

    @Query("UPDATE contacts SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE contacts SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteContact(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreContact(id: Long)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactPermanently(id: Long)

    @Query("SELECT * FROM contacts WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun getExpiredDeletedContactsSync(cutoffTimestamp: Long): List<Contact>

    @Query("DELETE FROM contacts WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun purgeOldDeletedContacts(cutoffTimestamp: Long)

    @Query("DELETE FROM contacts")
    suspend fun clearAllContacts()

    @Query("SELECT * FROM contacts WHERE isDeleted = 0 AND isArchived = 0 AND (name LIKE '%' || :query || '%' OR mobileNumber LIKE '%' || :query || '%' OR addressNotes LIKE '%' || :query || '%')")
    fun searchContacts(query: String): Flow<List<Contact>>
}
