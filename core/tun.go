package core

import (
	"context"
	"encoding/binary"
	"errors"
	"io"
	"net"
	"os"
	"sync"
)

var (
	ErrNotIPv4 = errors.New("not an ipv4 packet")
	ErrNotUDP  = errors.New("not a udp packet")
)

type PacketHandler interface {
	HandleDNS(ctx context.Context, wire []byte) ([]byte, error)
}

type TunRunner struct {
	file    *os.File
	handler PacketHandler
	mtu     int
	mu      sync.Mutex
	closed  bool
}

func NewTunRunner(fd int, handler PacketHandler, mtu int) *TunRunner {
	if mtu <= 0 {
		mtu = 1500
	}
	file := os.NewFile(uintptr(fd), "tun0")
	return &TunRunner{
		file:    file,
		handler: handler,
		mtu:     mtu,
	}
}

func (r *TunRunner) Close() error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.closed {
		return nil
	}
	r.closed = true
	if r.file != nil {
		return r.file.Close()
	}
	return nil
}

func (r *TunRunner) Run(ctx context.Context) error {
	buf := make([]byte, r.mtu)

	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
		}

		n, err := r.file.Read(buf)
		if err != nil {
			if errors.Is(err, io.EOF) || errors.Is(err, os.ErrClosed) {
				return nil
			}
			return err
		}

		packet := buf[:n]
		if len(packet) < 20 {
			continue
		}

		version := packet[0] >> 4
		if version != 4 {
			continue
		}

		ihl := int(packet[0]&0x0F) * 4
		if len(packet) < ihl+8 {
			continue
		}

		proto := packet[9]
		if proto != 17 {
			continue
		}

		dstPort := binary.BigEndian.Uint16(packet[ihl+2 : ihl+4])
		if dstPort != 53 {
			continue
		}

		udpLen := int(binary.BigEndian.Uint16(packet[ihl+4 : ihl+6]))
		if len(packet) < ihl+udpLen || udpLen < 8 {
			continue
		}

		dnsPayload := packet[ihl+8 : ihl+udpLen]
		replyPayload, err := r.handler.HandleDNS(ctx, dnsPayload)
		if err != nil || len(replyPayload) == 0 {
			continue
		}

		replyPacket := craftIPv4UDPReply(packet[:ihl+8], replyPayload)
		_, _ = r.file.Write(replyPacket)
	}
}

func craftIPv4UDPReply(reqHeader []byte, payload []byte) []byte {
	ihl := int(reqHeader[0]&0x0F) * 4
	totalLen := 20 + 8 + len(payload)
	reply := make([]byte, totalLen)

	reply[0] = 0x45
	reply[1] = 0x00
	binary.BigEndian.PutUint16(reply[2:4], uint16(totalLen))
	binary.BigEndian.PutUint16(reply[4:6], 0)
	binary.BigEndian.PutUint16(reply[6:8], 0x4000)
	reply[8] = 64
	reply[9] = 17
	binary.BigEndian.PutUint16(reply[10:12], 0)

	copy(reply[12:16], reqHeader[16:20])
	copy(reply[16:20], reqHeader[12:16])

	ipChecksum := calculateChecksum(reply[:20])
	binary.BigEndian.PutUint16(reply[10:12], ipChecksum)

	copy(reply[20:22], reqHeader[ihl+2:ihl+4])
	copy(reply[22:24], reqHeader[ihl:ihl+2])
	binary.BigEndian.PutUint16(reply[24:26], uint16(8+len(payload)))
	binary.BigEndian.PutUint16(reply[26:28], 0)

	copy(reply[28:], payload)

	udpChecksum := calculateUDPChecksum(reply[12:16], reply[16:20], reply[20:])
	binary.BigEndian.PutUint16(reply[26:28], udpChecksum)

	return reply
}

func calculateChecksum(data []byte) uint16 {
	var sum uint32
	for i := 0; i < len(data)-1; i += 2 {
		sum += uint32(binary.BigEndian.Uint16(data[i : i+2]))
	}
	if len(data)%2 == 1 {
		sum += uint32(data[len(data)-1]) << 8
	}
	for sum > 0xFFFF {
		sum = (sum & 0xFFFF) + (sum >> 16)
	}
	return ^uint16(sum)
}

func calculateUDPChecksum(srcIP, dstIP net.IP, udpSegment []byte) uint16 {
	var sum uint32

	sum += uint32(binary.BigEndian.Uint16(srcIP[0:2]))
	sum += uint32(binary.BigEndian.Uint16(srcIP[2:4]))
	sum += uint32(binary.BigEndian.Uint16(dstIP[0:2]))
	sum += uint32(binary.BigEndian.Uint16(dstIP[2:4]))

	sum += 17
	sum += uint32(len(udpSegment))

	for i := 0; i < len(udpSegment)-1; i += 2 {
		sum += uint32(binary.BigEndian.Uint16(udpSegment[i : i+2]))
	}
	if len(udpSegment)%2 == 1 {
		sum += uint32(udpSegment[len(udpSegment)-1]) << 8
	}

	for sum > 0xFFFF {
		sum = (sum & 0xFFFF) + (sum >> 16)
	}

	res := ^uint16(sum)
	if res == 0 {
		return 0xFFFF
	}
	return res
}
