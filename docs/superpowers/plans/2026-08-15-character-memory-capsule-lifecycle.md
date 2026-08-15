# Character Memory Capsule Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permanently delete a custom character and all character-scoped runtime data while optionally preserving an encrypted, exportable memory capsule that can revive a new UUID without restoring old chat history.

**Architecture:** Room 11 adds append-only capsule and cleanup-task tables through an explicit 10→11 migration. Deletion has two boundaries: one Room transaction atomically stores the capsule, removes runtime rows, and records pending external cleanup; a retryable idempotent processor then clears DataStore/files/in-memory state. Revival validates the token, decrypts the payload, creates a new custom-character UUID, optionally copies selected memories into the new UUID partition, and marks the source capsule restored in one transaction.

**Tech Stack:** Kotlin, Android SDK 34/minSdk 26, Room 2.6.1 with `room-testing`, Hilt, Coroutines/Flow, Gson, Android Keystore AES-GCM through `EncryptionHelper`, Jetpack Compose, Android Storage Access Framework, JUnit 4, AndroidX Test.

## Global Constraints

- Execute this plan only after `2026-08-15-custom-character-data-flow.md` passes its automated and emulator gate.
- Keep `versionCode = 10` and `versionName = "2.1.0-beta.9"` unchanged.
- Migration 10→11 may create new tables and indexes only; it must not rebuild, drop, truncate, or alter existing application tables.
- Do not use `fallbackToDestructiveMigration()` to make a failed 10→11 migration appear successful.
- A capsule is not a runnable character and never appears in `CharacterCatalog`.
- A capsule stores role configuration, relationship summary, selected memories, and statistics; complete chat history is excluded.
- Capsule payload and stored revival token use the existing Android Keystore encryption capability. The database stores only a SHA-256 token hash for validation.
- Export requires an explicit privacy warning and a user-selected destination through the Storage Access Framework.
- Revival always creates a fresh UUID. It never reuses `sourceCharacterId` and never moves old message rows.
- `REINTRODUCTION` restores configuration and relationship summary only. `WITH_MEMORIES` additionally copies selected memory snapshots into the new UUID partition.
- Invalid, damaged, or already-used tokens write no custom character, memory, or capsule status change.
- Room rollback claims apply only to Room rows. Files, DataStore, and in-memory caches are cleaned after commit by an idempotent persisted task.
- Commit steps are executed only after the user explicitly authorizes commits.

---

## File Structure

### New database files

- `app/src/main/java/com/companion/cc/data/local/entity/CharacterMemoryCapsuleEntity.kt`
- `app/src/main/java/com/companion/cc/data/local/entity/CharacterCleanupTaskEntity.kt`
- `app/src/main/java/com/companion/cc/data/local/dao/CharacterMemoryCapsuleDao.kt`
- `app/src/main/java/com/companion/cc/data/local/dao/CharacterCleanupTaskDao.kt`

### New domain/data files

- `app/src/main/java/com/companion/cc/domain/model/CharacterMemoryCapsule.kt`
- `app/src/main/java/com/companion/cc/domain/character/CharacterMemoryCapsuleRepository.kt`
- `app/src/main/java/com/companion/cc/data/character/RoomCharacterMemoryCapsuleRepository.kt`
- `app/src/main/java/com/companion/cc/domain/character/CapsuleCipher.kt`
- `app/src/main/java/com/companion/cc/data/character/AndroidCapsuleCipher.kt`
- `app/src/main/java/com/companion/cc/domain/character/CharacterCapsuleBuilder.kt`
- `app/src/main/java/com/companion/cc/domain/character/CharacterDeletionService.kt`
- `app/src/main/java/com/companion/cc/data/character/DefaultCharacterDeletionService.kt`
- `app/src/main/java/com/companion/cc/domain/character/CharacterCleanupProcessor.kt`
- `app/src/main/java/com/companion/cc/data/character/DefaultCharacterCleanupProcessor.kt`
- `app/src/main/java/com/companion/cc/domain/character/CharacterRevivalService.kt`
- `app/src/main/java/com/companion/cc/data/character/DefaultCharacterRevivalService.kt`
- `app/src/main/java/com/companion/cc/domain/manager/CharacterRuntimeStore.kt`
- `app/src/main/java/com/companion/cc/data/character/CapsuleExportService.kt`

### New UI files

- `app/src/main/java/com/companion/cc/ui/character/CharacterFarewellViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/character/CharacterFarewellScreen.kt`
- `app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultScreen.kt`

### Existing files to modify

- `app/build.gradle.kts`
- `app/src/main/java/com/companion/cc/data/local/database/AppDatabase.kt`
- `app/src/main/java/com/companion/cc/data/local/database/AppDatabaseMigrations.kt`
- `app/src/main/java/com/companion/cc/data/local/dao/MessageDao.kt`
- `app/src/main/java/com/companion/cc/data/local/dao/TagDao.kt`
- `app/src/main/java/com/companion/cc/data/local/dao/VectorMemoryDao.kt`
- `app/src/main/java/com/companion/cc/domain/usecase/MoodStatePersistence.kt`
- `app/src/main/java/com/companion/cc/data/local/SettingsManager.kt`
- `app/src/main/java/com/companion/cc/domain/manager/MemoryLayerManager.kt`
- `app/src/main/java/com/companion/cc/domain/manager/CharacterCustomizationManager.kt`
- `app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/character/CharacterListScreen.kt`
- `app/src/main/java/com/companion/cc/ui/navigation/Screen.kt`
- `app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/companion/cc/di/AppModule.kt`
- `app/src/main/java/com/companion/cc/di/RepositoryModule.kt`
- `app/src/main/java/com/companion/cc/CCApplication.kt`

