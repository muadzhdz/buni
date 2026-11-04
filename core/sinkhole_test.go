package core

import (
	"strings"
	"testing"
)

func TestBlocklistMatching(t *testing.T) {
	bl := NewBlocklist()
	bl.Add("ads.example.com")
	bl.Add("tracker.net")

	if !bl.Matches("ads.example.com") {
		t.Fatalf("expected exact match")
	}

	if !bl.Matches("sub.tracker.net") {
		t.Fatalf("expected subdomain match")
	}

	if !bl.Matches("deep.sub.tracker.net") {
		t.Fatalf("expected deep subdomain match")
	}

	if bl.Matches("example.com") {
		t.Fatalf("did not expect parent match")
	}

	if bl.Matches("safe-tracker.net.com") {
		t.Fatalf("did not expect suffix substring match")
	}
}

func TestBlocklistLoadHosts(t *testing.T) {
	data := `
127.0.0.1 adservice.google.com
0.0.0.0 telemetry.app.io
bad-domain.org
`
	bl := NewBlocklist()
	if err := bl.Load(strings.NewReader(data)); err != nil {
		t.Fatalf("failed to load: %v", err)
	}

	if bl.Count() != 3 {
		t.Fatalf("expected count 3, got %d", bl.Count())
	}

	if !bl.Matches("adservice.google.com") {
		t.Fatalf("expected adservice.google.com to match")
	}

	if !bl.Matches("telemetry.app.io") {
		t.Fatalf("expected telemetry.app.io to match")
	}

	if !bl.Matches("bad-domain.org") {
		t.Fatalf("expected bad-domain.org to match")
	}
}
