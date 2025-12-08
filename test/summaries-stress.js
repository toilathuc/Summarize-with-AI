import http from "k6/http";
import { sleep } from "k6";

export const options = {
  stages: [
    { duration: "5s", target: 20 }, // warm up
    { duration: "10s", target: 50 }, // load nhẹ
    { duration: "10s", target: 80 }, // thử giới hạn
    { duration: "10s", target: 100 }, // max realistic
    { duration: "5s", target: 0 }, // cooldown
  ],

  thresholds: {
    http_req_failed: ["rate<0.01"], // error < 1%
    http_req_duration: ["p(95)<1000"], // p95 < 1s
  },

  noConnectionReuse: false,
  discardResponseBodies: true,
};

// FastAPI đang chạy port 8000
const BASE_URL = __ENV.BASE_URL || "http://localhost:8000";

export default function () {
  http.get(`${BASE_URL}/api/summaries`);
  sleep(1); // 1 request / VU / giây
}

// Ghi dữ liệu JSON cho k6-html-reporter
export function handleSummary(data) {
  console.log("Preparing summary for FastAPI /api/summaries...");
  return {
    "report/raw/fastapi-summaries-stress.json": JSON.stringify(data),
  };
}
