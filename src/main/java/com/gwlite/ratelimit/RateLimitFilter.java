package com.gwlite.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.gwlite.monitoring.AppMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Fixes drawback #5 (rate limiting half): a simple fixed-window counter
 * per user (or IP, if unauthenticated) using Redis INCR + EXPIRE.
 *
 * Trade-off worth knowing: fixed-window counters allow a burst of up to
 * 2x the limit right at the window boundary (e.g. 100 requests at 0:59 and
 * another 100 at 1:00). A sliding-window-log or token-bucket algorithm
 * avoids that at the cost of more Redis calls. Fixed-window is the right
 * choice here because it's simple, cheap, and good enough for abuse
 * prevention rather than precise billing-grade throttling.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final AppMetrics appMetrics;

    private static final int LIMIT_PER_MINUTE = 100;
    private static final long WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String identity = resolveIdentity(request);
        String key = "ratelimit:" + identity + ":" + (System.currentTimeMillis() / (WINDOW_SECONDS * 1000));

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count > LIMIT_PER_MINUTE) {
            appMetrics.recordRateLimitRejection();
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again shortly.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveIdentity(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Using the raw token as the bucket key is sufficient here since it's
            // unique per session; in production you'd extract just the user id.
            return "user:" + authHeader.substring(7).hashCode();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
