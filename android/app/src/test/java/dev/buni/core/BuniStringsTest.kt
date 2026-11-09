package dev.buni.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuniStringsTest {

    @Test
    fun testAllKeysDefinedInBothLanguages() {
        val keys = BuniStrings.getSupportedKeys()
        assertTrue(keys.isNotEmpty())

        for (key in keys) {
            assertTrue("Key $key missing in EN map", BuniStrings.hasKey(key, Language.EN))
            assertTrue("Key $key missing in ID map", BuniStrings.hasKey(key, Language.ID))

            val en = BuniStrings.get(key, Language.EN)
            val id = BuniStrings.get(key, Language.ID)

            assertTrue("Key $key has blank EN string", en.isNotBlank())
            assertTrue("Key $key has blank ID string", id.isNotBlank())
        }
    }

    @Test
    fun testUnknownKeyFallback() {
        val unknown = "non_existent_key_12345"
        assertEquals(unknown, BuniStrings.get(unknown, Language.EN))
        assertEquals(unknown, BuniStrings.get(unknown, Language.ID))
    }
}
