package dev.buni.core

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BlocklistIntegrityTest {

    @Test
    fun testBuiltinBlocklistIntegrity() {
        val candidates = listOf(
            File("src/main/assets/blocklist.txt"),
            File("app/src/main/assets/blocklist.txt"),
            File("android/app/src/main/assets/blocklist.txt")
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("blocklist.txt asset file not found", file)

        val domains = mutableSetOf<String>()
        file!!.forEachLine { line ->
            val clean = DnsEngine.cleanDomainLine(line)
            if (clean != null) {
                domains.add(clean)
            }
        }

        assertTrue("Blocklist must contain curated OEM rules", domains.size >= 100)

        val criticalTrackers = listOf(
            "admob.com",
            "doubleclick.net",
            "st-sdk.vivo.com",
            "analytics.transsion.com",
            "sec.miui.com"
        )

        for (tracker in criticalTrackers) {
            val matched = DnsEngine.findMatchingBlockedDomain(tracker, domains)
            assertNotNull("Blocklist should block critical tracker: $tracker", matched)
        }
    }
}
