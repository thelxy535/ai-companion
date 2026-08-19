package com.companion.cc.di

import android.content.Context
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.remote.api.SiliconFlowApi
import com.companion.cc.domain.manager.AIProviderManager
import com.companion.cc.domain.manager.CompanionStateManager
import com.companion.cc.domain.manager.SystemPromptManager
import com.companion.cc.domain.usecase.*
import com.companion.cc.data.local.repository.MemoryRepository
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
