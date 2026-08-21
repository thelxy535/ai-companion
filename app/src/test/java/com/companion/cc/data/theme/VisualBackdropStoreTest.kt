package com.companion.cc.data.theme

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VisualBackdropStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var store: VisualBackdropStore
    private lateinit var managedDirectory: File

    @Before
    fun setUp() {
        store = VisualBackdropStore(context)
        managedDirectory = File(context.filesDir, "visual-backdrops")
        managedDirectory.deleteRecursively()
        managedDirectory.mkdirs()
    }

    @After
    fun tearDown() {
        managedDirectory.deleteRecursively()
    }

    @Test
    fun `discard managed removes a file inside the owned backdrop directory`() {
        val managedFile = File(managedDirectory, "chat.webp").apply {
            writeText("owned")
        }

        val reference = managedFile.toURI().toString()
        store.discardManaged(reference)

        assertFalse(managedFile.exists())
    }

    @Test
    fun `discard managed refuses to delete a file outside the owned backdrop directory`() {
        val unrelatedFile = File(context.cacheDir, "must-keep.txt").apply {
            writeText("unrelated")
        }

        store.discardManaged(Uri.fromFile(unrelatedFile).toString())

        assertTrue(unrelatedFile.exists())
        unrelatedFile.delete()
    }

    @Test
    fun `discard managed refuses a sibling path with the same prefix`() {
        val siblingFile = File(context.filesDir, "visual-backdrops-copy/keep.webp").apply {
            parentFile?.mkdirs()
            writeText("unrelated")
        }

        store.discardManaged(Uri.fromFile(siblingFile).toString())

        assertTrue(siblingFile.exists())
        siblingFile.delete()
        siblingFile.parentFile?.delete()
    }

    @Test
    fun `imports a supported image into the managed directory`() {
        val reference = store.importImage(ByteArrayInputStream(validPngBytes()))

        assertTrue(reference.startsWith("file:"))
        assertEquals(1, managedDirectory.listFiles()?.size)
        assertTrue(requireNotNull(managedDirectory.listFiles()).single().isFile)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an unsupported file without leaving a managed file`() {
        try {
            store.importImage(ByteArrayInputStream("not an image".encodeToByteArray()))
        } finally {
            assertEquals(0, managedDirectory.listFiles()?.size)
        }
    }

    private fun validPngBytes(): ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    )
}
