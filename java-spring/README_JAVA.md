Java Spring Boot skeleton for Summarize-with-AI

This folder contains a minimal Spring Boot (Maven) skeleton targeting JDK 21.

What is included:

- pom.xml with Spring Boot starter web
- Basic controller exposing `/api/summaries` (reads `data/outputs/summaries.json`)
- Correlation ID servlet filter that reads `X-Correlation-ID` or generates one and injects into logs

How to build (Windows PowerShell):

```powershell
cd java-spring
mvn -v
mvn -DskipTests package
java -jar target/summarizer-0.0.1-SNAPSHOT.jar
```

Notes:

- The service reads the JSON file at `data/outputs/summaries.json` relative to the workspace root. Keep the same path as the Python project.
- Gemini client and other services are not yet ported; this is an incremental scaffold.
