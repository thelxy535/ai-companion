package com.companion.cc.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.companion.cc.data.local.dao.CustomCharacterDao
import com.companion.cc.data.local.dao.InteractionTimeDao
import com.companion.cc.data.local.dao.MemoryDao
import com.companion.cc.data.local.dao.StatsDao
import com.companion.cc.data.local.dao.UserEventDao
import com.companion.cc.data.local.dao.UserProfileDao
import com.companion.cc.data.local.dao.VectorMemoryDao
import com.companion.cc.data.local.entity.CustomCharacterEntity
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
        MemorySourceEntity::class,
        MemoryReviewEntity::class,
        MemoryNodeEntity::class,
        MemoryEvidenceEntity::class,
        MemoryVersionEntity::class
    ],
    version = 11,  // 鏂板锛氳嚜瀹氫箟瑙掕壊琛?    exportSchema = false
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
    abstract fun memorySourceDao(): com.companion.cc.data.local.dao.MemorySourceDao
    abstract fun memoryReviewDao(): com.companion.cc.data.local.dao.MemoryReviewDao
    abstract fun memoryNodeDao(): com.companion.cc.data.local.dao.MemoryNodeDao
    abstract fun memoryEvidenceDao(): com.companion.cc.data.local.dao.MemoryEvidenceDao
    abstract fun memoryVersionDao(): com.companion.cc.data.local.dao.MemoryVersionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cc_database"  // 浣跨敤涓庤澶囦竴鑷寸殑鏁版嵁搴撳悕绉?                )
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
                        APP_MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
