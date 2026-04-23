#!/bin/bash

echo " Starting Raspberry Pi IoT Gateway "

while true; do
    echo "[Entrypoint] Running Bootstrap phase..."
    uv run bootstrap.py
    
    BOOTSTRAP_EXIT=$?
    if [ $BOOTSTRAP_EXIT -ne 0 ]; then
        echo "[Entrypoint] Bootstrap failed (Exit $BOOTSTRAP_EXIT). Retrying in 10s..."
        sleep 10
        continue
    fi

    echo "[Entrypoint] Launching Main Application Loop..."
    uv run main.py
    APP_EXIT=$?

    # Special Exit Code 42: configuration was updated, process requested restart
    if [ $APP_EXIT -eq 42 ]; then
        echo "[Entrypoint] Config Update Signal (42) detected. Hot-restarting..."
        sleep 1
        continue
    # exit Code 0: Clean shutdown
    elif [ $APP_EXIT -eq 0 ]; then
        echo "[Entrypoint] Application shut down gracefully. Exiting script."
        break
    # Any other code: Error/Crash
    else
        echo "[Entrypoint] Application crashed (Exit $APP_EXIT). Cool-off restart in 5s..."
        sleep 5
    fi
done
