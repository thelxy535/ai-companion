package com.companion.cc.data.local.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class MemoryCapsuleV2TransferServiceTest {
    @Test
    fun importRejectsPayloadLargerThanTenMegabytesBeforeDecoding() = runTest {
        val service = MemoryCapsuleV2TransferService(
            builder = mock(),
            importer = mock(),
            codec = mock()
        )

        val result = service.importCapsuleJson("x".repeat(10 * 1024 * 1024 + 1), "user:u:companion:c")

        assertTrue(result.isFailure)
    }
}