### New tests

- `app/src/test/java/com/companion/cc/domain/character/CharacterCapsuleBuilderTest.kt`
- `app/src/test/java/com/companion/cc/data/character/RoomCharacterMemoryCapsuleRepositoryTest.kt`
- `app/src/test/java/com/companion/cc/ui/character/CharacterFarewellViewModelTest.kt`
- `app/src/androidTest/java/com/companion/cc/data/local/database/AppDatabaseMigration10To11Test.kt`
- `app/src/androidTest/java/com/companion/cc/data/character/CharacterDeletionTransactionTest.kt`
- `app/src/androidTest/java/com/companion/cc/data/character/CharacterRevivalTransactionTest.kt`

---

### Task 1: Enable Room Schema and Migration Testing

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/companion/cc/data/local/database/AppDatabase.kt`
- Create generated baseline: `app/schemas/com.companion.cc.data.local.database.AppDatabase/10.json`

**Interfaces:**
- Consumes: current Room version 10 schema.
- Produces: committed v10 schema JSON and `MigrationTestHelper` dependencies.

- [ ] **Step 1: Configure schema export without changing the database version**

Add to `app/build.gradle.kts`:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
```

Add to dependencies:

```kotlin
androidTestImplementation("androidx.room:room-testing:$roomVersion")
```

Change `AppDatabase` from `exportSchema = false` to `exportSchema = true`. Keep `version = 10` for this step.

- [ ] **Step 2: Generate and inspect the v10 schema**

```powershell
.\gradlew.bat :app:kspDebugKotlin --rerun-tasks --stacktrace
Get-ChildItem "app\schemas\com.companion.cc.data.local.database.AppDatabase"
```

Expected: `10.json` exists and lists all current tables, including `custom_characters`; no production database is opened or mutated.

- [ ] **Step 3: Run the pre-migration test gate**

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit schema-test setup if authorized**

```powershell
git add app/build.gradle.kts app/src/main/java/com/companion/cc/data/local/database/AppDatabase.kt app/schemas/com.companion.cc.data.local.database.AppDatabase/10.json
git commit -m "test: export Room v10 schema"
```

---

### Task 2: Define Capsule, Cleanup, Selection, and Revival Models

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/model/CharacterMemoryCapsule.kt`
- Create: `app/src/main/java/com/companion/cc/data/local/entity/CharacterMemoryCapsuleEntity.kt`
- Create: `app/src/main/java/com/companion/cc/data/local/entity/CharacterCleanupTaskEntity.kt`
- Create: `app/src/main/java/com/companion/cc/data/local/dao/CharacterMemoryCapsuleDao.kt`
- Create: `app/src/main/java/com/companion/cc/data/local/dao/CharacterCleanupTaskDao.kt`

**Interfaces:**
- Produces: versioned encrypted capsule rows, pending cleanup rows, selection options, previews, receipts, and revival modes.

- [ ] **Step 1: Add complete domain models**

```kotlin
package com.companion.cc.domain.model

import com.companion.cc.domain.model.CustomCharacter

enum class CharacterMemoryCapsuleStatus { AVAILABLE, RESTORED, ARCHIVED }
enum class CharacterCleanupStatus { PENDING, COMPLETE }
enum class CharacterRevivalMode { REINTRODUCTION, WITH_MEMORIES }

data class CapsuleSelection(
    val includeCharacterSettings: Boolean = true,
    val includeRelationshipSummary: Boolean = true,
    val selectedMemoryIds: Set<String> = emptySet(),
    val createRevivalToken: Boolean = true,
)

data class CapsuleMemorySnapshot(
    val sourceMemoryId: String,
    val content: String,
    val importance: Float,
    val topics: List<String>,
    val emotion: String?,
    val timestamp: Long,
)

data class CapsuleRelationshipSnapshot(
    val relationshipLevel: Int,
    val warmth: Int?,
    val careLevel: Int?,
    val coldness: Int,
    val lastInteractionTime: Long,
)

data class CapsuleInteractionStats(
    val messageCount: Int,
    val vectorMemoryCount: Int,
    val firstInteractionAt: Long?,
    val lastInteractionAt: Long?,
)

data class CharacterMemoryCapsulePayload(
    val schemaVersion: Int = 1,
    val sourceCharacter: CustomCharacter,
    val relationship: CapsuleRelationshipSnapshot?,
    val selectedMemories: List<CapsuleMemorySnapshot>,
    val stats: CapsuleInteractionStats,
)

data class CharacterMemoryCapsule(
    val id: String,
    val userId: String,
    val sourceCharacterId: String,
    val payload: CharacterMemoryCapsulePayload,
    val status: CharacterMemoryCapsuleStatus,
    val createdAt: Long,
    val deletedAt: Long,
    val restoredAt: Long?,
    val restoredCharacterId: String?,
    val exportedAt: Long?,
)

data class CharacterMemoryCapsulePreview(
    val characterName: String,
    val avatar: String?,
    val relationship: CapsuleRelationshipSnapshot?,
    val availableMemories: List<CapsuleMemorySnapshot>,
    val selectedMemories: List<CapsuleMemorySnapshot>,
    val stats: CapsuleInteractionStats,
)

data class DeletionReceipt(
    val characterId: String,
    val capsuleId: String?,
    val revivalToken: String?,
    val cleanupTaskId: String,
)

