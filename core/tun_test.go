package core

import (
	"encoding/binary"
	"net"
	"testing"
)

func TestCraftIPv4UDPReply(t *testing.T) {
	req := make([]byte, 28)
	req[0] = 0x45
	binary.BigEndian.PutUint16(req[2:4], 28)
	req[9] = 17

	copy(req[12:16], net.ParseIP("192.168.1.100").To4())
	copy(req[16:20], net.ParseIP("1.1.1.1").To4())

	binary.BigEndian.PutUint16(req[20:22], 54321)
	binary.BigEndian.PutUint16(req[22:24], 53)
	binary.BigEndian.PutUint16(req[24:26], 8)

	payload := []byte{0x01, 0x02, 0x03, 0x04}
	reply := craftIPv4UDPReply(req, payload)

	if len(reply) != 20+8+len(payload) {
		t.Fatalf("expected length %d, got %d", 32, len(reply))
	}

	srcIP := net.IP(reply[12:16])
	dstIP := net.IP(reply[16:20])

	if !srcIP.Equal(net.ParseIP("1.1.1.1")) {
		t.Fatalf("expected src ip 1.1.1.1, got %s", srcIP)
	}

	if !dstIP.Equal(net.ParseIP("192.168.1.100")) {
		t.Fatalf("expected dst ip 192.168.1.100, got %s", dstIP)
	}

	srcPort := binary.BigEndian.Uint16(reply[20:22])
	dstPort := binary.BigEndian.Uint16(reply[22:24])

	if srcPort != 53 {
		t.Fatalf("expected src port 53, got %d", srcPort)
	}

	if dstPort != 54321 {
		t.Fatalf("expected dst port 54321, got %d", dstPort)
	}

	ipChecksum := calculateChecksum(reply[:20])
	if ipChecksum != 0 {
		t.Fatalf("expected valid ip checksum 0 on verification, got %x", ipChecksum)
	}
}
