package com.project.performanceTrack.service;

/*
 * This file tests the UserService class.
 *
 * UserService handles all user management operations:
 * - Getting all users
 * - Finding a specific user by ID
 * - Creating a new user (with email validation, password encoding, notifications)
 * - Updating an existing user
 * - Getting all team members under a specific manager
 *
 * Notice how we test BOTH the happy path (things go well)
 * AND the sad path (things go wrong) for each method.
 */

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

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    /*
     * All the fakes UserService depends on.
     */
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

    /*
     * Three different user types for our tests:
     * - testUser: a regular employee
     * - adminUser: the admin performing operations
     * - managerUser: a manager who can have team members
     */
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

    /*
     * ==================== TESTS FOR getAllUsers() ====================
     */

    @Test
    @DisplayName("getAllUsers() should return list of all users")
    void getAllUsers_ShouldReturnAllUsers() {

        /*
         * ARRANGE: Database has 2 users - our test user and admin.
         */
        List<User> users = Arrays.asList(testUser, adminUser);
        when(userRepo.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        /*
         * ASSERT: We should get both users back.
         * Also verify that findAll() was actually called (not some other method).
         */
        assertEquals(2, result.size(), "Should return 2 users");
        verify(userRepo).findAll();
    }

    /*
     * ==================== TESTS FOR getUserById() ====================
     */

    @Test
    @DisplayName("getUserById() should return user when found")
    void getUserById_WithValidId_ShouldReturnUser() {

        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals(1, result.getUserId());
    }

    @Test
    @DisplayName("getUserById() should throw ResourceNotFoundException when user not found")
    void getUserById_WithInvalidId_ShouldThrowResourceNotFoundException() {

        /*
         * ARRANGE: No user with ID 999 exists.
         */
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(999)
        );

        assertEquals("User not found", exception.getMessage());
    }

    /*
     * ==================== TESTS FOR createUser() ====================
     */

    @Test
    @DisplayName("createUser() should create user successfully when email is unique")
    void createUser_WithUniqueEmail_ShouldCreateUser() {

        /*
         * ARRANGE: Build a complete create user request.
         */
        CreateUserRequest req = new CreateUserRequest();
        req.setName("New User");
        req.setEmail("new@example.com");
        req.setPassword("secret123");
        req.setRole(UserRole.EMPLOYEE);
        req.setDept("Engineering");
        req.setStatus(UserStatus.ACTIVE);

        /*
         * Set up the chain of events:
         * 1. Email doesn't exist yet (Optional.empty())
         * 2. Password gets encoded
         * 3. User gets saved with a new ID (using thenAnswer to simulate DB generating ID)
         * 4. Admin user is found for audit logging
         */
        when(userRepo.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(pwdEncoder.encode("secret123")).thenReturn("encodedSecret");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            /*
             * thenAnswer lets us write custom logic instead of just returning a fixed value.
             * Here we grab the user that was passed to save() and give it an ID,
             * simulating what the database would do.
             */
            User saved = invocation.getArgument(0);
            saved.setUserId(10);
            return saved;
        });
        when(userRepo.findById(100)).thenReturn(Optional.of(adminUser));

        /*
         * ACT: Create the user (admin with ID 100 is doing the creating).
         */
        User result = userService.createUser(req, 100);

        /*
         * ASSERT: Check the user was created correctly.
         */
        assertNotNull(result);
        assertEquals("New User", result.getName());
        assertEquals("encodedSecret", result.getPasswordHash());

        /*
         * Verify an account creation notification was sent to the new user.
         * The notification service should have been called with specific parameters.
         */
        verify(notificationService).sendNotification(
                any(User.class),
                eq(NotificationType.ACCOUNT_CREATED),
                anyString(),
                eq("User"),
                anyInt(),
                eq("HIGH"),
                eq(false)
        );

        /*
         * Verify the action was recorded in the audit log.
         */
        verify(auditLogService).logAudit(any(), eq("USER_CREATED"), anyString(), eq("User"), anyInt(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("createUser() should throw BadRequestException when email already exists")
    void createUser_WithDuplicateEmail_ShouldThrowBadRequestException() {

        /*
         * ARRANGE: The email "john@example.com" is already taken.
         */
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("john@example.com");

        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        /*
         * ASSERT: Should throw BadRequestException - can't have duplicate emails!
         */
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.createUser(req, 100)
        );

        assertEquals("Email already exists", exception.getMessage());

        /*
         * Critical: verify that save() was NEVER called.
         * We should abort before saving if the email is already taken.
         */
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("createUser() should assign manager when managerId is provided")
    void createUser_WithManagerId_ShouldAssignManager() {

        /*
         * ARRANGE: Create request with a managerId specified.
         * This means the new employee should be assigned to manager with ID 50.
         */
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

        User result = userService.createUser(req, 100);

        /*
         * ASSERT: The new employee's manager should be set to our managerUser.
         */
        assertNotNull(result);
        assertEquals(managerUser, result.getManager(), "Manager should be assigned");
    }

    /*
     * ==================== TESTS FOR updateUser() ====================
     */

    @Test
    @DisplayName("updateUser() should update user fields correctly")
    void updateUser_WithValidData_ShouldUpdateUser() {

        /*
         * ARRANGE: Request to update user 1's name, role, and department.
         */
        CreateUserRequest req = new CreateUserRequest();
        req.setName("Updated Name");
        req.setRole(UserRole.MANAGER);
        req.setDept("HR");
        req.setStatus(UserStatus.ACTIVE);

        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        when(userRepo.findById(100)).thenReturn(Optional.of(adminUser));

        User result = userService.updateUser(1, req, 100);

        /*
         * ASSERT: The returned user should have the updated values.
         */
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals(UserRole.MANAGER, result.getRole());
        assertEquals("HR", result.getDepartment());
    }

    @Test
    @DisplayName("updateUser() should throw ResourceNotFoundException when user not found")
    void updateUser_WithInvalidUserId_ShouldThrowResourceNotFoundException() {

        /*
         * ARRANGE: User 999 doesn't exist.
         */
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        /*
         * ASSERT: Can't update a user that doesn't exist!
         */
        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUser(999, new CreateUserRequest(), 100)
        );
    }

    /*
     * ==================== TESTS FOR getTeamMembers() ====================
     */

    @Test
    @DisplayName("getTeamMembers() should return list of team members for a manager")
    void getTeamMembers_ShouldReturnTeamList() {

        /*
         * ARRANGE: Manager with ID 50 has one team member - our testUser.
         */
        List<User> team = Arrays.asList(testUser);
        when(userRepo.findByManager_UserId(50)).thenReturn(team);

        List<User> result = userService.getTeamMembers(50);

        /*
         * ASSERT: Should return the correct team members.
         */
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }
}