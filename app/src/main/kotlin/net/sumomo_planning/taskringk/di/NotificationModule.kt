package net.sumomo_planning.taskringk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sumomo_planning.taskringk.data.repository.HybridNotificationRepositoryImpl
import net.sumomo_planning.taskringk.domain.repository.NotificationRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: HybridNotificationRepositoryImpl,
    ): NotificationRepository
}