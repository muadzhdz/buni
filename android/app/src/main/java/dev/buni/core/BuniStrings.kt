package dev.buni.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class Language {
    EN,
    ID
}

val LocalBuniLanguage = staticCompositionLocalOf { Language.EN }

val buniLanguage: Language
    @Composable
    @ReadOnlyComposable
    get() = LocalBuniLanguage.current

@Composable
fun buniString(key: String): String = BuniStrings.get(key, buniLanguage)

object BuniStrings {

    fun get(key: String, lang: Language): String {
        return when (lang) {
            Language.EN -> enStrings[key] ?: key
            Language.ID -> idStrings[key] ?: key
        }
    }

    private val enStrings = mapOf(
        "app_tagline" to "the hidden network shield",
        "tunnel_tagline" to "wireguard privacy tunnel",
        "standby" to "STANDBY",
        "shield_tab" to "SHIELD",
        "shield_sub" to "Local Zero-Latency",
        "tunnel_tab" to "TUNNEL",
        "tunnel_sub" to "WireGuard Node",
        "tap_to_shield" to "TAP TO SHIELD",
        "tap_to_tunnel" to "TAP TO TUNNEL",
        "shield_engaged" to "SHIELD ENGAGED",
        "tunnel_engaged" to "TUNNEL ENGAGED",
        "shield_desc" to "Direct WAN · Local DNS Sinkhole · Cloudflare",
        "tunnel_desc" to "Encrypted Uplink · Remote WireGuard Node",
        "queries" to "QUERIES",
        "blocked" to "BLOCKED",
        "latency" to "LATENCY",
        "live_inspection" to "LIVE INSPECTION",
        "all" to "ALL",
        "awaiting_packets" to "> awaiting packet stream...",
        "events" to "EVENTS",
        "split_tunneling" to "split-tunneling",
        "split_sub" to "bypass apps from vpn interface directly to cellular/wi-fi",
        "search_apps" to "search apps...",
        "bypassed_apps" to "BYPASSED APPS",
        "reading_packages" to "> reading installed packages...",
        "bypass" to "BYPASS",
        "shield" to "SHIELD",
        "rules_filters" to "rules & filters",
        "rules_sub" to "manage local sinkhole rules and upstream resolver",
        "protection_categories" to "PROTECTION CATEGORIES",
        "ads_title" to "Ad Networks & Popups",
        "ads_sub" to "DoubleClick, AdMob, UnityAds, AppLovin",
        "trackers_title" to "Mobile Telemetry & Trackers",
        "trackers_sub" to "Adjust, AppsFlyer, Branch.io, Crashlytics",
        "malware_title" to "Malware & Phishing Domains",
        "malware_sub" to "Strict domain blocklist matching",
        "upstream_resolver" to "UPSTREAM RESOLVER",
        "remote_node" to "remote node",
        "remote_node_sub" to "wireguard full encrypted tunnel uplink",
        "protocol" to "PROTOCOL",
        "endpoint_address" to "ENDPOINT ADDRESS",
        "peer_public_key" to "PEER PUBLIC KEY",
        "about_tunnel" to "ABOUT TUNNEL MODE",
        "about_tunnel_desc" to "When activated on the main dashboard, Buni encapsulates 100% of device network traffic (0.0.0.0/0) through this remote node. Your public IP is completely masked.",
        "appearance_settings" to "APPEARANCE & LANGUAGE",
        "theme_title" to "Color Theme",
        "theme_sub_dark" to "Pure OLED Black",
        "theme_sub_light" to "Clean Zinc Light",
        "language_title" to "App Language",
        "language_sub_en" to "English (US)",
        "language_sub_id" to "Bahasa Indonesia",
        "nav_shield" to "SHIELD",
        "nav_apps" to "APPS",
        "nav_rules" to "RULES",
        "nav_node" to "NODE"
    )

    private val idStrings = mapOf(
        "app_tagline" to "perisai jaringan tak kasat mata",
        "tunnel_tagline" to "tunnel privasi wireguard",
        "standby" to "STANDBY",
        "shield_tab" to "PERISAI",
        "shield_sub" to "Lokal Tanpa Latensi",
        "tunnel_tab" to "TUNNEL",
        "tunnel_sub" to "Node WireGuard",
        "tap_to_shield" to "KETUK UNTUK AMANKAN",
        "tap_to_tunnel" to "KETUK UNTUK TUNNEL",
        "shield_engaged" to "PERISAI AKTIF",
        "tunnel_engaged" to "TUNNEL AKTIF",
        "shield_desc" to "Rute Langsung · Sinkhole DNS Lokal · Cloudflare",
        "tunnel_desc" to "Uplink Terenkripsi · Node WireGuard Remote",
        "queries" to "KUERI",
        "blocked" to "DIBLOKIR",
        "latency" to "LATENSI",
        "live_inspection" to "INSPEKSI LANGSUNG",
        "all" to "SEMUA",
        "awaiting_packets" to "> menunggu aliran paket...",
        "events" to "PERISTIWA",
        "split_tunneling" to "split-tunneling",
        "split_sub" to "kecualikan aplikasi dari vpn langsung ke seluler/wi-fi",
        "search_apps" to "cari aplikasi...",
        "bypassed_apps" to "APLIKASI DILEWATI",
        "reading_packages" to "> membaca aplikasi terpasang...",
        "bypass" to "LEWATI",
        "shield" to "LINDUNGI",
        "rules_filters" to "aturan & filter",
        "rules_sub" to "kelola filter lokal dan resolver upstream",
        "protection_categories" to "KATEGORI PERLINDUNGAN",
        "ads_title" to "Jaringan Iklan & Popup",
        "ads_sub" to "DoubleClick, AdMob, UnityAds, AppLovin",
        "trackers_title" to "Telemetri & Pelacak Seluler",
        "trackers_sub" to "Adjust, AppsFlyer, Branch.io, Crashlytics",
        "malware_title" to "Domain Malware & Phishing",
        "malware_sub" to "Pencocokan daftar blokir domain ketat",
        "upstream_resolver" to "RESOLVER UPSTREAM",
        "remote_node" to "node remote",
        "remote_node_sub" to "uplink tunnel terenkripsi penuh wireguard",
        "protocol" to "PROTOKOL",
        "endpoint_address" to "ALAMAT ENDPOINT",
        "peer_public_key" to "PUBLIC KEY PEER",
        "about_tunnel" to "TENTANG MODE TUNNEL",
        "about_tunnel_desc" to "Saat diaktifkan di dashboard utama, Buni membungkus 100% lalu lintas data perangkat (0.0.0.0/0) melalui node remote ini. IP publik lu disamarkan secara total.",
        "appearance_settings" to "TAMPILAN & BAHASA",
        "theme_title" to "Tema Warna",
        "theme_sub_dark" to "Hitam OLED Murni",
        "theme_sub_light" to "Zinc Terang Bersih",
        "language_title" to "Bahasa Aplikasi",
        "language_sub_en" to "English (US)",
        "language_sub_id" to "Bahasa Indonesia",
        "nav_shield" to "PERISAI",
        "nav_apps" to "APLIKASI",
        "nav_rules" to "ATURAN",
        "nav_node" to "NODE"
    )
}
