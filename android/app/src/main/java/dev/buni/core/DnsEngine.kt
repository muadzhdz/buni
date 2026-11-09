package dev.buni.core

import java.util.Locale

enum class DomainCategory {
    ADS,
    TRACKERS,
    MALWARE
}

object DnsEngine {

    fun parseDomain(wire: ByteArray): String {
        if (wire.size < 12) {
            return ""
        }
        var offset = 12
        val labels = ArrayList<String>()

        while (offset < wire.size) {
            val len = wire[offset].toInt() and 0xFF
            offset++
            if (len == 0) {
                break
            }
            if (len > 63 || offset + len > wire.size) {
                return ""
            }
            labels.add(String(wire, offset, len, Charsets.US_ASCII))
            offset += len
        }

        return labels.joinToString(".")
    }

    fun isValidIpv4(ip: String): Boolean {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        val ipv4Regex = Regex("^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])$")
        return ipv4Regex.matches(trimmed)
    }

    fun isValidIpv6(ip: String): Boolean {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        return try {
            val addr = java.net.InetAddress.getByName(trimmed)
            addr is java.net.Inet6Address
        } catch (ignored: Exception) {
            false
        }
    }

    fun isValidIpAddress(ip: String): Boolean {
        return isValidIpv4(ip) || isValidIpv6(ip)
    }

