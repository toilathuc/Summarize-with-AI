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
    // p(95) < 2s cho toàn hệ thống (cả read + refresh)
    http_req_duration: ["p(95)<2000"],
  },

  // Chống spam socket → giảm TIME_WAIT khi test localhost
  noConnectionReuse: false,
  discardResponseBodies: true,
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
  const num = Math.random();

  if (num <= 0.5) {
    // ~50% request là refresh
    http.post(`${BASE_URL}/api/refresh`);
  } else {
    // ~50% request là đọc summaries
    http.get(`${BASE_URL}/api/summaries`);
  }

  // 1 request / VU / giây → RPS ổn định, dễ so sánh before/after
  sleep(1);
}

// PHẦN QUAN TRỌNG CHO REPORTER
export function handleSummary(data) {
  console.log("Preparing the end-of-test summary (mix-test)...");
  return {
    "report/raw/mix-test.json": JSON.stringify(data),
  };
}