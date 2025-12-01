#!/usr/bin/env bash
set -euo pipefail
BASE="$(cd "$(dirname "$0")"/.. && pwd)"
NAME="vpnx-macos-dist"
TMP="$(mktemp -d)"
mkdir -p "$TMP/$NAME"
cp -R "$BASE/vpnx" "$TMP/$NAME/"
cp -R "$BASE/Menubar" "$TMP/$NAME/"
cp -R "$BASE/scripts" "$TMP/$NAME/"
cp "$BASE/README.md" "$TMP/$NAME/"
( cd "$TMP" && zip -r "$BASE/${NAME}.zip" "$NAME" >/dev/null )
echo "Packed: $BASE/${NAME}.zip"