data class RevivalReceipt(
    val capsuleId: String,
    val characterId: String,
    val restoredMemoryCount: Int,
)
```

- [ ] **Step 2: Add Room entities**

```kotlin
@Entity(
    tableName = "character_memory_capsules",
    indices = [
        Index("userId"),
        Index(value = ["revivalTokenHash"], unique = true),
        Index("status"),
    ],
)
data class CharacterMemoryCapsuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceCharacterId: String,
    val schemaVersion: Int,
    val revivalTokenHash: String,
    val encryptedRevivalToken: String,
    val encryptedPayload: String,
    val status: String,
    val createdAt: Long,
    val deletedAt: Long,
    val restoredAt: Long?,
    val restoredCharacterId: String?,
    val exportedAt: Long?,
)
```

```kotlin
@Entity(
    tableName = "character_cleanup_tasks",
    indices = [Index("status"), Index(value = ["userId", "characterId"])],
)
data class CharacterCleanupTaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val characterId: String,
    val avatarReference: String?,
    val status: String,
    val attempts: Int,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 3: Add capsule DAO**

```kotlin
@Dao
interface CharacterMemoryCapsuleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CharacterMemoryCapsuleEntity)

    @Query("SELECT * FROM character_memory_capsules WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(userId: String, id: String): CharacterMemoryCapsuleEntity?

    @Query("SELECT * FROM character_memory_capsules WHERE revivalTokenHash = :tokenHash AND userId = :userId LIMIT 1")
    suspend fun getByTokenHash(userId: String, tokenHash: String): CharacterMemoryCapsuleEntity?

    @Query("SELECT * FROM character_memory_capsules WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<CharacterMemoryCapsuleEntity>>

    @Query("UPDATE character_memory_capsules SET status = :status, restoredAt = :restoredAt, restoredCharacterId = :characterId WHERE id = :id AND userId = :userId AND status = 'AVAILABLE'")
    suspend fun markRestored(userId: String, id: String, status: String, restoredAt: Long, characterId: String): Int

    @Query("UPDATE character_memory_capsules SET exportedAt = :exportedAt WHERE id = :id AND userId = :userId")
    suspend fun markExported(userId: String, id: String, exportedAt: Long): Int
}
```

- [ ] **Step 4: Add cleanup DAO**

```kotlin
@Dao
interface CharacterCleanupTaskDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CharacterCleanupTaskEntity)

    @Query("SELECT * FROM character_cleanup_tasks WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<CharacterCleanupTaskEntity>

    @Query("SELECT * FROM character_cleanup_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CharacterCleanupTaskEntity?

    @Query("UPDATE character_cleanup_tasks SET attempts = attempts + 1, lastError = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun recordFailure(id: String, error: String, updatedAt: Long): Int

    @Query("UPDATE character_cleanup_tasks SET status = 'COMPLETE', lastError = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markComplete(id: String, updatedAt: Long): Int
}
```

- [ ] **Step 5: Compile model and DAO declarations**

