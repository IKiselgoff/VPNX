#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="${VPNX_APK:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
VPS="${VPNX_VPS:-root@45.146.165.85}"
ADB_PORT="${VPNX_ADB_PORT:-25566}"
CONTROL_PORT="${VPNX_CONTROL_PORT:-25567}"
PACKAGE="com.ikiselgoff.vpnx"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for command in adb ssh ssh-keygen ssh-keyscan openssl; do
    command -v "$command" >/dev/null || { echo "Missing command: $command" >&2; exit 1; }
done
test -s "$APK" || { echo "APK not found: $APK" >&2; exit 1; }
[[ "$ADB_PORT" =~ ^[0-9]+$ && "$CONTROL_PORT" =~ ^[0-9]+$ ]] || { echo "Ports must be numeric" >&2; exit 1; }
(( ADB_PORT >= 1024 && ADB_PORT <= 65535 && CONTROL_PORT >= 1024 && CONTROL_PORT <= 65535 && ADB_PORT != CONTROL_PORT )) || {
    echo "Ports must be distinct and between 1024 and 65535" >&2
    exit 1
}

adb start-server >/dev/null
devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
[[ "$(printf '%s\n' "$devices" | sed '/^$/d' | wc -l | tr -d ' ')" == 1 ]] || {
    echo "Connect and authorize exactly one Android device over USB" >&2
    exit 1
}
SERIAL="$devices"
COMMENT="vpnx-android-${SERIAL//[^A-Za-z0-9_.-]/_}"

echo "Installing VPNX on $SERIAL"
adb -s "$SERIAL" install -r "$APK"

ssh-keygen -q -t rsa -b 3072 -m PEM -N '' -C "$COMMENT" -f "$TMP/maintenance_id_rsa"
ssh-keyscan -t rsa,ecdsa,ed25519 "${VPS#*@}" > "$TMP/maintenance_known_hosts" 2>/dev/null
openssl rand -hex 32 > "$TMP/maintenance_control_token"
printf '%s\n' "$ADB_PORT" > "$TMP/maintenance_adb_port"
printf '%s\n' "$CONTROL_PORT" > "$TMP/maintenance_control_port"

for file in maintenance_id_rsa maintenance_known_hosts maintenance_control_token maintenance_adb_port maintenance_control_port; do
    adb -s "$SERIAL" push "$TMP/$file" "/data/local/tmp/$file" >/dev/null
    adb -s "$SERIAL" shell "run-as $PACKAGE cp /data/local/tmp/$file files/$file && run-as $PACKAGE chmod 600 files/$file && rm -f /data/local/tmp/$file"
done

AUTH_LINE="command=\"/bin/sleep 31536000\",restrict,port-forwarding,permitlisten=\"127.0.0.1:$ADB_PORT\",permitlisten=\"127.0.0.1:$CONTROL_PORT\" $(cat "$TMP/maintenance_id_rsa.pub")"
printf '%s\n' "$AUTH_LINE" | ssh "$VPS" "umask 077; mkdir -p ~/.ssh; touch ~/.ssh/authorized_keys; grep -Fq '$COMMENT' ~/.ssh/authorized_keys || cat >> ~/.ssh/authorized_keys"

adb -s "$SERIAL" shell "cmd deviceidle whitelist +$PACKAGE; am set-inactive $PACKAGE false; cmd activity set-standby-bucket $PACKAGE active; cmd appops set $PACKAGE RUN_IN_BACKGROUND allow; cmd appops set $PACKAGE RUN_ANY_IN_BACKGROUND allow; cmd appops set $PACKAGE START_FOREGROUND allow" || true
adb -s "$SERIAL" shell am start -n "$PACKAGE/.MainActivity"

if adb -s "$SERIAL" shell pm path moe.shizuku.privileged.api >/dev/null 2>&1; then
    adb -s "$SERIAL" shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh || true
else
    echo "Shizuku is not installed; VPNX itself is ready, but extended shell diagnostics require Shizuku."
fi

echo "Enabling ADB TCP on port 5555; the USB ADB connection may briefly restart."
adb -s "$SERIAL" tcpip 5555 || true
echo "Provisioned: VPS ADB 127.0.0.1:$ADB_PORT, control 127.0.0.1:$CONTROL_PORT"
echo "On the phone, confirm the Android VPN dialog once."
