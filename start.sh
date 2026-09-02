#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
docker compose up -d --build
echo "SmartPM is running at http://localhost"
