# Custom Character Unified Data Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make built-in and custom characters share one current-user-aware catalog, save lifecycle, navigation path, persisted prompt source, and character-scoped data access without changing the app version.

**Architecture:** `CurrentUserProvider` becomes the only source of user identity. `DefaultCharacterCatalog` merges built-in configuration with the current user's Room-backed custom characters, while `DefaultCharacterPromptResolver` resolves a fresh `CompanionConfig` before every model request. ViewModels consume these domain services instead of string literals, booleans, or page-local reloads.

**Tech Stack:** Kotlin 1.x, Android SDK 34/minSdk 26, Jetpack Compose Material 3, Hilt 2.48, Room 2.6.1, DataStore 1.0.0, Kotlin Coroutines/Flow 1.7.3, JUnit 4, Mockito Kotlin, Robolectric.

## Global Constraints

- Keep `versionCode = 10` and `versionName = "2.1.0-beta.9"` unchanged.
- Built-in characters remain asset/config-backed with IDs `xiaocan` and `muse`; do not migrate them into Room.
- Custom characters remain Room-backed and retain UUID IDs.
- Every custom-character read or mutation is scoped by both the current user ID and character ID.
- No UI method accepts or invents a user ID; no production path uses the literal `"default"` as the active user.
- A missing character is an explicit error state and must never fall back to `xiaocan` or a generic default character.
- Prompt resolution reads the latest Room row before every model request; an in-memory map may optimize but may not be the source of truth.
- This plan does not implement memory capsules, permanent deletion, semantic-theme migration, or historical v7→current migration safety; those are covered by the follow-up capsule plan and later approved phases.
- Do not remove `fallbackToDestructiveMigration()` in this plan; its removal requires the separate historical migration-safety phase.
- Commit steps are executed only after the user explicitly authorizes commits. Until then, run the same `git add` commands only as a review checklist, not as an action.

---

## File Structure

### New production files

- `app/src/main/java/com/companion/cc/domain/identity/CurrentUserProvider.kt` — identity contract.
- `app/src/main/java/com/companion/cc/data/identity/DataStoreCurrentUserProvider.kt` — DataStore-backed identity implementation.
- `app/src/main/java/com/companion/cc/domain/character/LegacyCharacterOwnershipMigrator.kt` — one-time safe reassignment of legacy `userId="default"` rows.
- `app/src/main/java/com/companion/cc/domain/model/ChatCharacter.kt` — unified built-in/custom domain model.
- `app/src/main/java/com/companion/cc/domain/character/CharacterCatalog.kt` — catalog contract.
- `app/src/main/java/com/companion/cc/data/character/DefaultCharacterCatalog.kt` — merged Flow implementation.
- `app/src/main/java/com/companion/cc/domain/character/CharacterPromptResolver.kt` — persisted prompt contract and result model.
- `app/src/main/java/com/companion/cc/data/character/DefaultCharacterPromptResolver.kt` — built-in/custom prompt resolution.
- `app/src/main/java/com/companion/cc/data/mapper/CustomCharacterPromptMapper.kt` — pure conversion from `CustomCharacter` to `CompanionConfig`.
- `app/src/main/java/com/companion/cc/ui/character/CharacterSaveState.kt` — explicit save state.
- `app/src/test/java/com/companion/cc/MainDispatcherRule.kt` — deterministic ViewModel coroutine rule.
- `app/src/test/java/com/companion/cc/TestFixtures.kt` — complete shared constructors for `CompanionConfig`, custom characters, messages, resolved prompts, and empty memory contexts.

### Existing production files to modify

- `app/src/main/java/com/companion/cc/data/local/dao/CustomCharacterDao.kt`
- `app/src/main/java/com/companion/cc/domain/repository/CustomCharacterRepository.kt`
- `app/src/main/java/com/companion/cc/domain/manager/CharacterCustomizationManager.kt`
- `app/src/main/java/com/companion/cc/domain/manager/PersonalityManager.kt`
- `app/src/main/java/com/companion/cc/domain/manager/MemoryLayerManager.kt`
- `app/src/main/java/com/companion/cc/data/local/dao/MessageDao.kt`
- `app/src/main/java/com/companion/cc/domain/repository/MessageRepository.kt`
- `app/src/main/java/com/companion/cc/data/repository/MessageRepositoryImpl.kt`
- `app/src/main/java/com/companion/cc/di/RepositoryModule.kt`
- `app/src/main/java/com/companion/cc/ui/character/CharacterCustomizationViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/character/CharacterCustomizationScreen.kt`
- `app/src/main/java/com/companion/cc/ui/character/CharacterListScreen.kt`
- `app/src/main/java/com/companion/cc/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/home/ImmersiveHomeScreen.kt`
- `app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/chat/NaturalChatScreen.kt`
- `app/src/main/java/com/companion/cc/ui/navigation/Screen.kt`
- `app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/companion/cc/ui/memory/MemoryViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/stats/StatsViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/data/DataManagementViewModel.kt`
- `app/src/main/java/com/companion/cc/ui/data/ImmersiveDataManagementScreen.kt`
- `app/src/main/java/com/companion/cc/ui/data/DataManagementScreen.kt`

### New tests

- `app/src/test/java/com/companion/cc/data/identity/DataStoreCurrentUserProviderTest.kt`
- `app/src/test/java/com/companion/cc/domain/character/LegacyCharacterOwnershipMigratorTest.kt`
- `app/src/test/java/com/companion/cc/data/character/DefaultCharacterCatalogTest.kt`
- `app/src/test/java/com/companion/cc/ui/character/CharacterCustomizationViewModelTest.kt`
- `app/src/test/java/com/companion/cc/ui/home/HomeViewModelTest.kt`
- `app/src/test/java/com/companion/cc/data/character/DefaultCharacterPromptResolverTest.kt`
- `app/src/test/java/com/companion/cc/ui/chat/ChatCharacterLoadingTest.kt`
- `app/src/test/java/com/companion/cc/ui/stats/StatsViewModelTest.kt`
- `app/src/test/java/com/companion/cc/ui/memory/MemoryViewModelTest.kt`

---

### Task 0: Capture a Reproducible Baseline

**Files:**
- Verify: `app/build.gradle.kts`
- Verify: `.gitignore`
- Verify: `docs/superpowers/specs/2026-08-15-custom-character-unified-data-flow-design.md`

**Interfaces:**
- Consumes: existing Gradle wrapper and current source tree.
- Produces: recorded passing/failing baseline and a safe source-only Git snapshot if commit authorization is granted.

- [ ] **Step 1: Record repository state**

Run:

```powershell
git status --short --branch
git log --oneline --decorate -3
```

Expected: branch `master`, root commit `a707814`, and existing application files shown as untracked.

- [ ] **Step 2: Check that ignored secrets and generated files remain ignored**

Run:

```powershell
git check-ignore -v local.properties app/build .gradle
git status --short --ignored | Select-String -Pattern 'local.properties|app/build|\.gradle'
```

Expected: `local.properties`, build output, and Gradle caches are ignored; no keystore or `google-services.json` is staged.

- [ ] **Step 3: Run the pre-change compile and unit-test baseline**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin --stacktrace
.\gradlew.bat :app:testDebugUnitTest --stacktrace
```

Expected: both commands end with `BUILD SUCCESSFUL`. If either fails, preserve the exact failure as the baseline and stop before feature edits.

- [ ] **Step 4: Create a source-only baseline commit only if explicitly authorized**

```powershell
git add .gitignore app/src app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties gradle/wrapper gradlew gradlew.bat docs/superpowers/specs/2026-08-15-custom-character-unified-data-flow-design.md
git diff --cached --check
git commit -m "chore: snapshot Android source baseline"
```

Expected: generated outputs and local credentials are absent from the commit. Without commit authorization, skip this step and retain the external backup at `D:\CC-Switch\backups\cc-native-android-20260815-114343\` as the rollback source.

---

### Task 1: Add the Current User Identity Boundary

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/identity/CurrentUserProvider.kt`
- Create: `app/src/main/java/com/companion/cc/data/identity/DataStoreCurrentUserProvider.kt`
- Modify: `app/src/main/java/com/companion/cc/di/RepositoryModule.kt`
- Create: `app/src/test/java/com/companion/cc/data/identity/DataStoreCurrentUserProviderTest.kt`

