// report/k6-report.js
const htmlReport = require("k6-html-reporter");
const path = require("path");

const options = {
  // JSON mà k6 đã ghi ra (đường dẫn tính từ project root)
  jsonFile: path.join(__dirname, "raw", "fastapi-summaries-stress.json"),
  // output có thể là file .html hoặc folder, lib sẽ tự xử lý
  output: path.join(__dirname, "fastapi-summaries-report.html"),
};

htmlReport.generateSummaryReport(options);
