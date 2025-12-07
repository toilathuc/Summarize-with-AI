package unit;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.service.SummarizationOrchestrator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BÀI TEST CHỨNG MINH SỨC MẠNH CỦA HEXAGONAL
 * 
 * Mục tiêu: Chứng minh ta có thể test toàn bộ luồng xử lý (Orchestrator)
 * mà KHÔNG CẦN:
 * 1. Kết nối Internet (Không gọi Google Gemini thật)
 * 2. Không cần Spring Boot (Chạy cực nhanh)
 * 3. Dễ dàng giả lập lỗi để test tính năng Fallback
 */
public class HexagonalArchitectureProofTest {

    // 1. Tạo một Adapter giả (Fake Adapter) - Điều cực dễ trong Hexagonal
    // Trong kiến trúc cũ, việc này khó hơn nhiều vì code dính chặt vào thư viện cụ thể.
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
            // Giả lập AI trả về JSON đúng chuẩn
            return """
                {
                    "summaries": [
                        {
                            "title": "Fake Title",
                            "url": "http://test.com",
                            "bullets": ["Fake summary bullet 1", "Fake summary bullet 2"],
                            "why_it_matters": "Fake reason",
                            "type": "news"
                        }
                    ]
                }
                """;
        }
    }

    @Test
    void proof_SystemWorks_Without_Real_AI() throws Exception {
        // GIVEN: Một bài báo giả
        FeedArticle article = new FeedArticle();
        article.setTitle("Test Article");
        article.setUrl("http://test.com");

        // VÀ: Một Adapter giả (AI hoạt động tốt)
        SummarizerPort fakeAi = new FakeAiAdapter(false);
        
        // KHI: Chạy logic chính (Orchestrator)
        // Lưu ý: Chúng ta khởi tạo class logic mà không cần Spring (@Autowired)
        // Đây là điểm mạnh: Logic độc lập hoàn toàn.
        SummarizationOrchestrator orchestrator = new SummarizationOrchestrator(
                fakeAi, 
                "Fake Prompt Template", 
                1 // Batch size
        );

        // THÌ: Kết quả trả về phải đúng như mong đợi
        // (Hàm summarize trả về List<SummaryResult>)
        List<SummaryResult> results = orchestrator.summarize(List.of(article));
        
        assertEquals(1, results.size());
        assertEquals("Fake summary bullet 1", results.get(0).getBullets().get(0));
        System.out.println("✅ Test 1 Passed: Logic hoạt động tốt với AI giả lập.");
    }

    @Test
    void proof_SystemHandlesError_Gracefully() throws Exception {
        // GIVEN: AI bị lỗi (Simulated)
        SummarizerPort brokenAi = new FakeAiAdapter(true);
        
        SummarizationOrchestrator orchestrator = new SummarizationOrchestrator(
                brokenAi, 
                "Fake Prompt Template", 
                1
        );

        FeedArticle article = new FeedArticle();
        article.setUrl("http://error.com");
        article.setTitle("Error Article");

        // KHI: Gọi hàm tóm tắt
        List<SummaryResult> results = orchestrator.summarize(List.of(article));

        // THÌ: Hệ thống phải trả về Fallback thay vì sập
        assertEquals(1, results.size());
        assertTrue(results.get(0).getBullets().get(0).contains("⚠️ Fallback: Summary unavailable"));
        System.out.println("✅ Test 2 Passed: Hệ thống phát hiện lỗi AI và dùng Fallback đúng như thiết kế.");
    }
}
