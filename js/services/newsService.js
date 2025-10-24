import { delay } from "../utils/delay.js";

const SUMMARY_PATH = "./summaries.json";
const SUMMARIES_API = "/api/summaries"; // New fast endpoint
const REFRESH_ENDPOINT = "/api/refresh";
const REFRESH_STATUS_ENDPOINT = "/api/refresh/status";

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
 * Legacy fetch from static file - kept for compatibility.
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

/**
 * Trigger full backend update - fetches from Techmeme + AI summarization.
 * This is slow (30-90 seconds) and should only be used when user explicitly
 * requests fresh data from the source.
 */
export async function triggerBackendUpdate({
  onProgress,
  pollInterval = 2000,
  maxAttempts = 120,
  minDuration = 2000,
} = {}) {
  const startTime = Date.now();

  try {
    onProgress?.("Đang kết nối với server...");

    const refreshResponse = await fetch(REFRESH_ENDPOINT, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    if (!refreshResponse.ok) {
      const refreshResult = await refreshResponse.json().catch(() => ({}));
      const message =
        refreshResult?.detail ||
        refreshResult?.message ||
        `Server error: ${refreshResponse.status}`;
      throw new Error(message);
    }

    const refreshResult = await refreshResponse.json();
    console.log("Refresh started:", refreshResult);

    onProgress?.("Đang lấy dữ liệu mới từ Techmeme...");

    let attempts = 0;
    let status = null;

    while (attempts < maxAttempts) {
      await delay(pollInterval);
      attempts += 1;

      try {
        const statusResponse = await fetch(REFRESH_STATUS_ENDPOINT);
        if (!statusResponse.ok) {
          console.warn(`Status check failed: ${statusResponse.status}`);
          continue;
        }

        status = await statusResponse.json();
        console.log(`Status check (attempt ${attempts}):`, status);

        // Check if job completed (either success or failure)
        if (status && status.completed === true) {
          console.log("Job completed:", status);
          break;
        }

        // Update progress message
        onProgress?.(getProgressMessage(attempts));
      } catch (error) {
        console.warn(`Status polling error (attempt ${attempts}):`, error);
        // Ignore transient polling errors and keep trying
      }
    }

    // Validate final status
    if (!status) {
      throw new Error("Không thể lấy trạng thái cập nhật từ server");
    }

    if (!status.completed) {
      throw new Error(
        "Quá trình cập nhật mất quá nhiều thời gian. Vui lòng thử lại sau."
      );
    }

    if (!status.success) {
      const errorMsg = status.error || "Cập nhật thất bại trên server";
      const error = new Error(errorMsg);
      error.detail = status.error;
      throw error;
    }
  } catch (error) {
    console.error("Refresh error:", error);
    throw error;
  }

  onProgress?.("Đang tải dữ liệu đã cập nhật...");

  const data = await fetchNewsData({ cacheBust: true });

  const elapsed = Date.now() - startTime;
  if (elapsed < minDuration) {
    onProgress?.("Đang hoàn tất...");
    await delay(minDuration - elapsed);
  }

  return { data, status };
}

/**
 * Backward compatibility alias - calls the slow backend update.
 * Use refreshNewsFast() for instant UI refresh instead.
 */
export async function refreshNews(options) {
  return triggerBackendUpdate(options);
}

function getProgressMessage(attempt) {
  if (attempt < 10) {
    return "Đang xử lý dữ liệu với AI...";
  }
  if (attempt < 20) {
    return "Đang tạo tóm tắt nội dung...";
  }
  return "Đang hoàn thiện dữ liệu...";
}
