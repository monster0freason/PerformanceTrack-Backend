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

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // Parameter names that should never be logged
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "passwordHash", "oldPwd", "newPwd",
            "token", "jwt", "secret", "authorization"
    );

    // --- Pointcuts ---

    @Pointcut("execution(* com.project.performanceTrack.controller..*(..))")
    public void controllerMethods() {}

    @Pointcut("execution(* com.project.performanceTrack.service..*(..))")
    public void serviceMethods() {}

    // --- Controller Logging (INFO level) ---

    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // Get HTTP method and URI if available
        String httpInfo = getHttpRequestInfo();

        log.info("=> {}.{}() {}", className, methodName, httpInfo);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("<= {}.{}() completed in {}ms", className, methodName, duration);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("<= {}.{}() failed after {}ms: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    // --- Service Logging (DEBUG level with params) ---

    @Around("serviceMethods()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Map<String, Object> safeParams = getSafeParams(joinPoint);

        log.debug("=> {}.{}() args={}", className, methodName, safeParams);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.debug("<= {}.{}() completed in {}ms", className, methodName, duration);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.warn("<= {}.{}() failed after {}ms: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    // --- Helper Methods ---

    private String getHttpRequestInfo() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return "[" + request.getMethod() + " " + request.getRequestURI() + "]";
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private Map<String, Object> getSafeParams(JoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null) {
            return params;
        }

        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            Object value = args[i];

            // Skip servlet objects - they're noise
            if (value instanceof HttpServletRequest || value instanceof HttpServletResponse) {
                continue;
            }

            // Mask sensitive parameters
            if (isSensitive(name)) {
                params.put(name, "***");
            } else {
                params.put(name, value);
            }
        }
        return params;
    }

    private boolean isSensitive(String paramName) {
        String lower = paramName.toLowerCase();
        return SENSITIVE_PARAMS.stream().anyMatch(s -> lower.contains(s));
    }
}
