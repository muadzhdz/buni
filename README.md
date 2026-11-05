# buni

The hidden network shield.

A defensive, high-performance network privacy client, in-memory DNS sinkhole, and WireGuard uplink orchestrator for Android.

## Architecture

- **SHIELD**: Zero-latency userspace DNS sinkhole on `tun0` (`10.233.0.1/30`). Evaluates DNS queries against an in-memory hash trie, terminating tracker and ad telemetries at `0.0.0.0` before WAN transit. Clean queries resolve over protected DoH (RFC 8484) sockets.
- **TUNNEL**: Full-device encapsulation (`0.0.0.0/0`, `::/0`) into authenticated ChaCha20-Poly1305 WireGuard datagrams.

## Building

```bash
cd android
./gradlew assembleRelease
```

## Verification

```bash
cd core
go test -v -race ./...
```

## License

GPL-3.0-or-later
