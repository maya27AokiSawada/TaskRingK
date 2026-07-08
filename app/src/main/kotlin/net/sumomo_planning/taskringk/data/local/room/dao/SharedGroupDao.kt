package net.sumomo_planning.taskringk.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.data.local.room.entity.SharedGroupEntity

@Dao
interface SharedGroupDao {
    @Query("SELECT * FROM shared_groups ORDER BY updatedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<SharedGroupEntity>>

    @Query("SELECT * FROM shared_groups WHERE groupId = :groupId")
    fun observeById(groupId: String): Flow<SharedGroupEntity?>

    @Upsert
    suspend fun upsert(entity: SharedGroupEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SharedGroupEntity>)

    @Query("DELETE FROM shared_groups WHERE groupId = :groupId")
    suspend fun deleteById(groupId: String)

    @Query("DELETE FROM shared_groups")
    suspend fun clearAll()
}
