#!/usr/bin/env python3
"""
🚀 QUICK START SCRIPT
Chạy chương trình một cách đơn giản nhất
"""

import os
import sys
import subprocess
import webbrowser
from pathlib import Path

def check_requirements():
    """Kiểm tra requirements"""
    print("🔍 Checking requirements...")
    
    # Check Python version
    if sys.version_info < (3, 8):
        print("❌ Python 3.8+ required")
        return False
    
    print(f"✅ Python {sys.version.split()[0]}")
    
    # Check required files
    required_files = [
        "serve_website.py",
        "news.html", 
        "styles.css",
        "script.js"
    ]
    
    for file in required_files:
        if not Path(file).exists():
            print(f"❌ Missing file: {file}")
            return False
    
    print("✅ All required files found")
    
    # Check if summaries.json exists
    if not Path("summaries.json").exists():
        print("⚠️ summaries.json not found")
        if Path("data/outputs/summaries.json").exists():
            print("📁 Copying from data/outputs/...")
            import shutil
            shutil.copy("data/outputs/summaries.json", "summaries.json")
            print("✅ summaries.json copied")
        else:
            print("❌ No data file found. Need to run update_news.py first")
            return False
    else:
        print("✅ summaries.json found")
    
    return True

def install_missing_packages():
    """Cài đặt packages thiếu"""
    print("📦 Checking packages...")
    
    required_packages = [
        "feedparser",
        "beautifulsoup4", 
        "requests"
    ]
    
    missing = []
    for package in required_packages:
        try:
            __import__(package.replace("-", "_"))
        except ImportError:
            missing.append(package)
    
    if missing:
        print(f"📥 Installing missing packages: {missing}")
        try:
            subprocess.check_call([sys.executable, "-m", "pip", "install"] + missing)
            print("✅ Packages installed")
        except subprocess.CalledProcessError:
            print("❌ Failed to install packages")
            return False
    else:
        print("✅ All packages available")
    
    return True

def start_server():
    """Khởi động server"""
    print("🚀 Starting server...")
    
    try:
        # Start server in background
        process = subprocess.Popen(
            [sys.executable, "serve_website.py"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        # Wait a bit for server to start
        import time
        time.sleep(2)
        
        # Check if process is still running
        if process.poll() is None:
            print("✅ Server started successfully!")
            print("🌐 Opening browser...")
            
            # Open browser
            webbrowser.open("http://localhost:8000")
            
            print("\n" + "="*50)
            print("🎉 CHƯƠNG TRÌNH ĐÃ CHẠY THÀNH CÔNG!")
            print("="*50)
            print("🌐 Website: http://localhost:8000")
            print("📱 Features:")
            print("  • Xem tin tức tech từ Techmeme")
            print("  • Tìm kiếm và lọc tin")
            print("  • Nút refresh góc trái (có hiệu ứng)")
            print("  • Responsive design")
            print("\n🔄 Để cập nhật dữ liệu mới:")
            print("  python update_news.py")
            print("\n⏹️ Để dừng: Ctrl+C")
            print("="*50)
            
            # Keep running
            try:
                process.wait()
            except KeyboardInterrupt:
                print("\n👋 Stopping server...")
                process.terminate()
                
        else:
            print("❌ Server failed to start")
            stdout, stderr = process.communicate()
            if stderr:
                print(f"Error: {stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Error starting server: {e}")
        return False
    
    return True

def show_menu():
    """Hiển thị menu lựa chọn"""
    print("\n🚀 TECH NEWS WEBSITE")
    print("="*40)
    print("1. 🏃 Quick Start (dùng dữ liệu có sẵn)")
    print("2. 🔄 Update data rồi chạy")
    print("3. 🧪 Test connection")
    print("4. 📚 View guide")
    print("5. ❌ Exit")
    print("="*40)
    
    choice = input("Chọn (1-5): ").strip()
    return choice

def update_data():
    """Cập nhật dữ liệu mới"""
    print("🔄 Updating data...")
    
    try:
        result = subprocess.run(
            [sys.executable, "update_news.py"],
            capture_output=True,
            text=True,
            timeout=60
        )
        
        if result.returncode == 0:
            print("✅ Data updated successfully!")
            return True
        else:
            print(f"❌ Update failed: {result.stderr}")
            return False
            
    except subprocess.TimeoutExpired:
        print("❌ Update timeout")
        return False
    except Exception as e:
        print(f"❌ Update error: {e}")
        return False

def test_connection():
    """Test kết nối"""
    print("🧪 Testing connection...")
    
    try:
        result = subprocess.run(
            [sys.executable, "test_quick.py"],
            capture_output=True,
            text=True,
            timeout=30
        )
        
        print(result.stdout)
        if result.stderr:
            print(result.stderr)
            
    except Exception as e:
        print(f"❌ Test error: {e}")

def main():
    """Main function"""
    print("🚀 TECH NEWS WEBSITE - QUICK START")
    print("="*50)
    
    # Check current directory
    if not Path("serve_website.py").exists():
        print("❌ Please run this script from the project directory")
        print("📁 Expected path: e:\\Viscode\\Demo_Skola")
        return
    
    while True:
        choice = show_menu()
        
        if choice == "1":
            if check_requirements() and install_missing_packages():
                start_server()
            break
            
        elif choice == "2":
            if check_requirements() and install_missing_packages():
                if update_data():
                    start_server()
                else:
                    print("⚠️ Starting with existing data...")
                    start_server()
            break
            
        elif choice == "3":
            test_connection()
            
        elif choice == "4":
            if Path("RUN_GUIDE.md").exists():
                print("📚 Opening guide...")
                os.system("notepad RUN_GUIDE.md")
            else:
                print("❌ Guide not found")
                
        elif choice == "5":
            print("👋 Goodbye!")
            break
            
        else:
            print("❌ Invalid choice")

if __name__ == "__main__":
    main()