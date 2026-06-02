#!/usr/bin/env bash
set -e

# Ensure werkflow-network exists before starting business-service.
# Safe no-op when the enterprise stack is already running (network already exists).
docker network create werkflow-network 2>/dev/null || true

docker compose up -d business-service
