package com.companion.cc.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * AppDatabase 数据库迁移策略
 * 保护用户数据，避免 fallbackToDestructiveMigration 清空数据
 */

/**
 * 迁移 1 -> 2：添加 MoodStateEntity 表
 */
val APP_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS mood_states (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                companionId TEXT NOT NULL,
                coldness INTEGER NOT NULL DEFAULT 0,
                perfunctoryCount INTEGER NOT NULL DEFAULT 0,
                deepTopicCount INTEGER NOT NULL DEFAULT 0,
                warmth INTEGER,
                careLevel INTEGER,
                negativeCount INTEGER,
                positiveEventCount INTEGER,
                relationshipLevel INTEGER NOT NULL DEFAULT 1,
                lastInteractionTime INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}

/**
 * 迁移 2 -> 3：添加 VectorMemoryEntity 表
 */
val APP_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS vector_memories (
                id TEXT PRIMARY KEY NOT NULL,
                content TEXT NOT NULL,
                embedding TEXT NOT NULL,
                type TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                userId TEXT NOT NULL,
                companionId TEXT NOT NULL,
                metadata TEXT NOT NULL
            )
        """)

        // 创建索引以提升查询性能
        database.execSQL("CREATE INDEX IF NOT EXISTS index_vector_memories_userId_companionId ON vector_memories(userId, companionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_vector_memories_timestamp ON vector_memories(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_vector_memories_type ON vector_memories(type)")
    }
}

/**
 * 迁移 3 -> 4：添加消息重要性和动作字段
 */
val APP_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加 importance 字段
        database.execSQL("ALTER TABLE messages ADD COLUMN importance INTEGER NOT NULL DEFAULT 50")

        // 添加 action 字段
        database.execSQL("ALTER TABLE messages ADD COLUMN action TEXT")

        // 创建索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_importance ON messages(importance)")
    }
}

/**
 * 迁移 4 -> 5：添加收藏字段
 */
val APP_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加 is_favorited 字段
        database.execSQL("ALTER TABLE messages ADD COLUMN is_favorited INTEGER NOT NULL DEFAULT 0")

        // 创建索引以加速收藏查询
        database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_is_favorited ON messages(is_favorited)")
    }
}

/**
 * 迁移 5 -> 6：添加图片支持字段（多模态）
 */
val APP_MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加 image_url 字段
        database.execSQL("ALTER TABLE messages ADD COLUMN image_url TEXT")

        // 添加 image_analysis 字段
        database.execSQL("ALTER TABLE messages ADD COLUMN image_analysis TEXT")
    }
}

/**
 * 迁移 6 -> 7：添加标签系统
 */
val APP_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 创建 tags 表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS tags (
                id TEXT PRIMARY KEY NOT NULL,
                user_id TEXT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#2196F3',
                created_at INTEGER NOT NULL
            )
        """)

        // 创建 message_tags 关联表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS message_tags (
                message_id TEXT NOT NULL,
                tag_id TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY(message_id, tag_id)
            )
        """)

        // 创建索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_tags_user_id ON tags(user_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_message_tags_message_id ON message_tags(message_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_message_tags_tag_id ON message_tags(tag_id)")
    }
}

/**
 * 迁移 7 -> 8：添加真实感知系统（时间上下文和事件追踪）
 */
val APP_MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 创建 interaction_times 表（交互时间记录）
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS interaction_times (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId TEXT NOT NULL,
                companionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                messageCount INTEGER NOT NULL DEFAULT 1,
                sessionId TEXT NOT NULL
            )
        """)

        // 创建 user_events 表（用户事件追踪）
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_events (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                companionId TEXT NOT NULL,
                eventType TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                metadata TEXT NOT NULL DEFAULT '{}'
            )
        """)

        // 创建索引以提高查询性能
        database.execSQL("CREATE INDEX IF NOT EXISTS index_interaction_times_user_companion ON interaction_times(userId, companionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_interaction_times_timestamp ON interaction_times(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_events_user_companion ON user_events(userId, companionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_events_timestamp ON user_events(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_events_type ON user_events(eventType)")
    }
}

/**
 * 迁移 8 -> 9：数据库统一和类型转换器完善
 *
 * 此迁移确保从 cc_companion_database (v8) 或 cc_database (v7) 统一到 cc_database (v9)
 * 使用 PRAGMA table_info 检查现有列，只添加缺失的列
 *
 * 注意：如果从 CCDatabase v7 直接升级，需要先经过 v8 的表创建
 */
val APP_MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 注意：这个迁移假设已经从 7->8，所有 v8 的表都已存在
        // 如果不存在，说明配置有问题

        // 检查并补齐 messages 表的所有必要列
        val cursor = database.query("PRAGMA table_info(messages)")
        val existingColumns = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndex("name"))
            existingColumns.add(columnName)
        }
        cursor.close()

        // 补齐可能缺失的列
        if ("importance" !in existingColumns) {
            database.execSQL("ALTER TABLE messages ADD COLUMN importance INTEGER NOT NULL DEFAULT 50")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_importance ON messages(importance)")
        }
        if ("action" !in existingColumns) {
            database.execSQL("ALTER TABLE messages ADD COLUMN action TEXT")
        }
        if ("is_favorited" !in existingColumns) {
            database.execSQL("ALTER TABLE messages ADD COLUMN is_favorited INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_is_favorited ON messages(is_favorited)")
        }
        if ("image_url" !in existingColumns) {
            database.execSQL("ALTER TABLE messages ADD COLUMN image_url TEXT")
        }
        if ("image_analysis" !in existingColumns) {
            database.execSQL("ALTER TABLE messages ADD COLUMN image_analysis TEXT")
        }
    }
}

/**
 * 迁移 9 -> 10：添加自定义角色表
 *
 * 新增功能：角色自定义
 * - 用户可以创建自己的AI角色
 * - 支持人格特质、背景故事、语音配置等
 */
val APP_MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS custom_characters (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                name TEXT NOT NULL,
                avatar TEXT,
                description TEXT NOT NULL,
                personality TEXT NOT NULL,
                backstory TEXT NOT NULL,
                greetingMessage TEXT NOT NULL,
                exampleDialogues TEXT NOT NULL,
                voiceConfig TEXT,
                behaviorRules TEXT,
                isCustom INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_custom_characters_userId
            ON custom_characters(userId)
        """)

        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_custom_characters_createdAt
            ON custom_characters(createdAt)
        """)
    }
}

/**
 * Migration 10 -> 11: Memory 2.0 source, review, node, evidence and version tables.
 * Existing message, legacy memory and vector tables are intentionally untouched.
 */
val APP_MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS memory_sources (
                id TEXT NOT NULL PRIMARY KEY,
                scopeKey TEXT NOT NULL,
                messageId TEXT,
                contentSnapshot TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                occurredAt INTEGER NOT NULL,
                contentHash TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_sources_scopeKey_occurredAt ON memory_sources(scopeKey, occurredAt)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_memory_sources_scopeKey_contentHash ON memory_sources(scopeKey, contentHash)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS memory_reviews (
                id TEXT NOT NULL PRIMARY KEY,
                scopeKey TEXT NOT NULL,
                kind TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                confidence REAL NOT NULL,
                status TEXT NOT NULL DEFAULT 'pending',
                sourceIdsJson TEXT NOT NULL DEFAULT '[]',
                proposalHash TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                resolvedAt INTEGER,
                resolutionNote TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_reviews_scopeKey_status_createdAt ON memory_reviews(scopeKey, status, createdAt)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_memory_reviews_scopeKey_proposalHash ON memory_reviews(scopeKey, proposalHash)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS memory_nodes (
                id TEXT NOT NULL PRIMARY KEY,
                scopeKey TEXT NOT NULL,
                kind TEXT NOT NULL,
                subjectRole TEXT NOT NULL,
                subjectKey TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                importance INTEGER NOT NULL DEFAULT 50,
                confidence REAL NOT NULL DEFAULT 0.5,
                validFrom INTEGER NOT NULL,
                validUntil INTEGER,
                status TEXT NOT NULL DEFAULT 'active',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                currentVersion INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_nodes_scopeKey_status_updatedAt ON memory_nodes(scopeKey, status, updatedAt)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_nodes_scopeKey_kind_createdAt ON memory_nodes(scopeKey, kind, createdAt)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS memory_evidence (
                nodeId TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                evidenceRole TEXT NOT NULL DEFAULT 'support',
                confidence REAL NOT NULL DEFAULT 0.5,
                summarySnapshot TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(nodeId, sourceId),
                FOREIGN KEY(nodeId) REFERENCES memory_nodes(id) ON DELETE CASCADE,
                FOREIGN KEY(sourceId) REFERENCES memory_sources(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_evidence_sourceId ON memory_evidence(sourceId)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS memory_versions (
                nodeId TEXT NOT NULL,
                version INTEGER NOT NULL,
                kind TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                importance INTEGER NOT NULL,
                confidence REAL NOT NULL,
                validFrom INTEGER NOT NULL,
                validUntil INTEGER,
                changeReason TEXT NOT NULL,
                actor TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(nodeId, version),
                FOREIGN KEY(nodeId) REFERENCES memory_nodes(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_versions_nodeId ON memory_versions(nodeId)")
    }
}
