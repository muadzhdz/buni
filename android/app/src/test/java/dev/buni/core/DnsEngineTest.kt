package dev.buni.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsEngineTest {

    @Test
    fun testParseDomainValid() {
        val query = byteArrayOf(
            0x12, 0x34,
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
            0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0x00,
            0x00, 0x01,
            0x00, 0x01
        )

        val domain = DnsEngine.parseDomain(query)
        assertEquals("example.com", domain)
    }

    @Test
    fun testParseDomainMalformed() {
        assertEquals("", DnsEngine.parseDomain(byteArrayOf()))
        assertEquals("", DnsEngine.parseDomain(ByteArray(10)))
        val truncated = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x05, 'a'.code.toByte())
        assertEquals("", DnsEngine.parseDomain(truncated))
    }

    @Test
    fun testBuildSinkholeResponseA() {
        val query = byteArrayOf(
            0xAB.toByte(), 0xCD.toByte(),
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
            0x03, 'a'.code.toByte(), 'd'.code.toByte(), 's'.code.toByte(),
            0x00,
            0x00, 0x01,
            0x00, 0x01
        )

        val resp = DnsEngine.buildSinkholeResponse(query)
        assertNotNull(resp)
        assertEquals(query[0], resp!![0])
        assertEquals(query[1], resp[1])

        val flags = ((resp[2].toInt() and 0xFF) shl 8) or (resp[3].toInt() and 0xFF)
        assertTrue((flags and 0x8000) != 0)
        assertEquals(0, flags and 0x000F)

        assertEquals(0x00.toByte(), resp[6])
        assertEquals(0x01.toByte(), resp[7])

        val len = resp.size
        assertEquals(0.toByte(), resp[len - 4])
        assertEquals(0.toByte(), resp[len - 3])
        assertEquals(0.toByte(), resp[len - 2])
        assertEquals(0.toByte(), resp[len - 1])
    }

    @Test
    fun testBuildSinkholeResponseAAAA() {
        val query = byteArrayOf(
            0x42, 0x42,
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
            0x03, 'a'.code.toByte(), 'd'.code.toByte(), 's'.code.toByte(),
            0x00,
            0x00, 0x1C,
            0x00, 0x01
        )

        val resp = DnsEngine.buildSinkholeResponse(query)
        assertNotNull(resp)
        assertEquals(0x00.toByte(), resp!![6])
        assertEquals(0x01.toByte(), resp[7])

        val len = resp.size
        for (i in (len - 16) until len) {
            assertEquals(0.toByte(), resp[i])
        }
    }

    @Test
    fun testBuildSinkholeResponseNoDataForOtherTypes() {
        val query = byteArrayOf(
            0x77, 0x77,
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
            0x03, 'a'.code.toByte(), 'd'.code.toByte(), 's'.code.toByte(),
            0x00,
            0x00, 0x10,
            0x00, 0x01
        )

        val resp = DnsEngine.buildSinkholeResponse(query)
        assertNotNull(resp)
        assertEquals(0x00.toByte(), resp!![6])
        assertEquals(0x00.toByte(), resp[7])
        assertEquals(query.size, resp.size)
    }

    @Test
    fun testFindMatchingBlockedDomain() {
        val blocked = setOf("doubleclick.net", "track.vivo.com.cn")

        assertEquals("doubleclick.net", DnsEngine.findMatchingBlockedDomain("doubleclick.net", blocked))
        assertEquals("doubleclick.net", DnsEngine.findMatchingBlockedDomain("ad.doubleclick.net", blocked))
        assertEquals("doubleclick.net", DnsEngine.findMatchingBlockedDomain("sub.ad.doubleclick.net", blocked))
        assertEquals("track.vivo.com.cn", DnsEngine.findMatchingBlockedDomain("api.track.vivo.com.cn", blocked))
        assertNull(DnsEngine.findMatchingBlockedDomain("google.com", blocked))
        assertNull(DnsEngine.findMatchingBlockedDomain("notdoubleclick.net", blocked))
    }

    @Test
    fun testClassifyDomain() {
        assertEquals(DomainCategory.ADS, DnsEngine.classifyDomain("admob.com"))
        assertEquals(DomainCategory.TRACKERS, DnsEngine.classifyDomain("analytics.transsion.com"))
        assertEquals(DomainCategory.MALWARE, DnsEngine.classifyDomain("sec.miui.com"))
        assertEquals(DomainCategory.MALWARE, DnsEngine.classifyDomain("phishing-portal.com"))
    }

    @Test
    fun testIsDomainBlockedWithToggles() {
        val blocked = setOf("admob.com", "analytics.example.com", "sec.miui.com")

        assertTrue(DnsEngine.isDomainBlocked("admob.com", blocked, filterAds = true, filterTrackers = true, filterMalware = true))
        assertFalse(DnsEngine.isDomainBlocked("admob.com", blocked, filterAds = false, filterTrackers = true, filterMalware = true))

        assertTrue(DnsEngine.isDomainBlocked("analytics.example.com", blocked, filterAds = true, filterTrackers = true, filterMalware = true))
        assertFalse(DnsEngine.isDomainBlocked("analytics.example.com", blocked, filterAds = true, filterTrackers = false, filterMalware = true))

        assertTrue(DnsEngine.isDomainBlocked("sec.miui.com", blocked, filterAds = true, filterTrackers = true, filterMalware = true))
        assertFalse(DnsEngine.isDomainBlocked("sec.miui.com", blocked, filterAds = true, filterTrackers = true, filterMalware = false))
    }

    @Test
    fun testCraftReplyPacket() {
        val reqPacket = ByteArray(28)
        reqPacket[0] = 0x45.toByte()
        reqPacket[12] = 10.toByte()
        reqPacket[13] = 0.toByte()
        reqPacket[14] = 0.toByte()
        reqPacket[15] = 2.toByte()

        reqPacket[16] = 10.toByte()
        reqPacket[17] = 233.toByte()
        reqPacket[18] = 0.toByte()
        reqPacket[19] = 2.toByte()

        reqPacket[20] = 0xC0.toByte()
        reqPacket[21] = 0x01.toByte()
        reqPacket[22] = 0x00.toByte()
        reqPacket[23] = 0x35.toByte()

        val payload = byteArrayOf(1, 2, 3, 4, 5, 6)
        val reply = DnsEngine.craftReplyPacket(reqPacket, 20, payload)

        assertEquals(28 + payload.size, reply.size)
        assertEquals(10.toByte(), reply[12])
        assertEquals(233.toByte(), reply[13])
        assertEquals(0.toByte(), reply[14])
        assertEquals(2.toByte(), reply[15])

        assertEquals(10.toByte(), reply[16])
        assertEquals(0.toByte(), reply[17])
        assertEquals(0.toByte(), reply[18])
        assertEquals(2.toByte(), reply[19])

        assertEquals(0x00.toByte(), reply[20])
        assertEquals(0x35.toByte(), reply[21])
        assertEquals(0xC0.toByte(), reply[22])
        assertEquals(0x01.toByte(), reply[23])
    }

    @Test
    fun testIsValidIpv4() {
        assertTrue(DnsEngine.isValidIpv4("1.1.1.1"))
        assertTrue(DnsEngine.isValidIpv4("8.8.8.8"))
        assertTrue(DnsEngine.isValidIpv4("94.140.14.14"))
        assertTrue(DnsEngine.isValidIpv4("192.168.1.1"))
        assertTrue(DnsEngine.isValidIpv4("0.0.0.0"))
        assertTrue(DnsEngine.isValidIpv4("255.255.255.255"))

        assertFalse(DnsEngine.isValidIpv4("256.0.0.1"))
        assertFalse(DnsEngine.isValidIpv4("1.1.1.1.1"))
        assertFalse(DnsEngine.isValidIpv4("1.1.1"))
        assertFalse(DnsEngine.isValidIpv4("-1.0.0.0"))
        assertFalse(DnsEngine.isValidIpv4(""))
        assertFalse(DnsEngine.isValidIpv4("   "))
        assertFalse(DnsEngine.isValidIpv4("abc.def.ghi.jkl"))
        assertFalse(DnsEngine.isValidIpv4("1.1.1.01"))
    }

    @Test
    fun testCleanDomainLine() {
        assertEquals("doubleclick.net", DnsEngine.cleanDomainLine("doubleclick.net"))
        assertEquals("tracking.example.com", DnsEngine.cleanDomainLine("0.0.0.0 tracking.example.com"))
        assertEquals("bad.ad.org", DnsEngine.cleanDomainLine("127.0.0.1 bad.ad.org"))
        assertEquals("ads.domain.com", DnsEngine.cleanDomainLine("ads.domain.com."))
        assertEquals("ad.network.com", DnsEngine.cleanDomainLine("0.0.0.0 \t   ad.network.com"))

        assertNull(DnsEngine.cleanDomainLine("# this is a comment"))
        assertNull(DnsEngine.cleanDomainLine("   "))
        assertNull(DnsEngine.cleanDomainLine(""))
        assertNull(DnsEngine.cleanDomainLine("127.0.0.1 localhost"))
        assertNull(DnsEngine.cleanDomainLine("0.0.0.0 broadcasthost"))
    }

    @Test
    fun testMalformedPacketsTruncated() {
        assertEquals("", DnsEngine.parseDomain(ByteArray(0)))
        assertEquals("", DnsEngine.parseDomain(ByteArray(5)))
        assertEquals("", DnsEngine.parseDomain(ByteArray(11)))

        assertNull(DnsEngine.buildSinkholeResponse(ByteArray(0)))
        assertNull(DnsEngine.buildSinkholeResponse(ByteArray(11)))
    }

    @Test
    fun testOddLengthChecksum() {
        val oddData = byteArrayOf(0x45, 0x00, 0x00, 0x3C, 0x1C)
        val csum = DnsEngine.calculateChecksum(oddData, 0, oddData.size)
        assertTrue(csum in 1..0xFFFF)
    }

    @Test
    fun testIsValidIpv6() {
        assertTrue(DnsEngine.isValidIpv6("2001:4860:4860::8888"))
        assertTrue(DnsEngine.isValidIpv6("2606:4700:4700::1111"))
        assertTrue(DnsEngine.isValidIpv6("::1"))
        assertTrue(DnsEngine.isValidIpv6("fd00:1:fd00:1::2"))

        assertFalse(DnsEngine.isValidIpv6("1.1.1.1"))
        assertFalse(DnsEngine.isValidIpv6("invalid:ipv6:address:::"))
        assertFalse(DnsEngine.isValidIpv6(""))
        assertFalse(DnsEngine.isValidIpv6("   "))
    }

    @Test
    fun testIsValidIpAddress() {
        assertTrue(DnsEngine.isValidIpAddress("1.1.1.1"))
        assertTrue(DnsEngine.isValidIpAddress("8.8.4.4"))
        assertTrue(DnsEngine.isValidIpAddress("2001:4860:4860::8888"))
        assertTrue(DnsEngine.isValidIpAddress("fd00:1:fd00:1::2"))

        assertFalse(DnsEngine.isValidIpAddress("not_an_ip"))
        assertFalse(DnsEngine.isValidIpAddress(""))
        assertFalse(DnsEngine.isValidIpAddress("   "))
    }

    @Test
    fun testCraftReplyPacketIpv6() {
        val req = ByteArray(60)
        req[0] = 0x60.toByte()
        req[6] = 17.toByte()
        for (i in 8 until 24) {
            req[i] = (i and 0xFF).toByte()
        }
        for (i in 24 until 40) {
            req[i] = (0xF0 or (i and 0x0F)).toByte()
        }
        req[40] = 0x30.toByte()
        req[41] = 0x39.toByte()
        req[42] = 0x00.toByte()
        req[43] = 0x35.toByte()
        req[44] = 0x00.toByte()
        req[45] = 0x14.toByte()

        val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val reply = DnsEngine.craftReplyPacketIpv6(req, payload)

        assertEquals(40 + 8 + payload.size, reply.size)
        assertEquals(6, (reply[0].toInt() shr 4) and 0x0F)
        assertEquals(17.toByte(), reply[6])
        assertEquals(64.toByte(), reply[7])

        for (i in 0 until 16) {
            assertEquals(req[24 + i], reply[8 + i])
            assertEquals(req[8 + i], reply[24 + i])
        }

        assertEquals(0x00.toByte(), reply[40])
        assertEquals(0x35.toByte(), reply[41])
        assertEquals(0x30.toByte(), reply[42])
        assertEquals(0x39.toByte(), reply[43])

        val udpLen = ((reply[44].toInt() and 0xFF) shl 8) or (reply[45].toInt() and 0xFF)
        assertEquals(8 + payload.size, udpLen)

        val csum = ((reply[46].toInt() and 0xFF) shl 8) or (reply[47].toInt() and 0xFF)
        assertTrue(csum != 0)

        for (i in payload.indices) {
            assertEquals(payload[i], reply[48 + i])
        }
    }
}
