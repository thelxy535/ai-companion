package com.companion.cc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportDataPrivacyTest {
    @Test
    fun exportSettingsDoesNotExposeCredentialByDefault() {
        assertEquals("", ExportSettings().apiKey)
    }
}
