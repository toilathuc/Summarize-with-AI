#!/usr/bin/env python3
"""
Simple wrapper to run update using Windows Python
"""
import subprocess
import sys
from pathlib import Path

def main():
    # Windows Python paths to try
    python_paths = [
        r"C:\Users\ADMIN\AppData\Local\Programs\Python\Python313\python.exe",
        r"C:\Python313\python.exe",
        r"C:\Python312\python.exe", 
        r"C:\Python311\python.exe"
    ]
    
    # Find working Python
    working_python = None
    for python_path in python_paths:
        if Path(python_path).exists():
            try:
                # Test if it can import google.generativeai
                result = subprocess.run([
                    python_path, "-c", 
                    "import google.generativeai; print('OK')"
                ], capture_output=True, text=True, timeout=10)
                if result.returncode == 0:
                    working_python = python_path
                    break
            except Exception:
                continue
    
    if not working_python:
        print("❌ Không tìm thấy Python có google-generativeai package")
        print("💡 Cài đặt bằng: py -m pip install google-generativeai")
        return 1
    
    print(f"🐍 Sử dụng Python: {working_python}")
    
    # Create simple pipeline script
    pipeline_script = """
import sys
sys.path.append('src')
from src.pipelines.news_pipeline import NewsPipeline
from src.services.storage_service import StorageService
import json
from datetime import datetime
from pathlib import Path
import shutil

def run():
    print("🔄 Đang lấy dữ liệu từ Techmeme...")
    pipeline = NewsPipeline()
    result = pipeline.run(top_n=25)
    
    # Copy to root
    storage = StorageService()
    if storage.output_path.exists():
        shutil.copy2(storage.output_path, "summaries.json")
        
        # Add timestamp
        with open("summaries.json", "r", encoding="utf-8") as f:
            data = json.load(f)
        data["last_updated"] = datetime.now().isoformat()
        data["total_items"] = len(data.get("items", []))
        with open("summaries.json", "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        print(f"✅ Cập nhật {len(data.get('items', []))} bài thành công!")
    else:
        print("❌ Không tìm thấy output file")

if __name__ == "__main__":
    run()
"""
    
    # Write temp script
    temp_script = Path("temp_update_pipeline.py")
    temp_script.write_text(pipeline_script, encoding="utf-8")
    
    try:
        # Run with Windows Python
        result = subprocess.run([
            working_python, str(temp_script)
        ], cwd=Path.cwd(), timeout=300)
        
        return result.returncode
    finally:
        # Cleanup
        if temp_script.exists():
            temp_script.unlink()

if __name__ == "__main__":
    sys.exit(main())