```powershell
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: compilation fails until Task 3 adds entities to `AppDatabase`; no production version is changed in this task's intermediate state.

- [ ] **Step 6: Commit if authorized together with Task 3**

Do not commit a database entity set that is absent from `AppDatabase`; commit Tasks 2 and 3 atomically after migration tests pass.

---

### Task 3: Add and Verify the Append-Only Room 10→11 Migration

**Files:**
- Modify: `app/src/main/java/com/companion/cc/data/local/database/AppDatabase.kt`
- Modify: `app/src/main/java/com/companion/cc/data/local/database/AppDatabaseMigrations.kt`
- Modify: `app/src/main/java/com/companion/cc/di/AppModule.kt`
- Create generated schema: `app/schemas/com.companion.cc.data.local.database.AppDatabase/11.json`
- Create: `app/src/androidTest/java/com/companion/cc/data/local/database/AppDatabaseMigration10To11Test.kt`

**Interfaces:**
- Consumes: Room v10 schema.
- Produces: Room v11 with capsule and cleanup DAOs and preserved existing rows.

- [ ] **Step 1: Write the migration test first**

```kotlin
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration10To11Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate10To11_preservesExistingRowsAndCreatesOnlyNewTables() {
        helper.createDatabase(TEST_DB, 10).apply {
            execSQL(
                "INSERT INTO custom_characters (id,userId,name,avatar,description,personality,backstory,greetingMessage,exampleDialogues,voiceConfig,behaviorRules,isCustom,createdAt,updatedAt) VALUES ('old-role','user-real','Old',NULL,'d','{}','b','g','[]',NULL,NULL,1,1,1)"
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 11, true, APP_MIGRATION_10_11).use { db ->
            db.query("SELECT count(*) FROM custom_characters WHERE id='old-role'").use {
                assertTrue(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
            db.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('character_memory_capsules','character_cleanup_tasks')").use {
                assertTrue(it.moveToFirst())
                assertEquals(2, it.getInt(0))
            }
        }
    }

    private companion object { const val TEST_DB = "migration-10-11" }
}
```

- [ ] **Step 2: Run the test and verify the missing-migration failure**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.companion.cc.data.local.database.AppDatabaseMigration10To11Test --stacktrace
```

Expected: compilation fails because `APP_MIGRATION_10_11` and schema 11 do not exist.

- [ ] **Step 3: Add the migration SQL**

```kotlin
val APP_MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS character_memory_capsules (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                sourceCharacterId TEXT NOT NULL,
                schemaVersion INTEGER NOT NULL,
                revivalTokenHash TEXT NOT NULL,
                encryptedRevivalToken TEXT NOT NULL,
                encryptedPayload TEXT NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                deletedAt INTEGER NOT NULL,
                restoredAt INTEGER,
                restoredCharacterId TEXT,
                exportedAt INTEGER
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_character_memory_capsules_userId ON character_memory_capsules(userId)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_character_memory_capsules_revivalTokenHash ON character_memory_capsules(revivalTokenHash)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_character_memory_capsules_status ON character_memory_capsules(status)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS character_cleanup_tasks (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                characterId TEXT NOT NULL,
                avatarReference TEXT,
                status TEXT NOT NULL,
                attempts INTEGER NOT NULL,
                lastError TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_character_cleanup_tasks_status ON character_cleanup_tasks(status)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_character_cleanup_tasks_userId_characterId ON character_cleanup_tasks(userId,characterId)")
    }
}
```

- [ ] **Step 4: Register entities, DAOs, version, and migration**

Set `version = 11`; append both new entities; add abstract DAO methods; append `APP_MIGRATION_10_11` after `APP_MIGRATION_9_10`. Add Hilt DAO providers in `AppModule`.

- [ ] **Step 5: Generate schema 11 and run migration test**

```powershell
.\gradlew.bat :app:kspDebugKotlin --rerun-tasks --stacktrace
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.companion.cc.data.local.database.AppDatabaseMigration10To11Test --stacktrace
```

Expected: `11.json` is generated; test passes; old custom row count remains 1.

- [ ] **Step 6: Audit migration SQL for destructive operations**

```powershell
rg -n 'DROP|DELETE FROM|ALTER TABLE|fallbackToDestructiveMigration' app/src/main/java/com/companion/cc/data/local/database/AppDatabaseMigrations.kt
```

Expected for `APP_MIGRATION_10_11`: no `DROP`, `DELETE`, or `ALTER`; the existing fallback line remains only in `AppDatabase.kt` for the later migration-safety phase and is not used by the explicit 10→11 test.

- [ ] **Step 7: Commit Tasks 2–3 if authorized**

```powershell
git add app/build.gradle.kts app/src/main/java/com/companion/cc/data/local app/src/main/java/com/companion/cc/domain/model/CharacterMemoryCapsule.kt app/src/main/java/com/companion/cc/di/AppModule.kt app/src/androidTest/java/com/companion/cc/data/local/database/AppDatabaseMigration10To11Test.kt app/schemas/com.companion.cc.data.local.database.AppDatabase/11.json
git commit -m "feat: add encrypted memory capsule schema"
```

---

### Task 4: Build and Encrypt Capsule Previews

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/character/CapsuleCipher.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/AndroidCapsuleCipher.kt`
- Create: `app/src/main/java/com/companion/cc/domain/character/CharacterCapsuleBuilder.kt`
- Modify: `app/src/main/java/com/companion/cc/data/local/dao/MessageDao.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/usecase/MoodStatePersistence.kt`
- Create: `app/src/test/java/com/companion/cc/domain/character/CharacterCapsuleBuilderTest.kt`

**Interfaces:**
- Consumes: scoped character, message statistics, mood row, and vector memories.
- Produces: deterministic preview and encrypted persisted payload input.

- [ ] **Step 1: Add snapshot queries**

Add to `MessageDao`:

```kotlin
@Query("SELECT * FROM messages WHERE user_id = :userId AND companion_id = :companionId ORDER BY timestamp ASC")
suspend fun getMessagesSnapshot(userId: String, companionId: String): List<MessageEntity>

@Query("SELECT COUNT(*) FROM messages WHERE user_id = :userId AND companion_id = :companionId")
suspend fun getMessageCount(userId: String, companionId: String): Int
```

Add to `MoodStateDao`:

```kotlin
@Query("SELECT * FROM mood_states WHERE userId = :userId AND companionId = :companionId LIMIT 1")
suspend fun getState(userId: String, companionId: String): MoodStateEntity?
```

- [ ] **Step 2: Define the cipher boundary**

```kotlin
interface CapsuleCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}
```

```kotlin
@Singleton
class AndroidCapsuleCipher @Inject constructor(
    private val encryptionHelper: EncryptionHelper,
) : CapsuleCipher {
    override fun encrypt(plainText: String): String =
        encryptionHelper.encrypt(plainText) ?: throw CapsuleEncryptionException()

    override fun decrypt(cipherText: String): String =
        encryptionHelper.decrypt(cipherText) ?: throw CapsuleDecryptionException()
}
```

Bind `CapsuleCipher` in `RepositoryModule`.

- [ ] **Step 3: Write builder tests**

Use mocks for scoped DAOs/repository. Prove: selected memory IDs are the only payload memories; full message content is not copied; stats contain count/range; preview writes nothing. Verify no DAO `insert`, `delete`, or `update` method is called.

- [ ] **Step 4: Implement CharacterCapsuleBuilder**

```kotlin
@Singleton
class CharacterCapsuleBuilder @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val characterRepository: CustomCharacterRepository,
    private val messageDao: MessageDao,
    private val vectorMemoryDao: VectorMemoryDao,
    private val moodStateDao: MoodStateDao,
) {
    suspend fun preview(characterId: String, selection: CapsuleSelection): CharacterMemoryCapsulePreview {
        val userId = currentUserProvider.requireUserId()
        val character = characterRepository.getCharacterById(userId, characterId)
            ?: throw UnknownCharacterException(characterId)
        val messages = messageDao.getMessagesSnapshot(userId, characterId)
        val vectors = vectorMemoryDao.getAllMemories(userId, characterId)
        val mood = moodStateDao.getState(userId, characterId)
        val memories = vectors.map { it.toCapsuleSnapshot() }
        val selected = memories.filter { it.sourceMemoryId in selection.selectedMemoryIds }
        return CharacterMemoryCapsulePreview(
            characterName = character.name,
            avatar = character.avatar,
            relationship = mood?.toCapsuleRelationship(),
            availableMemories = memories,
            selectedMemories = selected,
            stats = CapsuleInteractionStats(
                messageCount = messages.size,
                vectorMemoryCount = vectors.size,
                firstInteractionAt = messages.firstOrNull()?.timestamp,
                lastInteractionAt = messages.lastOrNull()?.timestamp,
            ),
        )
    }

    suspend fun buildPayload(characterId: String, selection: CapsuleSelection): CharacterMemoryCapsulePayload {
        val preview = preview(characterId, selection)
        val userId = currentUserProvider.requireUserId()
        val character = characterRepository.getCharacterById(userId, characterId)
            ?: throw UnknownCharacterException(characterId)
        return CharacterMemoryCapsulePayload(
            sourceCharacter = character,
            relationship = preview.relationship.takeIf { selection.includeRelationshipSummary },
            selectedMemories = preview.selectedMemories,
            stats = preview.stats,
        )
    }
}
```

Implement private pure entity mapping extensions in the same file.

- [ ] **Step 5: Run builder tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.domain.character.CharacterCapsuleBuilderTest" --stacktrace
```

