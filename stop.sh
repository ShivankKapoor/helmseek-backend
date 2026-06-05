#!/bin/bash
set -e

IMAGE="helmseek-backend"
CONTAINER="helmseek-backend"

echo "Stopping container..."
podman stop "$CONTAINER" 2>/dev/null || true
podman rm "$CONTAINER" 2>/dev/null || true

echo "Removing image..."
podman rmi "$IMAGE" 2>/dev/null || true

echo "HelmSeek backend stopped and image removed."
