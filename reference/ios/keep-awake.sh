#!/bin/zsh
# Posts tv.quven.glass.keepawake to the device every 60 seconds; the reference app restarts its idle countdown on it.
# Usage: keep-awake.sh <device identifier>
device=$1
while true; do
  xcrun devicectl device notification post --device "$device" --name tv.quven.glass.keepawake --quiet >/dev/null 2>&1 \
    && echo "$(date +%H:%M:%S) keep-awake sent" || echo "$(date +%H:%M:%S) keep-awake failed"
  sleep 60
done
