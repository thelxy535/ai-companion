package com.companion.cc.ui.data

import android.content.Context
import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.local.dao.MemoryDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.VectorMemoryDao
import com.companion.cc.data.local.repository.MemoryCapsuleV2Importer
import com.companion.cc.data.local.repository.MemoryCapsuleV2Transfer
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.usecase.ExportDataUseCase
import com.companion.cc.domain.usecase.ImportDataUseCase
import com.companion.cc.domain.usecase.ImportResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataManagementViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun exportSuccessCreatesPendingDocumentAndReturnsToIdle() = runTest(main.dispatcher) {
        val export = mock<ExportDataUseCase>()
        whenever(export()).thenReturn("{\"version\":\"1.0\"}")
        val viewModel = createViewModel(exportDataUseCase = export)

        viewModel.exportData()
        advanceUntilIdle()

        assertEquals("{\"version\":\"1.0\"}", viewModel.pendingDataExport.value?.json)
        assertFalse(viewModel.isProcessing.value)
        verify(export)()
    }

    @Test
    fun consumingPendingDataExportClearsOneShotRequest() = runTest(main.dispatcher) {
        val export = mock<ExportDataUseCase>()
        whenever(export()).thenReturn("{}")
        val viewModel = createViewModel(exportDataUseCase = export)
        viewModel.exportData()
        advanceUntilIdle()

        viewModel.consumePendingDataExport()

        assertNull(viewModel.pendingDataExport.value)
    }

    @Test
    fun exportFailureShowsMessageAndReturnsToIdle() = runTest(main.dispatcher) {
        val export = mock<ExportDataUseCase>()
        whenever(export()).thenThrow(IllegalStateException("仓库不可用"))
        val viewModel = createViewModel(exportDataUseCase = export)

        viewModel.exportData()
        advanceUntilIdle()

        assertEquals("导出失败: 仓库不可用", viewModel.resultMessage.value)
        assertNull(viewModel.pendingDataExport.value)
        assertFalse(viewModel.isProcessing.value)
    }

    @Test
    fun importDelegatesToUseCaseAndShowsSuccess() = runTest(main.dispatcher) {
        val import = mock<ImportDataUseCase>()
        whenever(import("payload")).thenReturn(ImportResult.Success(3, true))
        val viewModel = createViewModel(importDataUseCase = import)

        viewModel.importData("payload")
        advanceUntilIdle()

        verify(import)("payload")
        assertEquals("成功导入 3 条消息", viewModel.resultMessage.value)
        assertFalse(viewModel.isProcessing.value)
    }

    @Test
    fun importErrorIsVisibleAndReturnsToIdle() = runTest(main.dispatcher) {
        val import = mock<ImportDataUseCase>()
        whenever(import("payload")).thenReturn(ImportResult.Error("版本不受支持"))
        val viewModel = createViewModel(importDataUseCase = import)

        viewModel.importData("payload")
        advanceUntilIdle()

        assertEquals("导入失败: 版本不受支持", viewModel.resultMessage.value)
        assertFalse(viewModel.isProcessing.value)
    }

    @Test
    fun exportMemoryCapsuleUsesTheCurrentUserAndCharacterScope() = runTest(main.dispatcher) {
        val transfer = mock<MemoryCapsuleV2Transfer>()
        whenever(transfer.exportCapsuleJson("user:user-1:companion:character-1"))
            .thenReturn(Result.success("{\"format\":\"cc-switch.memory-capsule\"}"))
        val viewModel = createViewModel(capsuleTransfer = transfer)

        viewModel.exportMemoryCapsule("character-1")
        advanceUntilIdle()

        verify(transfer).exportCapsuleJson("user:user-1:companion:character-1")
        assertEquals(
            "{\"format\":\"cc-switch.memory-capsule\"}",
            viewModel.pendingCapsuleExport.value?.json
        )
    }

    @Test
    fun importMemoryCapsuleUsesTheCurrentUserAndCharacterScope() = runTest(main.dispatcher) {
        val transfer = mock<MemoryCapsuleV2Transfer>()
        val report = MemoryCapsuleV2Importer.Report(insertedNodes = 2)
        whenever(
            transfer.importCapsuleJson(
                "{\"format\":\"cc-switch.memory-capsule\"}",
                "user:user-1:companion:character-1"
            )
        ).thenReturn(Result.success(report))
        val viewModel = createViewModel(capsuleTransfer = transfer)

        viewModel.importMemoryCapsule(
            json = "{\"format\":\"cc-switch.memory-capsule\"}",
            companionId = "character-1"
        )
        advanceUntilIdle()

        verify(transfer).importCapsuleJson(
            "{\"format\":\"cc-switch.memory-capsule\"}",
            "user:user-1:companion:character-1"
        )
        assertEquals(report, viewModel.lastCapsuleImportReport.value)
    }

    private suspend fun createViewModel(
        messageRepository: MessageRepository = mock(),
        capsuleTransfer: MemoryCapsuleV2Transfer = mock(),
        exportDataUseCase: ExportDataUseCase = mock(),
        importDataUseCase: ImportDataUseCase = mock()
    ): DataManagementViewModel {
        val settingsManager = mock<SettingsManager>()
        val memoryDao = mock<MemoryDao>()
        val memoryNodeDao = mock<MemoryNodeDao>()
        val vectorMemoryDao = mock<VectorMemoryDao>()
        val users = mock<CurrentUserProvider>()
        val context: Context = RuntimeEnvironment.getApplication()

        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(messageRepository.observeMessageCount("user-1")).thenReturn(flowOf(0))
        whenever(memoryDao.observeMemoryCount("user-1")).thenReturn(flowOf(0))
        whenever(memoryNodeDao.observeActiveCount("user-1")).thenReturn(flowOf(0))
        whenever(vectorMemoryDao.observeCount("user-1")).thenReturn(flowOf(0))

        return DataManagementViewModel(
            messageRepository = messageRepository,
            settingsManager = settingsManager,
            memoryDao = memoryDao,
            memoryNodeDao = memoryNodeDao,
            vectorMemoryDao = vectorMemoryDao,
            currentUserProvider = users,
            context = context,
            capsuleTransfer = capsuleTransfer,
            exportDataUseCase = exportDataUseCase,
            importDataUseCase = importDataUseCase
        )
    }
}
