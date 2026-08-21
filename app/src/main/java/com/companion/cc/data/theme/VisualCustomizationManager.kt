package com.companion.cc.data.theme

import android.net.Uri
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.BackdropKind
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.domain.model.VisualBackdrop
import com.companion.cc.domain.model.VisualCustomization
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Typed visual preferences plus safe private-backdrop lifecycle management. */
@Singleton
class VisualCustomizationManager @Inject constructor(
    private val settingsManager: SettingsManager,
    private val codec: VisualCustomizationCodec,
    private val backdropStore: VisualBackdropStore
) {
    val customizationFlow: Flow<VisualCustomization> =
        settingsManager.visualCustomizationFlow.map(codec::decode)

    suspend fun save(customization: VisualCustomization) {
        settingsManager.saveVisualCustomization(codec.encode(customization))
    }

    suspend fun replaceBackdrop(target: BackdropTarget, source: Uri): VisualCustomization {
        val current = customizationFlow.first()
        val stagedReference = backdropStore.stage(source)
        val previous = current.backdropFor(target)
        val next = current.withBackdrop(
            target,
            VisualBackdrop(
                kind = BackdropKind.IMAGE,
                managedImageReference = stagedReference,
                imageOpacity = previous.imageOpacity,
                scrimOpacity = previous.scrimOpacity
            )
        )

        try {
            save(next)
        } catch (error: Exception) {
            backdropStore.discardManaged(stagedReference)
            throw error
        }
        discardReferencesRemovedBy(current, next)
        return next
    }

    suspend fun clearBackdrop(target: BackdropTarget): VisualCustomization {
        val current = customizationFlow.first()
        val previous = current.backdropFor(target)
        val next = current.withBackdrop(
            target,
            VisualBackdrop(
                imageOpacity = previous.imageOpacity,
                scrimOpacity = previous.scrimOpacity
            )
        )
        save(next)
        discardReferencesRemovedBy(current, next)
        return next
    }

    suspend fun reset(): VisualCustomization {
        val current = customizationFlow.first()
        val next = VisualCustomization.default()
        save(next)
        discardReferencesRemovedBy(current, next)
        return next
    }

    private fun VisualCustomization.withBackdrop(
        target: BackdropTarget,
        backdrop: VisualBackdrop
    ): VisualCustomization = copy(backdrops = backdrops + (target to backdrop))

    private fun discardReferencesRemovedBy(
        previous: VisualCustomization,
        next: VisualCustomization
    ) {
        (previous.managedReferences() - next.managedReferences())
            .forEach(backdropStore::discardManaged)
    }

    private fun VisualCustomization.managedReferences(): Set<String> =
        backdrops.values.mapNotNull { backdrop ->
            backdrop.managedImageReference.takeIf { backdrop.kind == BackdropKind.IMAGE }
        }.toSet()
}