**Interfaces:**
- Consumes: `SettingsManager.userIdFlow: Flow<String>`.
- Produces: `CurrentUserProvider.userId: Flow<String>` and `CurrentUserProvider.requireUserId(): String`.

- [ ] **Step 1: Write the failing identity test**

```kotlin
package com.companion.cc.data.identity

import com.companion.cc.data.local.SettingsManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DataStoreCurrentUserProviderTest {
    @Test
    fun `requireUserId returns the persisted nonblank id`() = runTest {
        val settings = mock<SettingsManager>()
        whenever(settings.userIdFlow).thenReturn(flowOf("user-real"))
        val provider = DataStoreCurrentUserProvider(settings)

        assertEquals("user-real", provider.requireUserId())
    }

    @Test
    fun `userId ignores blank values and removes duplicates`() = runTest {
        val settings = mock<SettingsManager>()
        whenever(settings.userIdFlow).thenReturn(
            flowOf("", "user-real", "user-real", "user-next")
        )
        val provider = DataStoreCurrentUserProvider(settings)

        assertEquals(listOf("user-real", "user-next"), provider.userId.toList())
    }
}
```

- [ ] **Step 2: Run the test and verify the missing-type failure**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.data.identity.DataStoreCurrentUserProviderTest" --stacktrace
```

Expected: compilation fails because `DataStoreCurrentUserProvider` and `CurrentUserProvider` do not exist.

- [ ] **Step 3: Add the identity contract and implementation**

```kotlin
package com.companion.cc.domain.identity

import kotlinx.coroutines.flow.Flow

interface CurrentUserProvider {
    val userId: Flow<String>
    suspend fun requireUserId(): String
}
```

```kotlin
package com.companion.cc.data.identity

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.identity.CurrentUserProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreCurrentUserProvider @Inject constructor(
    settingsManager: SettingsManager,
) : CurrentUserProvider {
    override val userId: Flow<String> = settingsManager.userIdFlow
        .filter(String::isNotBlank)
        .distinctUntilChanged()

    override suspend fun requireUserId(): String = userId.first()
}
```

- [ ] **Step 4: Bind the implementation in Hilt**

Add to `RepositoryModule`:

```kotlin
@Binds
@Singleton
abstract fun bindCurrentUserProvider(
    implementation: com.companion.cc.data.identity.DataStoreCurrentUserProvider,
): com.companion.cc.domain.identity.CurrentUserProvider
```

- [ ] **Step 5: Run the focused and existing tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.data.identity.DataStoreCurrentUserProviderTest" --stacktrace
.\gradlew.bat :app:testDebugUnitTest --stacktrace
```

Expected: both commands end with `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/identity/CurrentUserProvider.kt app/src/main/java/com/companion/cc/data/identity/DataStoreCurrentUserProvider.kt app/src/main/java/com/companion/cc/di/RepositoryModule.kt app/src/test/java/com/companion/cc/data/identity/DataStoreCurrentUserProviderTest.kt
git commit -m "feat: add current user identity boundary"
```

---

### Task 2: Scope Custom Characters and Migrate Legacy Ownership Safely

**Files:**
- Modify: `app/src/main/java/com/companion/cc/data/local/dao/CustomCharacterDao.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/repository/CustomCharacterRepository.kt`
- Create: `app/src/main/java/com/companion/cc/domain/character/LegacyCharacterOwnershipMigrator.kt`
- Create: `app/src/test/java/com/companion/cc/domain/character/LegacyCharacterOwnershipMigratorTest.kt`

**Interfaces:**
- Consumes: `CurrentUserProvider.requireUserId()` and Room `custom_characters`.
- Produces: user-scoped reads/mutations and `LegacyOwnershipMigrationResult`.

- [ ] **Step 1: Add failing tests for migration success and conflict**

```kotlin
package com.companion.cc.domain.character

import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LegacyCharacterOwnershipMigratorTest {
    private val users = mock<CurrentUserProvider>()
    private val repository = mock<CustomCharacterRepository>()

    @Test
    fun `moves legacy rows when real user has no characters`() = runTest {
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(repository.getCharacterCount("user-real")).thenReturn(0)
        whenever(repository.getCharacterCount("default")).thenReturn(2)
        whenever(repository.reassignCharacters("default", "user-real")).thenReturn(2)

        val result = LegacyCharacterOwnershipMigrator(users, repository).migrateIfNeeded()

        assertEquals(LegacyOwnershipMigrationResult.Migrated(2), result)
    }

    @Test
    fun `keeps legacy rows when target already owns characters`() = runTest {
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(repository.getCharacterCount("user-real")).thenReturn(1)
        whenever(repository.getCharacterCount("default")).thenReturn(2)

        val result = LegacyCharacterOwnershipMigrator(users, repository).migrateIfNeeded()

        assertEquals(LegacyOwnershipMigrationResult.Conflict(2, 1), result)
        verify(repository, never()).reassignCharacters("default", "user-real")
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.domain.character.LegacyCharacterOwnershipMigratorTest" --stacktrace
```

Expected: missing migrator/result methods.

- [ ] **Step 3: Add user-scoped DAO operations**

Add to `CustomCharacterDao`:

```kotlin
@Query("SELECT * FROM custom_characters WHERE id = :characterId AND userId = :userId LIMIT 1")
suspend fun getCharacterById(userId: String, characterId: String): CustomCharacterEntity?

@Query("SELECT * FROM custom_characters WHERE id = :characterId AND userId = :userId LIMIT 1")
fun getCharacterByIdFlow(userId: String, characterId: String): Flow<CustomCharacterEntity?>

@Query("DELETE FROM custom_characters WHERE id = :characterId AND userId = :userId")
suspend fun delete(userId: String, characterId: String): Int

@Query("UPDATE custom_characters SET userId = :targetUserId WHERE userId = :sourceUserId")
suspend fun reassignCharacters(sourceUserId: String, targetUserId: String): Int
```

Remove the unscoped `getCharacterById`, `getCharacterByIdFlow`, and `delete(characterId)` declarations after all callers compile against the replacements.

- [ ] **Step 4: Update repository signatures**

```kotlin
suspend fun getCharacterById(userId: String, characterId: String): CustomCharacter? =
    characterDao.getCharacterById(userId, characterId)?.let(CustomCharacterMapper::toDomain)

fun getCharacterByIdFlow(userId: String, characterId: String): Flow<CustomCharacter?> =
    characterDao.getCharacterByIdFlow(userId, characterId).map { it?.let(CustomCharacterMapper::toDomain) }

suspend fun deleteCharacter(userId: String, characterId: String): Boolean =
    characterDao.delete(userId, characterId) == 1

suspend fun reassignCharacters(sourceUserId: String, targetUserId: String): Int =
    characterDao.reassignCharacters(sourceUserId, targetUserId)
```

- [ ] **Step 5: Implement the migration result and service**

```kotlin
package com.companion.cc.domain.character

import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.CustomCharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LegacyOwnershipMigrationResult {
    data object NotNeeded : LegacyOwnershipMigrationResult
    data class Migrated(val count: Int) : LegacyOwnershipMigrationResult
    data class Conflict(val legacyCount: Int, val targetCount: Int) : LegacyOwnershipMigrationResult
}

@Singleton
class LegacyCharacterOwnershipMigrator @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val repository: CustomCharacterRepository,
) {
    suspend fun migrateIfNeeded(): LegacyOwnershipMigrationResult {
        val targetUserId = currentUserProvider.requireUserId()
        if (targetUserId == LEGACY_USER_ID) return LegacyOwnershipMigrationResult.NotNeeded

        val legacyCount = repository.getCharacterCount(LEGACY_USER_ID)
        if (legacyCount == 0) return LegacyOwnershipMigrationResult.NotNeeded

        val targetCount = repository.getCharacterCount(targetUserId)
        if (targetCount > 0) {
            return LegacyOwnershipMigrationResult.Conflict(legacyCount, targetCount)
        }

        return LegacyOwnershipMigrationResult.Migrated(
            repository.reassignCharacters(LEGACY_USER_ID, targetUserId)
        )
    }

    private companion object {
        const val LEGACY_USER_ID = "default"
    }
}
```

