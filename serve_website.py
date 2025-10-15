#!/usr/bin/env python3
"""
Simple HTTP server to serve the news website with refresh API
Usage: python serve_website.py
"""
import http.server
import socketserver
import webbrowser
import os
import sys
import json
import subprocess
import threading
import time
from pathlib import Path
from urllib.parse import urlparse, parse_qs

# Configuration
PORT = 8000
HOST = "localhost"

class CustomHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    """Custom handler to serve files with proper MIME types and API endpoints"""
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=os.getcwd(), **kwargs)
    
    def end_headers(self):
        # Add CORS headers for local development
        # Cors là Cross-Origin Resource Sharing 
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()
    
    def do_GET(self):
        # Parse URL
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        query = parse_qs(parsed_path.query)
        
        # API endpoint to refresh data
        if path == '/api/refresh':
            self.handle_refresh_api()
            return
            
        # API endpoint to check refresh status
        if path == '/api/refresh/status':
            self.handle_refresh_status()
            return
        
        # Redirect root to news.html
        if path == '/':
            self.send_response(302)
            self.send_header('Location', '/news.html')
            self.end_headers()
            return
        
        # Serve files normally
        super().do_GET()
    
    def handle_refresh_api(self):
        """Handle data refresh request"""
        try:
            print("🔄 Refresh API called")
            
            # Start refresh in background
            def run_update():
                try:
                    print("📝 Starting background update process...")
                    # Run the full AI update script
                    import os
                    result = subprocess.run([
                        'python', 'update_news.py'
                    ], capture_output=True, text=True, timeout=600, shell=True, cwd=os.getcwd())
                    
                    print(f"📊 Update result: return_code={result.returncode}")
                    print(f"📝 Output: {result.stdout[:200]}...")
                    if result.stderr:
                        print(f"❌ Error: {result.stderr[:200]}...")
                    
                    # Store result
                    refresh_status['completed'] = True
                    refresh_status['success'] = result.returncode == 0
                    refresh_status['output'] = result.stdout
                    refresh_status['error'] = result.stderr
                    
                except Exception as e:
                    print(f"❌ Background update failed: {e}")
                    refresh_status['completed'] = True
                    refresh_status['success'] = False
                    refresh_status['error'] = str(e)
            
            # Reset status
            global refresh_status
            refresh_status = {
                'started': True,
                'completed': False,
                'success': False,
                'timestamp': time.time(),
                'output': '',
                'error': ''
            }
            
            print(f"🚀 Starting refresh at {refresh_status['timestamp']}")
            
            # Start background thread
            thread = threading.Thread(target=run_update)
            thread.daemon = True
            thread.start()
            
            print("✅ Background thread started")
            
            # Return immediate response
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            response = {
                'status': 'started',
                'message': 'Data refresh started in background',
                'timestamp': refresh_status['timestamp']
            }
            
            self.wfile.write(json.dumps(response).encode())
            
        except Exception as e:
            self.send_response(500)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            error_response = {
                'status': 'error',
                'message': f'Failed to start refresh: {str(e)}'
            }
            
            self.wfile.write(json.dumps(error_response).encode())
    
    def handle_refresh_status(self):
        """Handle refresh status check"""
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        
        global refresh_status
        response = refresh_status.copy() if 'refresh_status' in globals() else {
            'started': False,
            'completed': True,
            'success': True,
            'message': 'No refresh in progress'
        }
        
        self.wfile.write(json.dumps(response).encode())

# Global variable to track refresh status
refresh_status = {
    'started': False,
    'completed': True,
    'success': True
}

def start_server():
    """Start the HTTP server"""
    try:
        with socketserver.TCPServer((HOST, PORT), CustomHTTPRequestHandler) as httpd:
            url = f"http://{HOST}:{PORT}"
            print(f"🚀 Starting server at {url}")
            print(f"📁 Serving directory: {os.getcwd()}")
            print(f"🌐 Open your browser to view: {url}")
            print("💡 Press Ctrl+C to stop the server")
            print("-" * 50)
            
            # Auto-open browser
            try:
                webbrowser.open(url)
                print("✅ Browser opened automatically")
            except Exception as e:
                print(f"⚠️  Could not open browser automatically: {e}")
                print(f"   Please manually open: {url}")
            
            print("-" * 50)
            httpd.serve_forever()
            
    except KeyboardInterrupt:
        print("\n👋 Server stopped by user")
        sys.exit(0)
    except OSError as e:
        if e.errno == 10048:  # Port already in use on Windows
            print(f"❌ Port {PORT} is already in use!")
            print(f"   Try a different port or stop the other server")
            sys.exit(1)
        else:
            print(f"❌ Error starting server: {e}")
            sys.exit(1)

def check_files():
    """Check if required files exist"""
    required_files = [
        'news.html',
        'styles.css',
        'js/main.js'
    ]
    
    missing_files = []
    for file in required_files:
        if not Path(file).exists():
            missing_files.append(file)
    
    if missing_files:
        print("❌ Missing required files:")
        for file in missing_files:
            print(f"   - {file}")
        print("\n💡 Make sure you're running this from the correct directory")
        sys.exit(1)
    
    # Check if summaries.json exists, if not try to copy from data/outputs
    if not Path('summaries.json').exists():
        print("⚠️  summaries.json not found in root")
        output_path = Path('data/outputs/summaries.json')
        if output_path.exists():
            print("📁 Copying from data/outputs/summaries.json...")
            import shutil
            shutil.copy2(output_path, 'summaries.json')
            print("✅ summaries.json copied successfully")
        else:
            print("❌ No data file found!")
            print("💡 Please run: python update_news.py first")
            sys.exit(1)
    
    print("✅ All required files found")

if __name__ == "__main__":
    print("🔍 Checking files...")
    check_files()
    
    print("🌟 Tech News Website Server")
    print("=" * 30)
    start_server()
