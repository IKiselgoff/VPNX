#!/usr/bin/env bash
set -euo pipefail
BASE="$(cd "$(dirname "$0")"/.. && pwd)"

mkdir -p "$HOME/.vpnx/bin" "$HOME/.vpnx/nodes"
cp "$BASE/vpnx/bin/vpnx" "$HOME/.vpnx/bin/vpnx"
chmod +x "$HOME/.vpnx/bin/vpnx"

if ! grep -q 'export PATH="$HOME/.vpnx/bin:$PATH"' "$HOME/.zshrc" 2>/dev/null; then
  echo 'export PATH="$HOME/.vpnx/bin:$PATH"' >> "$HOME/.zshrc"
fi

[ -f "$HOME/.vpnx/config.base.json" ] || cp "$BASE/vpnx/base/config.base.json" "$HOME/.vpnx/config.base.json"

echo "-> Installing/Checking Xray..."
"$HOME/.vpnx/bin/vpnx" install || true

cat <<EOF
Done. Open a new terminal (or run: source ~/.zshrc)

Examples:
  vpnx import NL-2025 'vless://...' --default
  vpnx start
  vpnx stop
  vpnx status
EOF
