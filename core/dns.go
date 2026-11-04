package core

import (
	"bytes"
	"context"
	"encoding/binary"
	"errors"
	"io"
	"net/http"
	"strings"
	"time"
)

var (
	ErrPacketTooShort = errors.New("packet too short")
	ErrInvalidLabel   = errors.New("invalid domain label")
)

const (
	TypeA     uint16 = 1
	TypeAAAA  uint16 = 28
	ClassINET uint16 = 1
)

type DNSQuestion struct {
	Name   string
	Type   uint16
	Class  uint16
	Offset int
}

func ParseQuestion(wire []byte) (*DNSQuestion, error) {
	if len(wire) < 12 {
		return nil, ErrPacketTooShort
	}

	qdCount := binary.BigEndian.Uint16(wire[4:6])
	if qdCount == 0 {
		return nil, errors.New("no question in packet")
	}

	offset := 12
	var labels []string

	for {
		if offset >= len(wire) {
			return nil, ErrPacketTooShort
		}
		length := int(wire[offset])
		offset++
		if length == 0 {
			break
		}
		if length > 63 || offset+length > len(wire) {
			return nil, ErrInvalidLabel
		}
		labels = append(labels, string(wire[offset:offset+length]))
		offset += length
	}

	if offset+4 > len(wire) {
		return nil, ErrPacketTooShort
	}

	qtype := binary.BigEndian.Uint16(wire[offset : offset+2])
	qclass := binary.BigEndian.Uint16(wire[offset+2 : offset+4])
	offset += 4

	return &DNSQuestion{
		Name:   strings.ToLower(strings.Join(labels, ".")),
		Type:   qtype,
		Class:  qclass,
		Offset: offset,
	}, nil
}

func BuildSinkholeResponse(query []byte, q *DNSQuestion) ([]byte, error) {
	if len(query) < q.Offset {
		return nil, ErrPacketTooShort
	}

	buf := make([]byte, 0, q.Offset+16)
	buf = append(buf, query[:q.Offset]...)

	flags := binary.BigEndian.Uint16(buf[2:4])
	flags |= 0x8000
	flags |= 0x0400
	flags &^= 0x000F
	binary.BigEndian.PutUint16(buf[2:4], flags)

	binary.BigEndian.PutUint16(buf[4:6], 1)
	binary.BigEndian.PutUint16(buf[6:8], 1)
	binary.BigEndian.PutUint16(buf[8:10], 0)
	binary.BigEndian.PutUint16(buf[10:12], 0)

	buf = append(buf, 0xc0, 0x0c)

	if q.Type == TypeA {
		ans := make([]byte, 14)
		binary.BigEndian.PutUint16(ans[0:2], TypeA)
		binary.BigEndian.PutUint16(ans[2:4], ClassINET)
		binary.BigEndian.PutUint32(ans[4:8], 300)
		binary.BigEndian.PutUint16(ans[8:10], 4)
		copy(ans[10:14], []byte{0, 0, 0, 0})
		buf = append(buf, ans...)
	} else if q.Type == TypeAAAA {
		ans := make([]byte, 26)
		binary.BigEndian.PutUint16(ans[0:2], TypeAAAA)
		binary.BigEndian.PutUint16(ans[2:4], ClassINET)
		binary.BigEndian.PutUint32(ans[4:8], 300)
		binary.BigEndian.PutUint16(ans[8:10], 16)
		buf = append(buf, ans...)
	} else {
		binary.BigEndian.PutUint16(buf[6:8], 0)
	}

	return buf, nil
}

type DohClient struct {
	Endpoint   string
	HTTPClient *http.Client
}

func NewDohClient(endpoint string, timeout time.Duration) *DohClient {
	if endpoint == "" {
		endpoint = "https://cloudflare-dns.com/dns-query"
	}
	return &DohClient{
		Endpoint: endpoint,
		HTTPClient: &http.Client{
			Timeout: timeout,
		},
	}
}

func (c *DohClient) Query(ctx context.Context, wire []byte) ([]byte, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.Endpoint, bytes.NewReader(wire))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/dns-message")
	req.Header.Set("Accept", "application/dns-message")

	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, errors.New("upstream doh error: " + resp.Status)
	}

	return io.ReadAll(resp.Body)
}
