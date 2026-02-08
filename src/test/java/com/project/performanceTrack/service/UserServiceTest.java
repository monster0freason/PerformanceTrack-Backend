package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.CreateUserRequest;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.NotificationType;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.enums.UserStatus;
import com.project.performanceTrack.exception.BadRequestException;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 *
 * This test class covers all methods in UserService:
 * - getAllUsers()
 * - getUserById()
 * - createUser()
 * - updateUser()
 * - getTeamMembers()
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder pwdEncoder;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;
    private User managerUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPasswordHash("hashedPwd");
        testUser.setRole(UserRole.EMPLOYEE);
        testUser.setDepartment("Engineering");
        testUser.setStatus(UserStatus.ACTIVE);

        adminUser = new User();
        adminUser.setUserId(100);
        adminUser.setName("Admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);

        managerUser = new User();
        managerUser.setUserId(50);
        managerUser.setName("Manager");
        managerUser.setEmail("manager@example.com");
        managerUser.setRole(UserRole.MANAGER);
    }

    // ==================== getAllUsers() ====================

    @Test
    @DisplayName("getAllUsers() should return list of all users")
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange: Mock the repository to return a list of users
        List<User> users = Arrays.asList(testUser, adminUser);
        when(userRepo.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertEquals(2, result.size(), "Should return 2 users");
        verify(userRepo).findAll();
    }

    // ==================== getUserById() ====================

    @Test
    @DisplayName("getUserById() should return user when found")
    void getUserById_WithValidId_ShouldReturnUser() {
        // Arrange
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1);

        // Assert
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals(1, result.getUserId());
    }

    @Test
    @DisplayName("getUserById() should throw ResourceNotFoundException when user not found")
    void getUserById_WithInvalidId_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(999)
        );

        assertEquals("User not found", exception.getMessage());
    }

    // ==================== createUser() ====================

    @Test
    @DisplayName("createUser() should create user successfully when email is unique")
    void createUser_WithUniqueEmail_ShouldCreateUser() {
        // Arrange
        CreateUserRequest req = new CreateUserRequest();
        req.setName("New User");
        req.setEmail("new@example.com");
        req.setPassword("secret123");
        req.setRole(UserRole.EMPLOYEE);
        req.setDept("Engineering");
        req.setStatus(UserStatus.ACTIVE);

        when(userRepo.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(pwdEncoder.encode("secret123")).thenReturn("encodedSecret");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            // Return the same user but with an ID (simulating database save)
            User saved = invocation.getArgument(0);
            saved.setUserId(10);
            return saved;
        });
        when(userRepo.findById(100)).thenReturn(Optional.of(adminUser));

        // Act
        User result = userService.createUser(req, 100);

        // Assert
        assertNotNull(result);
        assertEquals("New User", result.getName());
        assertEquals("encodedSecret", result.getPasswordHash());

        // Verify notification was sent
        verify(notificationService).sendNotification(
                any(User.class),
                eq(NotificationType.ACCOUNT_CREATED),
                anyString(),
                eq("User"),
                anyInt(),
                eq("HIGH"),
                eq(false)
        );

        // Verify audit log was created
        verify(auditLogService).logAudit(any(), eq("USER_CREATED"), anyString(), eq("User"), anyInt(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("createUser() should throw BadRequestException when email already exists")
    void createUser_WithDuplicateEmail_ShouldThrowBadRequestException() {
        // Arrange
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("john@example.com");

        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.createUser(req, 100)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("createUser() should assign manager when managerId is provided")
    void createUser_WithManagerId_ShouldAssignManager() {
        // Arrange
        CreateUserRequest req = new CreateUserRequest();
        req.setName("New Employee");
        req.setEmail("emp@example.com");
        req.setPassword("secret");
        req.setRole(UserRole.EMPLOYEE);
        req.setDept("Engineering");
        req.setStatus(UserStatus.ACTIVE);
        req.setMgrId(50);

        when(userRepo.findByEmail("emp@example.com")).thenReturn(Optional.empty());
        when(pwdEncoder.encode("secret")).thenReturn("encoded");
        when(userRepo.findById(50)).thenReturn(Optional.of(managerUser));
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(11);
            return saved;
        });
        when(userRepo.findById(100)).thenReturn(Optional.of(adminUser));

        // Act
        User result = userService.createUser(req, 100);

        // Assert
        assertNotNull(result);
        assertEquals(managerUser, result.getManager(), "Manager should be assigned");
    }

    // ==================== updateUser() ====================

    @Test
    @DisplayName("updateUser() should update user fields correctly")
    void updateUser_WithValidData_ShouldUpdateUser() {
        // Arrange
        CreateUserRequest req = new CreateUserRequest();
        req.setName("Updated Name");
        req.setRole(UserRole.MANAGER);
        req.setDept("HR");
        req.setStatus(UserStatus.ACTIVE);

        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        when(userRepo.findById(100)).thenReturn(Optional.of(adminUser));

        // Act
        User result = userService.updateUser(1, req, 100);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals(UserRole.MANAGER, result.getRole());
        assertEquals("HR", result.getDepartment());
    }

    @Test
    @DisplayName("updateUser() should throw ResourceNotFoundException when user not found")
    void updateUser_WithInvalidUserId_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUser(999, new CreateUserRequest(), 100)
        );
    }

    // ==================== getTeamMembers() ====================

    @Test
    @DisplayName("getTeamMembers() should return list of team members for a manager")
    void getTeamMembers_ShouldReturnTeamList() {
        // Arrange
        List<User> team = Arrays.asList(testUser);
        when(userRepo.findByManager_UserId(50)).thenReturn(team);

        // Act
        List<User> result = userService.getTeamMembers(50);

        // Assert
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }
}
