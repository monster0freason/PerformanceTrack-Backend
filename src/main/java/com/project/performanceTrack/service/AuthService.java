package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.LoginRequest;
import com.project.performanceTrack.dto.LoginResponse;
import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.exception.UnauthorizedException;
import com.project.performanceTrack.repository.AuditLogRepository;
import com.project.performanceTrack.repository.UserRepository;
import com.project.performanceTrack.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Authentication service for login/logout
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;

    private final PasswordEncoder pwdEncoder;

    private final JwtUtil jwtUtil;

    private final AuditLogRepository auditRepo;

    // User login
    public LoginResponse login(LoginRequest req) {
        // Find user by email
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check password
        if (!pwdEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check if user is active
        if (user.getStatus().name().equals("INACTIVE")) {
            throw new UnauthorizedException("Account is inactive");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getUserId(),
                user.getRole().name()
        );

        // Create audit log
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction("LOGIN");
        log.setDetails("User logged in successfully");
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        // Return login response
        return new LoginResponse(
                token,
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment()
        );
    }

    // User logout
    public void logout(Integer userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user != null) {
            // Create audit log
            AuditLog log = new AuditLog();
            log.setUser(user);
            log.setAction("LOGOUT");
            log.setDetails("User logged out");
            log.setStatus("SUCCESS");
            log.setTimestamp(LocalDateTime.now());
            auditRepo.save(log);
        }
    }

    // Change password
    public void changePassword(Integer userId, String oldPwd, String newPwd) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Verify old password
        if (!pwdEncoder.matches(oldPwd, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(pwdEncoder.encode(newPwd));
        userRepo.save(user);

        // Create audit log
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction("PASSWORD_CHANGED");
        log.setDetails("User changed password");
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);
    }
}
