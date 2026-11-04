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
        }
        return START_STICKY
    }

    private fun loadBlocklist() {
        blockedDomains.clear()
        try {
            assets.open("blocklist.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val clean = line.trim().lowercase(Locale.ROOT)
                    if (clean.isNotEmpty() && !clean.startsWith("#")) {
                        blockedDomains.add(clean)
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            return
        }

        loadBlocklist()

        try {
            val notification = buildNotification("Active", "Shielding network traffic")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

        val settings = dev.buni.core.BuniSettings(this)

        val builder = Builder().apply {
            setSession("Buni")
            setMtu(1500)
            addAddress("10.233.0.1", 30)
            addDnsServer("10.233.0.2")
            addRoute("10.233.0.2", 32)
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
        val buffer = ByteArray(1500)

        while (serviceScope.isActive) {
            try {
                val length = inStream.read(buffer)
                if (length < 28) {
                    continue
                }

                val version = (buffer[0].toInt() shr 4) and 0x0F
                if (version != 4) {
                    continue
                }

                val ihl = (buffer[0].toInt() and 0x0F) * 4
                if (length < ihl + 8) {
                    continue
                }

                val protocol = buffer[9].toInt() and 0xFF
                if (protocol != 17) {
                    continue
                }

                val dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)
                if (dstPort != 53) {
                    continue
                }

                val udpLen = ((buffer[ihl + 4].toInt() and 0xFF) shl 8) or (buffer[ihl + 5].toInt() and 0xFF)
                if (length < ihl + udpLen || udpLen < 8) {
                    continue
                }

                val dnsPayload = buffer.copyOfRange(ihl + 8, ihl + udpLen)
                val domain = parseDomainFromDns(dnsPayload)

                totalQueries.incrementAndGet()

                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val now = timeFormat.format(Date())

                if (domain.isNotEmpty() && isDomainBlocked(domain)) {
                    blockedQueries.incrementAndGet()
                    val sinkReply = buildSinkholeDnsResponse(dnsPayload)
                    if (sinkReply != null) {
                        val replyPacket = craftIPv4UdpReply(buffer, ihl, sinkReply)
                        outStream.write(replyPacket)
                    }
                    emitLog("[$now] SINK $domain -> DROPPED (0ms)")
                } else {
                    val start = System.currentTimeMillis()
                    val upstreamReply = forwardDnsProtected(dnsPayload)
                    val duration = System.currentTimeMillis() - start
                    lastLatency.set(duration)

                    if (upstreamReply != null && upstreamReply.isNotEmpty()) {
                        val replyPacket = craftIPv4UdpReply(buffer, ihl, upstreamReply)
                        outStream.write(replyPacket)
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

            } catch (e: Exception) {
                if (!serviceScope.isActive) {
                    break
                }
            }
        }
    }

    private fun forwardDnsProtected(payload: ByteArray): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = 2500

            val dnsStr = dev.buni.core.BuniSettings(this).getUpstreamDns()
            val targetIp = InetAddress.getByName(dnsStr)
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

    private fun isDomainBlocked(domain: String): Boolean {
        val d = domain.lowercase(Locale.ROOT)
        if (blockedDomains.contains(d)) {
            return true
        }
        var sub = d
        while (true) {
            val idx = sub.indexOf('.')
            if (idx == -1) {
                break
            }
            sub = sub.substring(idx + 1)
            if (blockedDomains.contains(sub)) {
                return true
            }
        }
        return false
    }

    private fun parseDomainFromDns(wire: ByteArray): String {
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

    private fun buildSinkholeDnsResponse(query: ByteArray): ByteArray? {
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
        offset += 4
        if (offset > query.size) {
            return null
        }

        val resp = ByteArray(offset + 16)
        System.arraycopy(query, 0, resp, 0, offset)

        var flags = ((resp[2].toInt() and 0xFF) shl 8) or (resp[3].toInt() and 0xFF)
        flags = flags or 0x8400
        flags = flags and 0xFFF0.inv()
        resp[2] = ((flags shr 8) and 0xFF).toByte()
        resp[3] = (flags and 0xFF).toByte()

        resp[4] = 0x00
        resp[5] = 0x01
        resp[6] = 0x00
        resp[7] = 0x01
        resp[8] = 0x00
        resp[9] = 0x00
        resp[10] = 0x00
        resp[11] = 0x00

        var idx = offset
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

        return resp
    }

    private fun craftIPv4UdpReply(req: ByteArray, ihl: Int, payload: ByteArray): ByteArray {
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

    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
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

    private fun calculateUdpChecksum(
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
        private const val NOTIFICATION_ID = 2331

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _statsFlow = MutableStateFlow(Triple(0L, 0L, 0L))
        val statsFlow = _statsFlow.asStateFlow()

        private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
        val logsFlow = _logsFlow.asStateFlow()
    }
}
