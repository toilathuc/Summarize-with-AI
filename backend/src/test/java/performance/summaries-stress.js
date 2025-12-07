import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 20 },   // warm up
    { duration: '10s', target: 50 },  // load nhẹ (SQLite mạnh nhất ở đây)
    { duration: '10s', target: 80 },  // thử giới hạn
    { duration: '10s', target: 100 }, // đẩy nhẹ (max realistic)
    { duration: '5s', target: 0 },    // cooldown
  ],

  thresholds: {
    http_req_failed: ['rate<0.20'],        // SQLite load >100 VUs sẽ có fail 10–20%
    http_req_duration: ['p(95)<2000'],     // p95 < 2s
  },

  // Chống spam socket → giảm TIME_WAIT khi test localhost
  noConnectionReuse: false,
  discardResponseBodies: true,
};

export default function () {
  http.get('http://localhost:8080/api/summaries');
  sleep(1);   // 1 request / VU / second → RPS ổn định và sát thực tế
}

// PHẦN QUAN TRỌNG CHO REPORTER
export function handleSummary(data) {
  console.log('Preparing the end-of-test summary...');
  return {
    // đường dẫn file JSON mà reporter sẽ đọc
    'report/raw/summaries-stress.json': JSON.stringify(data),
  };
}