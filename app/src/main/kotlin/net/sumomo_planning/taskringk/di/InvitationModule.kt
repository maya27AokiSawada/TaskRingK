package net.sumomo_planning.taskringk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sumomo_planning.taskringk.data.repository.HybridInvitationRepositoryImpl
import net.sumomo_planning.taskringk.domain.repository.InvitationRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class InvitationModule {

    @Binds
    @Singleton
    abstract fun bindInvitationRepository(
        impl: HybridInvitationRepositoryImpl,
    ): InvitationRepository
}