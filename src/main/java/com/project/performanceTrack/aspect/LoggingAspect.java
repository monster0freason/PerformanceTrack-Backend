package com.project.performanceTrack.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * This class is like a security camera system for your application.
 * It automatically watches and logs what happens when methods are called,
 * without you having to write logging code in every single method.
 *
 * It uses AOP (Aspect-Oriented Programming), which is a fancy way of saying:
 * "I can inject behavior (logging) into existing methods without touching their code."
 *
 * Think of it as a spy that sits between method calls and records what's happening.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /*
     * This is our security blacklist - parameter names that contain sensitive info
     * that should NEVER be logged (even in debug mode).
     *
     * Imagine if your logs showed "password=secret123" - that would be a huge security risk!
     * So whenever we see these words, we replace the actual value with "***".
     */
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "passwordHash", "oldPwd", "newPwd",
            "token", "jwt", "secret", "authorization"
    );

    /*
     * --- POINTCUTS ---
     * Pointcuts are like setting up motion detectors.
     * They define WHERE we want our logging to trigger.
     * We don't execute anything here, just mark the locations.
     */

    /*
     * This pointcut says: "I want to watch ALL methods in ALL controller classes"
     * The ".." means "any class in this package and its subpackages"
     * The ".*" at the end means "any method name"
     * The "(..)" means "with any parameters"
     */
    @Pointcut("execution(* com.project.performanceTrack.controller..*(..))")
    public void controllerMethods() {}

    /*
     * This pointcut says: "I want to watch ALL methods in ALL service classes"
     * Services typically contain business logic, so it's useful to log what happens there.
     */
    @Pointcut("execution(* com.project.performanceTrack.service..*(..))")
    public void serviceMethods() {}

    /*
     * --- CONTROLLER LOGGING ---
     *
     * This method wraps around every controller method call.
     * @Around means: "I want to run code BEFORE and AFTER the actual method executes"
     *
     * Controllers handle HTTP requests, so we log at INFO level (always visible in production).
     * We keep it simple here - just log the request and how long it took.
     */
    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {

        /*
         * Get the name of the class and method being called.
         * For example: "UserController" and "login"
         */
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        /*
         * Try to get the HTTP details (like "POST /api/v1/auth/login")
         * This helps us understand what endpoint was hit.
         */
        String httpInfo = getHttpRequestInfo();

        /*
         * Log the entry point with an arrow "=>" meaning "going into this method"
         * Example output: "=> UserController.login() [POST /api/v1/auth/login]"
         */
        log.info("=> {}.{}() {}", className, methodName, httpInfo);

        /*
         * Start the timer - we want to measure how long this request takes.
         * Performance monitoring is important!
         */
        long start = System.currentTimeMillis();

        try {
            /*
             * This is the magic line - it actually calls the real controller method.
             * Everything before this was preparation, everything after is cleanup.
             */
            Object result = joinPoint.proceed();

            /*
             * If we get here, the method succeeded!
             * Calculate how long it took and log it.
             * Example: "<= UserController.login() completed in 245ms"
             */
            long duration = System.currentTimeMillis() - start;
            log.info("<= {}.{}() completed in {}ms", className, methodName, duration);
            return result;

        } catch (Exception ex) {
            /*
             * Uh oh, something went wrong!
             * Log it as an ERROR so we know what failed and how long it took before failing.
             * Then re-throw the exception so the normal error handling can take over.
             */
            long duration = System.currentTimeMillis() - start;
            log.error("<= {}.{}() failed after {}ms: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    /*
     * --- SERVICE LOGGING ---
     *
     * This is similar to controller logging, but with more detail.
     * Services contain business logic, so we log at DEBUG level (only visible when debugging).
     * We also log the METHOD PARAMETERS here because they help us understand what data
     * is being processed.
     */
    @Around("serviceMethods()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {

        /*
         * Get class and method names, just like in controller logging.
         */
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        /*
         * Get the parameters that were passed to this method.
         * But be careful - we filter out sensitive stuff like passwords!
         */
        Map<String, Object> safeParams = getSafeParams(joinPoint);

        /*
         * Log the entry with parameters.
         * Example: "=> UserService.createUser() args={email=john@example.com, password=***}"
         * Notice the password is masked!
         */
        log.debug("=> {}.{}() args={}", className, methodName, safeParams);

        /*
         * Start timing, just like in controller logging.
         */
        long start = System.currentTimeMillis();

        try {
            /*
             * Execute the actual service method.
             */
            Object result = joinPoint.proceed();

            /*
             * Success! Log completion time at DEBUG level.
             */
            long duration = System.currentTimeMillis() - start;
            log.debug("<= {}.{}() completed in {}ms", className, methodName, duration);
            return result;

        } catch (Exception ex) {
            /*
             * Service methods use WARN level for errors instead of ERROR.
             * This is because service errors might be expected/handled (like validation errors),
             * while controller errors are usually more serious.
             */
            long duration = System.currentTimeMillis() - start;
            log.warn("<= {}.{}() failed after {}ms: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    /*
     * --- HELPER METHODS ---
     * These are utility methods that help us collect information safely.
     */

    /*
     * This method tries to extract HTTP request information.
     * It grabs the HTTP method (GET, POST, etc.) and the URL path.
     *
     * We wrap it in try-catch because sometimes this info isn't available
     * (like when testing service methods directly without an HTTP request).
     */
    private String getHttpRequestInfo() {
        try {
            /*
             * Get the current HTTP request from Spring's context.
             * This is like asking: "What HTTP request triggered this method call?"
             */
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();

                /*
                 * Format it nicely like: "[POST /api/v1/auth/login]"
                 */
                return "[" + request.getMethod() + " " + request.getRequestURI() + "]";
            }
        } catch (Exception ignored) {
            /*
             * If anything goes wrong, just ignore it.
             * We'd rather have no HTTP info than crash the logging.
             */
        }
        return "";
    }

    /*
     * This method extracts method parameters but makes them SAFE for logging.
     * It filters out sensitive data and unnecessary objects.
     *
     * Think of it as a bouncer checking IDs - some things can't get into the logs!
     */
    private Map<String, Object> getSafeParams(JoinPoint joinPoint) {

        /*
         * This will hold our safe parameters that are okay to log.
         */
        Map<String, Object> params = new HashMap<>();

        /*
         * Get the method signature so we can read parameter names and values.
         */
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        /*
         * If we can't get parameter names for some reason, just return empty.
         * This shouldn't happen but better safe than sorry.
         */
        if (paramNames == null) {
            return params;
        }

        /*
         * Loop through each parameter and decide if it's safe to log.
         */
        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            Object value = args[i];

            /*
             * Skip servlet request/response objects.
             * They're huge and contain tons of irrelevant data that would clutter the logs.
             * Plus, we already logged the HTTP info separately.
             */
            if (value instanceof HttpServletRequest || value instanceof HttpServletResponse) {
                continue;
            }

            /*
             * Check if this parameter name contains sensitive words.
             * If it does, replace the value with "***" so we never expose secrets.
             * Otherwise, log the actual value.
             */
            if (isSensitive(name)) {
                params.put(name, "***");
            } else {
                params.put(name, value);
            }
        }
        return params;
    }

    /*
     * This checks if a parameter name is sensitive.
     * It converts the name to lowercase and checks if it contains any
     * of our blacklisted words (password, token, etc.)
     *
     * For example:
     * - "userPassword" contains "password" → SENSITIVE
     * - "newPwdHash" contains "newpwd" → SENSITIVE
     * - "username" doesn't contain any blacklisted words → NOT SENSITIVE
     */
    private boolean isSensitive(String paramName) {
        String lower = paramName.toLowerCase();
        return SENSITIVE_PARAMS.stream().anyMatch(s -> lower.contains(s));
    }
}
