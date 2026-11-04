package core

import (
	"context"
	"sync/atomic"
	"time"
)

type LogEntry struct {
	Timestamp time.Time
	Type      string
	Domain    string
	Action    string
	Duration  time.Duration
}

type Stats struct {
	TotalQueries   uint64
	BlockedQueries uint64
	LastLatencyMs  uint64
}

type Engine struct {
	blocklist      *Blocklist
	doh            *DohClient
	logs           chan LogEntry
	totalQueries   atomic.Uint64
	blockedQueries atomic.Uint64
	lastLatencyMs  atomic.Uint64
}

func NewEngine(blocklist *Blocklist, doh *DohClient, logCapacity int) *Engine {
	if logCapacity <= 0 {
		logCapacity = 100
	}
	return &Engine{
		blocklist: blocklist,
		doh:       doh,
		logs:      make(chan LogEntry, logCapacity),
	}
}

func (e *Engine) HandleDNS(ctx context.Context, wire []byte) ([]byte, error) {
	q, err := ParseQuestion(wire)
	if err != nil {
		return nil, err
	}

	e.totalQueries.Add(1)

	if e.blocklist != nil && e.blocklist.Matches(q.Name) {
		e.blockedQueries.Add(1)
		resp, err := BuildSinkholeResponse(wire, q)
		if err != nil {
			return nil, err
		}
		e.emitLog(LogEntry{
			Timestamp: time.Now(),
			Type:      "SINK",
			Domain:    q.Name,
			Action:    "DROPPED",
			Duration:  0,
		})
		return resp, nil
	}

	start := time.Now()
	resp, err := e.doh.Query(ctx, wire)
	duration := time.Since(start)

	if err != nil {
		e.emitLog(LogEntry{
			Timestamp: time.Now(),
			Type:      "DOH",
			Domain:    q.Name,
			Action:    "ERROR",
			Duration:  duration,
		})
		return nil, err
	}

	e.lastLatencyMs.Store(uint64(duration.Milliseconds()))
	e.emitLog(LogEntry{
		Timestamp: time.Now(),
		Type:      "DOH",
		Domain:    q.Name,
		Action:    "RESOLVED",
		Duration:  duration,
	})

	return resp, nil
}

func (e *Engine) emitLog(entry LogEntry) {
	select {
	case e.logs <- entry:
	default:
	}
}

func (e *Engine) Logs() <-chan LogEntry {
	return e.logs
}

func (e *Engine) Stats() Stats {
	return Stats{
		TotalQueries:   e.totalQueries.Load(),
		BlockedQueries: e.blockedQueries.Load(),
		LastLatencyMs:  e.lastLatencyMs.Load(),
	}
}
