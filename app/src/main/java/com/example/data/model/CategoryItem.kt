package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Category",
    val tagColor: String = "Gray",
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false
)
