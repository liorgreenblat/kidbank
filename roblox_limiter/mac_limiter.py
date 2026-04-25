#!/usr/bin/env python3
"""
Roblox Play Time Limiter — Mac Version
Monitors Roblox, enforces a 22-minute play session, then blocks until midnight.
Run this script in the background before your child sits down to play.
Requires: pip install psutil
"""

import tkinter as tk
import psutil
import threading
import json
import time
import subprocess
from datetime import datetime, timedelta
from pathlib import Path

# ── Configuration ────────────────────────────────────────────────────────────
PLAY_TIME_MINUTES = 22
WARNING_MINUTES   = 5
CHECK_INTERVAL    = 2          # seconds between process checks
BLOCK_FILE        = Path.home() / ".roblox_block.json"

# Substring match (case-insensitive) — catches RobloxPlayer, RobloxPlayerLauncher, etc.
ROBLOX_KEYWORDS = ["roblox"]

# ── Hebrew messages (simple words for a 6-year-old) ──────────────────────────
MSG = {
    "start_title": "🎮 זמן רובלוקס!",
    "start_body":  f"!שלום גיבור\n!יש לך {PLAY_TIME_MINUTES} דקות לשחק\n⏱ הטיימר מתחיל עכשיו",

    "warn_title":  "⚠️ נשארו 5 דקות!",
    "warn_body":   "!היי\n!נשארו רק 5 דקות\n.המשחק ייסגר בקרוב — תתכונן",

    "end_title":   "🛑 הזמן נגמר!",
    "end_body":    "!כל הכבוד — שיחקת נהדר\n.הזמן שלך להיום הסתיים\n🌙 תוכל לשחק שוב מחר",

    "blocked_title": "❌ רובלוקס חסום להיום",
    "blocked_body":  ".כבר שיחקת את כל הזמן שלך היום\n😊 .תוכל לשחק שוב מחר",

    "timer_label": ":זמן שנותר",
}

# ── Colour palette ────────────────────────────────────────────────────────────
C = {
    "bg_dark":   "#0d0d1a",
    "bg_start":  "#0a1a2e",
    "bg_warn":   "#2d0000",
    "bg_end":    "#0a2200",
    "bg_block":  "#1a0000",
    "green":     "#00ff88",
    "orange":    "#ffaa00",
    "red":       "#ff4444",
    "white":     "#ffffff",
    "grey":      "#aaaaaa",
    "warn_fg":   "#ffcccc",
    "end_fg":    "#ccffcc",
    "block_fg":  "#ffaaaa",
}


