#!/bin/bash

PI_USER="pi"
PI_HOST="${PI_HOST:-raspberrypi}"
PI_PATH="/home/pi"

LOCAL_DIR="."

START_AFTER_DEPLOY=false

if [ "$1" = "start" ]; then
    START_AFTER_DEPLOY=true
fi

echo "Syncing files to pi..."

rsync -a --no-owner --no-group --delete \
  --rsync-path="sudo rsync" \
  --exclude="tests/" \
  --exclude="pytest.ini" \
  --exclude="requirements-dev.txt" \
  --exclude="app/__pycache__/" \
  --exclude="coverage.xml" \
  --exclude="deploy.sh" \
  ${LOCAL_DIR} ${PI_USER}@${PI_HOST}:${PI_PATH}

ssh ${PI_USER}@${PI_HOST} << "EOF"
sudo chown -R ${PI_USER}:${PI_USER} ${PI_PATH}
cd ${PI_PATH}
docker compose down
EOF

if [ "$START_AFTER_DEPLOY" = true ]; then
    echo "Starting docker services..."

    ssh ${PI_USER}@${PI_HOST} << "EOF"
cd ${PI_PATH}
docker compose down
docker compose up -d --build
./configure
EOF

    echo "Services started."
else
    echo "Services not started by script, start them manually on the pi."
fi

echo "Deployment finished."
