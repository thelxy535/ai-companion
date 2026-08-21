package com.companion.cc.data.identity

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.identity.CurrentUserProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed implementation of [CurrentUserProvider].
 *
 * Wraps [SettingsManager.userIdFlow] and ensures all emitted values are non-blank.
 */
@Singleton
class DataStoreCurrentUserProvider @Inject constructor(
    private val settingsManager: SettingsManager
) : CurrentUserProvider {

    override val userId: Flow<String> = settingsManager.userIdFlow
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctUntilChanged()

    override suspend fun requireUserId(): String {
        return userId.first()
    }
}
