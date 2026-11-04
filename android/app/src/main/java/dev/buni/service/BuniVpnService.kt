package dev.buni.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import dev.buni.BuniApp
import dev.buni.core.Bridge
import dev.buni.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BuniVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_CONNECT
        when (action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification("Active", "Shielding network traffic"))

        val builder = Builder().apply {
            setSession("Buni")
            setMtu(1500)
            addAddress("10.233.0.1", 32)
            addDnsServer("10.233.0.1")
            addRoute("10.233.0.1", 32)
        }

        try {
            vpnInterface = builder.establish()
            vpnInterface?.let { pfd ->
                val fd = pfd.detachFd()
                val blocklistData = try {
                    assets.open("blocklist.txt").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    ""
                }
                try {
                    Bridge.startShield(fd, blocklistData, "https://cloudflare-dns.com/dns-query")
                } catch (e: UnsatisfiedLinkError) {
                    Bridge.mockStartShield(fd)
                }
                _isRunning.value = true
                startMetricsPoller()
            }
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            Bridge.stopShield()
        } catch (ignored: UnsatisfiedLinkError) {
            Bridge.mockStopShield()
        }

        serviceScope.coroutineContext.cancel()
        vpnInterface?.close()
        vpnInterface = null
        _isRunning.value = false

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startMetricsPoller() {
        serviceScope.launch {
            while (isActive && _isRunning.value) {
                try {
                    val rawStats = try {
                        Bridge.getStats()
                    } catch (e: UnsatisfiedLinkError) {
                        val mock = Bridge.mockGetStats()
                        "${mock.first},${mock.second},${mock.third}"
                    }
                    val parts = rawStats.split(",")
                    if (parts.size >= 3) {
                        _statsFlow.value = Triple(
                            parts[0].toLongOrNull() ?: 0L,
                            parts[1].toLongOrNull() ?: 0L,
                            parts[2].toLongOrNull() ?: 0L
                        )
                    }

                    val logLine = try {
                        Bridge.pollLog()
                    } catch (e: UnsatisfiedLinkError) {
                        Bridge.mockPollLog()
                    }
                    if (logLine.isNotEmpty()) {
                        _logsFlow.value = (_logsFlow.value + logLine).takeLast(50)
                    }
                } catch (ignored: Exception) {
                }
                delay(1000)
            }
        }
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
