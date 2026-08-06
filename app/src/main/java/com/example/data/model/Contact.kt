package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobileNumber: String? = null,
    val email: String? = null,
    val profilePhoto: String? = null,
    val addressNotes: String? = null,
    val categoryTag: String = "Friend", // Friend, Family, Customer, Supplier, Other
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
