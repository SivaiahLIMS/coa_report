#!/bin/sh
set -e

DB_HOST=${DB_HOST:-postgres}
DB_PORT=${DB_PORT:-5432}
DB_WAIT_TIMEOUT=${DB_WAIT_TIMEOUT:-60}

echo "Waiting for database ${DB_HOST}:${DB_PORT} (timeout ${DB_WAIT_TIMEOUT}s)..."
start_time=$(date +%s)

while :; do
  if nc -z "${DB_HOST}" "${DB_PORT}" 2>/dev/null; then
    echo "Database ${DB_HOST}:${DB_PORT} is available"
    break
  fi

  now=$(date +%s)
  elapsed=$((now - start_time))
  if [ "$elapsed" -ge "${DB_WAIT_TIMEOUT}" ]; then
    echo "Timed out after ${DB_WAIT_TIMEOUT}s waiting for ${DB_HOST}:${DB_PORT}"
    exit 1
  fi

  sleep 1
done

# If command arguments are provided, exec them. Otherwise start the jar.
if [ "$#" -gt 0 ]; then
  exec "$@"
else
  exec java -jar app.jar
fi

