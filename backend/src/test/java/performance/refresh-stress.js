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
    // Job refresh nặng hơn → cho phép error cao hơn 5% một chút
    http_req_failed: ["rate<0.10"],
    // Refresh có thể lâu hơn, p(95) < 5s
    http_req_duration: ["p(95)<5000"],
  },

  noConnectionReuse: false,
  discardResponseBodies: true,
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
  http.post(`${BASE_URL}/api/refresh`);
  // 1 request / VU / giây → mô phỏng nhiều client cùng bấm "refresh"
  sleep(1);
}

// PHẦN QUAN TRỌNG CHO REPORTER
export function handleSummary(data) {
  console.log("Preparing the end-of-test summary (refresh-stress)...");
  return {
    "report/raw/refresh-stress.json": JSON.stringify(data),
  };
}
