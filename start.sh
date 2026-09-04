#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"

if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  docker compose up -d --build
  echo "SmartPM is running at http://localhost"
else
  echo "Docker is unavailable; starting SmartPM in local development mode."
  echo "This mode requires MySQL on localhost:3306."
  (mvn spring-boot:run) &
  backend_pid=$!
  (cd kanban-frontend && npm run dev -- --host 0.0.0.0) &
  frontend_pid=$!
  trap 'kill "$backend_pid" "$frontend_pid" 2>/dev/null || true' INT TERM EXIT
  echo "SmartPM is running at http://localhost:3000"
  wait
fi
