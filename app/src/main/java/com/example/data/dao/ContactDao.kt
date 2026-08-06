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
    @Query("SELECT * FROM contacts WHERE isArchived = 0 ORDER BY isPinned DESC, name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactById(id: Long): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactByIdSync(id: Long): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Query("UPDATE contacts SET isArchived = 1 WHERE id = :id")
    suspend fun archiveContact(id: Long)

    @Query("UPDATE contacts SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactPermanently(id: Long)

    @Query("SELECT * FROM contacts WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR mobileNumber LIKE '%' || :query || '%' OR addressNotes LIKE '%' || '%')")
    fun searchContacts(query: String): Flow<List<Contact>>
}
