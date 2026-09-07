#!/system/bin/sh

PACKAGE="com.ikiselgoff.vpnx"
STATE_DIR="/data/local/tmp/vpnx"
SCRIPT="$STATE_DIR/maintenance-watchdog.sh"
LOG_FILE="$STATE_DIR/maintenance-watchdog.log"
INSTANCE="${1:-a}"
PEER="b"
[ "$INSTANCE" = "b" ] && PEER="a"
PID_FILE="$STATE_DIR/maintenance-watchdog-$INSTANCE.pid"
PEER_PID_FILE="$STATE_DIR/maintenance-watchdog-$PEER.pid"
CHECK_INTERVAL=10
MAX_LOG_BYTES=65536

mkdir -p "$STATE_DIR"

is_watchdog_alive() {
  candidate="$1"
  role="$2"
  [ -n "$candidate" ] || return 1
  kill -0 "$candidate" 2>/dev/null || return 1
  tr '\000' ' ' < "/proc/$candidate/cmdline" 2>/dev/null | grep -Fq "$SCRIPT $role"
}

if [ -f "$PID_FILE" ]; then
  previous_pid="$(cat "$PID_FILE" 2>/dev/null)"
  is_watchdog_alive "$previous_pid" "$INSTANCE" && exit 0
fi
echo $$ > "$PID_FILE"

log_event() {
  if [ -f "$LOG_FILE" ] && [ "$(wc -c < "$LOG_FILE")" -gt "$MAX_LOG_BYTES" ]; then
    tail -c 32768 "$LOG_FILE" > "$LOG_FILE.tmp" && mv "$LOG_FILE.tmp" "$LOG_FILE"
  fi
  echo "$(date '+%Y-%m-%dT%H:%M:%S%z') instance=$INSTANCE $*" >> "$LOG_FILE"
}

cleanup() {
  current_pid="$(cat "$PID_FILE" 2>/dev/null)"
  [ "$current_pid" = "$$" ] && rm -f "$PID_FILE"
}
trap cleanup EXIT
trap 'cleanup; exit 0' INT TERM
log_event "watchdog_started pid=$$"

while true; do
  peer_pid="$(cat "$PEER_PID_FILE" 2>/dev/null)"
  if ! is_watchdog_alive "$peer_pid" "$PEER"; then
    rm -f "$PEER_PID_FILE"
    nohup "$SCRIPT" "$PEER" >/dev/null 2>&1 </dev/null &
    log_event "peer_restarted role=$PEER"
  fi

  if ! pidof "$PACKAGE" >/dev/null 2>&1; then
    cmd jobscheduler run -f "$PACKAGE" 8621 >/dev/null 2>&1 || true
    sleep 3
    if ! pidof "$PACKAGE" >/dev/null 2>&1; then
      monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
    fi
    log_event "vpnx_recovery_requested"
  fi

  sleep "$CHECK_INTERVAL"
done
