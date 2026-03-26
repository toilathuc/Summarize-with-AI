package unit;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.service.SummarizationOrchestrator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HexagonalArchitectureProofTest {

    static class FakeAiAdapter implements SummarizerPort {
        private final boolean shouldFail;

        public FakeAiAdapter(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public String generate(String prompt) throws Exception {
            if (shouldFail) {
                throw new RuntimeException("AI Service Down");
            }
            return "{\"summaries\":[{\"title\":\"Fake Title\",\"url\":\"http://test.com\",\"bullets\":[\"Fake summary bullet 1\",\"Fake summary bullet 2\"],\"why_it_matters\":\"Fake reason\",\"type\":\"news\"}]}";
        }
    }

    @Test
    void proof_SystemWorks_Without_Real_AI() throws Exception {
        FeedArticle article = new FeedArticle();
        article.setTitle("Test Article");
        article.setUrl("http://test.com");

        SummarizerPort fakeAi = new FakeAiAdapter(false);
        SummarizationOrchestrator orchestrator = new SummarizationOrchestrator(
                fakeAi,
                null,
                "Fake Prompt Template",
                1
        );

        List<SummaryResult> results = orchestrator.summarize(List.of(article));

        assertEquals(1, results.size());
        assertEquals("Fake summary bullet 1", results.get(0).getBullets().get(0));
        System.out.println("Test 1 passed: logic works with fake AI.");
    }

    @Test
    void proof_SystemHandlesError_Gracefully() throws Exception {
        SummarizerPort brokenAi = new FakeAiAdapter(true);
        SummarizationOrchestrator orchestrator = new SummarizationOrchestrator(
                brokenAi,
                null,
                "Fake Prompt Template",
                1
        );

        FeedArticle article = new FeedArticle();
        article.setUrl("http://error.com");
        article.setTitle("Error Article");

        List<SummaryResult> results = orchestrator.summarize(List.of(article));

        assertEquals(1, results.size());
        assertTrue(results.get(0).getBullets().get(0).contains("Fallback: Summary unavailable"));
        System.out.println("Test 2 passed: AI error falls back correctly.");
    }
}
