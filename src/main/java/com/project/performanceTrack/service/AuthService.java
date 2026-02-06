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

    // Injecting the AuditLogService instead of the Repository
    private final AuditLogService auditLogService;

    public LoginResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!pwdEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus().name().equals("INACTIVE")) {
            throw new UnauthorizedException("Account is inactive");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getUserId(),
                user.getRole().name()
        );

        // Use the centralized audit service
        auditLogService.logAudit(user, "LOGIN", "User logged in successfully", null, null, "SUCCESS");

        return new LoginResponse(
                token,
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment()
        );
    }

    public void logout(Integer userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user != null) {
            // Use the centralized audit service
            auditLogService.logAudit(user, "LOGOUT", "User logged out", null, null, "SUCCESS");
        }
    }

    @Transactional
    public void changePassword(Integer userId, String oldPwd, String newPwd) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!pwdEncoder.matches(oldPwd, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(pwdEncoder.encode(newPwd));
        userRepo.save(user);

        // Use the centralized audit service
        auditLogService.logAudit(user, "PASSWORD_CHANGED", "User changed password", "User", userId, "SUCCESS");
    }
}