Expected: `BUILD SUCCESSFUL`; preview is read-only and excludes unselected memory content.

- [ ] **Step 6: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/character/CapsuleCipher.kt app/src/main/java/com/companion/cc/data/character/AndroidCapsuleCipher.kt app/src/main/java/com/companion/cc/domain/character/CharacterCapsuleBuilder.kt app/src/main/java/com/companion/cc/data/local/dao/MessageDao.kt app/src/main/java/com/companion/cc/domain/usecase/MoodStatePersistence.kt app/src/main/java/com/companion/cc/di/RepositoryModule.kt app/src/test/java/com/companion/cc/domain/character/CharacterCapsuleBuilderTest.kt
git commit -m "feat: build encrypted character capsule payloads"
```

---

### Task 5: Implement Transactional Deletion and Retryable External Cleanup

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/character/CharacterDeletionService.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/DefaultCharacterDeletionService.kt`
- Create: `app/src/main/java/com/companion/cc/domain/manager/CharacterRuntimeStore.kt`
- Create: `app/src/main/java/com/companion/cc/domain/character/CharacterCleanupProcessor.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/DefaultCharacterCleanupProcessor.kt`
- Modify: `app/src/main/java/com/companion/cc/data/local/dao/MessageDao.kt`
- Modify: `app/src/main/java/com/companion/cc/data/local/dao/TagDao.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/usecase/MoodStatePersistence.kt`
- Modify: `app/src/main/java/com/companion/cc/data/local/SettingsManager.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/manager/MemoryLayerManager.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/CCApplication.kt`
- Create: `app/src/androidTest/java/com/companion/cc/data/character/CharacterDeletionTransactionTest.kt`

**Interfaces:**
- Produces: atomic Room deletion receipt and persisted external cleanup retries.

- [ ] **Step 1: Add scoped delete queries**

```kotlin
// MessageDao
@Query("DELETE FROM messages WHERE user_id = :userId AND companion_id = :companionId")
suspend fun deleteForCharacter(userId: String, companionId: String): Int

// TagDao — invoke before MessageDao.deleteForCharacter
@Query("DELETE FROM message_tags WHERE message_id IN (SELECT id FROM messages WHERE user_id = :userId AND companion_id = :companionId)")
suspend fun deleteLinksForCharacter(userId: String, companionId: String): Int

// MoodStateDao
@Query("DELETE FROM mood_states WHERE userId = :userId AND companionId = :companionId")
suspend fun deleteForCharacter(userId: String, companionId: String): Int
```

Existing VectorMemoryDao, InteractionTimeDao, and UserEventDao already expose scoped deletion; use those exact methods.

- [ ] **Step 2: Define deletion contract**

```kotlin
interface CharacterDeletionService {
    suspend fun previewCapsule(
        characterId: String,
        selection: CapsuleSelection,
    ): Result<CharacterMemoryCapsulePreview>

    suspend fun deletePermanently(
        characterId: String,
        selection: CapsuleSelection?,
    ): Result<DeletionReceipt>
}
```

- [ ] **Step 3: Add secure token helpers**

Generate 32 random bytes with `SecureRandom`, encode URL-safe Base64 without wrapping/padding, and hash UTF-8 token bytes with SHA-256. Persist only `tokenHash` plus the encrypted token.

- [ ] **Step 4: Write the transaction rollback test**

Build an in-memory `AppDatabase`, seed one row in every character-scoped table plus a custom role, and configure a test deletion service whose capsule DAO insert throws. Assert after `deletePermanently` that every original row still exists and no cleanup task exists. Add a success test asserting capsule+cleanup exist while character/messages/vector/mood/interactions/events/tag links are gone.

- [ ] **Step 5: Implement Room transaction deletion**

```kotlin
val result = runCatching {
    val userId = currentUserProvider.requireUserId()
    val character = customCharacterRepository.getCharacterById(userId, characterId)
        ?: throw UnknownCharacterException(characterId)
    val prepared = selection?.let { prepareEncryptedCapsule(userId, character, it) }
    val cleanupTask = newCleanupTask(userId, character)

    database.withTransaction {
        prepared?.let { capsuleDao.insert(it.entity) }
        tagDao.deleteLinksForCharacter(userId, characterId)
        messageDao.deleteForCharacter(userId, characterId)
        vectorMemoryDao.deleteAll(userId, characterId)
        moodStateDao.deleteForCharacter(userId, characterId)
        interactionTimeDao.deleteAllForUserAndCompanion(userId, characterId)
        userEventDao.deleteAllForUserAndCompanion(userId, characterId)
        check(customCharacterDao.delete(userId, characterId) == 1)
        cleanupTaskDao.insert(cleanupTask)
    }

    cleanupProcessor.process(cleanupTask.id)
    DeletionReceipt(characterId, prepared?.entity?.id, prepared?.plainToken, cleanupTask.id)
}
```

