package core

import (
	"context"
	"fmt"
	"strings"
	"sync"
	"time"
)

var (
	globalMu     sync.Mutex
	globalCancel context.CancelFunc
	globalRunner *TunRunner
	globalEngine *Engine
)

func StartShield(fd int, blocklistData string, dohEndpoint string) string {
	globalMu.Lock()
	defer globalMu.Unlock()

	if globalCancel != nil {
		globalCancel()
		if globalRunner != nil {
			_ = globalRunner.Close()
		}
	}

	bl := NewBlocklist()
	if blocklistData != "" {
		_ = bl.Load(strings.NewReader(blocklistData))
	}

	doh := NewDohClient(dohEndpoint, 4*time.Second)
	engine := NewEngine(bl, doh, 200)

	ctx, cancel := context.WithCancel(context.Background())
	runner := NewTunRunner(fd, engine, 1500)

	globalCancel = cancel
	globalRunner = runner
	globalEngine = engine

	go func() {
		_ = runner.Run(ctx)
	}()

	return ""
}

func StopShield() {
	globalMu.Lock()
	defer globalMu.Unlock()

	if globalCancel != nil {
		globalCancel()
		globalCancel = nil
	}
	if globalRunner != nil {
		_ = globalRunner.Close()
		globalRunner = nil
	}
	globalEngine = nil
}

func GetStats() string {
	globalMu.Lock()
	engine := globalEngine
	globalMu.Unlock()

	if engine == nil {
		return "0,0,0"
	}

	stats := engine.Stats()
	return fmt.Sprintf("%d,%d,%d", stats.TotalQueries, stats.BlockedQueries, stats.LastLatencyMs)
}

func PollLog() string {
	globalMu.Lock()
	engine := globalEngine
	globalMu.Unlock()

	if engine == nil {
		return ""
	}

	select {
	case entry := <-engine.Logs():
		return fmt.Sprintf("%s|%s|%s|%s|%d",
			entry.Timestamp.Format("15:04:05"),
			entry.Type,
			entry.Domain,
			entry.Action,
			entry.Duration.Milliseconds(),
		)
	default:
		return ""
	}
}
