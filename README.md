# buni

The hidden network shield.

Hybrid DNS privacy resolver, in-memory domain sinkhole, and Android network client.

---

## Architecture

```text
               +-------------------------------------------------------------+
               |                         buni core                           |
               |                                                             |
               |  +--------------------+             +--------------------+  |
               |  |  TunRunner (tun0)  |             |  DoH DNS Client    |  |
               |  |  - IPv4 Packet Loop|             |  - RFC 1035 Wire   |  |
               |  |  - UDP 53 Handler  |             |  - Cloudflare Upstr|  |
               |  |  - Reply Crafter   |             |  - Ad Sinkholing   |  |
               |  +---------+----------+             +---------+----------+  |
               +------------|----------------------------------|-------------+
                            |                                  |
   [ Android Apps ]         |                                  |
           |                v                                  v
           +=======> [ tun0: 10.233.0.1 ] ─► [ UDP :53 ] ──► [ Local Handler ]
                            |                                  | (HTTPS 443)
                            |                                  v
                            +───────────────────────────────► [ DoH Upstream ]
```

---

## Directory Layout

```text
buni/
├── core/
│   ├── dns.go            # RFC 1035 wire parser & DoH client
│   ├── sinkhole.go       # In-memory domain blocklist
│   ├── engine.go         # Coordinator & telemetry metrics
│   ├── tun.go            # TUN packet loop & IPv4 UDP crafter
│   └── bridge.go         # Android JNI bridge entrypoint
│
└── android/
    └── app/src/main/
        ├── AndroidManifest.xml
        └── java/dev/buni/
            ├── BuniApp.kt
            ├── service/BuniVpnService.kt
            ├── theme/Color.kt & Theme.kt
            └── ui/MainActivity.kt & DashboardScreen.kt
```

---

## Build & Test

```bash
make test
```
