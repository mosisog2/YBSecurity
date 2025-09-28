#!/usr/bin/env python3
"""
Simple HTTP Server for ML Dashboard Web Interface
Serves the HTML dashboard and provides API endpoints
"""

import http.server
import socketserver
import json
import os
import csv
from urllib.parse import parse_qs, urlparse
import threading
import webbrowser
import time

class DashboardRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=os.path.dirname(os.path.abspath(__file__)), **kwargs)
    
    def end_headers(self):
        # Enable CORS
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()
    
    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()
    
    def do_GET(self):
        parsed = urlparse(self.path)
        
        if parsed.path == '/api/datasets':
            self.handle_get_datasets()
        elif parsed.path.startswith('/api/data/'):
            dataset_name = parsed.path.split('/')[-1]
            self.handle_get_data(dataset_name, parsed.query)
        elif parsed.path == '/api/status':
            self.handle_get_status()
        else:
            # Serve static files
            if parsed.path == '/':
                self.path = '/web-dashboard.html'
            super().do_GET()
    
    def do_POST(self):
        parsed = urlparse(self.path)
        
        if parsed.path == '/api/analyze':
            self.handle_analyze_data()
        else:
            self.send_error(404)
    
    def handle_get_datasets(self):
        """Return available datasets"""
        datasets = []
        
        # Look for CSV files in current directory
        for file in os.listdir('.'):
            if file.endswith('.csv'):
                try:
                    with open(file, 'r', encoding='utf-8') as f:
                        reader = csv.reader(f)
                        headers = next(reader, [])
                        row_count = sum(1 for _ in reader)
                    
                    datasets.append({
                        'id': file,
                        'name': file.replace('_', ' ').replace('.csv', '').title(),
                        'type': 'csv',
                        'columns': len(headers),
                        'rows': row_count,
                        'headers': headers[:5]  # First 5 headers
                    })
                except Exception as e:
                    print(f"Error reading {file}: {e}")
        
        self.send_json_response(datasets)
    
    def handle_get_data(self, dataset_name, query_string):
        """Return data from specified dataset"""
        try:
            # Parse query parameters
            params = parse_qs(query_string) if query_string else {}
            limit = int(params.get('limit', [100])[0])
            offset = int(params.get('offset', [0])[0])
            
            print(f"📊 Loading dataset: {dataset_name} (limit: {limit}, offset: {offset})")
            
            data = []
            headers = []
            total_count = 0
            
            # Get the full path to the dataset file
            script_dir = os.path.dirname(os.path.abspath(__file__))
            file_path = os.path.join(script_dir, dataset_name)
            
            print(f"🔍 Looking for file: {file_path}")
            
            with open(file_path, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                headers = reader.fieldnames
                
                # Count total rows first
                all_rows = list(reader)
                total_count = len(all_rows)
                
                # Apply offset and limit
                limited_rows = all_rows[offset:offset + limit]
                data = limited_rows
            
            print(f"✅ Loaded {len(data)} records from {dataset_name}")
            
            response = {
                'data': data,
                'headers': headers,
                'total_count': total_count,
                'returned_count': len(data),
                'dataset': dataset_name
            }
            
            self.send_json_response(response)
            
        except FileNotFoundError:
            print(f"❌ Dataset {dataset_name} not found")
            self.send_error(404, f"Dataset {dataset_name} not found")
        except Exception as e:
            print(f"❌ Error reading dataset {dataset_name}: {str(e)}")
            self.send_error(500, f"Error reading dataset: {str(e)}")
    
    def handle_get_status(self):
        """Return server status"""
        status = {
            'status': 'running',
            'version': '1.0.0',
            'ml_enabled': True,
            'java_backend': True,
            'timestamp': time.time()
        }
        self.send_json_response(status)
    
    def handle_analyze_data(self):
        """Analyze uploaded data"""
        try:
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode('utf-8'))
            
            # Simple analysis (in real implementation, this would use ML)
            analysis = {
                'domain': self.detect_domain(data.get('headers', [])),
                'record_count': len(data.get('data', [])),
                'column_count': len(data.get('headers', [])),
                'data_quality': 85.5,
                'ml_confidence': 92.3,
                'insights': [
                    {
                        'type': 'volume',
                        'title': f"Dataset contains {len(data.get('data', []))} records",
                        'importance': 'medium'
                    },
                    {
                        'type': 'structure',
                        'title': f"Found {len(data.get('headers', []))} columns",
                        'importance': 'low'
                    }
                ]
            }
            
            self.send_json_response(analysis)
            
        except Exception as e:
            self.send_error(500, f"Analysis error: {str(e)}")
    
    def detect_domain(self, headers):
        """Simple domain detection based on column names"""
        header_text = ' '.join(headers).lower()
        
        if any(word in header_text for word in ['sales', 'revenue', 'price', 'amount']):
            return 'SALES_COMMERCE'
        elif any(word in header_text for word in ['employee', 'salary', 'department']):
            return 'HUMAN_RESOURCES'
        elif any(word in header_text for word in ['budget', 'expense', 'profit']):
            return 'FINANCE'
        else:
            return 'GENERAL'
    
    def send_json_response(self, data):
        """Send JSON response"""
        response = json.dumps(data, indent=2)
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(response.encode('utf-8'))

def start_server(port=8080):
    """Start the dashboard server"""
    handler = DashboardRequestHandler
    
    with socketserver.TCPServer(("", port), handler) as httpd:
        print(f"🌐 ML Dashboard Server starting on port {port}")
        print(f"📊 Dashboard URL: http://localhost:{port}")
        print(f"🔗 Direct link: http://localhost:{port}/web-dashboard.html")
        print("Press Ctrl+C to stop the server")
        
        # Auto-open browser after a short delay
        def open_browser():
            time.sleep(2)
            webbrowser.open(f'http://localhost:{port}/web-dashboard.html')
        
        browser_thread = threading.Thread(target=open_browser)
        browser_thread.daemon = True
        browser_thread.start()
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 Server stopped")
            httpd.shutdown()

if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    start_server(port)