- [ ] **Step 6: Run focused tests and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.domain.character.LegacyCharacterOwnershipMigratorTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`; compilation failures identify every old unscoped caller, which must be changed to pass `CurrentUserProvider.requireUserId()` rather than reintroducing defaults.

- [ ] **Step 7: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/data/local/dao/CustomCharacterDao.kt app/src/main/java/com/companion/cc/domain/repository/CustomCharacterRepository.kt app/src/main/java/com/companion/cc/domain/character/LegacyCharacterOwnershipMigrator.kt app/src/test/java/com/companion/cc/domain/character/LegacyCharacterOwnershipMigratorTest.kt
git commit -m "fix: scope custom characters to current user"
```

---

### Task 3: Build the Unified Character Catalog

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/model/ChatCharacter.kt`
- Create: `app/src/main/java/com/companion/cc/domain/character/CharacterCatalog.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/DefaultCharacterCatalog.kt`
- Modify: `app/src/main/java/com/companion/cc/di/RepositoryModule.kt`
- Create: `app/src/test/java/com/companion/cc/data/character/DefaultCharacterCatalogTest.kt`

**Interfaces:**
- Consumes: `CompanionConfigLoader.getEnabledCompanions()`, current-user custom-character Flow, and the legacy migrator.
- Produces: `CharacterCatalog.observeCharacters()` and user-scoped `CharacterCatalog.getCharacter(id)`.

- [ ] **Step 0: Add complete shared test fixtures**

Create `app/src/test/java/com/companion/cc/TestFixtures.kt`:

```kotlin
package com.companion.cc

import com.companion.cc.domain.character.ResolvedCharacterPrompt
import com.companion.cc.domain.model.ApiParametersConfig
import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.CompanionConfig
import com.companion.cc.domain.model.CompleteMemoryContext
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.EmotionalModelConfig
import com.companion.cc.domain.model.MemoryPreferences
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.Mood
import com.companion.cc.domain.model.PersonalityConfig
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.Prompts
import com.companion.cc.domain.model.Relationship
import com.companion.cc.domain.model.SpeakingStyle
import com.companion.cc.domain.model.VoiceConfig

object TestCharacters {
    val museConfig: CompanionConfig = config("muse", "缪斯")
    val muse: ChatCharacter = ChatCharacter.BuiltIn(museConfig)

    fun config(id: String, name: String = "角色"): CompanionConfig = CompanionConfig(
        id = id,
        name = name,
        emoji = "✨",
        avatar = "",
        enabled = true,
        personality = PersonalityConfig(
            coreTraits = listOf("可靠"),
            background = "背景",
            speakingStyle = SpeakingStyle("温和", "日常", "中等", true, true, "随意"),
            interests = listOf("音乐"),
            values = listOf("真诚"),
            relationship = Relationship("陪伴者", "适度", "关怀式", "你"),
            behaviorPatterns = listOf("倾听"),
        ),
        prompts = Prompts("system-$name", listOf("你好"), listOf("再见"), listOf("请再说一次")),
        emotionalModel = EmotionalModelConfig(Mood.CALM, 0.7f, 0.1f, 0.7f, 0.05f, emptyMap()),
        memoryPreferences = MemoryPreferences(0.5f, "daily", emptyList(), emptyList()),
        apiParameters = ApiParametersConfig(maxTokens = 2000),
    )

    fun custom(
        id: String,
        userId: String = "user-real",
        name: String = "Nova",
    ): CustomCharacter = CustomCharacter(
        id = id,
        userId = userId,
        name = name,
        avatar = null,
        description = "description",
        personality = PersonalityTraits.default(),
        backstory = "backstory",
        greetingMessage = "hello",
        exampleDialogues = emptyList(),
        voiceConfig = VoiceConfig.default(),
        behaviorRules = BehaviorRules.default(),
        createdAt = 1L,
        updatedAt = 1L,
    )

    fun customChat(id: String, name: String = "Nova"): ChatCharacter =
        ChatCharacter.Custom(custom(id = id, name = name))

    fun resolved(id: String): ResolvedCharacterPrompt =
        ResolvedCharacterPrompt(customChat(id), config(id))
}

object TestMessages {
    fun message(companionId: String, timestamp: Long): Message = Message(
        id = "message-$companionId-$timestamp",
        userId = "user-real",
        companionId = companionId,
        role = MessageRole.USER,
        content = "content-$companionId",
        timestamp = timestamp,
    )
}

object TestMemories {
    val emptyContext = CompleteMemoryContext(
        shortTerm = emptyList(),
        midTerm = emptyList(),
        longTerm = emptyList(),
        permanent = emptyList(),
    )
}
```

- [ ] **Step 1: Write failing catalog tests**

```kotlin
package com.companion.cc.data.character

import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.domain.character.LegacyCharacterOwnershipMigrator
import com.companion.cc.domain.character.LegacyOwnershipMigrationResult
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DefaultCharacterCatalogTest {
    @Test
    fun `catalog merges built in and current user custom characters with unique ids`() = runTest {
        val users = mock<CurrentUserProvider> { on { userId }.thenReturn(flowOf("user-real")) }
        val repository = mock<CustomCharacterRepository>()
        val loader = mock<CompanionConfigLoader>()
        val migrator = mock<LegacyCharacterOwnershipMigrator>()
        whenever(migrator.migrateIfNeeded()).thenReturn(LegacyOwnershipMigrationResult.NotNeeded)
        whenever(loader.getEnabledCompanions()).thenReturn(listOf(TestCharacters.museConfig))
        whenever(repository.getCharactersByUser("user-real")).thenReturn(
            flowOf(listOf(TestCharacters.custom(id = "custom-1", userId = "user-real")))
        )

        val result = DefaultCharacterCatalog(users, repository, loader, migrator)
            .observeCharacters().first()

        assertEquals(listOf("muse", "custom-1"), result.map { it.id })
    }

    @Test
    fun `getCharacter does not expose another user custom character`() = runTest {
        val users = mock<CurrentUserProvider>()
        val repository = mock<CustomCharacterRepository>()
        val loader = mock<CompanionConfigLoader>()
        val migrator = mock<LegacyCharacterOwnershipMigrator>()
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(loader.getCompanionConfig("foreign-id")).thenReturn(null)
        whenever(repository.getCharacterById("user-real", "foreign-id")).thenReturn(null)

        val result = DefaultCharacterCatalog(users, repository, loader, migrator)
            .getCharacter("foreign-id")

        assertNull(result)
    }
}
```

Import `com.companion.cc.TestCharacters` in the test; no production fixture code is introduced.

- [ ] **Step 2: Run the test and verify missing catalog types**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.data.character.DefaultCharacterCatalogTest" --stacktrace
```

Expected: compilation fails on `ChatCharacter`, `CharacterCatalog`, and `DefaultCharacterCatalog`.

- [ ] **Step 3: Add the unified model**

```kotlin
package com.companion.cc.domain.model

sealed interface ChatCharacter {
    val id: String
    val name: String
    val avatar: String?
    val greeting: String
    val isCustom: Boolean

    data class BuiltIn(val config: CompanionConfig) : ChatCharacter {
        override val id = config.id
        override val name = config.name
        override val avatar = config.avatar.ifBlank { config.emoji }
        override val greeting = config.prompts.greeting.firstOrNull().orEmpty()
        override val isCustom = false
    }

    data class Custom(val character: CustomCharacter) : ChatCharacter {
        override val id = character.id
        override val name = character.name
        override val avatar = character.avatar
        override val greeting = character.greetingMessage
        override val isCustom = true
    }
}
```

