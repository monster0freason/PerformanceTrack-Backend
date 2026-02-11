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

/*
 * This is a security filter that prevents brute-force login attacks.
 * Think of it like a bouncer at a club - if someone tries to login
 * too many times too quickly, we temporarily block them.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /*
     * This is our memory of who tried to login and when.
     * We store each IP address (like "192.168.1.1") and a list of timestamps
     * showing exactly when they made each login attempt.
     * ConcurrentHashMap is used because multiple users might be logging in at the same time.
     */
    private final Map<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

    /*
     * These are our security rules:
     * MAX_ATTEMPTS = 5 means you get 5 tries
     * WINDOW_MS = 60,000 milliseconds (1 minute) is how long we remember your attempts
     * So basically: "You can only try logging in 5 times per minute"
     */
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;

    /*
     * This method runs automatically for every request that comes to our server.
     * It's like a checkpoint - every request has to pass through here first.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        /*
         * First, we check: "Is this a login attempt?"
         * We only care about rate limiting login requests, not other stuff like
         * viewing a profile or loading images.
         */
        if (isLoginRequest(request)) {

            /*
             * Get the IP address of whoever is trying to login.
             * This is like checking the return address on a letter.
             */
            String ip = request.getRemoteAddr();

            /*
             * Now check: "Has this IP address been trying too many times?"
             */
            if (isRateLimited(ip)) {

                /*
                 * BLOCKED! They've exceeded the limit.
                 * We log it for security monitoring and send back an error response.
                 */
                log.warn("Rate limit exceeded for IP: {} on POST /api/v1/auth/login", ip);

                /*
                 * Send back HTTP status 429 which means "Too Many Requests"
                 * and include a friendly JSON message explaining what happened.
                 */
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":\"error\",\"msg\":\"Too many login attempts. Try again later.\",\"data\":null}");
                return;
            }
        }

        /*
         * If we get here, everything is okay - either it wasn't a login request,
         * or it was but they haven't hit the limit yet.
         * So we let the request continue to its destination.
         */
        filterChain.doFilter(request, response);
    }

    /*
     * This helper method checks if the current request is a login attempt.
     * We're looking for two things:
     * 1. The HTTP method is POST (not GET or PUT or anything else)
     * 2. The URL path is exactly "/api/v1/auth/login"
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI());
    }

    /*
     * This is where the magic happens - we check if an IP address has made
     * too many login attempts in the last minute.
     */
    private boolean isRateLimited(String ip) {

        /*
         * Get the current time in milliseconds.
         * This is like checking your watch right now.
         */
        long now = System.currentTimeMillis();

        /*
         * Get the list of timestamps for this IP address.
         * If this is their first attempt, create a new empty list for them.
         */
        List<Long> timestamps = requestCounts.computeIfAbsent(ip, k -> new ArrayList<>());

        /*
         * We use synchronized here because multiple requests from the same IP
         * might arrive at almost the same time, and we need to handle them
         * one at a time to avoid counting errors.
         */
        synchronized (timestamps) {

            /*
             * Clean up old timestamps.
             * If someone tried to login 2 minutes ago, we don't care anymore.
             * We only keep timestamps from the last 60 seconds.
             * This is like erasing old tally marks from a whiteboard.
             */
            timestamps.removeIf(t -> now - t > WINDOW_MS);

            /*
             * Now check: do they already have 5 attempts in the last minute?
             * If yes, they're blocked!
             */
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return true;
            }

            /*
             * They're still under the limit, so we:
             * 1. Record this new attempt by adding the current timestamp
             * 2. Let them through by returning false (not rate limited)
             */
            timestamps.add(now);
            return false;
        }
    }
}