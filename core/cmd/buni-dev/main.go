package main

import (
	"context"
	"flag"
	"fmt"
	"net"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"dev.buni/core"
)

func main() {
	addr := flag.String("addr", "127.0.0.1:5353", "listen udp address")
	dohURL := flag.String("doh", "https://cloudflare-dns.com/dns-query", "upstream doh url")
	blocklistArg := flag.String("block", "ads.example.com,telemetry.app.io,adservice.google.com", "comma-separated domains to sinkhole")
	flag.Parse()

	bl := core.NewBlocklist()
	for _, d := range strings.Split(*blocklistArg, ",") {
		bl.Add(strings.TrimSpace(d))
	}

	doh := core.NewDohClient(*dohURL, 4*time.Second)
	engine := core.NewEngine(bl, doh, 100)

	conn, err := net.ListenPacket("udp", *addr)
	if err != nil {
		fmt.Fprintf(os.Stderr, "failed to listen on %s: %v\n", *addr, err)
		os.Exit(1)
	}
	defer conn.Close()

	fmt.Printf("buni core dev runner active on %s\n", *addr)
	fmt.Printf("sinkhole loaded with %d domains\n", bl.Count())
	fmt.Printf("doh upstream: %s\n\n", *dohURL)

	go func() {
		for entry := range engine.Logs() {
			fmt.Printf("[%s] %-4s %-28s %-10s (%dms)\n",
				entry.Timestamp.Format("15:04:05"),
				entry.Type,
				entry.Domain,
				entry.Action,
				entry.Duration.Milliseconds(),
			)
		}
	}()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)

	go func() {
		<-sigChan
		fmt.Println("\nshutting down buni...")
		cancel()
		_ = conn.Close()
	}()

	buf := make([]byte, 1500)
	for {
		n, remoteAddr, err := conn.ReadFrom(buf)
		if err != nil {
			select {
			case <-ctx.Done():
				return
			default:
				continue
			}
		}

		wire := make([]byte, n)
		copy(wire, buf[:n])

		go func(clientAddr net.Addr, queryWire []byte) {
			resp, err := engine.HandleDNS(ctx, queryWire)
			if err != nil || len(resp) == 0 {
				return
			}
			_, _ = conn.WriteTo(resp, clientAddr)
		}(remoteAddr, wire)
	}
}
