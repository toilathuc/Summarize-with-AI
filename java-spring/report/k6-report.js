const reporter = require('k6-html-reporter');
const fs = require('fs');
const path = require('path');

// Define input and output paths
const rawDir = path.join(__dirname, 'raw');
const htmlDir = path.join(__dirname, 'html');

// Ensure output directory exists
if (!fs.existsSync(htmlDir)){
    fs.mkdirSync(htmlDir, { recursive: true });
}

// List of test files to process
const tests = ['summaries-stress', 'refresh-stress', 'mix-test'];

tests.forEach(testName => {
    const jsonPath = path.join(rawDir, `${testName}.json`);
    // k6-html-reporter creates a folder with index.html inside
    const outputDir = path.join(htmlDir, testName);

    if (fs.existsSync(jsonPath)) {
        const options = {
            jsonFile: jsonPath,
            output: outputDir,
        };

        reporter.generateSummaryReport(options);
        console.log(`✅ Generated report for ${testName} at ${outputDir}/report.html`);
    } else {
        console.warn(`⚠️ Skipping ${testName}: JSON file not found at ${jsonPath}`);
    }
});