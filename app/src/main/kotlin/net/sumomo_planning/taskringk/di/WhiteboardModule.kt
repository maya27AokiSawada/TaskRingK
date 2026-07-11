package net.sumomo_planning.taskringk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sumomo_planning.taskringk.data.repository.HybridWhiteboardRepositoryImpl
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class WhiteboardModule {

    @Binds
    @Singleton
    abstract fun bindWhiteboardRepository(
        impl: HybridWhiteboardRepositoryImpl,
    ): WhiteboardRepository
}