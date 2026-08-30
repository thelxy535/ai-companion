package com.companion.cc.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.data.repository.RoomCharacterRevivalTransaction
import com.companion.cc.domain.memory.MemoryCapsuleNode
import com.companion.cc.domain.memory.MemoryCapsuleRelation
import com.companion.cc.domain.memory.MemoryCapsuleSource
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.google.gson.JsonParser

class CharacterCapsuleMigrationInstrumentedTest {

    @Test
    fun revivalRewritesRelationEvidenceUsingSourceMapping() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val oldScope = "user:user-a:companion:old-character"
            val newCharacter = character("new-character")
            database.characterMemoryCapsuleDao().insert(
                CharacterMemoryCapsuleEntity(
                    id = "capsule-1",
                    userId = "user-a",
                    sourceCharacterId = "old-character",
                    schemaVersion = 2,
                    revivalTokenHash = "hash-1",
                    encryptedRevivalToken = "token",
                    encryptedPayload = "payload",
                    createdAt = 1L,
                    deletedAt = 2L
                )
            )

            val snapshot = MemoryCapsuleV2(
                scopeKey = oldScope,
                sources = listOf(
                    MemoryCapsuleSource(
                        id = "shared-id",
                        scopeKey = oldScope,
                        messageId = "old-message",
                        contentSnapshot = "A source",
                        sourceType = "conversation",
                        occurredAt = 1L,
                        contentHash = "hash-source",
                        createdAt = 1L
                    )
                ),
                nodes = listOf(
                    MemoryCapsuleNode(
                        id = "shared-id",
                        scopeKey = oldScope,
                        title = "A node",
                        content = "A memory",
                        validFrom = 1L,
                        createdAt = 1L,
                        updatedAt = 1L
                    )
                ),
                relations = listOf(
                    MemoryCapsuleRelation(
                        fromNodeId = "shared-id",
                        toNodeId = "shared-id",
                        relationType = "supports",
                        scopeKey = oldScope,
                        evidenceJson = "[\"shared-id\"]",
                        createdAt = 1L,
                        updatedAt = 1L
                    )
                )
            )

            val transaction = RoomCharacterRevivalTransaction(database)
            assertTrue(
                transaction.reviveAtomically(
                    capsule = database.characterMemoryCapsuleDao().findById("user-a", "capsule-1")!!,
                    character = newCharacter,
                    restoredAt = 3L,
                    memorySnapshot = snapshot
                ).not()
            )
            assertEquals(
                null,
                database.customCharacterDao().getCharacterByIdForUser("new-character", "user-a")
            )
            val sourceOnlySnapshot = snapshot.copy(
                nodes = snapshot.nodes.map { it.copy(id = "node-1") },
                relations = snapshot.relations.map {
                    it.copy(fromNodeId = "node-1", toNodeId = "node-1")
                }
            )
            assertTrue(
                transaction.reviveAtomically(
                    capsule = database.characterMemoryCapsuleDao().findById("user-a", "capsule-1")!!,
                    character = newCharacter,
                    restoredAt = 3L,
                    memorySnapshot = sourceOnlySnapshot
                )
            )

            val newScope = "user:user-a:companion:new-character"
            val source = database.memorySourceDao().findAllInScope(newScope).single()
            val node = database.memoryNodeDao().findAllInScope(newScope).single()
            val relation = database.memoryRelationDao().findAllInScope(newScope).single()
            val evidenceId = JsonParser().parse(relation.evidenceJson).asJsonArray.single().asString

