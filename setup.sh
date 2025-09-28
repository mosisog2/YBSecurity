#!/bin/bash
# LIDA Multi-Dataset Analytics Platform Setup Script

echo "🚀 Setting up LIDA Multi-Dataset Analytics Platform..."

# Check if Python 3.8+ is installed
python_version=$(python3 --version 2>&1 | grep -oP '\d+\.\d+' | head -1)
if [ -z "$python_version" ]; then
    echo "❌ Python 3 is not installed. Please install Python 3.8 or higher."
    exit 1
fi

echo "✅ Python $python_version detected"

# Create virtual environment
echo "📦 Creating virtual environment..."
python3 -m venv lida_env

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source lida_env/bin/activate

# Upgrade pip
echo "⬆️ Upgrading pip..."
pip install --upgrade pip

# Install requirements
echo "📥 Installing requirements..."
pip install -r requirements.txt

# Copy environment file
echo "⚙️ Setting up environment configuration..."
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "📝 Please edit .env file with your API keys"
fi

# Make scripts executable
echo "🔧 Making scripts executable..."
chmod +x scripts/*.py
chmod +x scripts/start_servers.sh

echo "✅ Setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Edit .env file with your OpenAI API key (optional but recommended)"
echo "2. Run: ./scripts/start_servers.sh"
echo "3. Open your browser to:"
echo "   - Multi-Dataset Dashboard: http://localhost:8084"
echo "   - Retail Analytics: http://localhost:8082"
echo "   - Original LIDA: http://localhost:8081"
echo ""
echo "📚 For more information, see docs/README.md"