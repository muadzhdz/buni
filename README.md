# buni

The hidden network shield.

A defensive, high-performance network privacy shield and in-memory DNS sinkhole for Android.

## Architecture

- **SHIELD**: Zero-latency userspace DNS sinkhole using Android `VpnService` bound to `10.233.0.1/30` routing only DNS traffic (`10.233.0.2/32`). Evaluates DNS queries against an in-memory blocklist with granular category controls (Ads, Trackers, Malware).
- **SYNTHETIC RESPONSES**: Sinkholed queries receive RFC-compliant immediate responses (0.0.0.0 for IPv4 A queries, `::` for IPv6 AAAA queries) with 0ms roundtrip latency.
- **ENCRYPTED RESOLVER**: Permitted queries resolve over protected TLS sockets using DNS over HTTPS (DoH / RFC 8484) or selected upstream DNS servers.
- **PER-APP BYPASS**: Applications can be selectively excluded from interception to route directly to WAN.

## Building

```bash
cd android
./gradlew assembleRelease
```

## License

GPL-3.0-or-later
