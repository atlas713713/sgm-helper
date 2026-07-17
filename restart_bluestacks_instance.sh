#!/bin/sh
set -eu

instance=${1:?Usage: $0 INSTANCE ADB_TARGET}
target=${2:?Usage: $0 INSTANCE ADB_TARGET}
player="/Applications/BlueStacks.app/Contents/MacOS/BlueStacks"
adb="/Applications/BlueStacks.app/Contents/MacOS/hd-adb"

echo "Stopping $instance..."
pids=$(pgrep -f "^$player --instance $instance( |$)" || true)
if [ -n "$pids" ]; then
  kill $pids
  for _ in $(seq 1 30); do
    pgrep -f "^$player --instance $instance( |$)" >/dev/null || break
    sleep 1
  done
  pids=$(pgrep -f "^$player --instance $instance( |$)" || true)
  [ -z "$pids" ] || kill -9 $pids
fi

echo "Starting $instance..."
open -na /Applications/BlueStacks.app --args --instance "$instance"

echo "Waiting for Android on $target..."
for _ in $(seq 1 180); do
  case "$target" in
    *:*) "$adb" connect "$target" >/dev/null 2>&1 || true ;;
  esac
  if [ "$("$adb" -s "$target" get-state 2>/dev/null || true)" = device ] &&
     [ "$("$adb" -s "$target" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then
    break
  fi
  sleep 1
done
[ "$("$adb" -s "$target" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] || {
  echo "Android did not finish booting on $target" >&2
  exit 1
}

echo "Starting SGM Helper and game..."
"$adb" -s "$target" shell input keyevent KEYCODE_WAKEUP
"$adb" -s "$target" shell wm dismiss-keyguard >/dev/null 2>&1 || true
"$adb" -s "$target" shell am start -W -n com.local.sgmhelper/.MainActivity >/dev/null
sleep 2
"$adb" -s "$target" shell am start -W -n hk.phx.khm.cs/com.unity3d.player.UnityPlayerActivity >/dev/null
sleep 5

helper_pid=$("$adb" -s "$target" shell pidof com.local.sgmhelper | tr -d '\r')
game_pid=$("$adb" -s "$target" shell pidof hk.phx.khm.cs | tr -d '\r')
[ -n "$helper_pid" ] && [ -n "$game_pid" ] || {
  echo "App launch verification failed (helper=$helper_pid game=$game_pid)" >&2
  exit 1
}
echo "Ready: $instance ($target), helper PID $helper_pid, game PID $game_pid"
