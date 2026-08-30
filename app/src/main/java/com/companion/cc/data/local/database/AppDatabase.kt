package com.companion.cc.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.companion.cc.data.local.dao.CustomCharacterDao
import com.companion.cc.data.local.dao.CharacterCleanupTaskDao
import com.companion.cc.data.local.dao.CharacterMemoryCapsuleDao
import com.companion.cc.data.local.dao.ScheduleDao
import com.companion.cc.data.local.dao.MemoryScopeQuarantineDao
import com.companion.cc.data.local.dao.InteractionTimeDao
import com.companion.cc.data.local.dao.MemoryDao
import com.companion.cc.data.local.dao.StatsDao
import com.companion.cc.data.local.dao.UserEventDao
import com.companion.cc.data.local.dao.UserProfileDao
import com.companion.cc.data.local.dao.VectorMemoryDao
import com.companion.cc.data.local.entity.CustomCharacterEntity
import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.data.local.entity.ScheduleEntity
import com.companion.cc.data.local.entity.InteractionTimeEntity
import com.companion.cc.data.local.entity.MemoryEntity
import com.companion.cc.data.local.entity.MessageEntity
import com.companion.cc.data.local.entity.UserEventEntity
import com.companion.cc.data.local.entity.UserProfileEntity
import com.companion.cc.data.local.entity.VectorMemoryConverters
import com.companion.cc.data.local.entity.VectorMemoryEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import com.companion.cc.data.local.entity.MemoryScopeQuarantineEntity
import com.companion.cc.domain.usecase.MoodStateDao
import com.companion.cc.domain.usecase.MoodStateEntity

@Database(
    entities = [
        MessageEntity::class,
        MemoryEntity::class,
        UserProfileEntity::class,
        MoodStateEntity::class,
        VectorMemoryEntity::class,
        com.companion.cc.data.local.entity.TagEntity::class,
        com.companion.cc.data.local.entity.MessageTagEntity::class,
        InteractionTimeEntity::class,
        UserEventEntity::class,
        CustomCharacterEntity::class,
        CharacterMemoryCapsuleEntity::class,
        CharacterCleanupTaskEntity::class,
        ScheduleEntity::class,
        MemorySourceEntity::class,
        MemoryReviewEntity::class,
        MemoryNodeEntity::class,
        MemoryEvidenceEntity::class,
        MemoryVersionEntity::class,
        MemoryRelationEntity::class,
        MemoryRetrievalTraceEntity::class,
        MemoryRetrievalFeedbackEntity::class,
        MemoryScopeQuarantineEntity::class
    ],
    version = 15,  // 隔离缺少用户身份的旧 Memory 2.0 scope
)
@TypeConverters(VectorMemoryConverters::class, AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): com.companion.cc.data.local.dao.MessageDao
    abstract fun statsDao(): StatsDao
    abstract fun memoryDao(): MemoryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun moodStateDao(): MoodStateDao
    abstract fun vectorMemoryDao(): VectorMemoryDao
    abstract fun tagDao(): com.companion.cc.data.local.dao.TagDao
    abstract fun interactionTimeDao(): InteractionTimeDao
    abstract fun userEventDao(): UserEventDao
    abstract fun customCharacterDao(): CustomCharacterDao
    abstract fun characterMemoryCapsuleDao(): CharacterMemoryCapsuleDao
    abstract fun characterCleanupTaskDao(): CharacterCleanupTaskDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun memorySourceDao(): com.companion.cc.data.local.dao.MemorySourceDao
    abstract fun memoryReviewDao(): com.companion.cc.data.local.dao.MemoryReviewDao
    abstract fun memoryNodeDao(): com.companion.cc.data.local.dao.MemoryNodeDao
    abstract fun memoryEvidenceDao(): com.companion.cc.data.local.dao.MemoryEvidenceDao
    abstract fun memoryVersionDao(): com.companion.cc.data.local.dao.MemoryVersionDao
    abstract fun memoryRelationDao(): com.companion.cc.data.local.dao.MemoryRelationDao
    abstract fun memoryRetrievalDao(): com.companion.cc.data.local.dao.MemoryRetrievalDao
    abstract fun memoryScopeQuarantineDao(): MemoryScopeQuarantineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cc_database"
                )
                    .addMigrations(
                        APP_MIGRATION_1_2,
                        APP_MIGRATION_2_3,
                        APP_MIGRATION_3_4,
                        APP_MIGRATION_4_5,
                        APP_MIGRATION_5_6,
                        APP_MIGRATION_6_7,
                        APP_MIGRATION_7_8,
                        APP_MIGRATION_8_9,
                        APP_MIGRATION_9_10,
                        APP_MIGRATION_10_11,
                        APP_MIGRATION_11_12,
                        APP_MIGRATION_12_13,
                        APP_MIGRATION_13_14,
                        APP_MIGRATION_14_15
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

