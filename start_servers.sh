#!/bin/bash

# Start all LIDA Analytics Platform servers
echo "🚀 Starting LIDA Analytics Platform servers..."

# Check if virtual environment exists
if [ ! -d "lida_env" ]; then
    echo "❌ Virtual environment not found. Please run ./setup.sh first"
    exit 1
fi

# Function to start a server in background
start_server() {
    local server_file=$1
    local port=$2
    local name=$3
    
    if [ -f "$server_file" ]; then
        echo "🔄 Starting $name on port $port..."
        cd "$(dirname "$server_file")"
        source ../lida_env/bin/activate
        nohup python "$(basename "$server_file")" > "../logs/${name}.log" 2>&1 &
        echo $! > "../logs/${name}.pid"
        cd - > /dev/null
        echo "✅ $name started (PID: $(cat "logs/${name}.pid"))"
    else
        echo "❌ Server file not found: $server_file"
    fi
}

# Create logs directory
mkdir -p logs

# Start all servers
start_server "backend/app.py" 8081 "original-server"
start_server "servers/retail-analytics-server.py" 8082 "retail-analytics"
start_server "servers/lida-multi-dataset-server.py" 8084 "multi-dataset"

# Wait a moment for servers to start
sleep 3

echo ""
echo "🎉 All servers started!"
echo ""
echo "📊 Available dashboards:"
echo "  • Original Dashboard:     http://localhost:8081"
echo "  • Retail Analytics:       http://localhost:8082"
echo "  • Multi-Dataset LIDA:     http://localhost:8084"
echo ""
echo "📋 Server logs are in the 'logs/' directory"
echo "🛑 To stop all servers, run: ./scripts/stop_servers.sh"
echo ""

# Show running processes
echo "🔍 Running server processes:"
ps aux | grep -E "(app.py|retail-analytics|multi-dataset)" | grep -v grep || echo "No processes found"