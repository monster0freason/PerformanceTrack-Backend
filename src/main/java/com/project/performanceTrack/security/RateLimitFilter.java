package com.project.performanceTrack.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Map of IP address -> list of request timestamps
    private final Map<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isLoginRequest(request)) {
            String ip = request.getRemoteAddr();

            if (isRateLimited(ip)) {
                log.warn("Rate limit exceeded for IP: {} on POST /api/v1/auth/login", ip);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":\"error\",\"msg\":\"Too many login attempts. Try again later.\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI());
    }

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = requestCounts.computeIfAbsent(ip, k -> new ArrayList<>());

        synchronized (timestamps) {
            // Remove timestamps older than 1 minute
            timestamps.removeIf(t -> now - t > WINDOW_MS);

            if (timestamps.size() >= MAX_ATTEMPTS) {
                return true;
            }

            timestamps.add(now);
            return false;
        }
    }
}
