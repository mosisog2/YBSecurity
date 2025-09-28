#!/bin/bash
# Start all LIDA Analytics servers

echo "🚀 Starting LIDA Multi-Dataset Analytics Platform..."

# Activate virtual environment
source lida_env/bin/activate

# Check if virtual environment is activated
if [ -z "$VIRTUAL_ENV" ]; then
    echo "❌ Virtual environment not activated. Please run setup.sh first."
    exit 1
fi

# Change to project root
cd "$(dirname "$0")/.."

echo "📊 Starting servers..."

# Start Multi-Dataset Server (Port 8084)
echo "🤖 Starting Multi-Dataset LIDA Server on port 8084..."
nohup python servers/lida-multi-dataset-server.py &
MULTI_PID=$!

# Start Retail Analytics Server (Port 8082)
echo "🛒 Starting Retail Analytics Server on port 8082..."
nohup python servers/retail-analytics-server.py &
RETAIL_PID=$!

# Start Original Backend Server (Port 8081)
if [ -f "backend/app.py" ]; then
    echo "🔧 Starting Original Backend Server on port 8081..."
    nohup python backend/app.py &
    ORIGINAL_PID=$!
else
    echo "⚠️ Original backend server not found, skipping..."
    ORIGINAL_PID=""
fi

# Save PIDs
echo $MULTI_PID > .multi-dataset.pid
echo $RETAIL_PID > .retail-analytics.pid
if [ -n "$ORIGINAL_PID" ]; then
    echo $ORIGINAL_PID > .original-lida.pid
fi

sleep 3

echo "✅ All servers started successfully!"
echo ""
echo "🌐 Available Dashboards:"
echo "   🤖 Multi-Dataset LIDA: http://localhost:8084/lida-multi-dataset-dashboard.html"
echo "   🛒 Retail Analytics:   http://localhost:8082/retail-analytics-dashboard.html"
echo "   🔧 Original LIDA:      http://localhost:8081/bulletproof-dashboard.html"
echo ""
echo "📋 Server PIDs saved to .*.pid files"
echo "📝 Logs available in logs/ directory"
echo ""
echo "To stop all servers, run: ./scripts/stop_servers.sh"