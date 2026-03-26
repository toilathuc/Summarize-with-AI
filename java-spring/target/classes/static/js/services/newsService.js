const SUMMARIES_API = "/api/summaries"; 
const REFRESH_API = "/api/refresh";





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

export async function triggerFullRefresh(top = 20) {
  const url = `${REFRESH_API}?top=${encodeURIComponent(top)}`;
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(
      `Refresh endpoint failed (${response.status}) ${text || ""}`.trim()
    );
  }

  return response.json();
}


export async function fetchNewsData() {
  const apiData = await refreshNewsFast();
  return {
    items: Array.isArray(apiData.items) ? apiData.items : [],
    lastUpdated: apiData.lastUpdated,
    raw: apiData,
  };
}
