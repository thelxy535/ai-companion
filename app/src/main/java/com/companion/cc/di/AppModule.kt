package com.companion.cc.di

import android.content.Context
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.remote.api.SiliconFlowApi
import com.companion.cc.domain.manager.AIProviderManager
import com.companion.cc.domain.manager.CompanionStateManager
import com.companion.cc.domain.manager.SystemPromptManager
import com.companion.cc.domain.usecase.*
import com.companion.cc.data.local.repository.MemoryCapsuleV2Builder
import com.companion.cc.data.local.repository.MemoryCapsuleV2Importer
import com.companion.cc.data.local.repository.MemoryCapsuleV2Transfer
import com.companion.cc.data.local.repository.MemoryCapsuleV2TransferService
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.data.repository.LocalCharacterExternalCleanup
import com.companion.cc.data.repository.RoomCharacterDeletionTransaction
import com.companion.cc.data.repository.RoomCharacterMemoryCapsuleRepository
import com.companion.cc.data.repository.RoomCharacterRevivalTransaction
import com.companion.cc.domain.character.CharacterRevivalTransaction
import com.companion.cc.domain.memory.MemoryRetrievalService
import com.companion.cc.domain.character.CharacterMemoryCapsuleRepository
import com.companion.cc.domain.character.CharacterCleanupProcessor
import com.companion.cc.domain.character.CharacterDeletionTransaction
import com.companion.cc.domain.character.CharacterExternalCleanup
import com.companion.cc.domain.character.CharacterMemoryCapsuleCipher
import com.companion.cc.domain.character.CharacterMemoryCapsuleToken
import com.companion.cc.domain.character.CharacterMemoryCapsuleDecryptor
import com.companion.cc.domain.character.CharacterMemoryCapsuleTokenSource
import com.companion.cc.domain.character.CharacterRevivalService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    // DAO Providers - 统一从 AppDatabase 提供所有 DAO
    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): com.companion.cc.data.local.dao.MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideStatsDao(database: AppDatabase): com.companion.cc.data.local.dao.StatsDao {
        return database.statsDao()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(database: AppDatabase): com.companion.cc.data.local.dao.MemoryDao {
        return database.memoryDao()
    }

    @Provides
    @Singleton
    fun provideMemoryNodeDao(database: AppDatabase): com.companion.cc.data.local.dao.MemoryNodeDao {
        return database.memoryNodeDao()
    }

    @Provides
    @Singleton
    fun provideUserProfileDao(database: AppDatabase): com.companion.cc.data.local.dao.UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun provideVectorMemoryDao(database: AppDatabase): com.companion.cc.data.local.dao.VectorMemoryDao {
        return database.vectorMemoryDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: AppDatabase): com.companion.cc.data.local.dao.TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideInteractionTimeDao(database: AppDatabase): com.companion.cc.data.local.dao.InteractionTimeDao {
        return database.interactionTimeDao()
    }

    @Provides
    @Singleton
    fun provideUserEventDao(database: AppDatabase): com.companion.cc.data.local.dao.UserEventDao {
        return database.userEventDao()
    }

    @Provides
    @Singleton
    fun provideMoodStateDao(database: AppDatabase): com.companion.cc.domain.usecase.MoodStateDao {
        return database.moodStateDao()
    }

    @Provides
    @Singleton
    fun provideCustomCharacterDao(database: AppDatabase): com.companion.cc.data.local.dao.CustomCharacterDao {
        return database.customCharacterDao()
    }

    @Provides
    @Singleton
    fun provideCharacterMemoryCapsuleDao(database: AppDatabase): com.companion.cc.data.local.dao.CharacterMemoryCapsuleDao {
        return database.characterMemoryCapsuleDao()
    }

    @Provides
    @Singleton
    fun provideCharacterCleanupTaskDao(database: AppDatabase): com.companion.cc.data.local.dao.CharacterCleanupTaskDao {
        return database.characterCleanupTaskDao()
    }
    @Provides
    @Singleton
    fun provideScheduleDao(database: AppDatabase): com.companion.cc.data.local.dao.ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    @Singleton
    fun provideMemoryRepository(database: AppDatabase): MemoryRepository = MemoryRepository(
        database = database,
        sourceDao = database.memorySourceDao(),
        reviewDao = database.memoryReviewDao(),
        nodeDao = database.memoryNodeDao(),
        evidenceDao = database.memoryEvidenceDao(),
        versionDao = database.memoryVersionDao(),
        relationDao = database.memoryRelationDao(),
        retrievalDao = database.memoryRetrievalDao()
    )

    @Provides
    @Singleton
    fun provideMemoryCapsuleV2Builder(
        database: AppDatabase,
        gson: Gson
    ): MemoryCapsuleV2Builder = MemoryCapsuleV2Builder(database, gson)

    @Provides
    @Singleton
    fun provideMemoryCapsuleV2Importer(
        database: AppDatabase
    ): MemoryCapsuleV2Importer = MemoryCapsuleV2Importer(database)

    @Provides
    @Singleton
    fun provideMemoryCapsuleV2Codec(
        gson: Gson
    ): com.companion.cc.domain.memory.MemoryCapsuleV2Codec =
        com.companion.cc.domain.memory.MemoryCapsuleV2Codec(gson)

    @Provides
    @Singleton
    fun provideMemoryCapsuleV2TransferService(
        builder: MemoryCapsuleV2Builder,
        importer: MemoryCapsuleV2Importer,
        codec: com.companion.cc.domain.memory.MemoryCapsuleV2Codec
    ): MemoryCapsuleV2TransferService = MemoryCapsuleV2TransferService(builder, importer, codec)

    @Provides
    @Singleton
    fun provideMemoryCapsuleV2Transfer(
        service: MemoryCapsuleV2TransferService
    ): MemoryCapsuleV2Transfer = service

    @Provides
    @Singleton
    fun provideCharacterRevivalTransaction(
        transaction: RoomCharacterRevivalTransaction
    ): CharacterRevivalTransaction = transaction

    @Provides
    @Singleton
    fun provideMemoryRetrievalService(
        repository: MemoryRepository
    ): MemoryRetrievalService = MemoryRetrievalService(repository)

    @Provides
    @Singleton
    fun provideCharacterMemoryCapsuleRepository(
        repository: RoomCharacterMemoryCapsuleRepository
    ): CharacterMemoryCapsuleRepository = repository

    @Provides
    @Singleton
    fun provideCharacterExternalCleanup(
        cleanup: LocalCharacterExternalCleanup
    ): CharacterExternalCleanup = cleanup

    @Provides
    @Singleton
    fun provideCharacterCleanupProcessor(
        dao: com.companion.cc.data.local.dao.CharacterCleanupTaskDao,
        cleanup: CharacterExternalCleanup
    ): CharacterCleanupProcessor = CharacterCleanupProcessor(dao, cleanup)

    @Provides
    @Singleton
    fun provideCharacterDeletionTransaction(
        transaction: RoomCharacterDeletionTransaction
    ): CharacterDeletionTransaction = transaction

    @Provides
    @Singleton
    fun provideCharacterMemoryCapsuleMemoryProvider(
        builder: MemoryCapsuleV2Builder
    ): com.companion.cc.domain.character.CharacterMemoryCapsuleMemoryProvider =
        com.companion.cc.domain.character.CharacterMemoryCapsuleMemoryProvider { userId, characterId ->
            builder.build(
                com.companion.cc.domain.memory.MemoryScopeKey.forCharacter(userId, characterId)
            ).getOrThrow()
        }

    @Provides
    @Singleton
    fun provideCharacterMemoryCapsuleTokenSource(): CharacterMemoryCapsuleTokenSource =
        CharacterMemoryCapsuleTokenSource { CharacterMemoryCapsuleToken.generate() }

    @Provides
    @Singleton
    fun provideCharacterMemoryCapsuleCipher(
        encryptionHelper: com.companion.cc.util.EncryptionHelper
    ): CharacterMemoryCapsuleCipher = CharacterMemoryCapsuleCipher { value ->
        requireNotNull(encryptionHelper.encrypt(value)) { "Capsule encryption failed" }
    }

    @Provides
    @Singleton
    fun provideCharacterMemoryCapsuleDecryptor(
        encryptionHelper: com.companion.cc.util.EncryptionHelper
    ): CharacterMemoryCapsuleDecryptor = CharacterMemoryCapsuleDecryptor { value ->
        requireNotNull(encryptionHelper.decrypt(value)) { "Capsule decryption failed" }
    }

    @Provides
    @Singleton
    fun provideCharacterRevivalService(
        capsules: CharacterMemoryCapsuleRepository,
        transaction: CharacterRevivalTransaction,
        builder: com.companion.cc.domain.character.CharacterMemoryCapsuleBuilder,
        decryptor: CharacterMemoryCapsuleDecryptor
    ): CharacterRevivalService = CharacterRevivalService(
        capsules = capsules,
        transaction = transaction,
        capsuleBuilder = builder,
        decryptor = decryptor
    )

    @Provides
    @Singleton
    fun provideCharacterMemoryCapsuleBuilder(
        gson: Gson
    ): com.companion.cc.domain.character.CharacterMemoryCapsuleBuilder =
        com.companion.cc.domain.character.CharacterMemoryCapsuleBuilder(gson)

    @Provides
    @Singleton
    fun provideCharacterDeletionService(
        characterRepository: com.companion.cc.domain.repository.CustomCharacterRepository,
        transaction: CharacterDeletionTransaction,
        builder: com.companion.cc.domain.character.CharacterMemoryCapsuleBuilder,
        tokenSource: CharacterMemoryCapsuleTokenSource,
        cipher: CharacterMemoryCapsuleCipher,
        memorySnapshotProvider: com.companion.cc.domain.character.CharacterMemoryCapsuleMemoryProvider
    ): com.companion.cc.domain.character.CharacterDeletionService =
        com.companion.cc.domain.character.CharacterDeletionService(
            characterRepository = characterRepository,
            transaction = transaction,
            capsuleBuilder = builder,
            tokenSource = tokenSource,
            cipher = cipher,
            memorySnapshotProvider = memorySnapshotProvider
        )

    @Provides
    @Singleton
    fun provideSystemPromptManager(
        @ApplicationContext context: Context
    ): SystemPromptManager = SystemPromptManager(context)

    @Provides
    @Singleton
    fun provideMuseColdnessEngine(): MuseColdnessEngine = MuseColdnessEngine()

    @Provides
    @Singleton
    fun provideXiaoCanWarmthEngine(): XiaoCanWarmthEngine = XiaoCanWarmthEngine()

    @Provides
    @Singleton
    fun provideMoodStatePersistence(
        database: AppDatabase
    ): MoodStatePersistence = MoodStatePersistence(database)

    @Provides
    @Singleton
    fun provideCompanionStateManager(
        persistence: MoodStatePersistence,
        museEngine: MuseColdnessEngine,
        xiaoCanEngine: XiaoCanWarmthEngine
    ): CompanionStateManager = CompanionStateManager(
        persistence, museEngine, xiaoCanEngine
    )

    @Provides
    @Singleton
    fun provideAIProviderManager(
        settingsManager: SettingsManager,
        okHttpClient: OkHttpClient,
        gson: Gson
    ): AIProviderManager = AIProviderManager(settingsManager, okHttpClient, gson)
}
