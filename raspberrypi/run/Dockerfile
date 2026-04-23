FROM ghcr.io/astral-sh/uv:python3.11-bookworm-slim

RUN apt-get update && apt-get install -y \
    bluez \
    dbus \
    libdbus-1-dev \
    libglib2.0-dev \
    gcc \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY . .

RUN chmod +x entrypoint.sh

# persistent volumes for config, DB and logs
# define these in docker-compose.yml as well
ENV CONFIG_PATH="/app/data/config.yaml"
ENV ROOM_ID=""
ENV API_URL=""

# start the entrypoint
ENTRYPOINT ["./entrypoint.sh"]
