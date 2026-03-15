#!/bin/bash
set -e

echo "=== DuDoong Local Dev Environment ==="

echo "[1/3] Starting Docker containers (MySQL + Redis)..."
docker-compose up -d

echo "[2/3] Waiting for MySQL to be ready..."
for i in $(seq 1 30); do
    if docker-compose exec -T mysql mysqladmin ping -h localhost -u dudoong -pdudoong --silent 2>/dev/null; then
        echo "MySQL is ready."
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

echo "[3/3] Starting DuDoong API with 'local' profile..."
echo "Swagger UI: http://localhost:8080/api/swagger-ui/index.html"
./gradlew :DuDoong-Api:bootRun --args='--spring.profiles.active=local'