Preparation may serialize/encrypt before the transaction, but no capsule row is persisted until inside `withTransaction`.

- [ ] **Step 6: Centralize runtime drafts/state**

```kotlin
@Singleton
class CharacterRuntimeStore @Inject constructor() {
    private val drafts = ConcurrentHashMap<String, String>()
    private val state = ConcurrentHashMap<String, Any>()

    fun saveDraft(characterId: String, text: String) { drafts[characterId] = text }
    fun getDraft(characterId: String): String = drafts[characterId].orEmpty()
    fun clear(characterId: String) {
        drafts.remove(characterId)
        state.remove(characterId)
    }
}
```

Replace `ChatViewModel`'s page-local `drafts` and `stateCache` maps with this singleton.

- [ ] **Step 7: Implement idempotent cleanup**

`DefaultCharacterCleanupProcessor.process(taskId)` loads the task and returns immediately if complete. Otherwise it calls:

```kotlin
settingsManager.saveCompanionAvatar(task.characterId, null)
characterRuntimeStore.clear(task.characterId)
contextManager.reset(task.userId, task.characterId)
midTermMemoryManager.clearCache(task.userId, task.characterId)
memoryLayerManager.clearCharacterCache(task.characterId)
personalityManager.removeCompanion(task.characterId)
onlineStatusManager.setOffline(task.characterId)
```

If `avatarReference` is an app-owned file URI/path, delete only after canonical-path validation proves it is under `context.filesDir` or `context.cacheDir`. On success mark complete; on failure record the message and leave status pending.

- [ ] **Step 8: Retry pending cleanup on app startup**

Inject `CharacterCleanupProcessor` and `@ApplicationScope CoroutineScope` into `CCApplication`; in `onCreate`, launch `processPending()`. Do not block the main thread.

- [ ] **Step 9: Run transaction tests and full unit tests**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.companion.cc.data.character.CharacterDeletionTransactionTest --stacktrace
.\gradlew.bat :app:testDebugUnitTest --stacktrace
```

Expected: rollback and success cases pass.

- [ ] **Step 10: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/character app/src/main/java/com/companion/cc/data/character app/src/main/java/com/companion/cc/domain/manager app/src/main/java/com/companion/cc/data/local/dao app/src/main/java/com/companion/cc/data/local/SettingsManager.kt app/src/main/java/com/companion/cc/domain/usecase/MoodStatePersistence.kt app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt app/src/main/java/com/companion/cc/CCApplication.kt app/src/androidTest/java/com/companion/cc/data/character/CharacterDeletionTransactionTest.kt
git commit -m "feat: delete character data transactionally"
```

---

### Task 6: Add Farewell Preview and Confirmed Deletion UI

**Files:**
- Create: `app/src/main/java/com/companion/cc/ui/character/CharacterFarewellViewModel.kt`
- Create: `app/src/main/java/com/companion/cc/ui/character/CharacterFarewellScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/CharacterListScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt`
- Create: `app/src/test/java/com/companion/cc/ui/character/CharacterFarewellViewModelTest.kt`

**Interfaces:**
- Consumes: `CharacterDeletionService.previewCapsule/deletePermanently`.
- Produces: preview, memory selection, explicit irreversible confirmation, and one deletion receipt event.

- [ ] **Step 1: Define UI state and test**

```kotlin
sealed interface FarewellUiState {
    data object Loading : FarewellUiState
    data class Ready(
        val preview: CharacterMemoryCapsulePreview,
        val selection: CapsuleSelection,
        val keepCapsule: Boolean,
    ) : FarewellUiState
    data object Deleting : FarewellUiState
    data class Deleted(val receipt: DeletionReceipt) : FarewellUiState
    data class Failure(val message: String) : FarewellUiState
}
```

Test that `confirmDelete()` does nothing until the user has checked the irreversible confirmation; when confirmed it passes `null` if capsule preservation is off and the selected options if on; duplicate taps during `Deleting` make one service call.

- [ ] **Step 2: Implement ViewModel state transitions**

Load preview using a default selection that keeps settings and relationship but no memories. Toggling a memory ID rebuilds selection and refreshes preview. `consumeDeleted()` changes `Deleted` to a terminal consumed state before navigation.

- [ ] **Step 3: Build farewell screen**

Render role name/avatar, relationship summary, message/memory counts, selectable memory cards, “保留记忆胶囊”, “生成复活凭证”, and a privacy notice. The destructive button remains disabled until an explicit checkbox labeled “我理解原角色和聊天数据将被永久删除” is checked.

- [ ] **Step 4: Route deletes to farewell instead of direct DAO deletion**

Add:

```kotlin
object CharacterFarewell : Screen("character_farewell/{characterId}") {
    fun createRoute(characterId: String) = "character_farewell/$characterId"
}
```

`CharacterListScreen` delete action calls `onDeleteCharacter(id)`. `NavGraph` navigates to farewell. On `Deleted`, pop through the character list to Home so no deleted-character page remains in the back stack.

