package com.companion.cc.di

import android.content.Context
import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.domain.audio.VoiceSpeechRecognizer
import com.companion.cc.domain.audio.VoiceTTSEngine
import com.companion.cc.domain.engine.EmotionalEngine
import com.companion.cc.domain.manager.*
import com.companion.cc.domain.repository.MessageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 新功能依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object NewFeaturesModule {

    @Provides
    @Singleton
    fun provideCompanionConfigLoader(
        @ApplicationContext context: Context,
        gson: com.google.gson.Gson
    ): CompanionConfigLoader {
        return CompanionConfigLoader(context, gson)
    }

    @Provides
    @Singleton
    fun provideVectorMemoryManager(): VectorMemoryManager {
        return VectorMemoryManager()
    }

    @Provides
    @Singleton
    fun provideMidTermMemoryManager(
        messageRepository: MessageRepository
    ): MidTermMemoryManager {
        return MidTermMemoryManager(messageRepository)
    }

    @Provides
    @Singleton
    fun provideMemoryLayerManager(
        messageRepository: MessageRepository,
        vectorMemoryManager: VectorMemoryManager,
        midTermMemoryManager: MidTermMemoryManager,
        personalityManager: PersonalityManager
    ): MemoryLayerManager {
        return MemoryLayerManager(
            messageRepository = messageRepository,
            vectorMemoryManager = vectorMemoryManager,
            midTermMemoryManager = midTermMemoryManager,
            personalityManager = personalityManager
        )
    }

    @Provides
    @Singleton
    fun provideEmotionalEngine(): EmotionalEngine {
        return EmotionalEngine()
    }

    // PersonalityManager 使用 @Inject 构造函数注入，不需要手动提供

    @Provides
    @Singleton
    fun provideContextManager(): ContextManager {
        return ContextManager()
    }

    @Provides
    @Singleton
    fun provideVoiceSpeechRecognizer(
        @ApplicationContext context: Context
    ): VoiceSpeechRecognizer {
        return VoiceSpeechRecognizer(context)
    }

    @Provides
    @Singleton
    fun provideVoiceTTSEngine(
        @ApplicationContext context: Context
    ): VoiceTTSEngine {
        return VoiceTTSEngine(context)
    }

    @Provides
    @Singleton
    fun provideVoiceManager(
        speechRecognizer: VoiceSpeechRecognizer,
        ttsEngine: VoiceTTSEngine
    ): VoiceManager {
        return VoiceManager(speechRecognizer, ttsEngine)
    }
}
