#!/bin/bash
# Stop all LIDA Analytics servers

echo "🛑 Stopping LIDA Multi-Dataset Analytics Platform..."

# Change to project root
cd "$(dirname "$0")/.."

# Function to stop server by PID file
stop_server() {
    local pid_file=$1
    local server_name=$2
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo "🛑 Stopping $server_name (PID: $pid)..."
            kill $pid
            rm "$pid_file"
        else
            echo "⚠️ $server_name was not running"
            rm "$pid_file"
        fi
    else
        echo "⚠️ No PID file found for $server_name"
    fi
}

# Stop all servers
stop_server ".multi-dataset.pid" "Multi-Dataset Server"
stop_server ".retail-analytics.pid" "Retail Analytics Server"
stop_server ".original-lida.pid" "Original LIDA Server"

# Also kill any remaining Python servers on our ports
echo "🧹 Cleaning up any remaining processes..."
pkill -f "lida-multi-dataset-server.py" 2>/dev/null || true
pkill -f "retail-analytics-server.py" 2>/dev/null || true
pkill -f "lida-dashboard-server.py" 2>/dev/null || true

echo "✅ All servers stopped successfully!"