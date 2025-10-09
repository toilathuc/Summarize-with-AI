#!/usr/bin/env python3
"""
Quick test script for refresh API
"""
import json
import shutil
from datetime import datetime
from pathlib import Path

def quick_refresh():
    """Quick refresh without external dependencies"""
    try:
        print("🔄 Quick refresh starting...")
        
        # Read current data
        if Path("summaries.json").exists():
            with open("summaries.json", "r", encoding="utf-8") as f:
                data = json.load(f)
            
            # Update timestamp
            data["last_updated"] = datetime.now().isoformat()
            
            # Update first item title to show it's refreshed
            if data.get("items") and len(data["items"]) > 0:
                data["items"][0]["title"] = f"[REFRESHED] {data['items'][0]['title']}"
            
            # Save back
            with open("summaries.json", "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            print(f"✅ Quick refresh completed at {data['last_updated']}")
            return True
        else:
            print("❌ No summaries.json found")
            return False
            
    except Exception as e:
        print(f"❌ Quick refresh failed: {e}")
        return False

if __name__ == "__main__":
    success = quick_refresh()
    exit(0 if success else 1)