"""
Client and helpers for retrieving and normalising Techmeme RSS items.
"""

from __future__ import annotations

import hashlib
import json
import re
import time
from email.utils import parsedate_to_datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple
from urllib.parse import parse_qsl, urlencode, urlparse, urlunparse

import feedparser
import requests
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from src.config import settings
from src.domain import FeedArticle

TECHMEME_HOSTS = {"techmeme.com", "www.techmeme.com"}
SAFE_KEYS = [
    "id",
    "guid",
    "title",
    "link",
    "summary",
    "description",
    "content",
    "author",
    "authors",
    "tags",
    "category",
    "published",
    "updated",
    "published_parsed",
    "updated_parsed",
    "links",
    "comments",
    "source",
    "media_content",
    "media_thumbnail",
    "enclosures",
]


class TechmemeFeedClient:
    """High-level interface for working with the Techmeme RSS feed."""

    def __init__(
        self,
        *,
        feed_url: Optional[str] = None,
        timeout: Optional[int] = None,
        user_agent: Optional[str] = None,
        session: Optional[requests.Session] = None,
    ) -> None:
        feed_cfg = settings.feed # Đầu tiên lấy cấu hình feed từ settings(setting có chức năng trung tâm để truy cập cấu hình thời gian chạy)
        self._feed_url = feed_url or feed_cfg.url # Nếu không có feed_url được cung cấp, sử dụng URL mặc định từ cấu hình 
        # ví dụ nếu không có feed_url thì sử dụng "https://www.techmeme.com/feed.xml"
        self._timeout = timeout or feed_cfg.timeout
        self._session = session or self._make_session(timeout=self._timeout, user_agent=user_agent or feed_cfg.user_agent)
        if not hasattr(self._session, "request_timeout"):
            self._session.request_timeout = self._timeout 
            
    @staticmethod
    def _make_session(*, timeout: int, user_agent: str) -> requests.Session:
        session = requests.Session()
        session.headers.update({"User-Agent": user_agent})
        retry = Retry(
            total=3,
            backoff_factor=0.5,
            status_forcelist=(429, 500, 502, 503, 504),
            allowed_methods=frozenset({"GET", "HEAD"}),
        )
        session.mount("https://", HTTPAdapter(max_retries=retry))
        session.mount("http://", HTTPAdapter(max_retries=retry))
        session.request_timeout = timeout  # type: ignore[attr-defined]
        return session

    @property
    def feed_url(self) -> str:
        return self._feed_url

    @property
    def session(self) -> requests.Session:
        return self._session

    def fetch_raw_feed(self) -> feedparser.FeedParserDict:
        response = self._session.get(self._feed_url, timeout=self._timeout)
        response.raise_for_status() 
        return feedparser.parse(response.content)

    def fetch_articles(self, *, limit: Optional[int] = None) -> List[FeedArticle]:
        feed = self.fetch_raw_feed()
        normalized = normalize_feed(feed)
        if limit is not None:
            normalized = normalized[:limit]
        return [FeedArticle.from_dict(item) for item in normalized]

    def enrich_article(self, url: str) -> Dict[str, Any]:
        return enrich_from_article(self._session, url)


def is_external(href: str) -> bool:
    try:
        host = (urlparse(href).hostname or "").lower()
        return bool(host) and host not in TECHMEME_HOSTS and href.startswith(("http://", "https://"))
    except Exception:
        return False


def strip_tracking(url: str) -> str:
    """Remove common tracking query parameters from a URL."""
    try:
        parsed = urlparse(url)
        query = [
            (k, v)
            for (k, v) in parse_qsl(parsed.query, keep_blank_values=True)
            if not (k.startswith("utm_") or k in {"fbclid", "gclid", "mc_cid", "mc_eid"})
        ]
        return urlunparse(parsed._replace(query=urlencode(query)))
    except Exception:
        return url