            assertEquals(source.id, evidenceId)
            assertEquals(null, source.messageId)
            assertNotEquals(node.id, evidenceId)
        } finally {
            database.close()
        }
    }

    private fun character(id: String) = CustomCharacter(
        id = id,
        userId = "user-a",
        name = "Moss",
        avatar = null,
        description = "quiet",
        personality = PersonalityTraits.default(),
        backstory = "story",
        greetingMessage = "hello",
        exampleDialogues = emptyList(),
        voiceConfig = null,
        behaviorRules = null
    )

    @Test
    fun migration12To13PreservesExistingRowsAndCreatesCapsuleTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-12-to-13-test.db"
        context.deleteDatabase(databaseName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        val version12Callback = object : SupportSQLiteOpenHelper.Callback(12) {
            override fun onCreate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE custom_characters (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL)"
                )
                database.execSQL(
                    "INSERT INTO custom_characters (id, userId) VALUES ('existing-character', 'user-a')"
                )
            }

            override fun onUpgrade(
                database: SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
            ) = Unit
        }
        val version12Helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(version12Callback)
                .build()
        )
        version12Helper.writableDatabase.close()
        version12Helper.close()

        val version13Callback = object : SupportSQLiteOpenHelper.Callback(13) {
            override fun onCreate(database: SupportSQLiteDatabase) = Unit

            override fun onUpgrade(
                database: SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
            ) {
                if (oldVersion == 12 && newVersion == 13) {
                    APP_MIGRATION_12_13.migrate(database)
                }
            }
        }
        val version13Helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(version13Callback)
                .build()
        )

        try {
            val migratedDatabase = version13Helper.writableDatabase
            val preserved = migratedDatabase.query(
                "SELECT userId FROM custom_characters WHERE id = 'existing-character'"
            )
            assertTrue(preserved.moveToFirst())
            assertEquals("user-a", preserved.getString(0))
            preserved.close()

            val tables = migratedDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            )
            val tableNames = buildSet {
                while (tables.moveToNext()) add(tables.getString(0))
            }
            tables.close()
            assertTrue("character_memory_capsules" in tableNames)
            assertTrue("character_cleanup_tasks" in tableNames)
        } finally {
            version13Helper.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migration13To14QuarantinesLegacyScopesWithoutDeletingRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-13-to-14-test.db"
        context.deleteDatabase(databaseName)
        val factory = FrameworkSQLiteOpenHelperFactory()

        val version13Callback = object : SupportSQLiteOpenHelper.Callback(13) {
            override fun onCreate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE memory_sources (id TEXT PRIMARY KEY NOT NULL, scopeKey TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE memory_reviews (id TEXT PRIMARY KEY NOT NULL, scopeKey TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE memory_nodes (id TEXT PRIMARY KEY NOT NULL, scopeKey TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE memory_relations (fromNodeId TEXT NOT NULL, toNodeId TEXT NOT NULL, relationType TEXT NOT NULL, scopeKey TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE memory_retrieval_traces (id TEXT PRIMARY KEY NOT NULL, scopeKey TEXT NOT NULL)"
                )
                database.execSQL(
                    "INSERT INTO memory_nodes (id, scopeKey) VALUES " +
                        "('legacy-node', 'companion:character-1'), " +
                        "('owned-node', 'user:user-a:companion:character-1')"
                )
            }

            override fun onUpgrade(
                database: SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
            ) = Unit
        }
        val version13Helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(version13Callback)
                .build()
        )
        version13Helper.writableDatabase.close()
        version13Helper.close()

        val version14Callback = object : SupportSQLiteOpenHelper.Callback(14) {
            override fun onCreate(database: SupportSQLiteDatabase) = Unit

            override fun onUpgrade(
                database: SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
            ) {
                if (oldVersion == 13 && newVersion == 14) {
                    APP_MIGRATION_13_14.migrate(database)
                }
            }
        }
        val version14Helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(version14Callback)
                .build()
        )

        try {
            val migratedDatabase = version14Helper.writableDatabase
            val quarantined = migratedDatabase.query(
                "SELECT resourceId, legacyScopeKey FROM memory_scope_quarantine " +
                    "WHERE resourceType = 'node'"
            )
            assertTrue(quarantined.moveToFirst())
            assertEquals("legacy-node", quarantined.getString(0))
            assertEquals("companion:character-1", quarantined.getString(1))
            quarantined.close()

            val nodeCount = migratedDatabase.query("SELECT COUNT(*) FROM memory_nodes")
            assertTrue(nodeCount.moveToFirst())
            assertEquals(2, nodeCount.getInt(0))
            nodeCount.close()
        } finally {
            version14Helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
