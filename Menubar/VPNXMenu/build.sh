#!/usr/bin/env bash
set -euo pipefail
APP="VPNXMenu"
SRC_DIR="$(cd "$(dirname "$0")" && pwd)/Sources"
BUILD_DIR="$(mktemp -d)"
APP_DIR="$PWD/${APP}.app"
BIN="${BUILD_DIR}/${APP}"

echo "Compiling..."
swiftc -O -sdk "$(xcrun --show-sdk-path --sdk macosx)" -target x86_64-apple-macosx10.15 \
  -framework Cocoa \
  -o "$BIN" "$SRC_DIR/main.swift" "$SRC_DIR/AppDelegate.swift"

echo "Packaging .app..."
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Resources"
cp Info.plist "$APP_DIR/Contents/Info.plist"
cp "$BIN" "$APP_DIR/Contents/MacOS/$APP"
echo "Done: $APP_DIR"
echo
echo "Tip: Move ${APP}.app to /Applications, then right-click → Open (Gatekeeper prompt)."
