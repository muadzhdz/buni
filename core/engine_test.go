package core

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestEngineHandleDNSSinkhole(t *testing.T) {
	bl := NewBlocklist()
	bl.Add("analytics.tracker.com")

	doh := NewDohClient("http://127.0.0.1:0", time.Second)
	engine := NewEngine(bl, doh, 10)

	wire := buildMockQuery("analytics.tracker.com", TypeA)
	resp, err := engine.HandleDNS(context.Background(), wire)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(resp) == 0 {
		t.Fatalf("expected sinkhole response")
	}

	stats := engine.Stats()
	if stats.TotalQueries != 1 {
		t.Fatalf("expected total 1, got %d", stats.TotalQueries)
	}
	if stats.BlockedQueries != 1 {
		t.Fatalf("expected blocked 1, got %d", stats.BlockedQueries)
	}

	select {
	case entry := <-engine.Logs():
		if entry.Type != "SINK" || entry.Action != "DROPPED" {
			t.Fatalf("unexpected log entry: %+v", entry)
		}
	default:
		t.Fatalf("expected log entry in channel")
	}
}

func TestEngineHandleDNSDoh(t *testing.T) {
	mockResponse := []byte{0x12, 0x34, 0x81, 0x80, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/dns-message")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write(mockResponse)
	}))
	defer server.Close()

	bl := NewBlocklist()
	doh := NewDohClient(server.URL, time.Second)
	engine := NewEngine(bl, doh, 10)

	wire := buildMockQuery("safe-site.org", TypeA)
	resp, err := engine.HandleDNS(context.Background(), wire)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(resp) != len(mockResponse) {
		t.Fatalf("expected response length %d, got %d", len(mockResponse), len(resp))
	}

	stats := engine.Stats()
	if stats.TotalQueries != 1 {
		t.Fatalf("expected total 1, got %d", stats.TotalQueries)
	}
	if stats.BlockedQueries != 0 {
		t.Fatalf("expected blocked 0, got %d", stats.BlockedQueries)
	}

	select {
	case entry := <-engine.Logs():
		if entry.Type != "DOH" || entry.Action != "RESOLVED" {
			t.Fatalf("unexpected log entry: %+v", entry)
		}
	default:
		t.Fatalf("expected log entry in channel")
	}
}