- [ ] **Step 4: Add the catalog contract**

```kotlin
package com.companion.cc.domain.character

import com.companion.cc.domain.model.ChatCharacter
import kotlinx.coroutines.flow.Flow

interface CharacterCatalog {
    fun observeCharacters(): Flow<List<ChatCharacter>>
    suspend fun getCharacter(id: String): ChatCharacter?
}
```

- [ ] **Step 5: Implement the merged catalog**

```kotlin
package com.companion.cc.data.character

import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.character.LegacyCharacterOwnershipMigrator
import com.companion.cc.domain.character.LegacyOwnershipMigrationResult
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.repository.CustomCharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

class LegacyCharacterOwnershipConflictException(
    val legacyCount: Int,
    val currentCount: Int,
) : IllegalStateException("Legacy and current-user custom characters both exist")

@Singleton
class DefaultCharacterCatalog @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val customCharacterRepository: CustomCharacterRepository,
    private val configLoader: CompanionConfigLoader,
    private val ownershipMigrator: LegacyCharacterOwnershipMigrator,
) : CharacterCatalog {
    override fun observeCharacters(): Flow<List<ChatCharacter>> = flow {
        when (val migration = ownershipMigrator.migrateIfNeeded()) {
            is LegacyOwnershipMigrationResult.Conflict -> throw LegacyCharacterOwnershipConflictException(
                migration.legacyCount,
                migration.targetCount,
            )
            else -> Unit
        }

        val builtIns = configLoader.getEnabledCompanions().map(ChatCharacter::BuiltIn)
        emitAll(currentUserProvider.userId.flatMapLatest { userId ->
            customCharacterRepository.getCharactersByUser(userId).map { custom ->
                (builtIns + custom.map(ChatCharacter::Custom)).distinctBy(ChatCharacter::id)
            }
        })
    }

    override suspend fun getCharacter(id: String): ChatCharacter? {
        configLoader.getCompanionConfig(id)?.let { return ChatCharacter.BuiltIn(it) }
        val userId = currentUserProvider.requireUserId()
        return customCharacterRepository.getCharacterById(userId, id)?.let(ChatCharacter::Custom)
    }
}
```

- [ ] **Step 6: Bind the catalog in Hilt**

```kotlin
@Binds
@Singleton
abstract fun bindCharacterCatalog(
    implementation: com.companion.cc.data.character.DefaultCharacterCatalog,
): com.companion.cc.domain.character.CharacterCatalog
```

- [ ] **Step 7: Run catalog tests and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.data.character.DefaultCharacterCatalogTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/model/ChatCharacter.kt app/src/main/java/com/companion/cc/domain/character/CharacterCatalog.kt app/src/main/java/com/companion/cc/data/character/DefaultCharacterCatalog.kt app/src/main/java/com/companion/cc/di/RepositoryModule.kt app/src/test/java/com/companion/cc/data/character/DefaultCharacterCatalogTest.kt
git commit -m "feat: add unified character catalog"
```

---

### Task 4: Replace Save Races with Explicit Save State

**Files:**
- Create: `app/src/main/java/com/companion/cc/ui/character/CharacterSaveState.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/manager/CharacterCustomizationManager.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/CharacterCustomizationViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/CharacterCustomizationScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/CharacterListScreen.kt`
- Create: `app/src/test/java/com/companion/cc/MainDispatcherRule.kt`
- Create: `app/src/test/java/com/companion/cc/ui/character/CharacterCustomizationViewModelTest.kt`

**Interfaces:**
- Consumes: `CurrentUserProvider`, `CharacterCatalog`, and scoped `CharacterCustomizationManager` methods.
- Produces: `StateFlow<CharacterSaveState>` where exactly one `Success(characterId)` follows a successful write.

- [ ] **Step 1: Add the main-dispatcher test rule**

```kotlin
package com.companion.cc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

- [ ] **Step 2: Write failing ViewModel tests for identity and duplicate submission**

```kotlin
package com.companion.cc.ui.character

import com.companion.cc.MainDispatcherRule
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.manager.CharacterCustomizationManager
import com.companion.cc.domain.model.CustomCharacter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CharacterCustomizationViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `create uses current user and emits success`() = runTest(main.dispatcher) {
        val manager = mock<CharacterCustomizationManager>()
        val users = mock<CurrentUserProvider>()
        val catalog = mock<CharacterCatalog>()
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(catalog.observeCharacters()).thenReturn(flowOf(emptyList()))
        whenever(manager.createCharacter(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(TestCharacters.custom(id = "new-id", userId = "user-real"))
        val vm = CharacterCustomizationViewModel(manager, users, catalog)
        vm.updateName("Nova")
        vm.updateDescription("desc")
        vm.updateBackstory("story")

        vm.saveCharacter()
        advanceUntilIdle()

        assertEquals(CharacterSaveState.Success("new-id"), vm.saveState.value)
        verify(manager).createCharacter(
            userId = "user-real",
            name = "Nova",
            description = "desc",
            personality = vm.personality.value,
            backstory = "story",
            greetingMessage = vm.greetingMessage.value,
            exampleDialogues = vm.exampleDialogues.value,
            voiceConfig = vm.voiceConfig.value,
            behaviorRules = vm.behaviorRules.value,
        )
    }

    @Test
    fun `second save while saving is ignored`() = runTest(main.dispatcher) {
        val gate = CompletableDeferred<CustomCharacter>()
        val manager = mock<CharacterCustomizationManager>()
        val users = mock<CurrentUserProvider>()
        val catalog = mock<CharacterCatalog>()
        whenever(users.requireUserId()).thenReturn("user-real")
        whenever(catalog.observeCharacters()).thenReturn(flowOf(emptyList()))
        whenever(manager.createCharacter(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenAnswer { gate.await() }
        val vm = CharacterCustomizationViewModel(manager, users, catalog)
        vm.updateName("Nova")
        vm.updateDescription("desc")
        vm.updateBackstory("story")

        vm.saveCharacter()
        vm.saveCharacter()
        advanceUntilIdle()

        verify(manager, times(1)).createCharacter(any(), any(), any(), any(), any(), any(), any(), any(), any())
    }
}
```

- [ ] **Step 3: Run tests and verify failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.character.CharacterCustomizationViewModelTest" --stacktrace
```

Expected: constructor and `saveState` compilation failures.

- [ ] **Step 4: Add save state**

```kotlin
package com.companion.cc.ui.character

sealed interface CharacterSaveState {
    data object Idle : CharacterSaveState
    data object Saving : CharacterSaveState
    data class Success(val characterId: String) : CharacterSaveState
    data class Failure(val message: String) : CharacterSaveState
}
```

- [ ] **Step 5: Make the manager user-scoped and return written objects**

Change manager signatures to:

```kotlin
suspend fun updateCharacter(userId: String, character: CustomCharacter): CustomCharacter {
    require(character.userId == userId) { "Character does not belong to current user" }
    characterRepository.updateCharacter(character)
    return character
}

suspend fun getCharacter(userId: String, characterId: String): CustomCharacter? =
    characterRepository.getCharacterById(userId, characterId)

fun getUserCharacters(userId: String): Flow<List<CustomCharacter>> =
    characterRepository.getCharactersByUser(userId)
```

Remove `PersonalityManager` registration/deletion from this manager; persisted prompt resolution replaces it in Task 6. Permanent deletion moves to the capsule plan.

- [ ] **Step 6: Refactor the ViewModel around catalog and save state**

Use this save implementation and derive custom rows from the catalog:

```kotlin
private val _saveState = MutableStateFlow<CharacterSaveState>(CharacterSaveState.Idle)
val saveState: StateFlow<CharacterSaveState> = _saveState.asStateFlow()

