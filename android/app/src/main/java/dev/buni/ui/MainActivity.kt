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
import dev.buni.core.buniString
import dev.buni.service.BuniVpnService
import dev.buni.theme.BuniTheme
import dev.buni.theme.Emerald500
import dev.buni.theme.buniColors
import dev.buni.util.BuniAudio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        val initialLocaleTag = if (settings.getLanguage() == Language.ID) "in" else "en"
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(initialLocaleTag)
        )

        setContent {
            var currentThemeMode by remember { mutableStateOf(settings.getThemeMode()) }
            var currentLanguage by remember { mutableStateOf(settings.getLanguage()) }
            var activeDnsLabel by remember { mutableStateOf(settings.getActiveDnsLabel()) }

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
                    var showVpnDisclosure by remember { mutableStateOf(false) }

                    if (showVpnDisclosure) {
                        VpnDisclosureDialog(
                            onConfirm = {
                                settings.setVpnConsentGiven(true)
                                showVpnDisclosure = false
                                BuniAudio.playPowerOn()
                                prepareAndStartVpn()
                            },
                            onDismiss = {
                                showVpnDisclosure = false
                            }
                        )
                    }

                    AppShell(
                        isActive = isRunning,
                        stats = stats,
                        logs = logs,
                        isDarkMode = isDarkMode,
                        currentThemeMode = currentThemeMode,
                        currentLanguage = currentLanguage,
                        activeDnsLabel = activeDnsLabel,
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
                            val localeTag = if (newLang == Language.ID) "in" else "en"
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                androidx.core.os.LocaleListCompat.forLanguageTags(localeTag)
                            )
                        },
                        onTogglePower = {
                            if (isRunning) {
                                BuniAudio.playPowerOff()
                                stopVpnService()
                            } else {
                                if (settings.isVpnConsentGiven()) {
                                    BuniAudio.playPowerOn()
                                    prepareAndStartVpn()
                                } else {
                                    showVpnDisclosure = true
                                }
                            }
                        },
                        onDnsChanged = {
                            activeDnsLabel = settings.getActiveDnsLabel()
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

@Composable
fun VpnDisclosureDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = buniColors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = buniString("vpn_disclosure_title"),
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Text(
                text = buniString("vpn_disclosure_desc"),
                color = colors.textSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Emerald500
                )
            ) {
                Text(
                    text = buniString("vpn_disclosure_accept"),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.textMuted
                )
            ) {
                Text(
                    text = buniString("vpn_disclosure_cancel"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    )
}
