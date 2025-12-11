const { generateSummaryReport } = require('k6-html-reporter');
const fs = require('fs');

const jsonFiles = [
  'report/raw/refresh-stress.json',
  'report/raw/summaries-stress.json',
  'report/raw/mix-test.json',
  'report/raw/frontend-load.json',
];

jsonFiles.forEach((file) => {
  const fileName = file.split('/').pop().replace('.json', '');
  const outputDir = `report/html/${fileName}`;

  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  generateSummaryReport({
    jsonFile: file,
    output: outputDir,
  });

  console.log(`✔ Created report for: ${file} → ${outputDir}`);
});
