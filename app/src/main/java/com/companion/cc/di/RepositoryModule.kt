package com.companion.cc.di

import com.companion.cc.data.character.DefaultCharacterCatalog
import com.companion.cc.data.identity.DataStoreCurrentUserProvider
import com.companion.cc.data.repository.MessageRepositoryImpl
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindCurrentUserProvider(
        dataStoreCurrentUserProvider: DataStoreCurrentUserProvider
    ): CurrentUserProvider

    @Binds
    @Singleton
    abstract fun bindCharacterCatalog(
        defaultCharacterCatalog: DefaultCharacterCatalog
    ): CharacterCatalog
}
