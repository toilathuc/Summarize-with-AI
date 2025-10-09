#!/usr/bin/env python3
"""
Test nhanh để kiểm tra các vấn đề cụ thể của hàm Techmeme
"""

import sys
from pathlib import Path
import json

# Add src to path
sys.path.insert(0, str(Path(__file__).parent / "src"))

from feeds.techmeme import fetch_feed, normalize_feed, make_session

def quick_test():
    """Test nhanh các vấn đề chính"""
    
    print("🧪 QUICK TECHMEME TEST")
    print("=" * 40)
    
    try:
        # 1. Test connection
        session = make_session(timeout=10)
        feed_url = "https://www.techmeme.com/feed.xml"
        
        print("1️⃣ Testing connection...")
        d = fetch_feed(session, feed_url)
        print(f"✅ Got {len(d.entries)} entries")
        
        # 2. Test normalization
        print("2️⃣ Testing normalization...")
        items = normalize_feed(d)
        print(f"✅ Normalized to {len(items)} items")
        
        # 3. Check data quality
        print("3️⃣ Checking data quality...")
        
        valid_items = 0
        items_with_urls = 0
        items_with_content = 0
        
        for item in items:
            # Check required fields
            if item.get('title') and item.get('techmeme_url'):
                valid_items += 1
            
            # Check URLs
            if item.get('original_url') and item['original_url'].startswith('http'):
                items_with_urls += 1
            
            # Check content
            if item.get('summary_text') or item.get('summary_html'):
                items_with_content += 1
        
        print(f"✅ Valid items: {valid_items}/{len(items)}")
        print(f"✅ Items with URLs: {items_with_urls}/{len(items)}")
        print(f"✅ Items with content: {items_with_content}/{len(items)}")
        
        # 4. Sample item analysis
        print("4️⃣ Sample item analysis...")
        if items:
            sample = items[0]
            print(f"Title: {sample.get('title', 'N/A')[:80]}...")
            print(f"Original URL: {sample.get('original_url', 'N/A')}")
            print(f"Summary length: {len(sample.get('summary_text', ''))}")
            print(f"Hash: {sample.get('hash', 'N/A')}")
        
        # 5. Check for common issues
        print("5️⃣ Checking common issues...")
        
        # Empty titles
        empty_titles = sum(1 for x in items if not x.get('title', '').strip())
        print(f"Empty titles: {empty_titles}")
        
        # Missing original URLs
        missing_urls = sum(1 for x in items if not x.get('original_url'))
        print(f"Missing original URLs: {missing_urls}")
        
        # Duplicate hashes
        hashes = [x.get('hash') for x in items if x.get('hash')]
        unique_hashes = set(hashes)
        print(f"Unique vs total hashes: {len(unique_hashes)}/{len(hashes)}")
        
        # 6. Test with actual pipeline format
        print("6️⃣ Testing pipeline format...")
        pipeline_data = {
            "items": items,
            "metadata": {
                "source": "techmeme",
                "fetched_at": "2025-10-02T07:00:00Z",
                "total_items": len(items),
                "feed_title": d.feed.get('title', 'Techmeme')
            }
        }
        
        # Save in pipeline format
        with open('test_pipeline_format.json', 'w', encoding='utf-8') as f:
            json.dump(pipeline_data, f, ensure_ascii=False, indent=2, default=str)
        
        print(f"✅ Saved pipeline format: test_pipeline_format.json")
        
        print("\n🎯 CONCLUSION:")
        if valid_items == len(items) and items_with_urls > 0:
            print("✅ Data quality is GOOD")
            return True
        else:
            print("⚠️ Data quality has ISSUES")
            return False
            
    except Exception as e:
        print(f"❌ ERROR: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_problematic_cases():
    """Test các trường hợp có thể gây vấn đề"""
    
    print("\n🔍 TESTING PROBLEMATIC CASES")
    print("=" * 40)
    
    try:
        session = make_session()
        d = fetch_feed(session, "https://www.techmeme.com/feed.xml")
        
        if not d.entries:
            print("❌ No entries to test")
            return
        
        # Test với entry có HTML phức tạp
        for i, entry in enumerate(d.entries[:3]):
            print(f"\nEntry #{i+1}:")
            print(f"Title length: {len(entry.get('title', ''))}")
            print(f"Summary length: {len(entry.get('summary', ''))}")
            
            # Check for problematic characters
            title = entry.get('title', '')
            if any(ord(c) > 127 for c in title):
                print("⚠️ Contains non-ASCII characters")
            
            # Check for very long content
            summary = entry.get('summary', '')
            if len(summary) > 1000:
                print(f"⚠️ Very long summary: {len(summary)} chars")
        
        print("✅ Problematic cases tested")
        
    except Exception as e:
        print(f"❌ Error in problematic cases: {e}")

if __name__ == "__main__":
    success = quick_test()
    test_problematic_cases()
    
    if success:
        print("\n🎉 ALL TESTS PASSED!")
        print("The Techmeme function is working correctly.")
    else:
        print("\n❌ SOME TESTS FAILED!")
        print("Check the output above for issues.")