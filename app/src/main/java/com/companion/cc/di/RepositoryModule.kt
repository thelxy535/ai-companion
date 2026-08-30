package com.companion.cc.di

import com.companion.cc.data.character.DefaultCharacterCatalog
import com.companion.cc.data.character.DefaultCharacterPromptResolver
import com.companion.cc.data.identity.DataStoreCurrentUserProvider
import com.companion.cc.data.repository.MessageRepositoryImpl
import com.companion.cc.data.local.repository.RoomScheduleRepository
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.character.CharacterPromptResolver
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.schedule.ScheduleRepository
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
    abstract fun bindScheduleRepository(
        roomScheduleRepository: RoomScheduleRepository
    ): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindCurrentUserProvider(
        dataStoreCurrentUserProvider: DataStoreCurrentUserProvider
    ): CurrentUserProvider

    @Binds
    @Singleton
    abstract fun bindCharacterPromptResolver(
        defaultCharacterPromptResolver: DefaultCharacterPromptResolver
    ): CharacterPromptResolver

    @Binds
    @Singleton
    abstract fun bindCharacterCatalog(
        defaultCharacterCatalog: DefaultCharacterCatalog
    ): CharacterCatalog
}