val characters: StateFlow<List<CustomCharacter>> = characterCatalog.observeCharacters()
    .map { list -> list.mapNotNull { (it as? ChatCharacter.Custom)?.character } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

fun saveCharacter() {
    if (_saveState.value == CharacterSaveState.Saving || !isFormValid()) return
    viewModelScope.launch {
        _saveState.value = CharacterSaveState.Saving
        _saveState.value = try {
            val userId = currentUserProvider.requireUserId()
            val saved = _currentCharacter.value?.let { current ->
                characterManager.updateCharacter(
                    userId,
                    current.copy(
                        name = _name.value,
                        description = _description.value,
                        backstory = _backstory.value,
                        greetingMessage = _greetingMessage.value,
                        personality = _personality.value,
                        behaviorRules = _behaviorRules.value,
                        voiceConfig = _voiceConfig.value,
                        exampleDialogues = _exampleDialogues.value,
                    ),
                )
            } ?: characterManager.createCharacter(
                userId = userId,
                name = _name.value,
                description = _description.value,
                personality = _personality.value,
                backstory = _backstory.value,
                greetingMessage = _greetingMessage.value,
                exampleDialogues = _exampleDialogues.value,
                voiceConfig = _voiceConfig.value,
                behaviorRules = _behaviorRules.value,
            )
            CharacterSaveState.Success(saved.id)
        } catch (error: Exception) {
            CharacterSaveState.Failure(error.message ?: "保存角色失败")
        }
    }
}

fun consumeSaveResult() {
    if (_saveState.value is CharacterSaveState.Success) {
        _saveState.value = CharacterSaveState.Idle
    }
}
```

`loadCharacterForEdit` must call `currentUserProvider.requireUserId()` and the scoped manager lookup. Remove `loadCharacters(userId)`, `saveCharacter(userId)`, `_isLoading`, and `_errorMessage` as save-success signals.

- [ ] **Step 7: Consume success exactly once in Compose**

Replace `shouldNavigateBack` and the `LaunchedEffect(isLoading, errorMessage)` block with:

```kotlin
val saveState by viewModel.saveState.collectAsState()

LaunchedEffect(saveState) {
    if (saveState is CharacterSaveState.Success) {
        viewModel.consumeSaveResult()
        onNavigateBack()
    }
}

val isSaving = saveState == CharacterSaveState.Saving
val saveError = (saveState as? CharacterSaveState.Failure)?.message
```

Call only `viewModel.saveCharacter()` from the final button; set `enabled = !isSaving`; display `saveError` without clearing form fields.

- [ ] **Step 8: Make CharacterListScreen read catalog state and defer deletion**

Continue showing only `ChatCharacter.Custom` entries. Remove the direct `viewModel.deleteCharacter(character.id)` call; route delete taps to a new `onDeleteCharacter: (String) -> Unit` callback so Task 4 does not preserve the current unsafe one-row delete. The navigation destination for the farewell flow is added in the capsule plan.

- [ ] **Step 9: Run focused tests and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.character.CharacterCustomizationViewModelTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`; two rapid saves verify one manager call.

- [ ] **Step 10: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/ui/character app/src/main/java/com/companion/cc/domain/manager/CharacterCustomizationManager.kt app/src/test/java/com/companion/cc/MainDispatcherRule.kt app/src/test/java/com/companion/cc/ui/character/CharacterCustomizationViewModelTest.kt
git commit -m "fix: make character save lifecycle explicit"
```

---

### Task 5: Make Home a Reactive Catalog Projection

**Files:**
- Modify: `app/src/main/java/com/companion/cc/data/local/dao/MessageDao.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/repository/MessageRepository.kt`
- Modify: `app/src/main/java/com/companion/cc/data/repository/MessageRepositoryImpl.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/home/ImmersiveHomeScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt`
- Create: `app/src/test/java/com/companion/cc/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: catalog Flow, current user Flow, per-character latest-message Flow, online status.
- Produces: `StateFlow<HomeUiState>` with sorted `HomeCharacterItem` rows and one navigation callback for every character.

- [ ] **Step 1: Add a latest-message repository contract and failing Home test**

```kotlin
fun observeLatestMessage(userId: String, companionId: String): Flow<Message?>
```

Test:

```kotlin
@Test
fun `home emits built in and custom rows sorted by latest message`() = runTest(main.dispatcher) {
    val catalog = mock<CharacterCatalog>()
    val users = mock<CurrentUserProvider>()
    val messages = mock<MessageRepository>()
    whenever(users.userId).thenReturn(flowOf("user-real"))
    whenever(catalog.observeCharacters()).thenReturn(
        flowOf(listOf(TestCharacters.muse, TestCharacters.customChat("custom-1")))
    )
    whenever(messages.observeLatestMessage("user-real", "muse"))
        .thenReturn(flowOf(TestMessages.message("muse", timestamp = 10L)))
    whenever(messages.observeLatestMessage("user-real", "custom-1"))
        .thenReturn(flowOf(TestMessages.message("custom-1", timestamp = 20L)))

    val vm = HomeViewModel(messages, users, mock(), catalog)
    advanceUntilIdle()

    assertEquals(listOf("custom-1", "muse"),
        (vm.uiState.value as HomeUiState.Content).items.map { it.character.id })
}
```

- [ ] **Step 2: Run the test and verify failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.home.HomeViewModelTest" --stacktrace
```

Expected: missing latest-message and Home state APIs.

- [ ] **Step 3: Implement the Room latest-message query**

Add to `MessageDao`:

```kotlin
@Query("SELECT * FROM messages WHERE user_id = :userId AND companion_id = :companionId ORDER BY timestamp DESC LIMIT 1")
fun observeLatestMessage(userId: String, companionId: String): Flow<MessageEntity?>
```

Map it in `MessageRepositoryImpl` using the existing entity-to-domain mapper and expose it in `MessageRepository`.

- [ ] **Step 4: Replace Home's nested collectors and page-local mutable list**

Add:

```kotlin
data class HomeCharacterItem(
    val character: ChatCharacter,
    val lastMessage: Message?,
    val isOnline: Boolean,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val items: List<HomeCharacterItem>) : HomeUiState
    data class Failure(val message: String) : HomeUiState
}
```

Build the state with `flatMapLatest` and `combine`:

```kotlin
val uiState: StateFlow<HomeUiState> = combine(
    currentUserProvider.userId,
    characterCatalog.observeCharacters(),
    onlineStatusManager.onlineCompanions,
) { userId, characters, online -> Triple(userId, characters, online) }
    .flatMapLatest { (userId, characters, online) ->
        if (characters.isEmpty()) {
            flowOf(HomeUiState.Content(emptyList()))
        } else {
            combine(characters.map { character ->
                messageRepository.observeLatestMessage(userId, character.id).map { last ->
                    HomeCharacterItem(character, last, character.id in online)
                }
            }) { rows ->
                HomeUiState.Content(rows.sortedByDescending { it.lastMessage?.timestamp ?: 0L })
            }
        }
    }
    .catch { emit(HomeUiState.Failure(it.message ?: "加载角色失败")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
```

- [ ] **Step 5: Render HomeUiState directly**

Remove both `LaunchedEffect` blocks that query Muse, Xiaocan, and custom characters. Map each `HomeCharacterItem` to the existing row UI and call a single `onCharacterSelected(item.character.id)` callback. Use `character.avatar ?: character.name.take(1)` and `lastMessage?.content ?: "还没有消息"`.

- [ ] **Step 6: Route all Home rows through the same chat route**

In `NavGraph`, replace `onCompanionSelected` and `onCustomCharacterSelected` with one callback that calls `Screen.Chat.createRoute(characterId)`.

- [ ] **Step 7: Run tests and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.home.HomeViewModelTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`; Home test proves the custom row is visible and sorted without a manual refresh.

- [ ] **Step 8: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/data/local/dao/MessageDao.kt app/src/main/java/com/companion/cc/domain/repository/MessageRepository.kt app/src/main/java/com/companion/cc/data/repository/MessageRepositoryImpl.kt app/src/main/java/com/companion/cc/ui/home app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt app/src/test/java/com/companion/cc/ui/home/HomeViewModelTest.kt
git commit -m "feat: render home from character catalog"
```

---

### Task 6: Resolve Persisted Character Prompts Before Every Request

**Files:**
- Create: `app/src/main/java/com/companion/cc/domain/character/CharacterPromptResolver.kt`
- Create: `app/src/main/java/com/companion/cc/data/mapper/CustomCharacterPromptMapper.kt`
- Create: `app/src/main/java/com/companion/cc/data/character/DefaultCharacterPromptResolver.kt`
- Modify: `app/src/main/java/com/companion/cc/di/RepositoryModule.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/manager/PersonalityManager.kt`
- Modify: `app/src/main/java/com/companion/cc/domain/manager/MemoryLayerManager.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt`
- Create: `app/src/test/java/com/companion/cc/data/character/DefaultCharacterPromptResolverTest.kt`

**Interfaces:**
- Consumes: `CharacterCatalog.getCharacter(id)` and fresh scoped custom-character repository reads.
- Produces: `ResolvedCharacterPrompt(character, config)` and `UnknownCharacterException`.

- [ ] **Step 1: Write failing resolver tests**

```kotlin
@Test
fun `custom prompt is rebuilt from latest Room character on every resolve`() = runTest {
    val catalog = mock<CharacterCatalog>()
    whenever(catalog.getCharacter("custom-1"))
        .thenReturn(TestCharacters.customChat("custom-1", name = "Before"))
        .thenReturn(TestCharacters.customChat("custom-1", name = "After"))
    val resolver = DefaultCharacterPromptResolver(catalog)

    val first = resolver.resolve("custom-1")
    val second = resolver.resolve("custom-1")

    assertEquals("Before", first.config.name)
    assertEquals("After", second.config.name)
    verify(catalog, times(2)).getCharacter("custom-1")
}

@Test(expected = UnknownCharacterException::class)
fun `missing id never falls back to a default prompt`() = runTest {
    val catalog = mock<CharacterCatalog>()
    whenever(catalog.getCharacter("missing")).thenReturn(null)
    DefaultCharacterPromptResolver(catalog).resolve("missing")
}
```

- [ ] **Step 2: Run the tests and verify missing resolver types**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.data.character.DefaultCharacterPromptResolverTest" --stacktrace
```

Expected: compilation failure.

- [ ] **Step 3: Define resolver types**

```kotlin
package com.companion.cc.domain.character

import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.CompanionConfig

data class ResolvedCharacterPrompt(
    val character: ChatCharacter,
    val config: CompanionConfig,
)

class UnknownCharacterException(val characterId: String) :
    NoSuchElementException("Character not found: $characterId")

interface CharacterPromptResolver {
    suspend fun resolve(characterId: String): ResolvedCharacterPrompt
}
```

- [ ] **Step 4: Extract a pure custom-character prompt mapper**

Move the existing prompt construction rules from `CharacterCustomizationManager` into:

```kotlin
object CustomCharacterPromptMapper {
    fun toCompanionConfig(character: CustomCharacter): CompanionConfig = CompanionConfig(
        id = character.id,
        name = character.name,
        emoji = "✨",
        avatar = character.avatar.orEmpty(),
        enabled = true,
        prompts = Prompts(
            system = buildSystemPrompt(character),
            greeting = listOf(character.greetingMessage),
            farewell = listOf("再见"),
            fallback = listOf("我不太明白你的意思，能再说一遍吗？"),
        ),
        personality = buildPersonalityConfig(character),
        emotionalModel = EmotionalModelConfig(
            defaultMood = Mood.CALM,
            moodStability = (1f - character.personality.neuroticism).coerceIn(0f, 1f),
            energyRecoveryRate = 0.1f,
            stressThreshold = 0.7f,
            affectionGrowthRate = 0.05f,
            moodTransitions = emptyMap(),
        ),
        memoryPreferences = MemoryPreferences(
            importanceThreshold = 0.5f,
            summaryFrequency = "daily",
            rememberTopics = character.behaviorRules?.topicPreferences.orEmpty(),
            forgetTopics = character.behaviorRules?.avoidTopics.orEmpty(),
        ),
        apiParameters = ApiParametersConfig(
            temperature = when {
                character.personality.openness > 0.8f -> 0.9f
                character.personality.openness > 0.6f -> 0.8f
                character.personality.openness > 0.4f -> 0.7f
                else -> 0.6f
            },
            topP = 0.9f,
            maxTokens = 2000,
        ),
    )

    private fun buildPersonalityConfig(character: CustomCharacter): PersonalityConfig {
        val rules = character.behaviorRules ?: BehaviorRules.default()
        val traits = buildList {
            if (character.personality.openness > 0.7f) add("富有创造力")
            if (character.personality.conscientiousness > 0.7f) add("负责可靠")
            if (character.personality.extraversion > 0.7f) add("外向活泼")
            if (character.personality.agreeableness > 0.7f) add("友善体贴")
            if (character.personality.neuroticism < 0.3f) add("情绪稳定")
            addAll(character.personality.customTraits.values)
        }.distinct()
        return PersonalityConfig(
            coreTraits = traits,
            background = character.backstory,
            speakingStyle = SpeakingStyle(
                tone = rules.responseStyle.displayName,
                vocabularyLevel = "日常",
                sentenceLength = "中等",
                useEmoji = rules.emojiFrequency != EmojiFrequency.NONE,
                useExclamation = rules.responseStyle != ResponseStyle.SERIOUS,
                formality = rules.formalityLevel.displayName,
            ),
            interests = rules.topicPreferences,
            values = character.personality.customTraits.values.toList(),
            relationship = Relationship(
                role = "陪伴者",
                distance = "适度",
                interactionStyle = rules.responseStyle.displayName,
                addressUser = "你",
            ),
            behaviorPatterns = listOf(
                "偏好话题：${rules.topicPreferences.joinToString("、")}",
                "避免话题：${rules.avoidTopics.joinToString("、")}",
            ).filterNot { it.endsWith("：") },
        )
    }

    private fun buildSystemPrompt(character: CustomCharacter): String = buildString {
        appendLine("# 角色设定")
        appendLine("你是 ${character.name}。${character.description}")
        appendLine()
        appendLine("## 背景故事")
        appendLine(character.backstory)
        appendLine()
        appendLine("## 人格特质")
        appendLine("- 开放性：${(character.personality.openness * 100).toInt()}%")
        appendLine("- 尽责性：${(character.personality.conscientiousness * 100).toInt()}%")
        appendLine("- 外向性：${(character.personality.extraversion * 100).toInt()}%")
        appendLine("- 宜人性：${(character.personality.agreeableness * 100).toInt()}%")
        appendLine("- 神经质：${(character.personality.neuroticism * 100).toInt()}%")
        character.personality.customTraits.forEach { (name, value) ->
            appendLine("- $name：$value")
        }
        character.behaviorRules?.let { rules ->
            appendLine()
            appendLine("## 行为规则")
            appendLine("- 回复风格：${rules.responseStyle.displayName}")
            appendLine("- 表情频率：${rules.emojiFrequency.displayName}")
            appendLine("- 正式程度：${rules.formalityLevel.displayName}")
            if (rules.topicPreferences.isNotEmpty()) {
                appendLine("- 偏好话题：${rules.topicPreferences.joinToString("、")}")
            }
            if (rules.avoidTopics.isNotEmpty()) {
                appendLine("- 避免话题：${rules.avoidTopics.joinToString("、")}")
            }
        }
        if (character.exampleDialogues.isNotEmpty()) {
            appendLine()
            appendLine("## 示例对话")
            character.exampleDialogues.take(5).forEach { dialogue ->
                appendLine("用户：${dialogue.user}")
                appendLine("${character.name}：${dialogue.assistant}")
            }
        }
        appendLine()
        appendLine("## 初次问候")
        appendLine(character.greetingMessage)
        appendLine()
        appendLine("请始终依据以上设定自然回应，并保持角色一致性。")
    }
}
```

- [ ] **Step 5: Implement and bind the resolver**

```kotlin
@Singleton
class DefaultCharacterPromptResolver @Inject constructor(
    private val characterCatalog: CharacterCatalog,
) : CharacterPromptResolver {
    override suspend fun resolve(characterId: String): ResolvedCharacterPrompt {
        val character = characterCatalog.getCharacter(characterId)
            ?: throw UnknownCharacterException(characterId)
        val config = when (character) {
            is ChatCharacter.BuiltIn -> character.config
            is ChatCharacter.Custom -> CustomCharacterPromptMapper.toCompanionConfig(character.character)
        }
        return ResolvedCharacterPrompt(character, config)
    }
}
```

Bind it in `RepositoryModule` with `@Binds @Singleton`.

- [ ] **Step 6: Make PersonalityManager a replaceable cache, not a source**

Replace the `customCompanions: MutableMap<String, CompanionPersonality>` field with:

```kotlin
private val resolvedCustomConfigs = ConcurrentHashMap<String, CompanionConfig>()

fun cacheResolvedCustomConfig(config: CompanionConfig) {
    resolvedCustomConfigs[config.id] = config
}

fun removeCompanion(companionId: String) {
    resolvedCustomConfigs.remove(companionId)
}

fun getCompanionConfig(companionId: String): CompanionConfig? =
    resolvedCustomConfigs[companionId] ?: configLoader.getCompanionConfig(companionId)
```

Delete `registerCompanion` and `convertToCompanionConfig`; `CustomCharacterPromptMapper` is now the only custom-to-config conversion. The cache is populated only with resolver output and is overwritten after every fresh resolution.

- [ ] **Step 7: Add explicit permanent-memory cache invalidation**

Add to `MemoryLayerManager`:

```kotlin
fun invalidateCharacter(characterId: String) {
    permanentMemoryCache.remove("permanent-$characterId")
}
```

This method is called after each custom config resolution so an edited personality cannot leave stale permanent memories.

- [ ] **Step 8: Resolve before both text and image model requests**

In each send pipeline in `ChatViewModel`, immediately after obtaining `userId` add:

```kotlin
val resolved = characterPromptResolver.resolve(companionId)
if (resolved.character.isCustom) {
    personalityManager.cacheResolvedCustomConfig(resolved.config)
    memoryLayerManager.invalidateCharacter(companionId)
}
val memoryContext = memoryLayerManager.getMemoryContext(
    userId = userId,
    companionId = companionId,
    currentMessage = enhancedUserMessage,
)
val systemPrompt = personalityManager.generateSystemPrompt(
    companionId = companionId,
    userId = userId,
    memoryContext = memoryContext,
)
```

For pure-text send, pass its actual user content instead of `enhancedUserMessage`. Resolve before every send, including retry and image paths; do not reuse a `ResolvedCharacterPrompt` across requests. Existing `adjustResponse(response, companionId)` then reads the just-refreshed cache.

- [ ] **Step 9: Run resolver tests and ChatViewModel compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.data.character.DefaultCharacterPromptResolverTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`; test verifies two Room-backed resolutions produce two names.

- [ ] **Step 10: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/character/CharacterPromptResolver.kt app/src/main/java/com/companion/cc/data/mapper/CustomCharacterPromptMapper.kt app/src/main/java/com/companion/cc/data/character/DefaultCharacterPromptResolver.kt app/src/main/java/com/companion/cc/di/RepositoryModule.kt app/src/main/java/com/companion/cc/domain/manager/PersonalityManager.kt app/src/main/java/com/companion/cc/domain/manager/MemoryLayerManager.kt app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt app/src/test/java/com/companion/cc/data/character/DefaultCharacterPromptResolverTest.kt
git commit -m "feat: resolve custom prompts from persistent data"
```

---

### Task 7: Unify Chat Navigation and Missing-Character Handling

**Files:**
- Create: `app/src/main/java/com/companion/cc/ui/chat/CharacterSessionLoader.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/chat/NaturalChatScreen.kt`
- Create: `app/src/test/java/com/companion/cc/ui/chat/ChatCharacterLoadingTest.kt`

**Interfaces:**
- Consumes: `CharacterCatalog.getCharacter(id)`.
- Produces: `ChatCharacterState.Loading`, `Ready(character)`, or `Missing(id)`; a single `chat/{characterId}` route.

- [ ] **Step 1: Write the missing-character loader test**

```kotlin
package com.companion.cc.ui.chat

import com.companion.cc.domain.character.CharacterCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChatCharacterLoadingTest {
    @Test
    fun `loader reports missing instead of substituting a default character`() = runTest {
        val catalog = mock<CharacterCatalog>()
        whenever(catalog.getCharacter("missing")).thenReturn(null)

        val result = CharacterSessionLoader(catalog).load("missing")

        assertEquals(ChatCharacterState.Missing("missing"), result)
    }
}
```

- [ ] **Step 2: Run the test and verify missing state/API failure**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.chat.ChatCharacterLoadingTest" --stacktrace
```

- [ ] **Step 3: Add explicit character state and loader**

Create `app/src/main/java/com/companion/cc/domain/character/CharacterSessionLoader.kt`:

```kotlin
package com.companion.cc.ui.chat

import com.companion.cc.domain.character.CharacterCatalog
import javax.inject.Inject

class CharacterSessionLoader @Inject constructor(
    private val characterCatalog: CharacterCatalog,
) {
    suspend fun load(characterId: String): ChatCharacterState = try {
        characterCatalog.getCharacter(characterId)
            ?.let(ChatCharacterState::Ready)
            ?: ChatCharacterState.Missing(characterId)
    } catch (error: Exception) {
        ChatCharacterState.Failure(error.message ?: "加载角色失败")
    }
}
```

Add the UI state next to `ChatViewModel`:

```kotlin
sealed interface ChatCharacterState {
    data object Loading : ChatCharacterState
    data class Ready(val character: ChatCharacter) : ChatCharacterState
    data class Missing(val characterId: String) : ChatCharacterState
    data class Failure(val message: String) : ChatCharacterState
}
```

Inject `CharacterSessionLoader` and replace `setCharacter(characterId, isCustomCharacter)` with:

```kotlin
fun setCharacter(characterId: String) {
    viewModelScope.launch {
        _characterState.value = ChatCharacterState.Loading
        val state = characterSessionLoader.load(characterId)
        _characterState.value = state
        if (state is ChatCharacterState.Ready) {
            _currentCharacterId.value = characterId
            _currentCompanionId.value = characterId
            loadMessages(characterId)
        }
    }
}
```

Ensure `loadMessages` cancels the previous collection before starting a new one; keep a `Job?` and call `cancel()` before launching.

- [ ] **Step 4: Remove boolean-based UI behavior**

Change `NaturalChatScreen` to accept only `characterId: String`; call `viewModel.setCharacter(characterId)` once in `LaunchedEffect(characterId)`. Remove the second direct `loadMessages` effect. Derive title/avatar from `ChatCharacterState.Ready`; render a full-page “角色不存在” state for `Missing` with a return button.

- [ ] **Step 5: Collapse routes**

Remove `Screen.CustomCharacterChat`. Rename the route placeholder to `characterId`:

```kotlin
object Chat : Screen("chat/{characterId}") {
    fun createRoute(characterId: String) = "chat/$characterId"
}
```

`NavGraph` must parse a non-null ID; if absent, navigate up instead of substituting `xiaocan`. Home and character-list callbacks both navigate via `Screen.Chat.createRoute(id)`.

- [ ] **Step 6: Run the state test and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.chat.ChatCharacterLoadingTest" --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`; missing ID never invokes message loading.

- [ ] **Step 7: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/domain/character/CharacterSessionLoader.kt app/src/main/java/com/companion/cc/ui/navigation app/src/main/java/com/companion/cc/ui/chat/ChatViewModel.kt app/src/main/java/com/companion/cc/ui/chat/NaturalChatScreen.kt app/src/test/java/com/companion/cc/ui/chat/ChatCharacterLoadingTest.kt
git commit -m "fix: unify character chat navigation"
```

---

### Task 8: Remove Remaining UI-Layer Default User IDs

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/memory/MemoryViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/stats/StatsViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/data/DataManagementViewModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/data/ImmersiveDataManagementScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/data/DataManagementScreen.kt`
- Create: `app/src/test/java/com/companion/cc/ui/memory/MemoryViewModelTest.kt`
- Create: `app/src/test/java/com/companion/cc/ui/stats/StatsViewModelTest.kt`

**Interfaces:**
- Consumes: `CurrentUserProvider.requireUserId()` and `CharacterPromptResolver.resolve(id)`.
- Produces: current-user and character-scoped memory, statistics, export, and storage-count operations.

- [ ] **Step 1: Add failing current-user tests**

For each ViewModel, mock `CurrentUserProvider.requireUserId()` as `"user-real"`, invoke its load/export method with only `characterId`, and verify every repository/DAO call uses `"user-real"` plus that exact character ID. Example:

```kotlin
@Test
fun `memory lookup uses current user and selected character`() = runTest(main.dispatcher) {
    whenever(users.requireUserId()).thenReturn("user-real")
    whenever(resolver.resolve("custom-1")).thenReturn(TestCharacters.resolved("custom-1"))
    whenever(memoryManager.getMemoryContext("user-real", "custom-1", ""))
        .thenReturn(TestMemories.emptyContext)

    viewModel.loadMemories("custom-1")
    advanceUntilIdle()

    verify(memoryManager).getMemoryContext("user-real", "custom-1", "")
}
```

- [ ] **Step 2: Run tests and verify current hardcoded calls fail assertions**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.companion.cc.ui.memory.MemoryViewModelTest" --tests "com.companion.cc.ui.stats.StatsViewModelTest" --stacktrace
```

Expected: failures show `"default"` instead of `"user-real"`.

- [ ] **Step 3: Inject identity and resolver into MemoryViewModel**

```kotlin
val userId = currentUserProvider.requireUserId()
val resolved = characterPromptResolver.resolve(companionId)
if (resolved.character.isCustom) {
    personalityManager.cacheResolvedCustomConfig(resolved.config)
    memoryLayerManager.invalidateCharacter(companionId)
}
val context = memoryLayerManager.getMemoryContext(
    userId = userId,
    companionId = companionId,
    currentMessage = "",
)
```

Add `MutableStateFlow<String?>` named `errorMessage`; set it to `error.message ?: "加载记忆失败"` in the catch branch and clear it before each load so the screen can show a retry action.

- [ ] **Step 4: Inject identity and resolver into StatsViewModel**

Replace all three `"default"` occurrences with one `val userId = currentUserProvider.requireUserId()`. Resolve the character config before `MemoryLayerManager.getMemoryContext`. Preserve `companionId` in message and vector-memory queries.

- [ ] **Step 5: Make DataManagementViewModel own identity lookup**

Change `exportData(userId, companionId)` to `exportData(companionId)`, `clearAllMessages(userId)` to `clearAllMessages()`, and `loadStorageInfo()` to call `currentUserProvider.requireUserId()`. Use database path `cc_database` instead of `companion.db`. Update both Compose screens to stop passing `"default"`.

- [ ] **Step 6: Add a static regression scan**

Run:

```powershell
rg -n 'userId\s*=\s*"default"|"default"\s*,\s*companionId|userId: String = "default"' app/src/main/java/com/companion/cc/ui
```

Expected: no matches. The literal remains allowed only in `LegacyCharacterOwnershipMigrator.LEGACY_USER_ID` and migration tests.

- [ ] **Step 7: Run tests and compile**

```powershell
.\gradlew.bat :app:testDebugUnitTest --stacktrace
.\gradlew.bat :app:compileDebugKotlin --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit if authorized**

```powershell
git add app/src/main/java/com/companion/cc/ui/memory/MemoryViewModel.kt app/src/main/java/com/companion/cc/ui/stats/StatsViewModel.kt app/src/main/java/com/companion/cc/ui/data app/src/test/java/com/companion/cc/ui/memory/MemoryViewModelTest.kt app/src/test/java/com/companion/cc/ui/stats/StatsViewModelTest.kt
git commit -m "fix: scope character tools to current user"
```

---

### Task 9: Verify the Unified Data Flow on an Emulator

**Files:**
- Create: `docs/superpowers/verification/2026-08-15-custom-character-data-flow.md`
- Do not modify: `app/build.gradle.kts` version fields.

**Interfaces:**
- Consumes: all prior task outputs.
- Produces: reproducible automated and device evidence for the first subsystem.

- [ ] **Step 1: Run the full automated gate**

```powershell
.\gradlew.bat clean :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL` with all new tests executed.

- [ ] **Step 2: Build and install the debug APK**

```powershell
adb devices
.\gradlew.bat :app:installDebug --stacktrace
adb shell am force-stop com.companion.cc.debug
adb shell monkey -p com.companion.cc.debug -c android.intent.category.LAUNCHER 1
```

Expected: one authorized emulator/device is `device`, installation succeeds, and the app launches.

- [ ] **Step 3: Execute the data-flow acceptance path**

Perform and record timestamps/log excerpts for:

```text
Create custom role → returns once → role appears on Home
Open role → send distinct message → leave and reopen → same history appears
Edit name and Big Five settings → returns once → Home title updates
Send another message → prompt/log reflects edited role configuration
Force-stop and relaunch → role, name, and persisted personality still work
Open built-in role → its history remains separate from custom UUID history
Open memory/stats/data pages → values use the active real user and selected UUID
Attempt an invalid chat route → “角色不存在”, never Xiaocan
```

- [ ] **Step 4: Capture database proof without mutating data**

Use `run-as` for the debug package and record only counts/ownership, not message content:

```powershell
adb shell run-as com.companion.cc.debug sqlite3 databases/cc_database "select userId,count(*) from custom_characters group by userId;"
adb shell run-as com.companion.cc.debug sqlite3 databases/cc_database "select companion_id,count(*) from messages group by companion_id order by companion_id;"
```

Expected: no active custom row is owned by `default`; each UUID has its own message count.

- [ ] **Step 5: Write the verification record**

The verification document must contain command, exit status, test count, device ID, database counts, manual step result, and any skipped step with reason. Do not state that deletion/capsule, theme, or historical migration safety is complete.

- [ ] **Step 6: Run a final diff and version guard**

```powershell
git diff --check
rg -n 'versionCode\s*=\s*10|versionName\s*=\s*"2\.1\.0-beta\.9"' app/build.gradle.kts
git status --short
```

Expected: no whitespace errors; both original version values remain.

- [ ] **Step 7: Commit verification evidence if authorized**

```powershell
git add docs/superpowers/verification/2026-08-15-custom-character-data-flow.md
git commit -m "test: verify custom character data flow"
```

---

## Self-Review

### Spec coverage in this plan

- Current-user identity and safe legacy ownership migration: Tasks 1–2.
- Unified built-in/custom catalog and Room-driven UI updates: Tasks 3 and 5.
- Explicit save state and duplicate-save prevention: Task 4.
- Fresh persisted prompt resolution after edit/restart: Task 6.
- One ID-only navigation path and explicit missing state: Task 7.
- Character-scoped memory/statistics/data entry points: Task 8.
- Automated and emulator acceptance for the unified flow: Task 9.

### Deliberately separated scope

The following approved requirements form a separately testable subsystem and are not silently omitted: memory capsule storage/encryption/export, transactional permanent deletion, external cleanup retries, revival token validation, new UUID restoration, and Room 10→11 migration. They must be implemented from `2026-08-15-character-memory-capsule-lifecycle.md` after this plan passes Task 9.

### Consistency checks

- `CurrentUserProvider.requireUserId()` is the only imperative identity API used by ViewModels/services.
- `CharacterCatalog.getCharacter(id)` is always current-user scoped for custom rows.
- `CharacterPromptResolver.resolve(id)` returns `ResolvedCharacterPrompt` and throws `UnknownCharacterException` for missing IDs.
- `CharacterSaveState.Success(characterId)` is consumed once before navigation.
- All chat navigation uses `Screen.Chat.createRoute(characterId)`.
- All prompts in model-send paths use a freshly resolved `CompanionConfig`.
