package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.LoginRequest;
import com.project.performanceTrack.dto.LoginResponse;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.exception.UnauthorizedException;
import com.project.performanceTrack.repository.UserRepository;
import com.project.performanceTrack.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder pwdEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    // Authenticates credentials, checks if the account is active, and generates a JWT.
    // Records a successful "LOGIN" event in the audit log for security tracking.
    // Returns a response containing the token and essential user profile data.
    public LoginResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!pwdEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus().name().equals("INACTIVE")) {
            throw new UnauthorizedException("Account is inactive");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole().name());

        auditLogService.logAudit(user, "LOGIN", "User logged in successfully", null, null, "SUCCESS");

        return new LoginResponse(token, user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getDepartment());
    }

    // Locates the user by ID and records a "LOGOUT" event in the audit trail.
    // Does not require a return value as it primarily handles state tracking.
    // Ensures user sessions are audited even if the token is simply cleared client-side.
    public void logout(Integer userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user != null) {
            auditLogService.logAudit(user, "LOGOUT", "User logged out", null, null, "SUCCESS");
        }
    }

    // Verifies the old password matches before encoding and saving the new password.
    // Performs an audit log entry to track sensitive security credential changes.
    // Wrapped in @Transactional to roll back if the database update fails.
    @Transactional
    public void changePassword(Integer userId, String oldPwd, String newPwd) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!pwdEncoder.matches(oldPwd, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(pwdEncoder.encode(newPwd));
        userRepo.save(user);

        auditLogService.logAudit(user, "PASSWORD_CHANGED", "User changed password", "User", userId, "SUCCESS");
    }
}