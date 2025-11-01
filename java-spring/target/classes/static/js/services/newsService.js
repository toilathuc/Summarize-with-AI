const SUMMARY_PATH = "./summaries.json";
const SUMMARIES_API = "/api/summaries"; // Fast endpoint served by FastAPI

// Slow `/api/refresh` endpoints were retired. Admins should run
// `python update_news.py` (or the Windows batch) whenever they need fresh data.

/**
 * Fast refresh - reload data from file without triggering backend update.
 * Returns in <1 second with freshness metadata.
 */
export async function refreshNewsFast() {
  try {
    const response = await fetch(SUMMARIES_API);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    return {
      items: Array.isArray(data.items) ? data.items : [],
      lastUpdated: data.last_updated,
      isStale: data.is_stale,
      freshness: data.freshness,
      count: data.count,
      correlationId: data.correlation_id,
    };
  } catch (error) {
    console.error("Fast refresh error:", error);
    throw error;
  }
}

/**
 * Legacy fetch from static file - kept for compatibility with existing scripts.
 */
export async function fetchNewsData({ cacheBust = false } = {}) {
  const url = cacheBust ? `${SUMMARY_PATH}?t=${Date.now()}` : SUMMARY_PATH;

  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  return {
    items: Array.isArray(data.items) ? data.items : [],
    lastUpdated: data.last_updated || data.lastUpdated || null,
    raw: data,
  };
}