package core

import (
	"encoding/binary"
	"net"
	"testing"
)

func buildMockQuery(domain string, qtype uint16) []byte {
	buf := make([]byte, 12)
	binary.BigEndian.PutUint16(buf[0:2], 0x1234)
	binary.BigEndian.PutUint16(buf[2:4], 0x0100)
	binary.BigEndian.PutUint16(buf[4:6], 1)

	parts := []string{"google", "com"}
	for _, p := range parts {
		buf = append(buf, byte(len(p)))
		buf = append(buf, []byte(p)...)
	}
	buf = append(buf, 0x00)

	typeClass := make([]byte, 4)
	binary.BigEndian.PutUint16(typeClass[0:2], qtype)
	binary.BigEndian.PutUint16(typeClass[2:4], ClassINET)
	buf = append(buf, typeClass...)

	return buf
}

func TestParseQuestion(t *testing.T) {
	wire := buildMockQuery("google.com", TypeA)
	q, err := ParseQuestion(wire)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if q.Name != "google.com" {
		t.Fatalf("expected google.com, got %s", q.Name)
	}

	if q.Type != TypeA {
		t.Fatalf("expected TypeA, got %d", q.Type)
	}

	if q.Class != ClassINET {
		t.Fatalf("expected ClassINET, got %d", q.Class)
	}
}

func TestBuildSinkholeResponse(t *testing.T) {
	wire := buildMockQuery("google.com", TypeA)
	q, err := ParseQuestion(wire)
	if err != nil {
		t.Fatalf("unexpected parse error: %v", err)
	}

	resp, err := BuildSinkholeResponse(wire, q)
	if err != nil {
		t.Fatalf("unexpected build error: %v", err)
	}

	if len(resp) < q.Offset+16 {
		t.Fatalf("response too short: %d", len(resp))
	}

	flags := binary.BigEndian.Uint16(resp[2:4])
	if flags&0x8000 == 0 {
		t.Fatalf("expected QR bit set in response flags")
	}

	ancount := binary.BigEndian.Uint16(resp[6:8])
	if ancount != 1 {
		t.Fatalf("expected ancount 1, got %d", ancount)
	}

	ipBytes := resp[len(resp)-4:]
	ip := net.IP(ipBytes)
	if !ip.Equal(net.IPv4zero) {
		t.Fatalf("expected 0.0.0.0, got %s", ip.String())
	}
}