class RobloxLimiter:
    def __init__(self):
        self.root         = tk.Tk()
        self.root.withdraw()           # hidden master window keeps event loop alive

        self.timer_window  = None
        self.time_var      = tk.StringVar(value=f"{PLAY_TIME_MINUTES:02d}:00")
        self.time_display  = None      # label inside timer widget

        self.timer_running = False
        self.seconds_left  = PLAY_TIME_MINUTES * 60
        self.warning_shown = False

    # ── Process helpers ───────────────────────────────────────────────────────

    def _is_roblox_running(self) -> bool:
        for proc in psutil.process_iter(["name"]):
            try:
                name = (proc.info["name"] or "").lower()
                if any(kw in name for kw in ROBLOX_KEYWORDS):
                    return True
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass
        return False

    def _kill_roblox(self):
        for proc in psutil.process_iter(["name"]):
            try:
                name = (proc.info["name"] or "").lower()
                if any(kw in name for kw in ROBLOX_KEYWORDS):
                    proc.terminate()
                    time.sleep(0.5)
                    try:
                        proc.kill()
                    except psutil.NoSuchProcess:
                        pass
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass

    # ── Block-file helpers ────────────────────────────────────────────────────

    def _is_blocked(self) -> bool:
        if not BLOCK_FILE.exists():
            return False
        try:
            data = json.loads(BLOCK_FILE.read_text())
            return datetime.now() < datetime.fromisoformat(data["block_until"])
        except Exception:
            return False

    def _set_block(self):
        midnight = (datetime.now() + timedelta(days=1)).replace(
            hour=0, minute=0, second=0, microsecond=0
        )
        BLOCK_FILE.write_text(json.dumps({"block_until": midnight.isoformat()}))

    # ── Sound ─────────────────────────────────────────────────────────────────

    def _play_sound(self, name: str = "Glass"):
        """Play a macOS system sound (non-blocking)."""
        try:
            subprocess.Popen(
                ["afplay", f"/System/Library/Sounds/{name}.aiff"],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            )
        except Exception:
            pass

    # ── UI helpers ────────────────────────────────────────────────────────────

    def _center(self, win: tk.Toplevel, w: int, h: int):
        sw = win.winfo_screenwidth()
        sh = win.winfo_screenheight()
        win.geometry(f"{w}x{h}+{(sw - w) // 2}+{(sh - h) // 2}")

    def _show_popup(self, title: str, body: str,
                    bg: str = C["bg_start"], fg: str = C["white"],
                    auto_close: int = 7000):
        """Create a centered, always-on-top popup with Hebrew text."""
        popup = tk.Toplevel(self.root)
        popup.title(title)
        popup.configure(bg=bg)
        popup.attributes("-topmost", True)
        popup.resizable(False, False)
        self._center(popup, 440, 230)

        tk.Label(
            popup, text=title,
            font=("Arial", 20, "bold"),
            fg=fg, bg=bg, justify="right",
        ).pack(pady=(22, 6), padx=24)

        tk.Label(
            popup, text=body,
            font=("Arial", 15),
            fg=fg, bg=bg,
            justify="right", wraplength=400,
        ).pack(pady=4, padx=24)

        popup.lift()
        popup.focus_force()

        if auto_close:
            popup.after(
                auto_close,
                lambda: popup.destroy() if popup.winfo_exists() else None,
            )
        return popup

    def _create_timer_widget(self):
        """Small always-on-top countdown widget in the top-right corner."""
        win = tk.Toplevel(self.root)
        win.title("⏱ טיימר")
        win.configure(bg=C["bg_dark"])
        win.attributes("-topmost", True)
        win.resizable(False, False)

        w, h = 210, 115
        sw = win.winfo_screenwidth()
        win.geometry(f"{w}x{h}+{sw - w - 20}+20")
        win.protocol("WM_DELETE_WINDOW", lambda: None)  # prevent manual close

        tk.Label(
            win, text=MSG["timer_label"],
            font=("Arial", 12), fg=C["grey"], bg=C["bg_dark"], justify="right",
        ).pack(pady=(12, 0))

        self.time_display = tk.Label(
            win, textvariable=self.time_var,
            font=("Arial", 34, "bold"), fg=C["green"], bg=C["bg_dark"],
        )
        self.time_display.pack()
        self.timer_window = win

    def _update_display(self):
        mins = self.seconds_left // 60
        secs = self.seconds_left % 60
        self.time_var.set(f"{mins:02d}:{secs:02d}")

        if self.time_display:
            if self.seconds_left <= WARNING_MINUTES * 60:
                colour = C["red"]
            elif self.seconds_left <= (WARNING_MINUTES + 5) * 60:
                colour = C["orange"]
            else:
                colour = C["green"]
            self.time_display.configure(fg=colour)

    # ── Timer logic (runs in background thread) ───────────────────────────────

    def _run_timer(self):
        while self.seconds_left > 0 and self.timer_running:
            self.root.after(0, self._update_display)

            if (self.seconds_left == WARNING_MINUTES * 60
                    and not self.warning_shown):
                self.warning_shown = True
                self._play_sound("Sosumi")
                self.root.after(
                    0,
                    lambda: self._show_popup(
                        MSG["warn_title"], MSG["warn_body"],
                        bg=C["bg_warn"], fg=C["warn_fg"], auto_close=9000,
                    ),
                )

            time.sleep(1)
            self.seconds_left -= 1

        if self.timer_running:          # reached zero (not stopped externally)
            self.root.after(0, self._on_time_up)

    def _on_time_up(self):
        self.timer_running = False
        self._play_sound("Basso")
        self._kill_roblox()
        self._set_block()
        self._close_timer_widget()
        self._show_popup(
            MSG["end_title"], MSG["end_body"],
            bg=C["bg_end"], fg=C["end_fg"], auto_close=12000,
        )

    # ── Session control ───────────────────────────────────────────────────────

    def _start_session(self):
        self.seconds_left  = PLAY_TIME_MINUTES * 60
        self.warning_shown = False
        self.timer_running = True
        self.time_var.set(f"{PLAY_TIME_MINUTES:02d}:00")

        self._create_timer_widget()
        self._play_sound("Glass")
        self._show_popup(MSG["start_title"], MSG["start_body"])

        threading.Thread(target=self._run_timer, daemon=True).start()

    def _stop_session(self):
        self.timer_running = False
        self._close_timer_widget()

    def _close_timer_widget(self):
        if self.timer_window and self.timer_window.winfo_exists():
            self.timer_window.destroy()
        self.timer_window  = None
        self.time_display  = None

    # ── Main monitor loop (runs in background thread) ─────────────────────────

    def _monitor(self):
        was_running         = False
        blocked_popup_shown = False

        while True:
            is_running = self._is_roblox_running()

            if is_running:
                if self._is_blocked():
                    self._kill_roblox()
                    if not blocked_popup_shown:
                        blocked_popup_shown = True
                        self.root.after(
                            0,
                            lambda: self._show_popup(
                                MSG["blocked_title"], MSG["blocked_body"],
                                bg=C["bg_block"], fg=C["block_fg"],
                            ),
                        )
                elif not was_running and not self.timer_running:
                    blocked_popup_shown = False
                    self.root.after(0, self._start_session)
            else:
                blocked_popup_shown = False
                if was_running and self.timer_running:
                    # Child closed the game manually — pause/stop the timer
                    self.root.after(0, self._stop_session)

            was_running = is_running
            time.sleep(CHECK_INTERVAL)

    # ── Entry point ───────────────────────────────────────────────────────────

    def run(self):
        threading.Thread(target=self._monitor, daemon=True).start()
        self.root.mainloop()


if __name__ == "__main__":
    import sys
    # When launched at login via LaunchAgent, wait for the desktop to settle
    if "--startup-delay" in sys.argv:
        time.sleep(10)
    print("Roblox Limiter is running in the background. Keep this terminal open.")
    RobloxLimiter().run()
