#!/bin/bash
set -e

IMAGE="helmseek-backend"
CONTAINER="helmseek-backend"
PORT=7666
ENV_FILE="$(pwd)/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: .env file not found at $ENV_FILE"
  exit 1
fi

echo "Stopping existing container..."
podman stop "$CONTAINER" 2>/dev/null || true
podman rm "$CONTAINER" 2>/dev/null || true

echo "Building image..."
podman build -t "$IMAGE" .

echo "Starting container..."
podman run -d \
  --name "$CONTAINER" \
  -p "$PORT:$PORT" \
  -v "$ENV_FILE:/app/.env:ro,Z" \
  --restart unless-stopped \
  "$IMAGE"

mkdir -p logs
LOG_FILE="logs/$(TZ='America/Chicago' date '+%Y-%m-%d_%H-%M-%S')_CST.log"
podman logs -f "$CONTAINER" >> "$LOG_FILE" 2>&1 &

echo "HelmSeek backend running on http://localhost:$PORT"
echo "Logging to $LOG_FILE"