    fun cleanDomainLine(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null
        }
        val parts = trimmed.split(Regex("\\s+"))
        val rawDomain = when {
            parts.size == 1 -> parts[0]
            parts.size >= 2 -> {
                if (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1") {
                    parts[1]
                } else {
                    parts[0]
                }
            }
            else -> return null
        }
        val clean = rawDomain.trimEnd('.').lowercase(Locale.ROOT)
        if (clean.isEmpty() || clean == "localhost" || clean == "broadcasthost" || clean == "localdomain") {
            return null
        }
        return clean
    }

    fun buildSinkholeResponse(query: ByteArray): ByteArray? {
        if (query.size < 12) {
            return null
        }
        var offset = 12
        while (offset < query.size) {
            val len = query[offset].toInt() and 0xFF
            offset++
            if (len == 0) {
                break
            }
            offset += len
        }
        if (offset + 4 > query.size) {
            return null
        }
        val qType = ((query[offset].toInt() and 0xFF) shl 8) or (query[offset + 1].toInt() and 0xFF)
        val questionEnd = offset + 4

        val isA = (qType == 1)
        val isAaaa = (qType == 28)

        val recordLen = when {
            isA -> 16
            isAaaa -> 28
            else -> 0
        }

        val resp = ByteArray(questionEnd + recordLen)
        System.arraycopy(query, 0, resp, 0, questionEnd)

        var flags = ((resp[2].toInt() and 0xFF) shl 8) or (resp[3].toInt() and 0xFF)
        flags = flags or 0x8400
        flags = flags and 0xFFF0
        resp[2] = ((flags shr 8) and 0xFF).toByte()
        resp[3] = (flags and 0xFF).toByte()

        resp[4] = 0x00
        resp[5] = 0x01
        resp[6] = 0x00
        resp[7] = if (recordLen > 0) 0x01.toByte() else 0x00.toByte()
        resp[8] = 0x00
        resp[9] = 0x00
        resp[10] = 0x00
        resp[11] = 0x00

        if (isA) {
            var idx = questionEnd
            resp[idx++] = 0xC0.toByte()
            resp[idx++] = 0x0C.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x01.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x01.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x01.toByte()
            resp[idx++] = 0x2C.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x04.toByte()
            resp[idx++] = 0.toByte()
            resp[idx++] = 0.toByte()
            resp[idx++] = 0.toByte()
            resp[idx++] = 0.toByte()
        } else if (isAaaa) {
            var idx = questionEnd
            resp[idx++] = 0xC0.toByte()
            resp[idx++] = 0x0C.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x1C.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x01.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x01.toByte()
            resp[idx++] = 0x2C.toByte()
            resp[idx++] = 0x00.toByte()
            resp[idx++] = 0x10.toByte()
            for (i in 0 until 16) {
                resp[idx++] = 0.toByte()
            }
        }

        return resp
    }

    fun craftReplyPacket(req: ByteArray, ihl: Int, payload: ByteArray): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val reply = ByteArray(totalLen)

        reply[0] = 0x45.toByte()
        reply[1] = 0x00.toByte()
        reply[2] = ((totalLen shr 8) and 0xFF).toByte()
        reply[3] = (totalLen and 0xFF).toByte()
        reply[4] = 0x00.toByte()
        reply[5] = 0x00.toByte()
        reply[6] = 0x40.toByte()
        reply[7] = 0x00.toByte()
        reply[8] = 64.toByte()
        reply[9] = 17.toByte()
        reply[10] = 0x00.toByte()
        reply[11] = 0x00.toByte()

        System.arraycopy(req, 16, reply, 12, 4)
        System.arraycopy(req, 12, reply, 16, 4)

        val ipCsum = calculateChecksum(reply, 0, 20)
        reply[10] = ((ipCsum shr 8) and 0xFF).toByte()
        reply[11] = (ipCsum and 0xFF).toByte()

        reply[20] = req[ihl + 2]
        reply[21] = req[ihl + 3]
        reply[22] = req[ihl]
        reply[23] = req[ihl + 1]

        val udpLen = 8 + payload.size
        reply[24] = ((udpLen shr 8) and 0xFF).toByte()
        reply[25] = (udpLen and 0xFF).toByte()
        reply[26] = 0x00.toByte()
        reply[27] = 0x00.toByte()

        System.arraycopy(payload, 0, reply, 28, payload.size)

        val udpCsum = calculateUdpChecksum(reply, 12, 16, 20, udpLen)
        reply[26] = ((udpCsum shr 8) and 0xFF).toByte()
        reply[27] = (udpCsum and 0xFF).toByte()

        return reply
    }

    fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        while (i < offset + length - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv().toInt() and 0xFFFF
    }

    fun calculateUdpChecksum(
        data: ByteArray,
        srcOffset: Int,
        dstOffset: Int,
        udpOffset: Int,
        udpLen: Int
    ): Int {
        var sum = 0L
        sum += ((data[srcOffset].toInt() and 0xFF) shl 8) or (data[srcOffset + 1].toInt() and 0xFF)
        sum += ((data[srcOffset + 2].toInt() and 0xFF) shl 8) or (data[srcOffset + 3].toInt() and 0xFF)
        sum += ((data[dstOffset].toInt() and 0xFF) shl 8) or (data[dstOffset + 1].toInt() and 0xFF)
        sum += ((data[dstOffset + 2].toInt() and 0xFF) shl 8) or (data[dstOffset + 3].toInt() and 0xFF)
        sum += 17L
        sum += udpLen.toLong()

        var i = udpOffset
        while (i < udpOffset + udpLen - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < udpOffset + udpLen) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val res = sum.inv().toInt() and 0xFFFF
        return if (res == 0) 0xFFFF else res
    }

    fun craftReplyPacketIpv6(req: ByteArray, payload: ByteArray): ByteArray {
        val udpLen = 8 + payload.size
        val totalLen = 40 + udpLen
        val reply = ByteArray(totalLen)

        reply[0] = 0x60.toByte()
        reply[1] = 0x00.toByte()
        reply[2] = 0x00.toByte()
        reply[3] = 0x00.toByte()
        reply[4] = ((udpLen shr 8) and 0xFF).toByte()
        reply[5] = (udpLen and 0xFF).toByte()
        reply[6] = 17.toByte()
        reply[7] = 64.toByte()

        System.arraycopy(req, 24, reply, 8, 16)
        System.arraycopy(req, 8, reply, 24, 16)

        reply[40] = req[42]
        reply[41] = req[43]
        reply[42] = req[40]
        reply[43] = req[41]

        reply[44] = ((udpLen shr 8) and 0xFF).toByte()
        reply[45] = (udpLen and 0xFF).toByte()
        reply[46] = 0x00.toByte()
        reply[47] = 0x00.toByte()

        System.arraycopy(payload, 0, reply, 48, payload.size)

        val udpCsum = calculateUdpChecksumIpv6(reply, 8, 24, 40, udpLen)
        reply[46] = ((udpCsum shr 8) and 0xFF).toByte()
        reply[47] = (udpCsum and 0xFF).toByte()

        return reply
    }

    fun calculateUdpChecksumIpv6(
        data: ByteArray,
        srcOffset: Int,
        dstOffset: Int,
        udpOffset: Int,
        udpLen: Int
    ): Int {
        var sum = 0L
        var i = srcOffset
        while (i < srcOffset + 16) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        i = dstOffset
        while (i < dstOffset + 16) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        sum += (udpLen.toLong() shr 16) and 0xFFFF
        sum += udpLen.toLong() and 0xFFFF
        sum += 17L

        i = udpOffset
        while (i < udpOffset + udpLen - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < udpOffset + udpLen) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val res = sum.inv().toInt() and 0xFFFF
        return if (res == 0) 0xFFFF else res
    }

    fun findMatchingBlockedDomain(domain: String, blockedDomains: Set<String>): String? {
        val d = domain.lowercase(Locale.ROOT)
        if (blockedDomains.contains(d)) {
            return d
        }
        var sub = d
        while (true) {
            val idx = sub.indexOf('.')
            if (idx == -1) {
                break
            }
            sub = sub.substring(idx + 1)
            if (blockedDomains.contains(sub)) {
                return sub
            }
        }
        return null
    }

    fun classifyDomain(domain: String): DomainCategory {
        val lower = domain.lowercase(Locale.ROOT)
        if (lower.contains("malware") || lower.contains("phish") || lower.contains("sec.miui") || lower.contains("installer") || lower.contains("diagmon")) {
            return DomainCategory.MALWARE
        }
        if (lower.contains("track") || lower.contains("telemetry") || lower.contains("metric") || lower.contains("stat") || lower.contains("analytics") || lower.contains("log") || lower.contains("appsflyer") || lower.contains("adjust") || lower.contains("branch.io") || lower.contains("pixel")) {
            return DomainCategory.TRACKERS
        }
        return DomainCategory.ADS
    }

    fun isDomainBlocked(
        domain: String,
        blockedDomains: Set<String>,
        filterAds: Boolean,
        filterTrackers: Boolean,
        filterMalware: Boolean
    ): Boolean {
        val matched = findMatchingBlockedDomain(domain, blockedDomains) ?: return false
        val category = classifyDomain(matched)
        return when (category) {
            DomainCategory.ADS -> filterAds
            DomainCategory.TRACKERS -> filterTrackers
            DomainCategory.MALWARE -> filterMalware
        }
    }
}
