package com.companion.cc.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MojibakeRepairTest {
    @Test
    fun repairsUtf8DecodedAsGbkWithoutChangingNormalChinese() {
        assertEquals("关系理解", MojibakeRepair.repair("鍏崇郴鐞嗚В"))
        assertEquals("助手隐藏关心", MojibakeRepair.repair("助手隐藏关心"))
    }

    @Test
    fun leavesEnglishAndEmptyTextUntouched() {
        assertEquals("relationship_narrative", MojibakeRepair.repair("relationship_narrative"))
        assertEquals("", MojibakeRepair.repair(""))
    }

    @Test
    fun repairsLatin1AndWindows1252Mojibake() {
        assertEquals("你好", MojibakeRepair.repair("ä½ å¥½"))
        assertEquals("记忆节点", MojibakeRepair.repair("è®°å¿†èŠ‚ç‚¹"))
    }
}
