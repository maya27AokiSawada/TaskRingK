package net.sumomo_planning.taskringk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sumomo_planning.taskringk.data.repository.HybridSharedListRepositoryImpl
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class ListModule {

    @Binds
    @Singleton
    abstract fun bindSharedListRepository(
        impl: HybridSharedListRepositoryImpl,
    ): SharedListRepository
}
