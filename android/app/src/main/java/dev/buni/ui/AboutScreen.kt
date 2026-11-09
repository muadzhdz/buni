package dev.buni.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.buniString
import dev.buni.theme.Emerald500
import dev.buni.theme.buniColors
import dev.buni.ui.components.ArrowBackIcon
import dev.buni.ui.components.BugIcon
import dev.buni.ui.components.BuniLogoIcon
import dev.buni.ui.components.CodeIcon
import dev.buni.ui.components.DownloadIcon
import dev.buni.ui.components.ExternalLinkIcon
import dev.buni.ui.components.MedalIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = buniColors
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var isChecking by remember { mutableStateOf(false) }
    var updateAvailableVersion by remember { mutableStateOf<String?>(null) }
    var updateDownloadUrl by remember { mutableStateOf<String?>(null) }
    var updateCheckResultText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateBack()
                    }),
                contentAlignment = Alignment.Center
            ) {
                ArrowBackIcon(
                    modifier = Modifier.size(24.dp),
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = buniString("about_buni"),
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BuniLogoIcon(
                    modifier = Modifier.size(44.dp),
                    tint = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = buniString("about_desc"),
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${buniString("version_prefix")} ${dev.buni.core.BuniVersion.NAME}",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = buniString("updates"),
            color = colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, if (updateAvailableVersion != null) Emerald500 else colors.border, RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (updateDownloadUrl != null) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateDownloadUrl))
                        context.startActivity(intent)
                    } else if (!isChecking) {
                        isChecking = true
                        updateCheckResultText = null
                        coroutineScope.launch {
                            val result = checkForUpdatesFromGitHub()
                            isChecking = false
                            if (result != null && result.first != "v${dev.buni.core.BuniVersion.NAME}") {
                                updateAvailableVersion = result.first
                                updateDownloadUrl = result.second
                                updateCheckResultText = result.first
                            } else {
                                updateCheckResultText = "v${dev.buni.core.BuniVersion.NAME}"
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadIcon(
                    modifier = Modifier.size(24.dp),
                    tint = if (updateAvailableVersion != null) Emerald500 else colors.textPrimary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (updateAvailableVersion != null) {
                            "${buniString("update_available")}: $updateAvailableVersion"
                        } else {
                            buniString("check_updates")
                        },
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isChecking -> buniString("checking_updates")
                            updateAvailableVersion != null -> buniString("tap_to_download_apk")
                            updateCheckResultText != null -> buniString("no_update")
                            else -> buniString("query_releases_desc")
                        },
                        color = if (updateAvailableVersion != null) Emerald500 else colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (updateAvailableVersion != null) Emerald500 else colors.surfaceVariant)
                    .border(1.dp, if (updateAvailableVersion != null) Emerald500 else colors.borderLight, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        isChecking -> "..."
                        updateAvailableVersion != null -> buniString("update_btn")
                        updateCheckResultText != null -> "OK"
                        else -> buniString("btn_check")
                    },
                    color = if (updateAvailableVersion != null) colors.surface else colors.textPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AboutLinkItem(
            title = buniString("latest_release"),
            subtitle = buniString("latest_release_sub"),
            icon = { DownloadIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/muadzhdz/buni/releases"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buniString("latest_release_caption"),
            color = colors.textMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = buniString("project"),
            color = colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutLinkItem(
                title = buniString("source_code"),
                subtitle = "github.com/muadzhdz/buni",
                icon = { CodeIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/muadzhdz/buni"))
                    context.startActivity(intent)
                }
            )

            AboutLinkItem(
                title = buniString("report_issue"),
                subtitle = buniString("report_issue_sub"),
                icon = { BugIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/muadzhdz/buni/issues"))
                    context.startActivity(intent)
                }
            )

            AboutLinkItem(
                title = buniString("license"),
                subtitle = "GPL-3.0-or-later",
                icon = { MedalIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/muadzhdz/buni/blob/main/LICENSE"))
                    context.startActivity(intent)
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

private suspend fun checkForUpdatesFromGitHub(): Pair<String, String>? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/muadzhdz/buni/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "Buni-Android-Client")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "https://github.com/muadzhdz/buni/releases")
                Pair(tagName, htmlUrl)
            } else {
                delay(800)
                null
            }
        } catch (e: Exception) {
            delay(800)
            null
        }
    }
}

@Composable
private fun AboutLinkItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val colors = buniColors
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        ExternalLinkIcon(
            modifier = Modifier.size(18.dp),
            tint = colors.textMuted
        )
    }
}
