package net.sumomo_planning.goshopping.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sumomo_planning.goshopping.data.local.room.dao.SharedGroupDao
import net.sumomo_planning.goshopping.data.local.room.dao.SharedListDao
import net.sumomo_planning.goshopping.data.local.room.dao.WhiteboardDao
import net.sumomo_planning.goshopping.data.local.room.entity.SharedGroupEntity
import net.sumomo_planning.goshopping.data.local.room.entity.SharedListEntity
import net.sumomo_planning.goshopping.data.local.room.entity.WhiteboardEntity

@Database(
    entities = [SharedGroupEntity::class, SharedListEntity::class, WhiteboardEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class GoShopDatabase : RoomDatabase() {
    abstract fun sharedGroupDao(): SharedGroupDao
    abstract fun sharedListDao(): SharedListDao
    abstract fun whiteboardDao(): WhiteboardDao
}
