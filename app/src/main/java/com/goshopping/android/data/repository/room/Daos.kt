package com.goshopping.android.data.repository.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SharedGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: SharedGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(groups: List<SharedGroupEntity>)

    @Query("SELECT * FROM shared_groups")
    suspend fun getAll(): List<SharedGroupEntity>

    @Query("SELECT * FROM shared_groups WHERE groupId = :groupId")
    suspend fun getById(groupId: String): SharedGroupEntity?

    @Query("DELETE FROM shared_groups WHERE groupId = :groupId")
    suspend fun deleteById(groupId: String)

    @Query("DELETE FROM shared_groups")
    suspend fun deleteAll()
}

@Dao
interface SharedListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: SharedListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lists: List<SharedListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: SharedItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<SharedItemEntity>)

    @Query("SELECT * FROM shared_lists WHERE groupId = :groupId")
    suspend fun getByGroup(groupId: String): List<SharedListEntity>

    @Query("SELECT * FROM shared_lists WHERE listId = :listId")
    suspend fun getById(listId: String): SharedListEntity?

    @Query("SELECT * FROM shared_items WHERE listId = :listId")
    suspend fun getItemsByList(listId: String): List<SharedItemEntity>

    @Query("UPDATE shared_items SET isDeleted = 1, deletedAt = :deletedAt WHERE itemId = :itemId AND listId = :listId")
    suspend fun softDeleteItem(itemId: String, listId: String, deletedAt: Long)

    @Query("DELETE FROM shared_lists WHERE listId = :listId")
    suspend fun deleteList(listId: String)

    @Query("DELETE FROM shared_items WHERE listId = :listId")
    suspend fun deleteItemsByList(listId: String)

    @Transaction
    suspend fun deleteListWithItems(listId: String) {
        deleteItemsByList(listId)
        deleteList(listId)
    }

    @Query("DELETE FROM shared_lists WHERE groupId = :groupId")
    suspend fun deleteListsByGroup(groupId: String)

    @Query("DELETE FROM shared_items WHERE groupId = :groupId")
    suspend fun deleteItemsByGroup(groupId: String)

    @Transaction
    suspend fun deleteGroupData(groupId: String) {
        deleteItemsByGroup(groupId)
        deleteListsByGroup(groupId)
    }

    @Query("DELETE FROM shared_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM shared_items")
    suspend fun deleteAllItems()
}
