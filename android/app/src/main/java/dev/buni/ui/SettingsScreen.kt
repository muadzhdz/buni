package dev.buni.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.BuniSettings
import dev.buni.core.Language
import dev.buni.core.ThemeMode
import dev.buni.core.buniString
import dev.buni.theme.Emerald500
import dev.buni.theme.White
import dev.buni.theme.buniColors
import dev.buni.ui.components.ChevronRightIcon
import dev.buni.ui.components.DnsServerIcon
import dev.buni.ui.components.InfoIcon
import dev.buni.ui.components.LanguageIcon
import dev.buni.ui.components.MalwareRuleIcon
import dev.buni.ui.components.MoonIcon
import dev.buni.ui.components.RadarRuleIcon
import dev.buni.ui.components.ShieldRuleIcon
import dev.buni.ui.components.SunIcon
import dev.buni.ui.components.SystemModeIcon

data class UpstreamDnsOption(
    val name: String,
    val ip: String,
    val description: String
)

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    currentLanguage: Language,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { BuniSettings(context) }
    val colors = buniColors
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var filterAds by remember { mutableStateOf(settings.isFilterAdsEnabled()) }
    var filterTrackers by remember { mutableStateOf(settings.isFilterTrackersEnabled()) }
    var filterMalware by remember { mutableStateOf(settings.isFilterMalwareEnabled()) }
    var selectedDns by remember { mutableStateOf(settings.getUpstreamDns()) }
    var endpoint by remember { mutableStateOf(settings.getWireGuardEndpoint()) }
    var peerPublicKey by remember { mutableStateOf(settings.getWireGuardPublicKey()) }

    val dnsOptions = listOf(
        UpstreamDnsOption("Cloudflare DNS", "1.1.1.1", buniString("dns_desc_cloudflare")),
        UpstreamDnsOption("Quad9 Security", "9.9.9.9", buniString("dns_desc_quad9")),
        UpstreamDnsOption("AdGuard Default", "94.140.14.14", buniString("dns_desc_adguard")),
        UpstreamDnsOption("Google Public", "8.8.8.8", buniString("dns_desc_google"))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SectionHeader(title = buniString("section_general"))

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                ThemeRadioRow(
                    label = buniString("theme_system"),
                    desc = buniString("theme_system_desc"),
                    isSelected = currentThemeMode == ThemeMode.SYSTEM,
                    icon = { SystemModeIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                    onSelect = { onThemeModeChange(ThemeMode.SYSTEM) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThemeRadioRow(
                    label = buniString("theme_light"),
                    desc = buniString("theme_light_desc"),
                    isSelected = currentThemeMode == ThemeMode.LIGHT,
                    icon = { SunIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                    onSelect = { onThemeModeChange(ThemeMode.LIGHT) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThemeRadioRow(
                    label = buniString("theme_dark"),
                    desc = buniString("theme_dark_desc"),
                    isSelected = currentThemeMode == ThemeMode.DARK,
                    icon = { MoonIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                    onSelect = { onThemeModeChange(ThemeMode.DARK) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingActionCard(
            icon = { LanguageIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
            title = buniString("language_title"),
            subtitle = if (currentLanguage == Language.EN) buniString("language_sub_en") else buniString("language_sub_id"),
            actionLabel = if (currentLanguage == Language.EN) "EN" else "ID",
            onClick = onToggleLanguage
        )

        Spacer(modifier = Modifier.height(8.dp))

        AboutBuniCard(
            version = "0.1.0-alpha",
            onClick = onOpenAbout
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_rules"))

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            icon = { ShieldRuleIcon(modifier = Modifier.size(24.dp), tint = if (filterAds) colors.accent else colors.textMuted) },
            title = buniString("ads_title"),
            subtitle = buniString("ads_sub") + " · " + buniString("oem_adblock_label"),
            enabled = filterAds,
            onToggle = {
                filterAds = !filterAds
                settings.setFilterAdsEnabled(filterAds)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            icon = { RadarRuleIcon(modifier = Modifier.size(24.dp), tint = if (filterTrackers) colors.accent else colors.textMuted) },
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
            icon = { MalwareRuleIcon(modifier = Modifier.size(24.dp), tint = if (filterMalware) colors.accent else colors.textMuted) },
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
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
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
                    text = if (endpoint.isNotEmpty()) buniString("node_ready") else buniString("node_unconfigured"),
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
            placeholder = { Text(buniString("example_endpoint"), color = colors.textMuted, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = peerPublicKey,
            onValueChange = {
                peerPublicKey = it
                settings.setWireGuardPublicKey(it)
            },
            placeholder = { Text(buniString("placeholder_pubkey"), color = colors.textMuted, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(100.dp))
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
private fun ThemeRadioRow(
    label: String,
    desc: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    onSelect: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
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
                    text = label,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = desc,
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (isSelected) colors.accent else colors.border, CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
    }
}

@Composable
private fun SettingActionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, if (enabled) colors.borderLight else colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
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

        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 24.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (enabled) Emerald500 else colors.surfaceVariant)
                .padding(2.dp),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
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
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DnsServerIcon(
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) colors.accent else colors.textMuted
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = option.name,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.description,
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (isSelected) colors.accent else colors.border, CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
    }
}

@Composable
private fun AboutBuniCard(
    version: String,
    onClick: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoIcon(
                modifier = Modifier.size(24.dp),
                tint = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = buniString("about_buni"),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${buniString("version_prefix")} $version",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        ChevronRightIcon(
            modifier = Modifier.size(18.dp),
            tint = colors.textMuted
        )
    }
}
