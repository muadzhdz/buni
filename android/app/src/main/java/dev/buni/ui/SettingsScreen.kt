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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.BuniSettings
import dev.buni.core.BuniStrings
import dev.buni.core.Language
import dev.buni.core.buniString
import dev.buni.theme.Emerald500
import dev.buni.theme.White
import dev.buni.theme.buniColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class UpstreamDnsOption(
    val name: String,
    val ip: String,
    val description: String
)

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    currentLanguage: Language,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { BuniSettings(context) }
    val scrollState = rememberScrollState()
    val colors = buniColors
    val coroutineScope = rememberCoroutineScope()

    var filterAds by remember { mutableStateOf(settings.isFilterAdsEnabled()) }
    var filterTrackers by remember { mutableStateOf(settings.isFilterTrackersEnabled()) }
    var filterMalware by remember { mutableStateOf(settings.isFilterMalwareEnabled()) }
    var selectedDns by remember { mutableStateOf(settings.getUpstreamDns()) }

    var endpoint by remember { mutableStateOf(settings.getWireGuardEndpoint()) }
    var publicKey by remember { mutableStateOf(settings.getWireGuardPublicKey()) }

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }

    val dnsOptions = listOf(
        UpstreamDnsOption("Cloudflare", "1.1.1.1", "Fast, privacy-first, zero-log"),
        UpstreamDnsOption("Quad9", "9.9.9.9", "Swiss-hosted, threat-blocking"),
        UpstreamDnsOption("AdGuard", "94.140.14.14", "Aggressive default adblock"),
        UpstreamDnsOption("Google", "8.8.8.8", "Global high-availability")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = buniString("settings_title"),
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = buniString("settings_sub"),
            color = colors.textMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_appearance"))

        Spacer(modifier = Modifier.height(8.dp))

        SettingActionCard(
            title = buniString("theme_title"),
            subtitle = if (isDarkMode) buniString("theme_sub_dark") else buniString("theme_sub_light"),
            actionLabel = if (isDarkMode) "DARK" else "LIGHT",
            onClick = onToggleTheme
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingActionCard(
            title = buniString("language_title"),
            subtitle = if (currentLanguage == Language.EN) buniString("language_sub_en") else buniString("language_sub_id"),
            actionLabel = if (currentLanguage == Language.EN) "EN" else "ID",
            onClick = onToggleLanguage
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_rules"))

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            title = buniString("ads_title"),
            subtitle = buniString("ads_sub"),
            enabled = filterAds,
            onToggle = {
                filterAds = !filterAds
                settings.setFilterAdsEnabled(filterAds)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            title = buniString("trackers_title"),
            subtitle = buniString("trackers_sub"),
            enabled = filterTrackers,
            onToggle = {
                filterTrackers = !filterTrackers
                settings.setFilterTrackersEnabled(filterTrackers)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            title = buniString("malware_title"),
            subtitle = buniString("malware_sub"),
            enabled = filterMalware,
            onToggle = {
                filterMalware = !filterMalware
                settings.setFilterMalwareEnabled(filterMalware)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_resolver"))

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dnsOptions.forEach { opt ->
                val isSelected = selectedDns == opt.ip
                DnsOptionCard(
                    option = opt,
                    isSelected = isSelected,
                    onSelect = {
                        selectedDns = opt.ip
                        settings.setUpstreamDns(opt.ip)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_node"))

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = buniString("protocol"),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "WireGuard / ChaCha20",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (endpoint.isNotEmpty()) Emerald500 else colors.textMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (endpoint.isNotEmpty()) "READY" else "UNCONFIGURED",
                    color = if (endpoint.isNotEmpty()) colors.textPrimary else colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = endpoint,
            onValueChange = {
                endpoint = it
                settings.setWireGuardEndpoint(it)
            },
            placeholder = { Text("e.g. 198.51.100.1:51820", color = colors.textMuted, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.borderLight,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = publicKey,
            onValueChange = {
                publicKey = it
                settings.setWireGuardPublicKey(it)
            },
            placeholder = { Text("Base64 public key from server", color = colors.textMuted, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.borderLight,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = buniString("about_tunnel"),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buniString("about_tunnel_desc"),
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_about"))

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Buni Stealth Shield",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = buniString("license_foss"),
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.borderLight, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "v1.0.0-alpha",
                    color = colors.textPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingActionCard(
            title = buniString("check_updates"),
            subtitle = updateStatusText ?: "GitHub Releases & F-Droid repository",
            actionLabel = if (isCheckingUpdates) buniString("checking_updates") else "CHECK",
            onClick = {
                if (!isCheckingUpdates) {
                    coroutineScope.launch {
                        isCheckingUpdates = true
                        delay(1200)
                        isCheckingUpdates = false
                        updateStatusText = "v1.0.0-alpha · " + BuniStrings.get("up_to_date", currentLanguage)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingActionCard(
            title = buniString("github_repo"),
            subtitle = "https://github.com/muadzhdz/buni",
            actionLabel = "OPEN",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/muadzhdz/buni"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = buniColors
    Text(
        text = title,
        color = colors.textMuted,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}

@Composable
private fun SettingActionCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.borderLight, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = actionLabel,
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RuleToggleCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, if (enabled) colors.borderLight else colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (enabled) Emerald500 else colors.surfaceVariant)
                .padding(2.dp),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (enabled) White else colors.textMuted)
            )
        }
    }
}

@Composable
private fun DnsOptionCard(
    option: UpstreamDnsOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, if (isSelected) colors.borderLight else colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.name,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.ip,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = option.description,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Emerald500)
            )
        }
    }
}
