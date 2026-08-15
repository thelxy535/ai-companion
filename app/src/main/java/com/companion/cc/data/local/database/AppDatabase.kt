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
        CustomCharacterEntity::class
    ],
    version = 10,  // 新增：自定义角色表
    exportSchema = false
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cc_database"  // 使用与设备一致的数据库名称
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
                        APP_MIGRATION_9_10
                    )
                    .fallbackToDestructiveMigration()  // 如果迁移失败，重建数据库
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
