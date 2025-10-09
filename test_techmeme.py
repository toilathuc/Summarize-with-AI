#!/usr/bin/env python3
"""
Test script để kiểm tra hàm lấy dữ liệu từ Techmeme
Hiển thị chi tiết dữ liệu trả về để debug
"""

import json
import sys
import os
from pathlib import Path
from pprint import pprint

# Thêm src vào path để import được
sys.path.insert(0, str(Path(__file__).parent / "src"))

from feeds.techmeme import fetch_feed, normalize_feed, make_session

def test_techmeme_data():
    """Test và hiển thị dữ liệu từ Techmeme RSS"""
    
    print("🔍 TESTING TECHMEME DATA FETCH")
    print("=" * 50)
    
    # Tạo session
    session = make_session(timeout=15)
    feed_url = "https://www.techmeme.com/feed.xml"
    
    try:
        print(f"📡 Đang fetch từ: {feed_url}")
        print("-" * 50)
        
        # 1. Fetch raw feed data
        print("1️⃣ FETCHING RAW FEED DATA...")
        d = fetch_feed(session, feed_url)
        
        print(f"✅ Feed title: {d.feed.get('title', 'N/A')}")
        print(f"✅ Feed description: {d.feed.get('description', 'N/A')}")
        print(f"✅ Number of entries: {len(d.entries)}")
        print(f"✅ Feed version: {d.get('version', 'N/A')}")
        
        if d.entries:
            print(f"✅ First entry keys: {list(d.entries[0].keys())}")
        
        print("-" * 50)
        
        # 2. Normalize feed
        print("2️⃣ NORMALIZING FEED DATA...")
        items = normalize_feed(d)
        
        print(f"✅ Normalized items count: {len(items)}")
        
        if items:
            print(f"✅ First item keys: {list(items[0].keys())}")
        
        print("-" * 50)
        
        # 3. Hiển thị chi tiết 3 item đầu
        print("3️⃣ CHI TIẾT 3 ITEMS ĐẦU TIÊN:")
        for i, item in enumerate(items[:3], 1):
            print(f"\n🔸 ITEM #{i}")
            print("-" * 30)
            
            # Thông tin cơ bản
            print(f"Title: {item.get('title', 'N/A')}")
            print(f"Techmeme URL: {item.get('techmeme_url', 'N/A')}")
            print(f"Original URL: {item.get('original_url', 'N/A')}")
            print(f"Published: {item.get('published_at', 'N/A')}")
            print(f"Author: {item.get('author_name', 'N/A')}")
            
            # Summary
            summary = item.get('summary_html', '')
            if summary:
                print(f"Summary (first 100 chars): {summary[:100]}...")
            
            # Links
            related = item.get('related_urls', [])
            if related:
                print(f"Related URLs ({len(related)}): {related[:2]}...")
            
            # Content
            content = item.get('content_html', '')
            if content:
                print(f"Content length: {len(content)} chars")
                print(f"Content preview: {content[:100]}...")
        
        print("-" * 50)
        
        # 4. Test một item với enrich (nếu có original_url)
        test_item = None
        for item in items:
            if item.get('original_url') and item['original_url'].startswith('http'):
                test_item = item
                break
        
        if test_item:
            print("4️⃣ TESTING ENRICH DATA...")
            from feeds.techmeme import enrich_from_article
            
            try:
                url = test_item['original_url']
                print(f"🔗 Enriching: {url}")
                
                enriched = enrich_from_article(session, url)
                print(f"✅ Enriched data keys: {list(enriched.keys())}")
                
                print("\n📊 ENRICHED DATA:")
                print(f"OG Title: {enriched.get('og_title', 'N/A')}")
                print(f"OG Description: {enriched.get('og_description', 'N/A')}")
                print(f"OG Image: {enriched.get('og_image', 'N/A')}")
                print(f"Author: {enriched.get('author', 'N/A')}")
                print(f"Date Published: {enriched.get('date_published', 'N/A')}")
                
            except Exception as e:
                print(f"❌ Enrich failed: {e}")
        
        print("-" * 50)
        
        # 5. Lưu sample data để xem
        print("5️⃣ SAVING SAMPLE DATA...")
        sample_data = {
            "feed_info": {
                "title": d.feed.get('title'),
                "description": d.feed.get('description'),
                "entries_count": len(d.entries),
                "version": d.get('version')
            },
            "items": items[:5]  # Chỉ lưu 5 items đầu
        }
        
        output_file = "test_techmeme_output.json"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(sample_data, f, ensure_ascii=False, indent=2, default=str)
        
        print(f"✅ Saved test data to: {output_file}")
        
        # 6. Summary
        print("\n📋 SUMMARY:")
        print(f"• Feed entries: {len(d.entries)}")
        print(f"• Normalized items: {len(items)}")
        print(f"• Items with original_url: {sum(1 for x in items if x.get('original_url'))}")
        print(f"• Items with content: {sum(1 for x in items if x.get('content_html'))}")
        print(f"• Items with author: {sum(1 for x in items if x.get('author_name'))}")
        
        return True
        
    except Exception as e:
        print(f"❌ ERROR: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_specific_entry():
    """Test chi tiết một entry cụ thể"""
    
    print("\n" + "="*50)
    print("🔬 DETAILED ENTRY ANALYSIS")
    print("="*50)
    
    session = make_session()
    d = fetch_feed(session, "https://www.techmeme.com/feed.xml")
    
    if not d.entries:
        print("❌ No entries found!")
        return
    
    # Lấy entry đầu tiên
    entry = d.entries[0]
    
    print("📝 RAW ENTRY STRUCTURE:")
    print("-" * 30)
    
    # In tất cả fields của entry
    for key, value in entry.items():
        print(f"{key}: {type(value).__name__}")
        if isinstance(value, str) and len(value) > 100:
            print(f"  └─ (Length: {len(value)}) {value[:100]}...")
        elif isinstance(value, (list, dict)):
            print(f"  └─ {value}")
        else:
            print(f"  └─ {value}")
    
    print("\n🔗 LINK EXTRACTION TEST:")
    print("-" * 30)
    
    from feeds.techmeme import extract_links
    techmeme_url, original_url, related_urls = extract_links(entry)
    
    print(f"Techmeme URL: {techmeme_url}")
    print(f"Original URL: {original_url}")
    print(f"Related URLs ({len(related_urls)}):")
    for i, url in enumerate(related_urls[:5], 1):
        print(f"  {i}. {url}")

if __name__ == "__main__":
    print("🚀 TECHMEME DATA TEST SCRIPT")
    print("="*50)
    
    # Test 1: Tổng quan
    success = test_techmeme_data()
    
    if success:
        # Test 2: Chi tiết entry
        test_specific_entry()
        
        print("\n✅ ALL TESTS COMPLETED!")
        print("Check 'test_techmeme_output.json' for detailed results")
    else:
        print("\n❌ TESTS FAILED!")