#!/usr/bin/env python3
"""
Simple update script without AI summarization for testing
"""
import sys
import os
import json
import shutil
from pathlib import Path
from datetime import datetime

# Add src to Python path
sys.path.append(str(Path(__file__).parent / "src"))

def simple_update():
    """Update với dữ liệu đơn giản, không cần AI"""
    
    print("🔄 SIMPLE UPDATE - NO AI")
    print("=" * 40)
    
    try:
        # 1. Fetch from Techmeme
        print("1️⃣ Fetching from Techmeme...")
        from services import FeedService, StorageService

        feed_service = FeedService()
        storage_service = StorageService()
        articles = feed_service.fetch_latest(limit=15)

        print(f"✅ Got {len(articles)} items")
        
        # 2. Convert to simple format (without AI summary)
        print("2️⃣ Converting to simple format...")

        # Helper to try to format published time into a readable string
        def _fmt_time(ts: str) -> str:
            if not ts:
                return ""
            try:
                # Try ISO format first
                return datetime.fromisoformat(ts).strftime("%Y-%m-%d %H:%M")
            except Exception:
                # Fallback: return raw string
                return ts

        simple_items = []
        for article in articles:
            # Prefer an explicit source field if available, else fall back to a short summary
            source = (
                article.raw.get('source')
                or article.raw.get('site')
                or (article.summary_text or '')[:80]
            )

            simple_item = {
                "title": article.title,
                "url": article.original_url or article.techmeme_url,
                "bullets": [
                    f"Nguồn: {source}",
                    f"Thời gian: {_fmt_time(article.raw.get('published_at', ''))}",
                    f"Link gốc: {article.techmeme_url}"
                ],
                "why_it_matters": "Tin tức công nghệ quan trọng từ Techmeme. Cần đọc để cập nhật xu hướng ngành.",
                "type": "news",
                "key_commands": [],
                "caveats": []
            }
            simple_items.append(simple_item)
        
        # 3. Create output format
        output_data = {
            "items": simple_items,
            "last_updated": datetime.now().isoformat(),
            "total_items": len(simple_items)
        }
        
        # 4. Save to outputs
        print("3️⃣ Saving data...")
        
        # Ensure directories exist
        Path(storage_service.output_path.parent).mkdir(parents=True, exist_ok=True)

        with open(storage_service.output_path, "w", encoding="utf-8") as f:
            json.dump(output_data, f, ensure_ascii=False, indent=2)
        
        # Copy to root
        shutil.copy2(storage_service.output_path, "summaries.json")
        
        print("✅ Data updated successfully!")
        print(f"📅 Updated at: {output_data['last_updated']}")
        print(f"📊 Items: {len(simple_items)}")
        
        return True
        
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = simple_update()
    if success:
        print("\n🎉 Simple update completed!")
    else:
        print("\n❌ Update failed!")
