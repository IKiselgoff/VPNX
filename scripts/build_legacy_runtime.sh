#!/bin/bash
set -euo pipefail
BASE="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT="${1:?Provide output directory outside Git}"
mkdir -p "$OUTPUT"
URL="https://github.com/XTLS/Xray-core/releases/download/v26.3.27/Xray-macos-64.zip"
curl -fLsS --max-time 120 "$URL" -o "$OUTPUT/xray.zip"
printf '%s  %s\n' f5b0471d3459eff1b82e48af0aeac186abcc3298210070afbbbd8437a4e8b203 "$OUTPUT/xray.zip" | shasum -a 256 -c -
unzip -oq "$OUTPUT/xray.zip" -d "$OUTPUT"
xcrun clang -target x86_64-apple-macos10.15 -dynamiclib -framework CoreFoundation -framework Security \
  -Wl,-reexport_framework,Security -Wl,-install_name,@executable_path/libVPNXSecurity.dylib \
  "$BASE/vpnx/compat/security_compat.c" -o "$OUTPUT/libVPNXSecurity.dylib"
install_name_tool -change /System/Library/Frameworks/Security.framework/Versions/A/Security \
  @executable_path/libVPNXSecurity.dylib "$OUTPUT/xray"
codesign --force --sign - "$OUTPUT/xray"
shasum -a 256 "$OUTPUT/xray" "$OUTPUT/libVPNXSecurity.dylib" > "$OUTPUT/runtime.sha256"
