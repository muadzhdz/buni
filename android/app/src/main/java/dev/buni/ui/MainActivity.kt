package dev.buni.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.buni.core.BuniSettings
import dev.buni.core.Language
import dev.buni.core.LocalBuniLanguage
import dev.buni.service.BuniVpnService
import dev.buni.theme.BuniTheme

class MainActivity : ComponentActivity() {

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val settings = BuniSettings(this)

        setContent {
            var isDarkMode by remember { mutableStateOf(settings.isDarkMode()) }
            var currentLanguage by remember { mutableStateOf(settings.getLanguage()) }

            CompositionLocalProvider(LocalBuniLanguage provides currentLanguage) {
                BuniTheme(darkTheme = isDarkMode) {
                    val isRunning by BuniVpnService.isRunning.collectAsState()
                    val stats by BuniVpnService.statsFlow.collectAsState()
                    val logs by BuniVpnService.logsFlow.collectAsState()

                    AppShell(
                        isActive = isRunning,
                        stats = stats,
                        logs = logs,
                        isDarkMode = isDarkMode,
                        currentLanguage = currentLanguage,
                        onToggleTheme = {
                            val newMode = !isDarkMode
                            settings.setDarkMode(newMode)
                            isDarkMode = newMode
                        },
                        onToggleLanguage = {
                            val newLang = if (currentLanguage == Language.EN) Language.ID else Language.EN
                            settings.setLanguage(newLang)
                            currentLanguage = newLang
                        },
                        onTogglePower = {
                            if (isRunning) {
                                stopVpnService()
                            } else {
                                prepareAndStartVpn()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, BuniVpnService::class.java).apply {
            action = BuniVpnService.ACTION_CONNECT
        }
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, BuniVpnService::class.java).apply {
            action = BuniVpnService.ACTION_DISCONNECT
        }
        startService(intent)
    }
}
