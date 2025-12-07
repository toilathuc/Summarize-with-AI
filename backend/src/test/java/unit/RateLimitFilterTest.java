package unit;

import com.example.summarizer.config.RateLimitFilter;
import com.example.summarizer.service.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitService rl;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        rl = mock(RateLimitService.class);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void refresh_should_be_rate_limited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(rl, 100, 5, 60);

        // Giả lập request
        when(req.getRemoteAddr()).thenReturn("1.1.1.1");
        when(req.getRequestURI()).thenReturn("/api/refresh");

        // Giả lập logic rate limit
        when(rl.allow("ip:1.1.1.1:refresh", 5, 60)).thenReturn(false);
        when(rl.getRetryAfter("ip:1.1.1.1:refresh")).thenReturn(30L);

        // FIX: Mock writer
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(res.getWriter()).thenReturn(pw);

        // RUN
        filter.doFilter(req, res, chain);

        // VERIFY
        verify(res).setStatus(429);
        verify(chain, never()).doFilter(req, res);

        pw.flush();
        assert(sw.toString().contains("\"scope\":\"refresh\""));
    }

    @Test
    void global_limit_should_apply() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(rl, 2, 5, 60);

        // Giả lập request
        when(req.getRemoteAddr()).thenReturn("7.7.7.7");
        when(req.getRequestURI()).thenReturn("/api/summaries");

        // Giả lập logic rate limit
        when(rl.allow("ip:7.7.7.7:all", 2, 60)).thenReturn(false);
        when(rl.getRetryAfter("ip:7.7.7.7:all")).thenReturn(10L);

        // FIX: Mock writer
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(res.getWriter()).thenReturn(pw);

        // RUN
        filter.doFilter(req, res, chain);

        // VERIFY
        verify(res).setStatus(429);
        verify(chain, never()).doFilter(req, res);

        pw.flush();
        assert(sw.toString().contains("\"scope\":\"global\""));
    }
}
