package net.sumomo_planning.taskringk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sumomo_planning.taskringk.data.repository.AuthRepositoryImpl
import net.sumomo_planning.taskringk.domain.repository.AuthRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