- [ ] **Step 5: Run tests and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.character.CharacterFarewellViewModelTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/ui/character app/src/main/java/com/companion/cc/ui/navigation app/src/test/java/com/companion/cc/ui/character/CharacterFarewellViewModelTest.kt
git commit -m "feat: add character farewell flow"
```

---

### Task 7: Add Capsule Repository, Vault, and Privacy-Aware Export

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/character/CharacterMemoryCapsuleRepository.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/RoomCharacterMemoryCapsuleRepository.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/CapsuleExportService.kt`
- Create: `app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultViewModel.kt`
- Create: `app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt`
- Create: `app/src/test/java/com/companion/cc/data/character/RoomCharacterMemoryCapsuleRepositoryTest.kt`

**Interfaces:**
- Produces: current-user capsule Flow, decrypted capsule access, revival-token access, and SAF export document.

- [ ] **Step 1: Define repository contract**

```kotlin
interface CharacterMemoryCapsuleRepository {
    fun observeCapsules(): Flow<List<CharacterMemoryCapsule>>
    suspend fun getCapsule(id: String): CharacterMemoryCapsule?
    suspend fun getRevivalToken(id: String): String
    suspend fun markExported(id: String, exportedAt: Long)
}
```

- [ ] **Step 2: Implement encrypted Room mapping**

`RoomCharacterMemoryCapsuleRepository` obtains current user ID, scopes every DAO call, decrypts payload/token with `CapsuleCipher`, and uses Gson to map `CharacterMemoryCapsulePayload`. A corrupt row raises `CapsuleDecryptionException`; it is surfaced as a vault error and not converted into an empty valid capsule.

- [ ] **Step 3: Write repository tests with a fake cipher**

Use an identity fake and DAO mock. Verify another user's capsule is never returned; corrupt payload emits failure; token remains encrypted at DAO boundary; status/date fields map exactly.

- [ ] **Step 4: Define export document and service**

```kotlin
data class CapsuleExportDocument(
    val format: String = "cc-character-memory-capsule",
    val formatVersion: Int = 1,
    val capsuleId: String,
    val revivalToken: String,
    val characterName: String,
    val payload: CharacterMemoryCapsulePayload,
    val exportedAt: Long,
)
```

`CapsuleExportService.export(capsuleId, uri)` obtains the decrypted payload/token, serializes pretty JSON, writes through `ContentResolver.openOutputStream(uri, "wt")`, flushes/closes with `use`, then marks exported only after the write succeeds.

- [ ] **Step 5: Build vault UI with SAF**

Add `Screen.MemoryCapsules`. The screen lists status, role name, creation date, selected-memory count, and restored target ID. Before launching `ActivityResultContracts.CreateDocument("application/json")`, show a modal warning: “导出文件包含角色设定和你选择的回忆，请只保存到可信位置。” Use a filename derived from sanitized character name plus capsule ID; never include user ID or token in the filename.

- [ ] **Step 6: Run repository tests and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.data.character.RoomCharacterMemoryCapsuleRepositoryTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/character/CharacterMemoryCapsuleRepository.kt app/src/main/java/com/companion/cc/data/character/RoomCharacterMemoryCapsuleRepository.kt app/src/main/java/com/companion/cc/data/character/CapsuleExportService.kt app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultViewModel.kt app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultScreen.kt app/src/main/java/com/companion/cc/ui/navigation app/src/test/java/com/companion/cc/data/character/RoomCharacterMemoryCapsuleRepositoryTest.kt
git commit -m "feat: add memory capsule vault and export"
```

---

### Task 8: Revive a Capsule into a Fresh Character UUID

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/character/CharacterRevivalService.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/DefaultCharacterRevivalService.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultScreen.kt`
- Create: `app/src/androidTest/java/com/companion/cc/data/character/CharacterRevivalTransactionTest.kt`

**Interfaces:**
- Consumes: available scoped capsule, revival token, and mode.
- Produces: new custom UUID, optional copied vector memories, and atomically restored capsule status.

- [ ] **Step 1: Define revival contract**

```kotlin
interface CharacterRevivalService {
    suspend fun revive(
        capsuleId: String,
        revivalToken: String,
        mode: CharacterRevivalMode,
    ): Result<RevivalReceipt>
}
```

- [ ] **Step 2: Write transaction tests first**

Seed an `AVAILABLE` capsule whose encrypted payload contains source ID `old-id` and one selected memory. Test:

1. Invalid token creates no role and leaves status `AVAILABLE`.
2. `REINTRODUCTION` creates `new-id != old-id`, copies zero messages and zero vector memories, and marks capsule `RESTORED`.
3. `WITH_MEMORIES` creates a new ID and inserts selected vector memories under `(user-real, new-id)`.
4. A second revival attempt creates nothing.
5. Forced memory insert failure rolls back the new role and status update.

- [ ] **Step 3: Implement token validation and atomic revival**

```kotlin
val tokenHash = sha256(revivalToken)
val entity = capsuleDao.getByTokenHash(userId, tokenHash)
    ?: throw InvalidRevivalTokenException()
check(entity.id == capsuleId)
check(entity.status == CharacterMemoryCapsuleStatus.AVAILABLE.name)
val payload = decodePayload(entity)
val newCharacterId = UUID.randomUUID().toString()
val revivedCharacter = payload.sourceCharacter.copy(
    id = newCharacterId,
    userId = userId,
    createdAt = now,
    updatedAt = now,
)

database.withTransaction {
    customCharacterDao.insert(CustomCharacterMapper.toEntity(revivedCharacter))
    if (mode == CharacterRevivalMode.WITH_MEMORIES) {
        vectorMemoryDao.insertAll(payload.selectedMemories.map { it.toEntity(userId, newCharacterId) })
    }
    check(capsuleDao.markRestored(
        userId, capsuleId, CharacterMemoryCapsuleStatus.RESTORED.name, now, newCharacterId
    ) == 1)
}
```

