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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.buni.core.BuniSettings
import dev.buni.core.Language
import dev.buni.core.LocalBuniLanguage
import dev.buni.core.ThemeMode
import dev.buni.service.BuniVpnService
import dev.buni.theme.BuniTheme
import dev.buni.util.BuniAudio

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
            var currentThemeMode by remember { mutableStateOf(settings.getThemeMode()) }
            var currentLanguage by remember { mutableStateOf(settings.getLanguage()) }
            var currentMode by remember { mutableStateOf(settings.getEngineMode()) }

            val systemInDark = isSystemInDarkTheme()
            val isDarkMode = when (currentThemeMode) {
                ThemeMode.SYSTEM -> systemInDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

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
                        currentThemeMode = currentThemeMode,
                        currentLanguage = currentLanguage,
                        currentMode = currentMode,
                        onModeChange = {
                            settings.setEngineMode(it)
                            currentMode = it
                        },
                        onThemeModeChange = {
                            settings.setThemeMode(it)
                            currentThemeMode = it
                        },
                        onToggleTheme = {
                            val nextMode = when (currentThemeMode) {
                                ThemeMode.SYSTEM -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.LIGHT
                                ThemeMode.LIGHT -> ThemeMode.SYSTEM
                            }
                            settings.setThemeMode(nextMode)
                            currentThemeMode = nextMode
                        },
                        onToggleLanguage = {
                            val newLang = if (currentLanguage == Language.EN) Language.ID else Language.EN
                            settings.setLanguage(newLang)
                            currentLanguage = newLang
                        },
                        onTogglePower = {
                            if (isRunning) {
                                BuniAudio.playPowerOff()
                                stopVpnService()
                            } else {
                                BuniAudio.playPowerOn()
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
