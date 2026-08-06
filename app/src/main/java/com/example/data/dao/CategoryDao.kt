package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<CategoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryItem>)

    @Update
    suspend fun updateCategory(category: CategoryItem)

    @Query("UPDATE categories SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE categories SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deletePermanently(id: Long)
}
