package net.sumomo_planning.taskringk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sumomo_planning.taskringk.data.repository.HybridSharedGroupRepositoryImpl
import net.sumomo_planning.taskringk.domain.repository.SharedGroupRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class GroupModule {

    @Binds
    @Singleton
    abstract fun bindSharedGroupRepository(
        impl: HybridSharedGroupRepositoryImpl,
    ): SharedGroupRepository
}
