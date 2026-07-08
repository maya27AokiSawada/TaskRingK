package net.sumomo_planning.taskringk.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.data.local.room.entity.WhiteboardEntity

@Dao
interface WhiteboardDao {
    @Query("SELECT * FROM whiteboards WHERE groupId = :groupId ORDER BY updatedAt DESC, createdAt DESC")
    fun observeByGroup(groupId: String): Flow<List<WhiteboardEntity>>

    @Query("SELECT * FROM whiteboards WHERE whiteboardId = :whiteboardId")
    fun observeById(whiteboardId: String): Flow<WhiteboardEntity?>

    @Upsert
    suspend fun upsert(entity: WhiteboardEntity)

    @Upsert
    suspend fun upsertAll(entities: List<WhiteboardEntity>)

    @Query("DELETE FROM whiteboards WHERE whiteboardId = :whiteboardId")
    suspend fun deleteById(whiteboardId: String)

    @Query("DELETE FROM whiteboards WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)

    @Query("DELETE FROM whiteboards")
    suspend fun clearAll()
}
