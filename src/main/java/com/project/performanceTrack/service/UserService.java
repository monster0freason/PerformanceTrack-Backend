package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.CreateUserRequest;
import com.project.performanceTrack.entity.AuditLog;
import com.project.performanceTrack.entity.Notification;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.NotificationStatus;
import com.project.performanceTrack.enums.NotificationType;
import com.project.performanceTrack.exception.BadRequestException;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.AuditLogRepository;
import com.project.performanceTrack.repository.NotificationRepository;
import com.project.performanceTrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// User management service
@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder pwdEncoder;

    @Autowired
    private NotificationRepository notifRepo;

    @Autowired
    private AuditLogRepository auditRepo;

    // Get all users
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // Get user by ID
    public User getUserById(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Create new user (Admin only)
    public User createUser(CreateUserRequest req, Integer adminId) {
        // Check if email already exists
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        // Create user entity
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(pwdEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setDepartment(req.getDept());
        user.setStatus(req.getStatus());

        // Set manager if provided
        if (req.getMgrId() != null) {
            User mgr = userRepo.findById(req.getMgrId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            user.setManager(mgr);
        }

        // Save user
        User savedUser = userRepo.save(user);

        // Create notification for new user
        Notification notif = new Notification();
        notif.setUser(savedUser);
        notif.setType(NotificationType.ACCOUNT_CREATED);
        notif.setMessage("Your account has been created. You can now log in.");
        notif.setStatus(NotificationStatus.UNREAD);
        notifRepo.save(notif);

        // Create audit log
        User admin = userRepo.findById(adminId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(admin);
        log.setAction("USER_CREATED");
        log.setDetails("Created user: " + savedUser.getName() + " (" + savedUser.getRole() + ")");
        log.setRelatedEntityType("User");
        log.setRelatedEntityId(savedUser.getUserId());
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return savedUser;
    }

    // Get team members for manager
    public List<User> getTeamMembers(Integer mgrId) {
        return userRepo.findByManager_UserId(mgrId);
    }

    // Update user (Admin only)
    public User updateUser(Integer userId, CreateUserRequest req, Integer adminId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update fields
        user.setName(req.getName());
        user.setRole(req.getRole());
        user.setDepartment(req.getDept());
        user.setStatus(req.getStatus());

        // Update manager if provided
        if (req.getMgrId() != null) {
            User mgr = userRepo.findById(req.getMgrId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            user.setManager(mgr);
        }

        // Save user
        User updated = userRepo.save(user);

        // Create audit log
        User admin = userRepo.findById(adminId).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(admin);
        log.setAction("USER_UPDATED");
        log.setDetails("Updated user: " + updated.getName());
        log.setRelatedEntityType("User");
        log.setRelatedEntityId(updated.getUserId());
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);

        return updated;
    }
}
