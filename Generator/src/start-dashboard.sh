#!/bin/bash
# Dashboard Server Launcher
# This script starts the ML Dashboard web server

echo "🚀 Starting ML Dashboard Server..."
cd "$(dirname "$0")"
python3 dashboard-server.py ${1:-8080}