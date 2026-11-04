package core

import (
	"bufio"
	"io"
	"strings"
	"sync"
)

type Blocklist struct {
	mu      sync.RWMutex
	domains map[string]struct{}
}

func NewBlocklist() *Blocklist {
	return &Blocklist{
		domains: make(map[string]struct{}),
	}
}

func (b *Blocklist) Add(domain string) {
	d := strings.ToLower(strings.TrimSpace(domain))
	d = strings.TrimSuffix(d, ".")
	if d == "" {
		return
	}
	b.mu.Lock()
	b.domains[d] = struct{}{}
	b.mu.Unlock()
}

func (b *Blocklist) Load(r io.Reader) error {
	scanner := bufio.NewScanner(r)
	b.mu.Lock()
	defer b.mu.Unlock()

	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		parts := strings.Fields(line)
		var domain string
		if len(parts) == 1 {
			domain = parts[0]
		} else if len(parts) >= 2 {
			domain = parts[1]
		}
		domain = strings.ToLower(strings.TrimSuffix(domain, "."))
		if domain != "" && domain != "localhost" {
			b.domains[domain] = struct{}{}
		}
	}
	return scanner.Err()
}

func (b *Blocklist) Matches(domain string) bool {
	d := strings.ToLower(strings.TrimSpace(domain))
	d = strings.TrimSuffix(d, ".")
	if d == "" {
		return false
	}

	b.mu.RLock()
	defer b.mu.RUnlock()

	if _, ok := b.domains[d]; ok {
		return true
	}

	sub := d
	for {
		idx := strings.IndexByte(sub, '.')
		if idx == -1 {
			break
		}
		sub = sub[idx+1:]
		if _, ok := b.domains[sub]; ok {
			return true
		}
	}

	return false
}

func (b *Blocklist) Count() int {
	b.mu.RLock()
	defer b.mu.RUnlock()
	return len(b.domains)
}
