#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
LIBXRAY_VERSION="v26.7.28"
XRAY_VERSION="v26.3.27"

mkdir -p "$ROOT/app/libs" "$ROOT/app/src/main/assets"

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
  -H 'X-HWID: 1233d4ebec70a307' \
  -H 'X-Device-Locale: ru' \
  -H 'Accept-Language: ru' \
  -H 'X-Ver-OS: 14' \
  -H 'X-Device-model: SM-X205' \
  -H 'User-Agent: Happ/4.7.1/android/2604040151590' \
  -H 'X-Device-OS: Android' \
  -H 'X-App-Version: 4.7.1' \
  'https://moonshard.org/_DDgzQApDZfjQ2JA' \
  -o "$ROOT/app/src/main/assets/bird-bootstrap.json"

echo "VPNX Android runtime downloaded."
