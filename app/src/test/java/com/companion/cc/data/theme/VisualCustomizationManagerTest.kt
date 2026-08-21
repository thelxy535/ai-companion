package com.companion.cc.data.theme

import android.net.Uri
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.BackdropKind
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.domain.model.VisualBackdrop
import com.companion.cc.domain.model.VisualCustomization
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class VisualCustomizationManagerTest {
    @Test
    fun `uses defaults when no stored customization exists`() = runTest {
        val settingsManager = mock<SettingsManager>()
        val backdropStore = mock<VisualBackdropStore>()
        whenever(settingsManager.visualCustomizationFlow).thenReturn(flowOf(null))
        val manager = VisualCustomizationManager(
            settingsManager = settingsManager,
            codec = VisualCustomizationCodec(com.google.gson.Gson()),
            backdropStore = backdropStore
        )

        assertEquals(VisualCustomization.default(), manager.customizationFlow.first())
    }

    @Test
    fun `serializes customization before saving it`() = runTest {
        val settingsManager = mock<SettingsManager>()
        val backdropStore = mock<VisualBackdropStore>()
        whenever(settingsManager.visualCustomizationFlow).thenReturn(flowOf(null))
        val codec = VisualCustomizationCodec(com.google.gson.Gson())
        val manager = VisualCustomizationManager(settingsManager, codec, backdropStore)
        val customization = VisualCustomization(accentHex = "#7A6FF0")

        manager.save(customization)

        val serialized = argumentCaptor<String>()
        verify(settingsManager).saveVisualCustomization(serialized.capture())
        assertEquals(customization, codec.decode(serialized.firstValue))
    }

    @Test
    fun `replacing a backdrop removes the superseded unmanaged reference`() = runTest {
        val oldReference = "file:/managed/old.webp"
        val newReference = "file:/managed/new.webp"
        val current = VisualCustomization(
            backdrops = mapOf(
                BackdropTarget.CHAT to VisualBackdrop(
                    kind = BackdropKind.IMAGE,
                    managedImageReference = oldReference
                )
            )
        )
        val settingsManager = mock<SettingsManager>()
        val backdropStore = mock<VisualBackdropStore>()
        val codec = VisualCustomizationCodec(com.google.gson.Gson())
        whenever(settingsManager.visualCustomizationFlow).thenReturn(flowOf(codec.encode(current)))
        whenever(backdropStore.stage(any())).thenReturn(newReference)
        val manager = VisualCustomizationManager(settingsManager, codec, backdropStore)

        val source = mock<Uri>()
        manager.replaceBackdrop(BackdropTarget.CHAT, source)

        verify(backdropStore).discardManaged(oldReference)
        val serialized = argumentCaptor<String>()
        verify(settingsManager).saveVisualCustomization(serialized.capture())
        assertEquals(newReference, codec.decode(serialized.firstValue)
            .backdropFor(BackdropTarget.CHAT).managedImageReference)
    }

    @Test
    fun `clearing one shared backdrop keeps the file used by another target`() = runTest {
        val sharedReference = "file:/managed/shared.webp"
        val current = VisualCustomization(
            backdrops = mapOf(
                BackdropTarget.CHAT to VisualBackdrop(BackdropKind.IMAGE, sharedReference),
                BackdropTarget.HOME to VisualBackdrop(BackdropKind.IMAGE, sharedReference)
            )
        )
        val settingsManager = mock<SettingsManager>()
        val backdropStore = mock<VisualBackdropStore>()
        val codec = VisualCustomizationCodec(com.google.gson.Gson())
        whenever(settingsManager.visualCustomizationFlow).thenReturn(flowOf(codec.encode(current)))
        val manager = VisualCustomizationManager(settingsManager, codec, backdropStore)

        manager.clearBackdrop(BackdropTarget.CHAT)

        verify(backdropStore, never()).discardManaged(sharedReference)
    }

    @Test
    fun `reset restores defaults and clears every unreferenced managed backdrop`() = runTest {
        val chatReference = "file:/managed/chat.webp"
        val utilityReference = "file:/managed/utility.webp"
        val current = VisualCustomization(
            accentHex = "#7A6FF0",
            effectsPreference = com.companion.cc.ui.theme.VisualEffectsPreference.REDUCED,
            backdrops = mapOf(
                BackdropTarget.CHAT to VisualBackdrop(BackdropKind.IMAGE, chatReference),
                BackdropTarget.UTILITY to VisualBackdrop(BackdropKind.IMAGE, utilityReference)
            )
        )
        val settingsManager = mock<SettingsManager>()
        val backdropStore = mock<VisualBackdropStore>()
        val codec = VisualCustomizationCodec(com.google.gson.Gson())
        whenever(settingsManager.visualCustomizationFlow).thenReturn(flowOf(codec.encode(current)))
        val manager = VisualCustomizationManager(settingsManager, codec, backdropStore)

        val reset = manager.reset()

        assertEquals(VisualCustomization.default(), reset)
        verify(backdropStore).discardManaged(chatReference)
        verify(backdropStore).discardManaged(utilityReference)
    }
}
