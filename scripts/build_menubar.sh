#!/usr/bin/env bash
set -euo pipefail
BASE="$(cd "$(dirname "$0")"/.. && pwd)"
PROJ="$BASE/Menubar/VPNXMenu"
cd "$PROJ"
./build.sh
echo "Built app at: $PROJ/VPNXMenu.app"
