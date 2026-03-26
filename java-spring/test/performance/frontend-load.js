import http from "k6/http";
import { sleep } from "k6";

export const options = {
  stages: [
    { duration: "5s", target: 20 }, 
    { duration: "10s", target: 50 }, 
    { duration: "10s", target: 80 }, 
    { duration: "10s", target: 100 }, 
    { duration: "5s", target: 0 }, 
  ],

  thresholds: {
    
    http_req_failed: ["rate<0.05"],
    
    http_req_duration: ["p(95)<2000"],
  },

  
  noConnectionReuse: false,
  discardResponseBodies: true,
};


const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
  
  http.get(`${BASE_URL}/`);

  
  sleep(1);
}


export function handleSummary(data) {
  console.log("Preparing the end-of-test summary (frontend-load)...");
  return {
    "report/raw/frontend-load.json": JSON.stringify(data),
  };
}
