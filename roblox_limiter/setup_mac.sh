#!/bin/bash
# One-time setup for the Mac version.
# Run once: bash setup_mac.sh

set -e

echo "Installing Python dependency..."
pip3 install psutil

echo ""
echo "Done! To start the limiter, run:"
echo "  python3 mac_limiter.py"
echo ""
echo "To have it start automatically at login:"
echo "  1. Open System Settings → General → Login Items"
echo "  2. Click + and add this script (or wrap it in a .command file)"