Do not insert message rows. Relationship snapshot is retained in capsule metadata for prompt/context display; it is not written into built-in-only mood counters unless a separate custom-relationship model is approved.

- [ ] **Step 4: Add vault revival UI**

For `AVAILABLE` capsules, offer “重新认识” and “带着回忆回来”. The in-app vault obtains the decrypted token from the repository and passes it to the service. Disable both buttons while reviving and consume one success event before navigating to `Screen.Chat.createRoute(newCharacterId)`.

- [ ] **Step 5: Run transaction tests and compile**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.companion.cc.data.character.CharacterRevivalTransactionTest --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: all five cases pass and every revived role has a new UUID.

- [ ] **Step 6: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/character/CharacterRevivalService.kt app/src/main/java/com/companion/cc/data/character/DefaultCharacterRevivalService.kt app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultViewModel.kt app/src/main/java/com/companion/cc/ui/character/MemoryCapsuleVaultScreen.kt app/src/androidTest/java/com/companion/cc/data/character/CharacterRevivalTransactionTest.kt
git commit -m "feat: revive capsules as new characters"
```

---

### Task 9: Run Full Automated and Device Acceptance

**Files:**
- Create: `docs/superpowers/verification/2026-08-15-character-memory-capsule.md`
- Do not modify: application version values.

**Interfaces:**
- Produces: evidence for migration, rollback, delete, export, cleanup retry, and revival isolation.

- [ ] **Step 1: Run all automated tests**

```powershell
.\gradlew.bat clean :app:compileDebugKotlin :app:testDebugUnitTest :app:connectedDebugAndroidTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`; migration, deletion rollback, and revival transaction classes all execute.

- [ ] **Step 2: Install debug build without clearing app data**

```powershell
adb devices
.\gradlew.bat :app:installDebug --stacktrace
adb shell am force-stop com.companion.cc.debug
adb shell monkey -p com.companion.cc.debug -c android.intent.category.LAUNCHER 1
```

Expected: existing v10 data is upgraded to v11 in place.

- [ ] **Step 3: Prove migration preserved preexisting counts**

Record pre/post counts for `messages`, `custom_characters`, `vector_memories`, `tags`, and `message_tags`. Confirm `PRAGMA user_version` is 11 and the two new tables exist. Do not inspect or publish message content.

- [ ] **Step 4: Execute deletion and capsule acceptance**

```text
Create disposable custom role → send two messages → produce vector memory
Open delete → preview counts/relationship → select one memory
Enable capsule/token → acknowledge permanent deletion → delete
Verify role disappears from Home/list/chat and old UUID rows are absent
Verify capsule appears in vault and cleanup task becomes COMPLETE
Export through SAF after privacy warning and inspect version/role/memory count
```

- [ ] **Step 5: Execute revival acceptance**

```text
Revive as REINTRODUCTION → new UUID → no old chat → settings restored
Delete revived disposable role or keep separate test data
Revive a second capsule WITH_MEMORIES → new UUID → selected memory restored
Verify original old UUID and each revived UUID remain distinct
Attempt reused/invalid token → no new role, clear error
Force-stop/relaunch → vault status and revived roles persist
```

- [ ] **Step 6: Exercise cleanup retry**

Create a test pending cleanup task or temporarily force one external cleanup operation to fail in a debug-only test path. Relaunch and confirm startup retry marks it complete. Remove the debug fault before final build.

- [ ] **Step 7: Write verification evidence and guard deferred scopes**

Record commands, exit codes, test counts, device ID, table counts, selected-memory counts, old/new UUIDs, export result, and retry result. Explicitly state that semantic theme and historical v7→current migration safety remain unverified.

- [ ] **Step 8: Final version/diff audit**

```powershell
git diff --check
rg -n 'versionCode\s*=\s*10|versionName\s*=\s*"2\.1\.0-beta\.9"' app/build.gradle.kts
git status --short
```

Expected: version unchanged and no whitespace errors.

- [ ] **Step 9: Commit verification evidence if authorized**

```powershell
git add docs/superpowers/verification/2026-08-15-character-memory-capsule.md
git commit -m "test: verify character capsule lifecycle"
```

---

## Self-Review

### Spec coverage

- Encrypted in-app capsule with version, token, snapshots, stats, lifecycle dates/status: Tasks 2–4 and 7.
- Read-only farewell preview and selected-memory behavior: Tasks 4 and 6.
- Atomic Room deletion with rollback: Task 5.
- Honest two-phase external cleanup and retry: Task 5.
- Export with explicit privacy warning: Task 7.
- New-UUID revival, two modes, no old chat import, invalid/used-token safety: Task 8.
- Append-only migration and existing-row preservation: Tasks 1–3.
- Automated plus device acceptance: Task 9.

### Type consistency

- `CapsuleSelection` is shared by preview and deletion.
- `CharacterMemoryCapsulePayload.schemaVersion` and entity `schemaVersion` are both `Int` and start at 1.
- `CharacterMemoryCapsuleStatus` persists via `.name`.
- `CharacterDeletionService.deletePermanently` returns `Result<DeletionReceipt>`.
- `CharacterRevivalService.revive` returns `Result<RevivalReceipt>`.
- Token validation always hashes plaintext and queries current-user scope.
- Every revival result character ID is generated during the transaction path and differs from `sourceCharacterId`.
