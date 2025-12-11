import http from "k6/http";
import { sleep } from "k6";

export const options = {
  stages: [
    { duration: "5s", target: 20 }, // warm up
    { duration: "10s", target: 50 }, // load nhẹ
    { duration: "10s", target: 80 }, // thử giới hạn
    { duration: "10s", target: 100 }, // đẩy max realistic
    { duration: "5s", target: 0 }, // cooldown
  ],

  thresholds: {
    // Mục tiêu cuối cùng: error < 5%
    http_req_failed: ["rate<0.05"],
    // p(95) < 2s cho load trang
    http_req_duration: ["p(95)<2000"],
  },

  // Chống spam socket → giảm TIME_WAIT khi test localhost
  noConnectionReuse: false,
  discardResponseBodies: true,
};

// Mặc định trỏ về Vite dev server (5173).
// QUAN TRỌNG: Bạn phải chạy frontend (npm run dev) trước khi chạy test này!
// Lưu ý: k6 mặc định dùng IPv4 (127.0.0.1), trong khi Vite có thể bind vào IPv6 (::1).
// Nếu gặp lỗi "connectex", hãy thử đổi localhost thành 127.0.0.1 hoặc cấu hình Vite host: true
const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:5173";

export default function () {
  // Giả lập người dùng truy cập trang chủ
  http.get(`${BASE_URL}/`);

  // 1 request / VU / giây
  sleep(1);
}

// PHẦN QUAN TRỌNG CHO REPORTER
export function handleSummary(data) {
  console.log("Preparing the end-of-test summary (frontend-load)...");
  return {
    "report/raw/frontend-load.json": JSON.stringify(data),
  };
}
