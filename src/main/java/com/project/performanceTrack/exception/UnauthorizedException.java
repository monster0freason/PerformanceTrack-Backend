package com.project.performanceTrack.exception;

// Custom exception for unauthorized access
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