def html_to_text(html: str) -> str:
    if not html:
        return ""
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup(["script", "style"]):
        tag.decompose()
    text = soup.get_text(" ", strip=True)
    return re.sub(r"\s+", " ", text).strip()


def to_iso8601(dt_like) -> str:
    try:
        if isinstance(dt_like, str):
            dt = parsedate_to_datetime(dt_like)
            if dt.tzinfo:
                return dt.astimezone(tz=None).isoformat()
            return dt.isoformat() + "Z"
        return time.strftime("%Y-%m-%dT%H:%M:%SZ", dt_like)
    except Exception:
        return time.strftime("%Y-%m-%dT%H:%M:%SZ")


def pick_links_from_entry_links(entry) -> Tuple[Optional[str], List[str]]:
    original = None
    related: List[str] = []
    for link in entry.get("links") or []:
        href = link.get("href")
        if not href:
            continue
        if is_external(href):
            cleaned = strip_tracking(href)
            if original is None:
                original = cleaned
            related.append(cleaned)
    seen: set[str] = set()
    unique_related = [x for x in related if not (x in seen or seen.add(x))]
    return original, unique_related


def pick_links_from_html(html: str) -> List[str]:
    if not html:
        return []
    soup = BeautifulSoup(html, "html.parser")
    output: List[str] = []
    for anchor in soup.find_all("a", href=True):
        href = anchor["href"]
        if is_external(href):
            output.append(strip_tracking(href))
    seen: set[str] = set()
    return [x for x in output if not (x in seen or seen.add(x))]


def extract_links(entry) -> Tuple[str, Optional[str], List[str]]:
    techmeme_url = entry.get("link") or entry.get("url") or ""
    original, related_from_links = pick_links_from_entry_links(entry)
    related_from_summary = pick_links_from_html(entry.get("summary") or entry.get("description") or "")

    related_from_content: List[str] = []
    if "content" in entry and entry["content"]:
        related_from_content = pick_links_from_html(entry["content"][0].get("value", ""))

    combined: List[str] = []
    seen: set[str] = set()
    for candidate in (related_from_links or []) + related_from_summary + related_from_content:
        if candidate not in seen:
            seen.add(candidate)
            combined.append(candidate)

    original_url = original or (combined[0] if combined else techmeme_url)
    return techmeme_url, original_url, combined


def as_plain_dict(entry: feedparser.FeedParserDict) -> Dict[str, Any]:
    output: Dict[str, Any] = {}
    for key in SAFE_KEYS:
        if key in entry:
            output[key] = entry[key]

    output["title"] = entry.get("title", "")
    output["techmeme_url"] = entry.get("link", "")

    output["summary_html"] = entry.get("summary", entry.get("description", ""))
    output["summary_text"] = html_to_text(output["summary_html"])

    if "content" in entry and entry["content"]:
        html = entry["content"][0].get("value", "")
        output["content_html"] = html
        output["content_text"] = html_to_text(html)

    if "tags" in entry:
        output["tags"] = [
            tag.get("term")
            for tag in entry["tags"]
            if isinstance(tag, dict) and "term" in tag
        ]

    if "media_content" in entry:
        output["media"] = [
            {"url": media.get("url"), "type": media.get("type")}
            for media in entry["media_content"]
            if isinstance(media, dict)
        ]
    elif "media_thumbnail" in entry:
        output["media"] = [
            {"url": media.get("url"), "type": "image"}
            for media in entry["media_thumbnail"]
            if isinstance(media, dict)
        ]

    if "enclosures" in entry:
        output["enclosures"] = [
            {"url": enclosure.get("href"), "type": enclosure.get("type")}
            for enclosure in entry["enclosures"]
            if isinstance(enclosure, dict)
        ]

    published = entry.get("published") or entry.get("updated")
    output["published_at"] = to_iso8601(published or time.gmtime())

    author = entry.get("author")
    if not author and isinstance(entry.get("authors"), list) and entry["authors"]:
        first_author = entry["authors"][0]
        author = first_author.get("name") if isinstance(first_author, dict) else str(first_author)
    output["author_name"] = author

    techmeme_url, original_url, related = extract_links(entry)
    output["techmeme_url"] = techmeme_url or output["techmeme_url"]
    output["original_url"] = original_url
    output["related_urls"] = related
    return output


