package unit;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.utils.ContentHashUtils;
import com.example.summarizer.utils.DiffUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffUtilsTest {

    @Test
    void diffDetectsNewUpdatedAndSkipped() {
        FeedArticle old = new FeedArticle("Old title", "https://example.com/a", "desc v1", "content v1", true);
        String hashOld = ContentHashUtils.contentHash(String.join("|",
                old.getUrl(), old.getTitle(), old.getDescription(), old.getContent()));

        Map<String, String> seen = Map.of(old.getUrl(), hashOld);

        FeedArticle unchanged = new FeedArticle("Old title", "https://example.com/a", "desc v1", "content v1", true);
        FeedArticle updated = new FeedArticle("New title", "https://example.com/a", "desc v1", "content v1", false);
        FeedArticle brandNew = new FeedArticle("Brand new", "https://example.com/c", "desc v0", "", false);

        DiffUtils.DiffResult result = DiffUtils.diff(
                List.of(unchanged, updated, brandNew),
                seen
        );

        assertEquals(1, result.skippedCount(), "unchanged should be skipped");
        assertEquals(1, result.updatedCount(), "title change should be updated");
        assertEquals(1, result.newCount(), "new URL should be new");
        assertTrue(result.newHashes().containsKey("https://example.com/a"));
        assertTrue(result.newHashes().containsKey("https://example.com/c"));
    }
}
