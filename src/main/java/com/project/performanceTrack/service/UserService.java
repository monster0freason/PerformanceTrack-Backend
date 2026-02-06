package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.CreateUserRequest;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.NotificationType;
import com.project.performanceTrack.exception.BadRequestException;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder pwdEncoder;

    // Injecting our new specialized services
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserById(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User createUser(CreateUserRequest req, Integer adminId) {
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(pwdEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setDepartment(req.getDept());
        user.setStatus(req.getStatus());

        if (req.getMgrId() != null) {
            User mgr = userRepo.findById(req.getMgrId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            user.setManager(mgr);
        }

        User savedUser = userRepo.save(user);

        // 1. Use the new Notification Service
        notificationService.sendNotification(
                savedUser,
                NotificationType.ACCOUNT_CREATED,
                "Your account has been created. You can now log in.",
                "User",
                savedUser.getUserId(),
                "HIGH",
                false
        );

        // 2. Use the new Audit Log Service
        User admin = userRepo.findById(adminId).orElse(null);
        auditLogService.logAudit(
                admin,
                "USER_CREATED",
                "Created user: " + savedUser.getName() + " (" + savedUser.getRole() + ")",
                "User",
                savedUser.getUserId(),
                "SUCCESS"
        );

        return savedUser;
    }

    public List<User> getTeamMembers(Integer mgrId) {
        return userRepo.findByManager_UserId(mgrId);
    }

    @Transactional
    public User updateUser(Integer userId, CreateUserRequest req, Integer adminId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(req.getName());
        user.setRole(req.getRole());
        user.setDepartment(req.getDept());
        user.setStatus(req.getStatus());

        if (req.getMgrId() != null) {
            User mgr = userRepo.findById(req.getMgrId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            user.setManager(mgr);
        }

        User updated = userRepo.save(user);

        // 2. Use the new Audit Log Service
        User admin = userRepo.findById(adminId).orElse(null);
        auditLogService.logAudit(
                admin,
                "USER_UPDATED",
                "Updated user: " + updated.getName(),
                "User",
                updated.getUserId(),
                "SUCCESS"
        );

        return updated;
    }
}