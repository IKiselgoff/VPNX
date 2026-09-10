#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
LIBXRAY_VERSION="v26.7.28"
XRAY_VERSION="v26.3.27"

mkdir -p "$ROOT/app/libs" "$ROOT/app/src/main/assets"

if [[ -n "${VPNX_BOOTSTRAP_KEY_FILE:-}" ]]; then
  install -m 600 "$VPNX_BOOTSTRAP_KEY_FILE" "$ROOT/app/src/main/assets/maintenance-bootstrap.pem"
fi

curl -LfsS --max-time 180 \
  "https://github.com/XTLS/libXray/releases/download/${LIBXRAY_VERSION}/libxray-android.zip" \
  -o "$TMP/libxray.zip"
unzip -q "$TMP/libxray.zip" -d "$TMP/libxray"
cp "$TMP/libxray/libxray-android/libXray.aar" "$ROOT/app/libs/libXray.aar"

curl -LfsS --max-time 180 \
  "https://github.com/XTLS/Xray-core/releases/download/${XRAY_VERSION}/Xray-android-arm64-v8a.zip" \
  -o "$TMP/xray.zip"
unzip -q "$TMP/xray.zip" geoip.dat geosite.dat -d "$TMP/xray"
cp "$TMP/xray/geoip.dat" "$ROOT/app/src/main/assets/geoip.dat"
cp "$TMP/xray/geosite.dat" "$ROOT/app/src/main/assets/geosite.dat"

curl -LfsS --max-time 60 \
  -H 'Accept: application/json' \
  'https://vpnx-bird.45-146-165-85.nip.io/95c5aacefee8bcb1b2547b13b6fa3320ce948fd07a5dd31f.json' \
  -o "$ROOT/app/src/main/assets/bird-bootstrap.json"

echo "VPNX Android runtime downloaded."
