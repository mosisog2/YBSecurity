# 🤖 LIDA Multi-Dataset Analytics Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Python 3.8+](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![LIDA](https://img.shields.io/badge/LIDA-0.0.14-green.svg)](https://github.com/microsoft/lida)

A comprehensive analytics platform that integrates Microsoft LIDA's LLM-powered automatic visualization generation with multi-domain datasets. Transform natural language queries into intelligent visualizations across e-commerce, financial, IoT, healthcare, and supply chain data.

## 🌟 Features

### 🤖 **AI-Powered Visualization Generation**
- **Natural Language Queries**: Ask questions in plain English
- **Intelligent Chart Selection**: Automatically chooses optimal visualization types
- **Domain-Aware Analysis**: Adapts to different data domains and contexts
- **Microsoft LIDA Integration**: LLM-powered automatic visualization generation

### 📊 **Multi-Dataset Support**
- **E-commerce Analytics**: Customer demographics, sales trends, regional analysis
- **Financial Performance**: Department budgets, quarterly analysis, ROI tracking
- **IoT Sensor Data**: Real-time monitoring, anomaly detection, correlation analysis
- **Healthcare Analytics**: Treatment costs, patient outcomes, satisfaction metrics
- **Supply Chain Management**: Supplier performance, quality metrics, delivery tracking
- **Retail Sales Data**: Store performance, product categories, economic indicators

### 🎨 **Interactive Dashboards**
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Real-time Updates**: Dynamic data loading and visualization
- **Professional UI**: Clean, modern interface with gradient backgrounds
- **Chart.js Integration**: High-quality, interactive visualizations

### 🔧 **Multiple Server Architectures**
- **Multi-Dataset Server**: 6 different datasets with domain-specific insights
- **Retail Analytics Server**: Focused on sales data with KPI monitoring
- **Original LIDA Server**: Full LIDA framework integration

## 🚀 Quick Start

### Prerequisites
- Python 3.8 or higher
- Git
- OpenAI API key (optional, for full LIDA functionality)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd YBSecurity
   ```

2. **Run the setup script**
   ```bash
   chmod +x setup.sh
   ./setup.sh
   ```

3. **Configure environment (optional)**
   ```bash
   cp .env.example .env
   # Edit .env with your OpenAI API key
   ```

4. **Start the servers**
   ```bash
   ./scripts/start_servers.sh
   ```

5. **Open your browser**
   - Multi-Dataset Dashboard: http://localhost:8084
   - Retail Analytics: http://localhost:8082
   - Original LIDA: http://localhost:8081

## 📁 Project Structure

```
YBSecurity/
├── 📂 servers/                 # Backend servers
│   ├── lida-multi-dataset-server.py
│   ├── retail-analytics-server.py
│   └── lida-dashboard-server.py
├── 📂 dashboards/              # Frontend interfaces
│   ├── lida-multi-dataset-dashboard.html
│   ├── retail-analytics-dashboard.html
│   └── bulletproof-dashboard.html
├── 📂 datasets/                # Data files
│   ├── amazon_sales_dataset.csv
│   ├── sample_data_ecommerce.csv
│   ├── sample_data_financial.csv
│   └── ...
├── 📂 scripts/                 # Utility scripts
│   ├── start_servers.sh
│   ├── stop_servers.sh
│   └── generate_sample_data.py
├── 📂 config/                  # Configuration files
│   └── settings.py
├── 📂 docs/                    # Documentation
├── 📂 logs/                    # Server logs
├── 📂 lida_env/               # Virtual environment
├── requirements.txt            # Python dependencies
├── setup.sh                   # Setup script
└── README.md                  # This file
```

## 💡 Usage Examples

### Natural Language Queries

**E-commerce Dataset:**
- "Show revenue trends by category over time"
- "Which region has the highest average order value?"
- "Compare premium vs regular customers"

**Financial Dataset:**
- "Compare budget vs actual spending by department"
- "Show revenue growth across quarters"
- "Which department has the best ROI?"

**IoT Sensors:**
- "Show temperature variations across all locations"
- "Create an alert status dashboard for all sensors"
- "Show correlation between humidity and CO2 levels"

**Healthcare:**
- "Show treatment costs by condition and age group"
- "Which conditions have the highest readmission rates?"
- "Compare satisfaction scores across insurance types"

## 🛠️ Development

### Adding New Datasets

1. Place your CSV file in the `datasets/` directory
2. Add dataset configuration to `datasets/lida_query_examples.json`
3. Update the server to include your dataset
4. Add example queries for your domain

### Server Configuration

Modify `config/settings.py` to adjust:
- Server ports
- Dataset limits
- LIDA model parameters
- Visualization defaults
- Color schemes

## 🔧 Architecture

### Backend Services
- **Python HTTP Servers**: Custom lightweight servers
- **LIDA Integration**: Microsoft LIDA framework for AI visualization
- **Data Processing**: Pandas, NumPy for data manipulation
- **Chart Generation**: Chart.js integration for interactive visualizations

### Frontend
- **Modern HTML5/CSS3**: Responsive design with CSS Grid/Flexbox
- **Vanilla JavaScript**: No heavy frameworks, optimized performance
- **Chart.js**: Professional visualization library
- **Progressive Enhancement**: Works without JavaScript

## 📊 Included Datasets

- **E-commerce** (4,283 records): Sales data with customer demographics
- **Financial** (25 records): Corporate performance by department
- **IoT Sensors** (10,815 records): Multi-location sensor readings
- **Healthcare** (1,000 records): Patient treatment and outcomes
- **Supply Chain** (240 records): Supplier performance metrics
- **Retail Sales** (374,247 records): Store sales with economic indicators

## 🙏 Acknowledgments

- **Microsoft LIDA**: LLM-powered automatic visualization generation
- **Chart.js**: Beautiful, responsive charts
- **OpenAI**: GPT models for natural language processing
- **Pandas**: Data manipulation and analysis

---

**Made with ❤️ for data scientists, analysts, and visualization enthusiasts**
