#!/usr/bin/env python3
"""
Cập nhật dữ liệu tin tức mới từ Techmeme
Usage: python update_news.py
"""
import sys
import os
import subprocess
import shutil
from pathlib import Path
import json
from datetime import datetime

# Force use Windows Python if available
PYTHON_PATHS = [
    r"C:\Users\ADMIN\AppData\Local\Programs\Python\Python313\python.exe",
    r"C:\Python313\python.exe",
    r"C:\Python312\python.exe", 
    r"C:\Python311\python.exe",
    sys.executable
]

def get_windows_python():
    """Tìm Python interpreter có thể dùng được với google-generativeai"""
    for python_path in PYTHON_PATHS:
        if os.path.exists(python_path):
            try:
                # Test import
                result = subprocess.run([
                    python_path, "-c", 
                    "import google.generativeai; print('OK')"
                ], capture_output=True, text=True, timeout=10)
                if result.returncode == 0:
                    return python_path
            except (subprocess.TimeoutExpired, FileNotFoundError):
                continue
    return None

# Add src to Python path
sys.path.append(str(Path(__file__).parent / "src"))

def print_step(step, message):
    """In bước thực hiện với format đẹp"""
    print(f"[{step}/4] {message}")
    print("-" * 50)

def run_pipeline():
    """Chạy pipeline để lấy dữ liệu mới"""
    print_step(1, "🔄 Đang lấy dữ liệu mới từ Techmeme...")
    
    # Tìm Python interpreter phù hợp
    windows_python = get_windows_python()
    if not windows_python:
        print("❌ Không tìm thấy Python interpreter có google-generativeai")
        print("💡 Cài đặt bằng: py -m pip install google-generativeai")
        return False
    
    print(f"✅ Sử dụng Python: {windows_python}")
    
    # Import và chạy pipeline trực tiếp
    try:
        # Ensure sys.path có src để import hoạt động
        src_path = str(Path(__file__).parent / "src")
        if src_path not in sys.path:
            sys.path.insert(0, src_path)
            
        from src.pipelines.news_pipeline import NewsPipeline
        pipeline = NewsPipeline()
        print("📊 Đang xử lý dữ liệu với AI...")
        pipeline.run(top_n=25)
        print("✅ Lấy dữ liệu thành công!")
        return True
    except Exception as e:
        print(f"❌ Lỗi khi lấy dữ liệu: {e}")
        import traceback
        print("🔍 Chi tiết lỗi:")
        traceback.print_exc()
        return False

def copy_data():
    """Copy dữ liệu từ outputs ra thư mục gốc"""
    print_step(2, "📁 Đang copy dữ liệu...")
    
    from src.services.storage_service import StorageService

    storage = StorageService()
    source = storage.output_path
    target = Path("summaries.json")
    
    try:
        if source.exists():
            shutil.copy2(source, target)
            print("✅ Copy dữ liệu thành công!")
            return True
        else:
            print(f"❌ Không tìm thấy file: {source}")
            return False
    except Exception as e:
        print(f"❌ Lỗi khi copy dữ liệu: {e}")
        return False

def update_timestamp():
    """Cập nhật timestamp trong file JSON"""
    print_step(3, "⏰ Cập nhật thời gian...")
    
    try:
        with open("summaries.json", "r", encoding="utf-8") as f:
            data = json.load(f)
        
        # Thêm timestamp
        data["last_updated"] = datetime.now().isoformat()
        data["total_items"] = len(data.get("items", []))
        
        with open("summaries.json", "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        print("✅ Cập nhật timestamp thành công!")
        return True
    except Exception as e:
        print(f"❌ Lỗi khi cập nhật timestamp: {e}")
        return False

def show_summary():
    """Hiển thị tóm tắt kết quả"""
    print_step(4, "📊 Tóm tắt kết quả")
    
    try:
        with open("summaries.json", "r", encoding="utf-8") as f:
            data = json.load(f)
        
        items = data.get("items", [])
        last_updated = data.get("last_updated", "Unknown")
        
        print(f"📈 Tổng số bài viết: {len(items)}")
        print(f"⏰ Cập nhật lúc: {last_updated}")
        
        if items:
            print(f"📰 Bài mới nhất: {items[0].get('title', 'N/A')[:80]}...")
            
            # Thống kê theo loại
            types = {}
            for item in items:
                t = item.get('type', 'other')
                types[t] = types.get(t, 0) + 1
            
            print("📊 Phân loại:")
            for type_name, count in types.items():
                print(f"   - {type_name}: {count} bài")
        
        print("\n🌐 Website sẽ tự động hiển thị dữ liệu mới!")
        print("💡 Refresh trang web (F5) để xem tin tức mới")
        
    except Exception as e:
        print(f"❌ Lỗi khi hiển thị tóm tắt: {e}")

def main():
    """Hàm chính"""
    print("🚀 CẬP NHẬT DỮ LIỆU TIN TỨC")
    print("=" * 40)
    print(f"⏰ Thời gian: {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    print("=" * 40)
    
    # Debug info
    print("🔍 Debug info:")
    print(f"   Python: {sys.executable}")
    print(f"   Working dir: {os.getcwd()}")
    print(f"   Sys.path[0]: {sys.path[0]}")
    
    # Kiểm tra môi trường
    if not Path("src").exists():
        print("❌ Không tìm thấy thư mục src!")
        print("💡 Hãy chạy script này từ thư mục gốc của project")
        sys.exit(1)
    
    # Thực hiện các bước
    success = True
    
    # Bước 1: Chạy pipeline
    if not run_pipeline():
        success = False
    
    # Bước 2: Copy dữ liệu
    if success and not copy_data():
        success = False
    
    # Bước 3: Cập nhật timestamp
    if success and not update_timestamp():
        success = False
    
    # Bước 4: Hiển thị tóm tắt
    if success:
        show_summary()
        print("\n🎉 CẬP NHẬT THÀNH CÔNG!")
    else:
        print("\n❌ CẬP NHẬT THẤT BẠI!")
        print("💡 Kiểm tra lỗi ở trên và thử lại")
        sys.exit(1)

if __name__ == "__main__":
    main()
