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
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // Retrieves a complete list of all users from the database.
    // Primarily used by administrative dashboards for user oversight.
    // Returns a list of User entities.
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // Fetches a specific user's details based on their unique numeric ID.
    // Throws a ResourceNotFoundException if the user does not exist in the system.
    // Returns the found User entity.
    public User getUserById(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Validates email uniqueness, encodes the password, and creates a new user record.
    // Triggers a welcome notification and creates an audit log entry for the admin action.
    // Uses @Transactional to ensure the user, notification, and log are all saved or none are.
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

        notificationService.sendNotification(
                savedUser,
                NotificationType.ACCOUNT_CREATED,
                "Your account has been created. You can now log in.",
                "User",
                savedUser.getUserId(),
                "HIGH",
                false
        );

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

    // Queries the repository for all users associated with a specific manager's ID.
    // Useful for organizational hierarchy views and performance management.
    // Returns a filtered list of User entities.
    public List<User> getTeamMembers(Integer mgrId) {
        return userRepo.findByManager_UserId(mgrId);
    }

    // Updates existing user profile information like name, department, or status.
    // Automatically generates an audit log to track modifications made by the administrator.
    // Ensures data consistency by wrapping the update and logging in a transaction.
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