def normalize_feed(feed: feedparser.FeedParserDict) -> List[Dict[str, Any]]:
    items: List[Dict[str, Any]] = []
    for entry in feed.entries:
        base = as_plain_dict(entry)
        digest = hashlib.sha256(
            (base.get("title", "") + base.get("techmeme_url", "")).encode()
        ).hexdigest()
        base.update({"source": "techmeme", "hash": digest})
        items.append(base)
    return items


def enrich_from_article(session: requests.Session, url: str) -> Dict[str, Any]:
    response = session.get(url, timeout=session.request_timeout)  # type: ignore[attr-defined]
    response.raise_for_status()
    soup = BeautifulSoup(response.text, "html.parser")

    meta: Dict[str, Any] = {}
    for tag in soup.select('meta[property^="og:"]'):
        prop = tag.get("property")
        if prop:
            meta[prop] = tag.get("content")
    for tag in soup.select('meta[name^="twitter:"]'):
        name = tag.get("name")
        if name:
            meta[name] = tag.get("content")

    ld_blocks: List[Any] = []
    for script in soup.find_all("script", type="application/ld+json"):
        try:
            data = script.string
            if data:
                ld_blocks.append(json.loads(data))
        except Exception:
            pass

    enriched: Dict[str, Any] = {
        "og_title": meta.get("og:title"),
        "og_description": meta.get("og:description"),
        "og_image": meta.get("og:image"),
        "og_site_name": meta.get("og:site_name"),
        "twitter_title": meta.get("twitter:title"),
        "twitter_description": meta.get("twitter:description"),
        "twitter_image": meta.get("twitter:image"),
        "ld_json": ld_blocks[:2],
    }

    try:
        for block in ld_blocks:
            if isinstance(block, dict) and block.get("@type") in {"Article", "NewsArticle", "BlogPosting"}:
                author = block.get("author")
                if author and "author" not in enriched:
                    if isinstance(author, dict):
                        enriched["author"] = author.get("name")
                    elif isinstance(author, list) and author and isinstance(author[0], dict):
                        enriched["author"] = author[0].get("name")
                if "datePublished" in block and "date_published" not in enriched:
                    enriched["date_published"] = block["datePublished"]
                if "headline" in block and not enriched.get("og_title"):
                    enriched["og_title"] = block["headline"]
    except Exception:
        pass

    return enriched

# file này gồm những class là:
# TechmemeFeedClient: Lớp này cung cấp các phương thức để lấy và xử lý dữ liệu từ nguồn cấp RSS của Techmeme.
# Các hàm hỗ trợ: Bao gồm các hàm như is_external, strip_tracking, html_to_text, to_iso8601, pick_links_from_entry_links, pick_links_from_html, extract_links, as_plain_dict, normalize_feed, enrich_from_article. 
# Các hàm này giúp xử lý và trích xuất thông tin từ các mục trong nguồn cấp dữ liệu, bao gồm việc làm sạch URL, chuyển đổi HTML thành văn bản thuần túy, chuẩn hóa định dạng ngày tháng,
#  và trích xuất các liên kết và siêu dữ liệu từ bài viết.
# Các hàm này hỗ trợ việc lấy và xử lý dữ liệu từ Techmeme một cách hiệu quả và có cấu trúc.
# Các hàm này có thể được sử dụng để xây dựng các ứng dụng hoặc dịch vụ liên quan đến việc thu thập và phân tích tin tức công nghệ từ Techmeme.
# Tóm lại, file này cung cấp một bộ công cụ để tương tác với nguồn cấp