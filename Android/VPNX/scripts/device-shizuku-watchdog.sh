#!/system/bin/sh

STATE_DIR="/data/local/tmp/vpnx"
SCRIPT="$STATE_DIR/shizuku-watchdog.sh"
LOG_FILE="$STATE_DIR/shizuku-watchdog.log"
INSTANCE="${1:-a}"
PEER="b"
[ "$INSTANCE" = "b" ] && PEER="a"
PID_FILE="$STATE_DIR/shizuku-watchdog-$INSTANCE.pid"
PEER_PID_FILE="$STATE_DIR/shizuku-watchdog-$PEER.pid"
LOCK="$STATE_DIR/shizuku-restart.lock"

mkdir -p "$STATE_DIR"

is_alive() {
  pid="$1"
  role="$2"
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null &&
    tr '\000' ' ' < "/proc/$pid/cmdline" 2>/dev/null | grep -Fq "$SCRIPT $role"
}

previous="$(cat "$PID_FILE" 2>/dev/null)"
is_alive "$previous" "$INSTANCE" && exit 0
echo $$ > "$PID_FILE"

log_event() {
  [ -f "$LOG_FILE" ] && [ "$(wc -c < "$LOG_FILE")" -gt 65536 ] &&
    tail -c 32768 "$LOG_FILE" > "$LOG_FILE.tmp" && mv "$LOG_FILE.tmp" "$LOG_FILE"
  echo "$(date '+%Y-%m-%dT%H:%M:%S%z') instance=$INSTANCE $*" >> "$LOG_FILE"
}

cleanup() {
  [ "$(cat "$PID_FILE" 2>/dev/null)" = "$$" ] && rm -f "$PID_FILE"
}
trap cleanup EXIT
trap 'cleanup; exit 0' INT TERM
log_event "watchdog_started pid=$$"

while true; do
  peer_pid="$(cat "$PEER_PID_FILE" 2>/dev/null)"
  if ! is_alive "$peer_pid" "$PEER"; then
    rm -f "$PEER_PID_FILE"
    nohup "$SCRIPT" "$PEER" >/dev/null 2>&1 </dev/null &
    log_event "peer_restarted role=$PEER"
  fi

  if ! pidof shizuku_server >/dev/null 2>&1; then
    owner="$(cat "$LOCK/pid" 2>/dev/null)"
    [ -n "$owner" ] && ! kill -0 "$owner" 2>/dev/null && rm -rf "$LOCK"
    if mkdir "$LOCK" 2>/dev/null; then
      echo $$ > "$LOCK/pid"
      apk="$(pm path moe.shizuku.privileged.api 2>/dev/null | sed -n 's/^package://p' | head -n 1)"
      starter="${apk%/base.apk}/lib/arm64/libshizuku.so"
      if [ -n "$apk" ] && [ -x "$starter" ]; then
        "$starter" >/dev/null 2>&1 </dev/null &
        sleep 2
        pidof shizuku_server >/dev/null 2>&1 && log_event "shizuku_restarted" ||
          log_event "shizuku_restart_failed"
      else
        log_event "shizuku_starter_missing"
      fi
      rm -rf "$LOCK"
    fi
  fi
  sleep 5
done
