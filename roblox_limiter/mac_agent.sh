#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# Roblox Limiter — macOS Launch Agent installer
#
# Usage:
#   bash mac_agent.sh install    ← register + start now
#   bash mac_agent.sh uninstall  ← stop + remove
#   bash mac_agent.sh status     ← show whether it's running
#
# The agent starts automatically at every login and restarts itself if it
# crashes (KeepAlive = true).
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

LABEL="com.family.roblox-limiter"
PLIST_DIR="$HOME/Library/LaunchAgents"
PLIST_FILE="$PLIST_DIR/$LABEL.plist"

# Resolve absolute paths at install time so the plist is self-contained
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_PATH="$SCRIPT_DIR/mac_limiter.py"
PYTHON_PATH="$(which python3)"
LOG_OUT="/tmp/roblox-limiter.log"
LOG_ERR="/tmp/roblox-limiter-error.log"

# ── helpers ───────────────────────────────────────────────────────────────────

check_python() {
    if ! command -v python3 &>/dev/null; then
        echo "ERROR: python3 not found. Install Python 3 first."
        exit 1
    fi
    if ! python3 -c "import psutil" &>/dev/null; then
        echo "psutil not installed — installing now..."
        pip3 install psutil
    fi
}

write_plist() {
    mkdir -p "$PLIST_DIR"
    cat > "$PLIST_FILE" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Identity -->
    <key>Label</key>
    <string>${LABEL}</string>

    <!-- Command to run -->
    <key>ProgramArguments</key>
    <array>
        <string>${PYTHON_PATH}</string>
        <string>${SCRIPT_PATH}</string>
        <string>--startup-delay</string>
    </array>

    <!-- Start at login -->
    <key>RunAtLoad</key>
    <true/>

    <!-- Restart automatically if it exits unexpectedly -->
    <key>KeepAlive</key>
    <true/>

    <!-- Logs -->
    <key>StandardOutPath</key>
    <string>${LOG_OUT}</string>
    <key>StandardErrorPath</key>
    <string>${LOG_ERR}</string>

    <!-- Nice low priority so it doesn't slow down login -->
    <key>ProcessType</key>
    <string>Background</string>
</dict>
</plist>
PLIST
    echo "Plist written to: $PLIST_FILE"
}

# ── commands ──────────────────────────────────────────────────────────────────

install() {
    check_python
    write_plist

    # Unload any old version first (ignore errors if not loaded)
    launchctl unload -w "$PLIST_FILE" 2>/dev/null || true
    launchctl load   -w "$PLIST_FILE"

    echo ""
    echo "✅  Roblox Limiter is now installed and running."
    echo "    It will start automatically at every login."
    echo "    Logs: $LOG_OUT"
}

uninstall() {
    if [ -f "$PLIST_FILE" ]; then
        launchctl unload -w "$PLIST_FILE" 2>/dev/null || true
        rm -f "$PLIST_FILE"
        echo "✅  Roblox Limiter uninstalled."
    else
        echo "Nothing to uninstall — plist not found."
    fi
}

status() {
    if launchctl list | grep -q "$LABEL"; then
        echo "✅  Running  ($LABEL)"
    else
        echo "❌  Not running  ($LABEL)"
    fi
    if [ -f "$PLIST_FILE" ]; then
        echo "   Plist : $PLIST_FILE"
    fi
    if [ -f "$LOG_ERR" ] && [ -s "$LOG_ERR" ]; then
        echo ""
        echo "── Last error log ──────────────────────────────────"
        tail -20 "$LOG_ERR"
    fi
}

# ── dispatch ──────────────────────────────────────────────────────────────────

case "${1:-install}" in
    install)   install   ;;
    uninstall) uninstall ;;
    status)    status    ;;
    *)
        echo "Usage: $0 [install|uninstall|status]"
        exit 1
        ;;
esac
