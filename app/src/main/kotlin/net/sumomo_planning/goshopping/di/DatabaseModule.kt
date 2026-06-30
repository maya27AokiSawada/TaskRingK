package net.sumomo_planning.goshopping.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import net.sumomo_planning.goshopping.data.local.room.GoShopDatabase
import net.sumomo_planning.goshopping.data.local.room.dao.SharedGroupDao
import net.sumomo_planning.goshopping.data.local.room.dao.SharedListDao
import net.sumomo_planning.goshopping.data.local.room.dao.WhiteboardDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GoShopDatabase =
        Room.databaseBuilder(context, GoShopDatabase::class.java, "goshop.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSharedGroupDao(database: GoShopDatabase): SharedGroupDao = database.sharedGroupDao()

    @Provides
    fun provideSharedListDao(database: GoShopDatabase): SharedListDao = database.sharedListDao()

    @Provides
    fun provideWhiteboardDao(database: GoShopDatabase): WhiteboardDao = database.whiteboardDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
