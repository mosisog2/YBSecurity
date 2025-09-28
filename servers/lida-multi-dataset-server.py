#!/home/student/YBSecurity/lida_env/bin/python3

import http.server
import socketserver
import json
import os
import sys
from urllib.parse import urlparse, parse_qs
from datetime import datetime
import pandas as pd
import numpy as np

class LidaMultiDatasetServer(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        # Load multiple datasets
        self.load_datasets()
        self.init_lida()
        super().__init__(*args, **kwargs)
    
    def load_datasets(self):
        """Load all sample datasets"""
        self.datasets = {}
        dataset_files = {
            'ecommerce': '../datasets/sample_data_ecommerce.csv',
            'financial': '../datasets/sample_data_financial.csv', 
            'iot_sensors': '../datasets/sample_data_iot_sensors.csv',
            'healthcare': '../datasets/sample_data_healthcare.csv',
            'supply_chain': '../datasets/sample_data_supply_chain.csv',
            'retail': '../datasets/amazon_sales_dataset.csv'
        }
        
        for name, path in dataset_files.items():
            try:
                if os.path.exists(path):
                    df = pd.read_csv(path)
                    self.datasets[name] = df
                    print(f"✅ Loaded {name} dataset: {len(df)} records")
                else:
                    print(f"⚠️ Dataset not found: {path}")
            except Exception as e:
                print(f"❌ Error loading {name}: {e}")
        
        # Load query examples
        try:
            with open('../datasets/lida_query_examples.json', 'r') as f:
                self.query_examples = json.load(f)
        except:
            self.query_examples = {}
        
        print(f"📊 Loaded {len(self.datasets)} datasets")
    
    def init_lida(self):
        """Initialize LIDA Manager"""
        self.lida_manager = None
        try:
            # Try multiple import approaches
            import sys
            lida_path = "/home/student/YBSecurity/lida_env/lib/python3.12/site-packages"
            if lida_path not in sys.path:
                sys.path.insert(0, lida_path)
            
            import lida
            print(f"✅ LIDA imported from: {lida.__file__}")
            
            from lida import Manager, TextGenerationConfig, SummarizationConfig
            
            # Initialize with text generation config
            text_gen_config = TextGenerationConfig(n=1, temperature=0.2, model="gpt-3.5-turbo", use_cache=True)
            summarization_config = SummarizationConfig(n=1, temperature=0.2, model="gpt-3.5-turbo", use_cache=True)
            
            self.lida_manager = Manager(text_gen=text_gen_config, summarization=summarization_config)
            print("🤖 LIDA Manager initialized successfully")
            
        except ImportError as e:
            print(f"⚠️ LIDA import failed: {e}")
            print("⚠️ Running in demo mode with intelligent visualization generation")
        except Exception as e:
            print(f"⚠️ LIDA initialization error: {e}")
            print("⚠️ Running in demo mode with intelligent visualization generation")

    def do_GET(self):
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        query_params = parse_qs(parsed_path.query)
        
        # Handle API endpoints
        if path.startswith('/api/'):
            self.handle_api_request(path, query_params)
            return
        
        # Serve static files
        if path == '/' or path == '/index.html':
            path = '/lida-multi-dataset-dashboard.html'
        
        # Try to serve the file from dashboards directory
        try:
            if path.startswith('/'):
                file_path = f"../dashboards{path}"
                if not os.path.exists(file_path):
                    file_path = path[1:]  # Remove leading slash and try current directory
            else:
                file_path = f"../dashboards/{path}"
                
            if os.path.exists(file_path):
                with open(file_path, 'rb') as f:
                    content = f.read()
                
                # Determine content type
                if file_path.endswith('.html'):
                    content_type = 'text/html'
                elif file_path.endswith('.js'):
                    content_type = 'application/javascript'
                elif file_path.endswith('.css'):
                    content_type = 'text/css'
                elif file_path.endswith('.json'):
                    content_type = 'application/json'
                else:
                    content_type = 'application/octet-stream'
                
                self.send_response(200)
                self.send_header('Content-Type', content_type)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(content)
            else:
                self.send_error(404, f"File not found: {file_path}")
                
        except Exception as e:
            print(f"Error serving file: {e}")
            self.send_error(500, str(e))
    
    def handle_api_request(self, path, query_params):
        """Handle API requests"""
        try:
            if path == '/api/datasets':
                self.send_datasets_list()
            elif path == '/api/dataset-info':
                dataset = query_params.get('dataset', [''])[0]
                self.send_dataset_info(dataset)
            elif path == '/api/query-examples':
                dataset = query_params.get('dataset', [''])[0]
                self.send_query_examples(dataset)
            elif path == '/api/lida-summarize':
                dataset = query_params.get('dataset', [''])[0]
                self.send_lida_summary(dataset)
            elif path == '/api/lida-goals':
                dataset = query_params.get('dataset', [''])[0]
                self.send_lida_goals(dataset)
            elif path == '/api/lida-visualize':
                self.handle_lida_visualize(query_params)
            else:
                self.send_error(404, f"API endpoint not found: {path}")
        except Exception as e:
            print(f"API Error: {e}")
            self.send_error(500, str(e))
    
    def send_datasets_list(self):
        """Send list of available datasets"""
        datasets_info = []
        for name, df in self.datasets.items():
            datasets_info.append({
                'name': name,
                'title': name.replace('_', ' ').title(),
                'records': len(df),
                'columns': list(df.columns),
                'description': self.get_dataset_description(name)
            })
        
        self.send_json_response({'datasets': datasets_info})
    
    def get_dataset_description(self, name):
        """Get description for dataset"""
        descriptions = {
            'ecommerce': 'E-commerce sales data with categories, regions, and customer demographics',
            'financial': 'Corporate financial performance by department and quarter',
            'iot_sensors': 'IoT sensor readings from multiple locations and sensor types',
            'healthcare': 'Patient treatment data with costs, outcomes, and demographics',
            'supply_chain': 'Supplier performance metrics including quality, cost, and delivery',
            'retail': 'Retail sales data from stores with economic indicators'
        }
        return descriptions.get(name, f'{name} dataset')
    
    def send_dataset_info(self, dataset_name):
        """Send detailed information about a specific dataset"""
        if dataset_name not in self.datasets:
            self.send_error(404, f"Dataset '{dataset_name}' not found")
            return
        
        df = self.datasets[dataset_name]
        info = {
            'name': dataset_name,
            'title': dataset_name.replace('_', ' ').title(),
            'description': self.get_dataset_description(dataset_name),
            'records': len(df),
            'columns': len(df.columns),
            'column_names': list(df.columns),
            'data_types': {col: str(df[col].dtype) for col in df.columns},
            'sample_data': df.head(5).to_dict('records') if len(df) > 0 else []
        }
        
        self.send_json_response(info)
    
    def send_query_examples(self, dataset_name):
        """Send query examples for a specific dataset"""
        examples = self.query_examples.get(dataset_name, [
            "Show me the data distribution",
            "Create a summary visualization",
            "Compare different categories",
            "Show trends over time"
        ])
        
        self.send_json_response({'examples': examples})
    
    def send_lida_summary(self, dataset_name):
        """Send LIDA summary for a dataset"""
        if dataset_name not in self.datasets:
            self.send_error(404, f"Dataset '{dataset_name}' not found")
            return
        
        df = self.datasets[dataset_name]
        
        # Basic summary if LIDA is not available
        summary = {
            'dataset': dataset_name,
            'shape': df.shape,
            'columns': list(df.columns),
            'dtypes': {col: str(df[col].dtype) for col in df.columns},
            'missing_values': df.isnull().sum().to_dict(),
            'basic_stats': df.describe().to_dict() if len(df.select_dtypes(include='number').columns) > 0 else {}
        }
        
        if self.lida_manager:
            try:
                # Try to use LIDA for advanced summary
                lida_summary = self.lida_manager.summarize(df)
                summary['lida_summary'] = lida_summary
            except Exception as e:
                print(f"LIDA summary error: {e}")
                summary['lida_error'] = str(e)
        
        self.send_json_response(summary)
    
    def send_lida_goals(self, dataset_name):
        """Send LIDA goals for a dataset"""
        if dataset_name not in self.datasets:
            self.send_error(404, f"Dataset '{dataset_name}' not found")
            return
        
        # Default visualization goals
        goals = [
            "Show distribution of numeric variables",
            "Compare categories across different dimensions",
            "Identify trends and patterns in the data",
            "Explore correlations between variables"
        ]
        
        if self.lida_manager:
            try:
                df = self.datasets[dataset_name]
                summary = self.lida_manager.summarize(df)
                lida_goals = self.lida_manager.goals(summary, n=5)
                goals = [goal.question for goal in lida_goals]
            except Exception as e:
                print(f"LIDA goals error: {e}")
        
        self.send_json_response({'goals': goals})
    
    def handle_lida_visualize(self, query_params):
        """Handle LIDA visualization requests"""
        dataset_name = query_params.get('dataset', [''])[0]
        query = query_params.get('query', [''])[0]
        
        if not dataset_name or dataset_name not in self.datasets:
            self.send_error(400, "Dataset parameter required and must exist")
            return
        
        if not query:
            self.send_error(400, "Query parameter required")
            return
        
        df = self.datasets[dataset_name]
        
        try:
            # Generate visualization data based on the query
            viz_data = self.generate_visualization_data(df, query, dataset_name)
            
            self.send_json_response({
                'success': True,
                'data': viz_data,
                'query': query,
                'dataset': dataset_name
            })
        except Exception as e:
            print(f"Visualization error: {e}")
            self.send_json_response({
                'success': False,
                'error': str(e),
                'query': query,
                'dataset': dataset_name
            })
    
    def generate_visualization_data(self, df, query, dataset_name):
        """Generate visualization data based on query"""
        # Simple query processing and data generation
        query_lower = query.lower()
        
        # Return sample of the dataset for visualization
        sample_size = min(100, len(df))
        sample_df = df.sample(n=sample_size) if len(df) > sample_size else df
        
        return sample_df.to_dict('records')

    def send_json_response(self, data):
        """Send JSON response with proper headers"""
        response_json = json.dumps(data, default=str)
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        self.wfile.write(response_json.encode())

def main():
    PORT = 8084
    
    print("🚀 Starting LIDA Multi-Dataset Server...")
    print("📊 Loading multiple sample datasets...")
    
    # Change to the server directory
    os.chdir('/home/student/YBSecurity/servers')
    
    try:
        with socketserver.TCPServer(("", PORT), LidaMultiDatasetServer) as httpd:
            print(f"✅ Server running at http://localhost:{PORT}")
            print(f"🤖 LIDA Multi-Dataset Dashboard: http://localhost:{PORT}/lida-multi-dataset-dashboard.html")
            print("🔄 Press Ctrl+C to stop the server")
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Server stopped")
    except Exception as e:
        print(f"❌ Server error: {e}")

if __name__ == "__main__":
    main()