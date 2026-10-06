#!/usr/bin/env bash
# Fails if any AndroidManifest.xml under DIR (default .) declares a network permission.
# Usage: scripts/check-no-network-permission.sh [DIR]
set -u
dir="${1:-.}"
pattern='android\.permission\.(INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE|CHANGE_NETWORK_STATE|CHANGE_WIFI_STATE)'

if grep -rEn --include=AndroidManifest.xml --exclude-dir=.git "$pattern" "$dir"; then
  echo "FAIL: network permission found (see ADR 0001)"
  exit 1
fi
echo "OK: no network permissions in $dir"
