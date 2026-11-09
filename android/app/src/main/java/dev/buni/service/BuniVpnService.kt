package dev.buni.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import dev.buni.BuniApp
import dev.buni.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class BuniVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val blockedDomains = HashSet<String>()

    private val totalQueries = AtomicLong(0)
    private val blockedQueries = AtomicLong(0)
    private val lastLatency = AtomicLong(0)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_CONNECT
        when (action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT -> stopVpn()
            ACTION_RELOAD_RULES -> {
                loadBlocklist()
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val now = timeFormat.format(Date())
                emitLog("[$now] Ruleset updated: ${blockedDomains.size} domains active")
            }
        }
        return START_STICKY
    }

    private fun loadBlocklist() {
        blockedDomains.clear()
        try {
            assets.open("blocklist.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val clean = dev.buni.core.DnsEngine.cleanDomainLine(line)
                    if (clean != null) {
                        blockedDomains.add(clean)
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        try {
            val otaFile = java.io.File(filesDir, "blocklist_ota.txt")
            if (otaFile.exists()) {
                otaFile.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val clean = dev.buni.core.DnsEngine.cleanDomainLine(line)
                        if (clean != null) {
                            blockedDomains.add(clean)
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        _activeRulesCount.value = blockedDomains.size
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            return
        }

        loadBlocklist()

        val settings = dev.buni.core.BuniSettings(this)
        val lang = settings.getLanguage()

        try {
            val statusTitle = dev.buni.core.BuniStrings.get("notif_shield_active", lang)
            val statusDesc = dev.buni.core.BuniStrings.get("notif_shield_desc", lang)
            val notification = buildNotification(statusTitle, statusDesc)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
        }

        val builder = Builder().apply {
            setSession("Buni Shield")
            setMtu(1500)
            addAddress("10.233.0.1", 30)
            addDnsServer("10.233.0.2")
            addRoute("10.233.0.2", 32)
            addAddress("fd00:1:fd00:1::1", 128)
            addDnsServer("fd00:1:fd00:1::2")
            addRoute("fd00:1:fd00:1::2", 128)
            setBlocking(true)

            val whitelisted = settings.getWhitelistedPackages()
            for (pkg in whitelisted) {
                try {
                    addDisallowedApplication(pkg)
                } catch (ignored: Exception) {
                }
            }
        }

        try {
            vpnInterface = builder.establish()
            vpnInterface?.let { pfd ->
                _isRunning.value = true
                serviceScope = CoroutineScope(Dispatchers.IO + Job())
                serviceScope.launch {
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val now = timeFormat.format(Date())
                    emitLog("[$now] ${dev.buni.core.BuniStrings.get("log_shield_active", lang)}")
                    runPacketLoop(pfd)
                }
            }
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        serviceScope.cancel()
        try {
            vpnInterface?.close()
        } catch (ignored: Exception) {
        }
        vpnInterface = null
        _isRunning.value = false

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (ignored: Exception) {
        }
        stopSelf()
    }

    private fun runPacketLoop(pfd: ParcelFileDescriptor) {
        val inStream = FileInputStream(pfd.fileDescriptor)
        val outStream = FileOutputStream(pfd.fileDescriptor)
        val writeLock = Any()
        val buffer = ByteArray(1500)

        while (serviceScope.isActive) {
            try {
                val length = inStream.read(buffer)
                if (length < 28) {
                    continue
                }

                val packetCopy = buffer.copyOf(length)
                val version = (packetCopy[0].toInt() shr 4) and 0x0F
                if (version == 4) {
                    val ihl = (packetCopy[0].toInt() and 0x0F) * 4
                    if (packetCopy.size < ihl + 8) {
                        continue
                    }

                    val protocol = packetCopy[9].toInt() and 0xFF
                    if (protocol != 17) {
                        continue
                    }

                    val dstPort = ((packetCopy[ihl + 2].toInt() and 0xFF) shl 8) or (packetCopy[ihl + 3].toInt() and 0xFF)
                    if (dstPort != 53) {
                        continue
                    }

                    val udpLen = ((packetCopy[ihl + 4].toInt() and 0xFF) shl 8) or (packetCopy[ihl + 5].toInt() and 0xFF)
                    if (packetCopy.size < ihl + udpLen || udpLen < 8) {
                        continue
                    }

                    val dnsPayload = packetCopy.copyOfRange(ihl + 8, ihl + udpLen)
                    totalQueries.incrementAndGet()

                    serviceScope.launch(Dispatchers.IO) {
                        processDnsPacket(packetCopy, ihl, dnsPayload, outStream, writeLock)
                    }
                } else if (version == 6) {
                    if (packetCopy.size < 48) {
                        continue
                    }

                    val nextHeader = packetCopy[6].toInt() and 0xFF
                    if (nextHeader != 17) {
                        continue
                    }

                    val dstPort = ((packetCopy[42].toInt() and 0xFF) shl 8) or (packetCopy[43].toInt() and 0xFF)
                    if (dstPort != 53) {
                        continue
                    }

                    val udpLen = ((packetCopy[44].toInt() and 0xFF) shl 8) or (packetCopy[45].toInt() and 0xFF)
                    if (packetCopy.size < 40 + udpLen || udpLen < 8) {
                        continue
                    }

                    val dnsPayload = packetCopy.copyOfRange(48, 40 + udpLen)
                    totalQueries.incrementAndGet()

                    serviceScope.launch(Dispatchers.IO) {
                        processDnsPacketIpv6(packetCopy, dnsPayload, outStream, writeLock)
                    }
                }

            } catch (e: Exception) {
                if (!serviceScope.isActive) {
                    break
                }
            }
        }
    }

    private fun processDnsPacket(
        packet: ByteArray,
        ihl: Int,
        dnsPayload: ByteArray,
        outStream: FileOutputStream,
        writeLock: Any
    ) {
        val domain = dev.buni.core.DnsEngine.parseDomain(dnsPayload)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = timeFormat.format(Date())
        val settings = dev.buni.core.BuniSettings(this)

        val isBlocked = domain.isNotEmpty() && dev.buni.core.DnsEngine.isDomainBlocked(
            domain,
            blockedDomains,
            settings.isFilterAdsEnabled(),
            settings.isFilterTrackersEnabled(),
            settings.isFilterMalwareEnabled()
        )

        if (isBlocked) {
            blockedQueries.incrementAndGet()
            val sinkReply = dev.buni.core.DnsEngine.buildSinkholeResponse(dnsPayload)
            if (sinkReply != null) {
                val replyPacket = dev.buni.core.DnsEngine.craftReplyPacket(packet, ihl, sinkReply)
                synchronized(writeLock) {
                    try {
                        outStream.write(replyPacket)
                    } catch (ignored: Exception) {
                    }
                }
            }
            emitLog("[$now] SINK $domain -> DROPPED (0ms)")
        } else {
            val start = System.currentTimeMillis()
            val upstreamReply = forwardDnsProtected(dnsPayload)
            val duration = System.currentTimeMillis() - start
            lastLatency.set(duration)

            if (upstreamReply != null && upstreamReply.isNotEmpty()) {
                val replyPacket = dev.buni.core.DnsEngine.craftReplyPacket(packet, ihl, upstreamReply)
                synchronized(writeLock) {
                    try {
                        outStream.write(replyPacket)
                    } catch (ignored: Exception) {
                    }
                }
                val displayDomain = if (domain.isNotEmpty()) domain else "dns.query"
                emitLog("[$now] PASS $displayDomain -> RESOLVED (${duration}ms)")
            } else {
                val displayDomain = if (domain.isNotEmpty()) domain else "dns.query"
                emitLog("[$now] PASS $displayDomain -> TIMEOUT")
            }
        }

        _statsFlow.value = Triple(
            totalQueries.get(),
            blockedQueries.get(),
            lastLatency.get()
        )
    }

    private fun processDnsPacketIpv6(
        packet: ByteArray,
        dnsPayload: ByteArray,
        outStream: FileOutputStream,
        writeLock: Any
    ) {
        val domain = dev.buni.core.DnsEngine.parseDomain(dnsPayload)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = timeFormat.format(Date())
        val settings = dev.buni.core.BuniSettings(this)

        val isBlocked = domain.isNotEmpty() && dev.buni.core.DnsEngine.isDomainBlocked(
            domain,
            blockedDomains,
            settings.isFilterAdsEnabled(),
            settings.isFilterTrackersEnabled(),
            settings.isFilterMalwareEnabled()
        )

        if (isBlocked) {
            blockedQueries.incrementAndGet()
            val sinkReply = dev.buni.core.DnsEngine.buildSinkholeResponse(dnsPayload)
            if (sinkReply != null) {
                val replyPacket = dev.buni.core.DnsEngine.craftReplyPacketIpv6(packet, sinkReply)
                synchronized(writeLock) {
                    try {
                        outStream.write(replyPacket)
                    } catch (ignored: Exception) {
                    }
                }
            }
            emitLog("[$now] SINK $domain -> DROPPED (0ms)")
        } else {
            val start = System.currentTimeMillis()
            val upstreamReply = forwardDnsProtected(dnsPayload)
            val duration = System.currentTimeMillis() - start
            lastLatency.set(duration)

            if (upstreamReply != null && upstreamReply.isNotEmpty()) {
                val replyPacket = dev.buni.core.DnsEngine.craftReplyPacketIpv6(packet, upstreamReply)
                synchronized(writeLock) {
                    try {
                        outStream.write(replyPacket)
                    } catch (ignored: Exception) {
                    }
                }
                val displayDomain = if (domain.isNotEmpty()) domain else "dns.query"
                emitLog("[$now] PASS $displayDomain -> RESOLVED (${duration}ms)")
            } else {
                val displayDomain = if (domain.isNotEmpty()) domain else "dns.query"
                emitLog("[$now] PASS $displayDomain -> TIMEOUT")
            }
        }

        _statsFlow.value = Triple(
            totalQueries.get(),
            blockedQueries.get(),
            lastLatency.get()
        )
    }

    private fun forwardDnsProtected(payload: ByteArray): ByteArray? {
        val dnsStr = dev.buni.core.BuniSettings(this).getUpstreamDns()
        val (primaryIp, secondaryIp, dohHost) = when (dnsStr) {
            "1.1.1.1" -> Triple("1.1.1.1", "1.0.0.1", "cloudflare-dns.com")
            "9.9.9.9" -> Triple("9.9.9.9", "149.112.112.112", "")
            "94.140.14.14" -> Triple("94.140.14.14", "94.140.15.15", "")
            "8.8.8.8" -> Triple("8.8.8.8", "8.8.4.4", "dns.google")
            else -> Triple(dnsStr, dnsStr, "")
        }

        val udp1 = queryUdp(primaryIp, payload, 700)
        if (udp1 != null && udp1.isNotEmpty()) {
            return udp1
        }

        if (secondaryIp != primaryIp) {
            val udp2 = queryUdp(secondaryIp, payload, 700)
            if (udp2 != null && udp2.isNotEmpty()) {
                return udp2
            }
        }

        if (dohHost.isNotEmpty()) {
            val dohResp = queryDohProtected(primaryIp, dohHost, payload)
            if (dohResp != null && dohResp.isNotEmpty()) {
                return dohResp
            }
            if (secondaryIp != primaryIp) {
                val dohResp2 = queryDohProtected(secondaryIp, dohHost, payload)
                if (dohResp2 != null && dohResp2.isNotEmpty()) {
                    return dohResp2
                }
            }
        }

        if (primaryIp != "8.8.8.8") {
            val googleFallback = queryUdp("8.8.8.8", payload, 800)
            if (googleFallback != null && googleFallback.isNotEmpty()) {
                return googleFallback
            }
        }

        return null
    }

    private fun queryUdp(ip: String, payload: ByteArray, timeoutMs: Int): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = timeoutMs

            val targetIp = InetAddress.getByName(ip)
            val sendPacket = DatagramPacket(payload, payload.size, targetIp, 53)
            socket.send(sendPacket)

            val recvBuffer = ByteArray(1500)
            val recvPacket = DatagramPacket(recvBuffer, recvBuffer.size)
            socket.receive(recvPacket)

            recvBuffer.copyOf(recvPacket.length)
        } catch (e: Exception) {
            null
        } finally {
            socket?.close()
        }
    }

    private fun queryDohProtected(endpointIp: String, hostHeader: String, payload: ByteArray): ByteArray? {
        var rawSocket: Socket? = null
        var sslSocket: javax.net.ssl.SSLSocket? = null
        return try {
            rawSocket = Socket()
            protect(rawSocket)
            rawSocket.soTimeout = 2000
            rawSocket.connect(java.net.InetSocketAddress(endpointIp, 443), 1500)

            val sslFactory = javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory()
            sslSocket = sslFactory.createSocket(rawSocket, hostHeader, 443, true) as javax.net.ssl.SSLSocket
            sslSocket.soTimeout = 2000
            sslSocket.startHandshake()

            val out = sslSocket.outputStream
            val request = "POST /dns-query HTTP/1.1\r\n" +
                    "Host: $hostHeader\r\n" +
                    "Content-Type: application/dns-message\r\n" +
                    "Accept: application/dns-message\r\n" +
                    "Content-Length: ${payload.size}\r\n" +
                    "Connection: close\r\n\r\n"

            out.write(request.toByteArray(Charsets.US_ASCII))
            out.write(payload)
            out.flush()

            val inStream = sslSocket.inputStream
            val headerBuilder = java.io.ByteArrayOutputStream()
            val singleByte = ByteArray(1)

            while (inStream.read(singleByte) != -1) {
                headerBuilder.write(singleByte[0].toInt())
                val headerStr = headerBuilder.toString("US-ASCII")
                if (headerStr.endsWith("\r\n\r\n")) {
                    break
                }
            }

            val headers = headerBuilder.toString("US-ASCII")
            var contentLength = 0
            for (line in headers.lines()) {
                if (line.lowercase(Locale.ROOT).startsWith("content-length:")) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    break
                }
            }

            if (contentLength <= 0 || contentLength > 4096) {
                return null
            }

            val body = ByteArray(contentLength)
            var bytesRead = 0
            while (bytesRead < contentLength) {
                val count = inStream.read(body, bytesRead, contentLength - bytesRead)
                if (count == -1) break
                bytesRead += count
            }

            if (bytesRead == contentLength) body else null
        } catch (e: Exception) {
            null
        } finally {
            try { sslSocket?.close() } catch (ignored: Exception) {}
            try { rawSocket?.close() } catch (ignored: Exception) {}
        }
    }

    private fun emitLog(line: String) {
        val current = _logsFlow.value
        _logsFlow.value = (current + line).takeLast(50)
    }

    private fun buildNotification(title: String, message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BuniApp.CHANNEL_ID)
            .setContentTitle("Buni // $title")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "dev.buni.action.CONNECT"
        const val ACTION_DISCONNECT = "dev.buni.action.DISCONNECT"
        const val ACTION_RELOAD_RULES = "dev.buni.action.RELOAD_RULES"
        private const val NOTIFICATION_ID = 2331

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _statsFlow = MutableStateFlow(Triple(0L, 0L, 0L))
        val statsFlow = _statsFlow.asStateFlow()

        private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
        val logsFlow = _logsFlow.asStateFlow()

        private val _activeRulesCount = MutableStateFlow(165)
        val activeRulesCount = _activeRulesCount.asStateFlow()
    }
}
