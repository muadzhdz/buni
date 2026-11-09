package dev.buni.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class DnsPipelineEndToEndTest {

    private fun buildDnsQueryPayload(domain: String, qType: Int = 1): ByteArray {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0x12, 0x34))
        bos.write(byteArrayOf(0x01, 0x00))
        bos.write(byteArrayOf(0x00, 0x01))
        bos.write(byteArrayOf(0x00, 0x00))
        bos.write(byteArrayOf(0x00, 0x00))
        bos.write(byteArrayOf(0x00, 0x00))

        val parts = domain.split(".")
        for (part in parts) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            bos.write(bytes.size)
            bos.write(bytes)
        }
        bos.write(0x00)

        bos.write((qType shr 8) and 0xFF)
        bos.write(qType and 0xFF)
        bos.write(0x00)
        bos.write(0x01)

        return bos.toByteArray()
    }

    private fun buildRawIpv4UdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val packet = ByteArray(totalLen)

        packet[0] = 0x45.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x12.toByte()
        packet[5] = 0x34.toByte()
        packet[6] = 0x40.toByte()
        packet[7] = 0x00.toByte()
        packet[8] = 64.toByte()
        packet[9] = 17.toByte()
        packet[10] = 0.toByte()
        packet[11] = 0.toByte()

        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)

        val ipCsum = DnsEngine.calculateChecksum(packet, 0, 20)
        packet[10] = ((ipCsum shr 8) and 0xFF).toByte()
        packet[11] = (ipCsum and 0xFF).toByte()

        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()

        val udpLen = 8 + payload.size
        packet[24] = ((udpLen shr 8) and 0xFF).toByte()
        packet[25] = (udpLen and 0xFF).toByte()
        packet[26] = 0.toByte()
        packet[27] = 0.toByte()

        System.arraycopy(payload, 0, packet, 28, payload.size)

        val udpCsum = DnsEngine.calculateUdpChecksum(packet, 12, 16, 20, udpLen)
        packet[26] = ((udpCsum shr 8) and 0xFF).toByte()
        packet[27] = (udpCsum and 0xFF).toByte()

        return packet
    }

    @Test
    fun testSinkholePipelineFullCycle() {
        val blockedDomains = setOf("doubleclick.net", "admob.com", "sec.miui.com")
        val dnsPayload = buildDnsQueryPayload("ad.doubleclick.net", qType = 1)

        val clientIp = byteArrayOf(10, 233.toByte(), 0, 1)
        val vpnIp = byteArrayOf(10, 233.toByte(), 0, 2)
        val clientPort = 54321
        val dnsPort = 53

        val rawRequest = buildRawIpv4UdpPacket(clientIp, vpnIp, clientPort, dnsPort, dnsPayload)

        val ihl = (rawRequest[0].toInt() and 0x0F) * 4
        assertEquals(20, ihl)

        val parsedDomain = DnsEngine.parseDomain(dnsPayload)
        assertEquals("ad.doubleclick.net", parsedDomain)

        val isBlocked = DnsEngine.isDomainBlocked(
            parsedDomain,
            blockedDomains,
            filterAds = true,
            filterTrackers = true,
            filterMalware = true
        )
        assertTrue(isBlocked)

        val sinkholeDnsReply = DnsEngine.buildSinkholeResponse(dnsPayload)
        assertNotNull(sinkholeDnsReply)

        val rawReply = DnsEngine.craftReplyPacket(rawRequest, ihl, sinkholeDnsReply!!)

        assertEquals(20 + 8 + sinkholeDnsReply.size, rawReply.size)

        assertEquals(0x45.toByte(), rawReply[0])
        assertEquals(64.toByte(), rawReply[8])
        assertEquals(17.toByte(), rawReply[9])

        assertEquals(vpnIp[0], rawReply[12])
        assertEquals(vpnIp[1], rawReply[13])
        assertEquals(vpnIp[2], rawReply[14])
        assertEquals(vpnIp[3], rawReply[15])

        assertEquals(clientIp[0], rawReply[16])
        assertEquals(clientIp[1], rawReply[17])
        assertEquals(clientIp[2], rawReply[18])
        assertEquals(clientIp[3], rawReply[19])

        val replyIpCsum = DnsEngine.calculateChecksum(rawReply, 0, 20)
        assertEquals(0, replyIpCsum)

        val replySrcPort = ((rawReply[20].toInt() and 0xFF) shl 8) or (rawReply[21].toInt() and 0xFF)
        val replyDstPort = ((rawReply[22].toInt() and 0xFF) shl 8) or (rawReply[23].toInt() and 0xFF)
        assertEquals(dnsPort, replySrcPort)
        assertEquals(clientPort, replyDstPort)

        val replyUdpLen = ((rawReply[24].toInt() and 0xFF) shl 8) or (rawReply[25].toInt() and 0xFF)
        assertEquals(8 + sinkholeDnsReply.size, replyUdpLen)

        val replyUdpCsum = ((rawReply[26].toInt() and 0xFF) shl 8) or (rawReply[27].toInt() and 0xFF)
        assertTrue(replyUdpCsum != 0)

        val dnsTxId = ((rawReply[28].toInt() and 0xFF) shl 8) or (rawReply[29].toInt() and 0xFF)
        assertEquals(0x1234, dnsTxId)

        val dnsFlags = ((rawReply[30].toInt() and 0xFF) shl 8) or (rawReply[31].toInt() and 0xFF)
        assertTrue((dnsFlags and 0x8000) != 0)
        assertTrue((dnsFlags and 0x0400) != 0)
        assertEquals(0, dnsFlags and 0x000F)

        val answerCount = ((rawReply[34].toInt() and 0xFF) shl 8) or (rawReply[35].toInt() and 0xFF)
        assertEquals(1, answerCount)

        val lastFour = rawReply.copyOfRange(rawReply.size - 4, rawReply.size)
        assertEquals(0.toByte(), lastFour[0])
        assertEquals(0.toByte(), lastFour[1])
        assertEquals(0.toByte(), lastFour[2])
        assertEquals(0.toByte(), lastFour[3])
    }

    @Test
    fun testNonBlockedPipelineDirectForward() {
        val blockedDomains = setOf("doubleclick.net")
        val dnsPayload = buildDnsQueryPayload("kernel.org", qType = 1)

        val parsedDomain = DnsEngine.parseDomain(dnsPayload)
        assertEquals("kernel.org", parsedDomain)

        val isBlocked = DnsEngine.isDomainBlocked(
            parsedDomain,
            blockedDomains,
            filterAds = true,
            filterTrackers = true,
            filterMalware = true
        )
        assertFalse(isBlocked)
    }
}
