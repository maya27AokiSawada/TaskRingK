package net.sumomo_planning.goshopping.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.goshopping.data.local.room.entity.SharedListEntity

@Dao
interface SharedListDao {
    @Query("SELECT * FROM shared_lists WHERE groupId = :groupId ORDER BY updatedAt DESC, createdAt DESC")
    fun observeByGroup(groupId: String): Flow<List<SharedListEntity>>

    @Query("SELECT * FROM shared_lists WHERE listId = :listId")
    fun observeById(listId: String): Flow<SharedListEntity?>

    @Upsert
    suspend fun upsert(entity: SharedListEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SharedListEntity>)

    @Query("DELETE FROM shared_lists WHERE listId = :listId")
    suspend fun deleteById(listId: String)

    @Query("DELETE FROM shared_lists WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)

    @Query("DELETE FROM shared_lists")
    suspend fun clearAll()
}
