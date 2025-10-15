import { delay } from "../utils/delay.js";

const SUMMARY_PATH = "./summaries.json";
const REFRESH_ENDPOINT = "/api/refresh";
const REFRESH_STATUS_ENDPOINT = "/api/refresh/status";

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

export async function refreshNews({
  onProgress,
  pollInterval = 5000,
  maxAttempts = 60,
  minDuration = 2000,
} = {}) {
  const startTime = Date.now();

  onProgress?.("Đang kết nối với server...");

  const refreshResponse = await fetch(REFRESH_ENDPOINT);
  const refreshResult = await refreshResponse.json().catch(() => ({}));

  if (!refreshResponse.ok) {
    const message =
      (refreshResult && refreshResult.message) || "Failed to start refresh";
    throw new Error(message);
  }

  onProgress?.("Đang lấy dữ liệu mới từ Techmeme...");

  let attempts = 0;
  let status = null;

  while (attempts < maxAttempts) {
    await delay(pollInterval);
    attempts += 1;

    try {
      const statusResponse = await fetch(REFRESH_STATUS_ENDPOINT);
      if (!statusResponse.ok) {
        continue;
      }

      status = await statusResponse.json();

      if (status.completed) {
        break;
      }

      onProgress?.(getProgressMessage(attempts));
    } catch {
      // Ignore transient polling errors and keep trying
    }
  }

  if (!status || !status.completed) {
    throw new Error("Refresh timeout - taking too long");
  }

  if (!status.success) {
    const error = new Error("Refresh failed on server");
    if (status.error) {
      error.detail = status.error;
    }
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

function getProgressMessage(attempt) {
  if (attempt < 10) {
    return "Đang xử lý dữ liệu với AI...";
  }
  if (attempt < 20) {
    return "Đang tạo tóm tắt nội dung...";
  }
  return "Đang hoàn thiện dữ liệu...";
}
