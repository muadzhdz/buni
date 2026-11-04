.PHONY: all test test-verbose fmt clean build-core

all: test build-core

test:
	cd core && go test ./...

test-verbose:
	cd core && go test -v ./...

fmt:
	cd core && gofmt -s -w .

build-core:
	cd core && go build -v ./...

clean:
	cd core && go clean
