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
import dev.buni.core.BuniStrings
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
import dev.buni.ui.components.DownloadIcon
import dev.buni.ui.components.BuniConfirmDialog
import dev.buni.service.BuniVpnService
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PendingConfirmAction(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

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
    onDnsChanged: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { BuniSettings(context) }
    val colors = buniColors
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var filterAds by remember { mutableStateOf(settings.isFilterAdsEnabled()) }
    var filterTrackers by remember { mutableStateOf(settings.isFilterTrackersEnabled()) }
    var filterMalware by remember { mutableStateOf(settings.isFilterMalwareEnabled()) }
    var selectedDns by remember { mutableStateOf(settings.getUpstreamDns()) }
    var customDnsInput by remember { mutableStateOf(settings.getCustomDns()) }
    var customDnsError by remember { mutableStateOf<String?>(null) }
    var pendingConfirm by remember { mutableStateOf<PendingConfirmAction?>(null) }

    val activeRulesCount by BuniVpnService.activeRulesCount.collectAsState()
    var lastRulesetUpdateTime by remember { mutableStateOf(settings.getLastRulesetUpdateTime()) }
    var isSyncingRuleset by remember { mutableStateOf(false) }
    var rulesetSyncMessage by remember { mutableStateOf<String?>(null) }

    val standardDnsOptions = listOf(
        UpstreamDnsOption("Cloudflare DNS", "1.1.1.1", buniString("dns_desc_cloudflare")),
        UpstreamDnsOption("Quad9 Security", "9.9.9.9", buniString("dns_desc_quad9")),
        UpstreamDnsOption("AdGuard Default", "94.140.14.14", buniString("dns_desc_adguard")),
        UpstreamDnsOption("Google Public", "8.8.8.8", buniString("dns_desc_google"))
    )
    val isCustomSelected = !standardDnsOptions.any { it.ip == selectedDns }

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
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onThemeModeChange(ThemeMode.SYSTEM)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThemeRadioRow(
                    label = buniString("theme_light"),
                    desc = buniString("theme_light_desc"),
                    isSelected = currentThemeMode == ThemeMode.LIGHT,
                    icon = { SunIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onThemeModeChange(ThemeMode.LIGHT)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThemeRadioRow(
                    label = buniString("theme_dark"),
                    desc = buniString("theme_dark_desc"),
                    isSelected = currentThemeMode == ThemeMode.DARK,
                    icon = { MoonIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onThemeModeChange(ThemeMode.DARK)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingActionCard(
            icon = { LanguageIcon(modifier = Modifier.size(24.dp), tint = colors.textPrimary) },
            title = buniString("language_title"),
            subtitle = if (currentLanguage == Language.EN) buniString("language_sub_en") else buniString("language_sub_id"),
            actionLabel = if (currentLanguage == Language.EN) "EN" else "ID",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleLanguage()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        AboutBuniCard(
            version = dev.buni.core.BuniVersion.NAME,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenAbout()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_rules"))

        Spacer(modifier = Modifier.height(8.dp))

        RulesetUpdateCard(
            rulesCount = activeRulesCount,
            lastUpdateTime = lastRulesetUpdateTime,
            isSyncing = isSyncingRuleset,
            syncMessage = rulesetSyncMessage,
            onSync = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isSyncingRuleset = true
                rulesetSyncMessage = null
                coroutineScope.launch {
                    val count = syncRulesetFromNetwork(context)
                    isSyncingRuleset = false
                    if (count != null) {
                        lastRulesetUpdateTime = System.currentTimeMillis()
                        settings.setLastRulesetUpdateTime(lastRulesetUpdateTime)
                        rulesetSyncMessage = BuniStrings.get("ruleset_sync_success", currentLanguage)
                    } else {
                        rulesetSyncMessage = BuniStrings.get("ruleset_sync_failed", currentLanguage)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            icon = { ShieldRuleIcon(modifier = Modifier.size(24.dp), tint = if (filterAds) colors.accent else colors.textMuted) },
            title = buniString("ads_title"),
            subtitle = buniString("ads_sub") + " · " + buniString("oem_adblock_label"),
            enabled = filterAds,
            onToggle = {
                val willEnable = !filterAds
                val title = BuniStrings.get("confirm_toggle_ads_title", currentLanguage)
                val message = BuniStrings.get(
                    if (willEnable) "confirm_toggle_ads_desc_enable" else "confirm_toggle_ads_desc_disable",
                    currentLanguage
                )
                pendingConfirm = PendingConfirmAction(title, message) {
                    filterAds = willEnable
                    settings.setFilterAdsEnabled(willEnable)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            icon = { RadarRuleIcon(modifier = Modifier.size(24.dp), tint = if (filterTrackers) colors.accent else colors.textMuted) },
            title = buniString("trackers_title"),
            subtitle = buniString("trackers_sub"),
            enabled = filterTrackers,
            onToggle = {
                val willEnable = !filterTrackers
                val title = BuniStrings.get("confirm_toggle_trackers_title", currentLanguage)
                val message = BuniStrings.get(
                    if (willEnable) "confirm_toggle_trackers_desc_enable" else "confirm_toggle_trackers_desc_disable",
                    currentLanguage
                )
                pendingConfirm = PendingConfirmAction(title, message) {
                    filterTrackers = willEnable
                    settings.setFilterTrackersEnabled(willEnable)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RuleToggleCard(
            icon = { MalwareRuleIcon(modifier = Modifier.size(24.dp), tint = if (filterMalware) colors.accent else colors.textMuted) },
            title = buniString("malware_title"),
            subtitle = buniString("malware_sub"),
            enabled = filterMalware,
            onToggle = {
                val willEnable = !filterMalware
                val title = BuniStrings.get("confirm_toggle_malware_title", currentLanguage)
                val message = BuniStrings.get(
                    if (willEnable) "confirm_toggle_malware_desc_enable" else "confirm_toggle_malware_desc_disable",
                    currentLanguage
                )
                pendingConfirm = PendingConfirmAction(title, message) {
                    filterMalware = willEnable
                    settings.setFilterMalwareEnabled(willEnable)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = buniString("section_resolver"))

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            standardDnsOptions.forEach { opt ->
                val isSelected = selectedDns == opt.ip
                DnsOptionCard(
                    option = opt,
                    isSelected = isSelected,
                    onSelect = {
                        if (selectedDns != opt.ip) {
                            val title = BuniStrings.get("confirm_change_dns_title", currentLanguage)
                            val template = BuniStrings.get("confirm_change_dns_desc", currentLanguage)
                            val message = template.replace("%1\$s", "${opt.name} (${opt.ip})")
                            pendingConfirm = PendingConfirmAction(title, message) {
                                selectedDns = opt.ip
                                settings.setUpstreamDns(opt.ip)
                                onDnsChanged()
                            }
                        }
                    }
                )
            }

            CustomDnsCard(
                isSelected = isCustomSelected,
                customIp = customDnsInput,
                errorMessage = customDnsError,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (customDnsInput.isNotEmpty()) {
                        selectedDns = customDnsInput
                        settings.setUpstreamDns(customDnsInput)
                        onDnsChanged()
                    } else {
                        selectedDns = "custom"
                    }
                },
                onIpChange = {
                    customDnsInput = it
                    customDnsError = null
                },
                onApply = {
                    val trimmed = customDnsInput.trim()
                    if (dev.buni.core.DnsEngine.isValidIpAddress(trimmed)) {
                        customDnsError = null
                        val title = BuniStrings.get("confirm_change_dns_title", currentLanguage)
                        val template = BuniStrings.get("confirm_change_dns_desc", currentLanguage)
                        val message = template.replace("%1\$s", "Custom ($trimmed)")
                        pendingConfirm = PendingConfirmAction(title, message) {
                            settings.setCustomDns(trimmed)
                            settings.setUpstreamDns(trimmed)
                            selectedDns = trimmed
                            onDnsChanged()
                            focusManager.clearFocus()
                        }
                    } else {
                        customDnsError = BuniStrings.get("custom_dns_invalid", currentLanguage)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))

        pendingConfirm?.let { conf ->
            BuniConfirmDialog(
                title = conf.title,
                message = conf.message,
                confirmText = BuniStrings.get("dialog_yes", currentLanguage),
                dismissText = BuniStrings.get("dialog_no", currentLanguage),
                onConfirm = {
                    conf.onConfirm()
                    pendingConfirm = null
                },
                onDismiss = {
                    pendingConfirm = null
                }
            )
        }
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

@Composable
private fun RulesetUpdateCard(
    rulesCount: Int,
    lastUpdateTime: Long,
    isSyncing: Boolean,
    syncMessage: String?,
    onSync: () -> Unit
) {
    val colors = buniColors
    val timeStr = if (lastUpdateTime <= 0L) {
        buniString("ruleset_updated_never")
    } else {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        "${buniString("ruleset_updated_at")}: ${format.format(Date(lastUpdateTime))}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DownloadIcon(
                        modifier = Modifier.size(24.dp),
                        tint = colors.accent
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = buniString("ruleset_title"),
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$rulesCount ${buniString("ruleset_rules_count")} · $timeStr",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSyncing) colors.surfaceVariant else colors.accent)
                        .clickable(enabled = !isSyncing, onClick = onSync)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSyncing) "..." else buniString("btn_update_ruleset"),
                        color = if (isSyncing) colors.textMuted else colors.background,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (syncMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = syncMessage,
                    color = colors.accent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun CustomDnsCard(
    isSelected: Boolean,
    customIp: String,
    errorMessage: String?,
    onSelect: () -> Unit,
    onIpChange: (String) -> Unit,
    onApply: () -> Unit
) {
    val colors = buniColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DnsServerIcon(
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) colors.accent else colors.textPrimary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = buniString("dns_custom_title"),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buniString("dns_custom_desc"),
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

        if (isSelected) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customIp,
                    onValueChange = onIpChange,
                    placeholder = {
                        Text(
                            text = buniString("custom_dns_hint"),
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent)
                        .clickable(onClick = onApply)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = buniString("custom_dns_apply"),
                        color = colors.background,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private suspend fun syncRulesetFromNetwork(context: android.content.Context): Int? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://raw.githubusercontent.com/muadzhdz/buni/main/android/app/src/main/assets/blocklist.txt")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Buni-Android-Client")

            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val lines = text.lines()
                    .mapNotNull { dev.buni.core.DnsEngine.cleanDomainLine(it) }
                if (lines.size >= 50) {
                    val otaFile = File(context.filesDir, "blocklist_ota.txt")
                    otaFile.writeText(lines.joinToString("\n"))
                    val intent = android.content.Intent(context, BuniVpnService::class.java).apply {
                        action = BuniVpnService.ACTION_RELOAD_RULES
                    }
                    context.startService(intent)
                    lines.size
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
