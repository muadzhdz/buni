<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="art/buni-logo-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="art/buni-logo-light.svg">
    <img src="art/buni-logo-dark.svg" width="128" height="128" alt="Buni Logo">
  </picture>
</p>

<h1 align="center">buni</h1>

<p align="center">
  <b>The hidden network shield.</b>
</p>

<p align="center">
  <a href="https://github.com/muadzhdz/buni/releases"><img src="https://img.shields.io/github/v/release/muadzhdz/buni?style=flat-square&color=10b981&label=Release" alt="Release" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-09090b?style=flat-square&logo=android&logoColor=white" alt="Platform" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://f-droid.org"><img src="https://img.shields.io/badge/F--Droid-Ready-1976D2?style=flat-square&logo=fdroid&logoColor=white" alt="F-Droid" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-09090b?style=flat-square" alt="License" /></a>
</p>

---

## Overview

**Buni** (from Sundanese / Old Malay *sembunyi* — hidden, shielded) is a lightweight, defensive, on-device network privacy shield and in-memory DNS sinkhole for Android.

Unlike commercial VPN services that route all your personal traffic to third-party remote servers, Buni operates **100% locally on your device** without root privileges. It establishes a loopback tunnel via Android `VpnService` that intercepts only DNS queries on port 53, evaluates them in memory, and immediately drops ads, telemetry, trackers, and invasive OEM background bloatware before any data leaves your phone.

---

## Architecture

```
                 [ Android Applications ]
                            │
               (DNS Queries - UDP Port 53)
                            ▼
              ┌───────────────────────────┐
              │  Android VpnService Loop  │
              │   tun0 (10.233.0.1/30)    │
              └─────────────┬─────────────┘
                            │
                [ In-Memory Domain Trie ]
                            │
             ┌──────────────┴──────────────┐
             ▼                             ▼
     [ Blocked Domain ]            [ Clean Domain ]
      • Ads & Popups                • Encrypted Uplink
      • OEM Telemetry               • Protected DoH / Custom
      • Mobile Trackers                            │
             │                                     ▼
             ▼                            (Internet / WAN)
      Synthetic Reply
   (0.0.0.0 / :: in 0ms)
```

- **Local Zero-Latency Sinkhole**: Intercepts UDP port 53 traffic routed strictly to `10.233.0.2/32` (IPv4) and `fd00:1:fd00:1::2/128` (IPv6). Queries matching blocklist rules are immediately answered with synthetic RFC-compliant replies (`0.0.0.0` for IPv4 A, `::` for IPv6 AAAA, NODATA for others) with 0ms roundtrip.
- **Encrypted DNS Uplink**: Permitted queries resolve through protected TLS sockets via DNS over HTTPS (DoH / RFC 8484) or selected upstream resolvers (Cloudflare, Quad9, AdGuard, Google).
- **Per-App Split Tunneling**: Selectively exclude specific applications to bypass the shield interface directly to cellular/Wi-Fi.
- **Granular Category Filters**: Real-time toggles for ad networks, mobile telemetry/trackers, and malicious OEM endpoints.
- **Dual Aesthetic Theme Engine**: Seamless switching between high-contrast daylight Zinc and OLED obsidian stealth themes.

---

## Building from Source

### Prerequisites
- JDK 17+
- Android SDK (API 35, Build-Tools 35.0.0+)
- Pure JVM / Zero NDK required

```bash
# Clone the repository
git clone https://github.com/muadzhdz/buni.git
cd buni/android

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

The output APK will be located at:
`android/app/build/outputs/apk/release/app-release.apk`

---

## Standalone Development Engine

For desktop and Linux systems development, a standalone Go DNS filtering engine and CLI tool is available under `core/`:

```bash
cd core
go test -v ./...
go run ./cmd/buni-dev
```

---

## Distribution & Updates

| Channel | Delivery Method | Verification |
| :--- | :--- | :--- |
| **F-Droid** | Automatic background updates via F-Droid client | Built from source and signed by F-Droid |
| **GitHub Releases** | In-app notification / Direct APK download | Signed by project release keystore |
| **Obtainium** | Tracked and installed directly via GitHub API | Matches GitHub release tags |

---

## License

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0-or-later). See the [LICENSE](LICENSE) file for the full license text.
