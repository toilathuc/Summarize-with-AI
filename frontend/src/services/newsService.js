const SUMMARIES_API = "/api/summaries";
const REFRESH_API = "/api/refresh";
const STATUS_API = "/api/refresh/status";

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
    };
  } catch (error) {
    console.error(error);
    throw error;
  }
}

export async function triggerFullRefresh(top = 20) {
  const url = `${REFRESH_API}?top=${encodeURIComponent(top)}`;
  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });

  const payload = await safeJson(res);

  if (res.status === 429) {
    return {
      status: "rate_limited",
      scope: payload?.scope || "refresh",
      retry_after_seconds: payload?.retry_after_seconds,
    };
  }

  if (res.status === 202) {
    return { status: "running" };
  }

  if (!res.ok) {
    throw new Error(
      `Refresh endpoint failed (${res.status}) ${JSON.stringify(payload)}`
    );
  }

  return payload;
}

export async function fetchNewsData() {
  const apiData = await refreshNewsFast();
  return {
    items: Array.isArray(apiData.items) ? apiData.items : [],
    lastUpdated: apiData.lastUpdated,
    raw: apiData,
  };
}

export async function fetchRefreshStatus() {
  try {
    const res = await fetch(STATUS_API);
    if (!res.ok) {
      throw new Error(`Status HTTP ${res.status}`);
    }
    const data = await res.json();
    return {
      running: Boolean(data.running),
      reason: data.reason || "unknown",
      lastRunAt: data.lastRunAt,
    };
  } catch (err) {
    console.error(err);
    return {
      running: false,
      reason: "unknown",
      lastRunAt: null,
      error: true,
    };
  }
}

async function safeJson(res) {
  try {
    return await res.json();
  } catch {
    return null;
  }
}
