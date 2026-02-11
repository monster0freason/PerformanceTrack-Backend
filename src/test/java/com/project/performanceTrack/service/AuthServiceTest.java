package com.project.performanceTrack.service;

/*
 * This is the most important and thorough test file in the project.
 * It tests AuthService - the core class that handles:
 * - Login (checking credentials and generating JWT tokens)
 * - Logout (recording that someone logged out)
 * - Change Password (validating old password and saving a new one)
 *
 * The comment at the top of the original file explained the key concepts well -
 * we've kept them below with more detail added.
 */

import com.project.performanceTrack.dto.LoginRequest;
import com.project.performanceTrack.dto.LoginResponse;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.enums.UserStatus;
import com.project.performanceTrack.exception.UnauthorizedException;
import com.project.performanceTrack.repository.UserRepository;
import com.project.performanceTrack.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    /*
     * All the dependencies AuthService needs - but FAKE versions of each.
     *
     * - userRepo: won't query the real database
     * - pwdEncoder: won't actually hash passwords
     * - jwtUtil: won't generate real JWT tokens
     * - auditLogService: won't save real audit logs
     */
    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder pwdEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditLogService auditLogService;

    /*
     * The REAL AuthService, but with all the above fakes injected into it.
     * This is the class we're actually testing.
     */
    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {

        /*
         * Create a realistic test user representing a normal employee.
         * Note: passwordHash is already "encoded" - in real life this would be a bcrypt hash.
         */
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPasswordHash("encodedPassword123");
        testUser.setRole(UserRole.EMPLOYEE);
        testUser.setDepartment("Engineering");
        testUser.setStatus(UserStatus.ACTIVE);

        /*
         * Create a login request - what the user sends from the frontend.
         */
        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    /*
     * ==================== TESTS FOR login() ====================
     */

    @Test
    @DisplayName("login() should return token and user info when credentials are valid")
    void login_WithValidCredentials_ShouldReturnLoginResponse() {

        /*
         * ARRANGE: Set up the full happy path.
         *
         * Step 1: When we look up the email, return our test user
         * Step 2: When we check if the password matches, say YES
         * Step 3: When we generate a token, return our fake token
         */
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
        when(jwtUtil.generateToken("john@example.com", 1, "EMPLOYEE")).thenReturn("fake-jwt-token");

        /*
         * ACT: Try to login.
         */
        LoginResponse response = authService.login(loginRequest);

        /*
         * ASSERT: Check all the fields in the response are correct.
         * Each assertion has a message explaining WHAT we're checking.
         */
        assertNotNull(response, "Response should not be null");
        assertEquals("fake-jwt-token", response.getToken(), "Token should match");
        assertEquals(1, response.getUserId(), "User ID should match");
        assertEquals("John Doe", response.getName(), "Name should match");
        assertEquals("john@example.com", response.getEmail(), "Email should match");
        assertEquals(UserRole.EMPLOYEE, response.getRole(), "Role should match");

        /*
         * Also verify that a LOGIN audit log was recorded.
         * eq() = must be exactly this value
         * anyString() = any string value is fine
         * isNull() = must be null
         */
        verify(auditLogService).logAudit(eq(testUser), eq("LOGIN"), anyString(), isNull(), isNull(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("login() should throw UnauthorizedException when email is not found")
    void login_WithInvalidEmail_ShouldThrowUnauthorizedException() {

        /*
         * ARRANGE: Simulate "no user found" by returning Optional.empty().
         * Optional.empty() is Java's way of saying "there's nothing here".
         */
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.empty());

        /*
         * ACT & ASSERT: Login should throw UnauthorizedException.
         *
         * assertThrows returns the exception so we can check its message too!
         */
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest),
                "Should throw UnauthorizedException for invalid email"
        );

        assertEquals("Invalid email or password", exception.getMessage());

        /*
         * IMPORTANT: Verify that we NEVER tried to generate a token.
         * If email doesn't exist, we should bail out early - no token generation!
         *
         * verify(x, never()).method() = assert this method was NEVER called
         */
        verify(jwtUtil, never()).generateToken(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("login() should throw UnauthorizedException when password is wrong")
    void login_WithWrongPassword_ShouldThrowUnauthorizedException() {

        /*
         * ARRANGE: User exists, but password check returns false (wrong password).
         */
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("password123", "encodedPassword123")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", exception.getMessage());

        /*
         * Again verify no token was generated - wrong password means no access!
         */
        verify(jwtUtil, never()).generateToken(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("login() should throw UnauthorizedException when account is inactive")
    void login_WithInactiveAccount_ShouldThrowUnauthorizedException() {

        /*
         * ARRANGE: User exists, password is correct, BUT account is INACTIVE.
         * An inactive account shouldn't be able to login even with right credentials.
         */
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("password123", "encodedPassword123")).thenReturn(true);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest)
        );

        /*
         * Note: the error message is different here - it's not "Invalid email or password"
         * but specifically "Account is inactive" so the user knows what's wrong.
         */
        assertEquals("Account is inactive", exception.getMessage());
    }

    /*
     * ==================== TESTS FOR logout() ====================
     */

    @Test
    @DisplayName("logout() should log audit event when user exists")
    void logout_WithExistingUser_ShouldLogAudit() {

        /*
         * ARRANGE: User with ID 1 exists in the database.
         */
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));

        /*
         * ACT: Logout the user.
         */
        authService.logout(1);

        /*
         * ASSERT: A LOGOUT audit log should have been created.
         * We don't check a return value because logout() returns void -
         * we only care that the side effect (audit log) happened.
         */
        verify(auditLogService).logAudit(eq(testUser), eq("LOGOUT"), anyString(), isNull(), isNull(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("logout() should not throw when user doesn't exist")
    void logout_WithNonExistentUser_ShouldNotThrow() {

        /*
         * ARRANGE: No user found with ID 999.
         */
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        /*
         * ASSERT: assertDoesNotThrow is the opposite of assertThrows.
         * It checks that the code runs WITHOUT throwing any exception.
         *
         * If a user doesn't exist, logout should silently do nothing,
         * not crash with an error!
         */
        assertDoesNotThrow(() -> authService.logout(999));

        /*
         * Also verify no audit log was created - there's no user to log for!
         */
        verify(auditLogService, never()).logAudit(any(), anyString(), anyString(), any(), any(), anyString());
    }

    /*
     * ==================== TESTS FOR changePassword() ====================
     */

    @Test
    @DisplayName("changePassword() should update password when old password is correct")
    void changePassword_WithCorrectOldPassword_ShouldSucceed() {

        /*
         * ARRANGE: Full happy path for password change.
         * - User exists
         * - Old password matches
         * - New password gets encoded
         * - User gets saved
         */
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("oldPass", "encodedPassword123")).thenReturn(true);
        when(pwdEncoder.encode("newPass")).thenReturn("encodedNewPass");
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        /*
         * ACT: Change the password.
         */
        authService.changePassword(1, "oldPass", "newPass");

        /*
         * ASSERT: Three things should have happened:
         * 1. New password was encoded
         * 2. User was saved with the new password
         * 3. Audit log was created with action "PASSWORD_CHANGED"
         */
        verify(pwdEncoder).encode("newPass");
        verify(userRepo).save(testUser);
        verify(auditLogService).logAudit(eq(testUser), eq("PASSWORD_CHANGED"), anyString(), eq("User"), eq(1), eq("SUCCESS"));
    }

    @Test
    @DisplayName("changePassword() should throw when old password is wrong")
    void changePassword_WithWrongOldPassword_ShouldThrowUnauthorizedException() {

        /*
         * ARRANGE: User exists but old password check fails.
         */
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("wrongOldPass", "encodedPassword123")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.changePassword(1, "wrongOldPass", "newPass")
        );

        assertEquals("Current password is incorrect", exception.getMessage());

        /*
         * Critical check: The user should NOT be saved if the old password was wrong!
         * We never want to overwrite a password when the user couldn't prove who they are.
         */
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("changePassword() should throw when user is not found")
    void changePassword_WithNonExistentUser_ShouldThrowUnauthorizedException() {

        /*
         * ARRANGE: No user found with ID 999.
         */
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        /*
         * ASSERT: Should throw an exception - can't change password for a non-existent user!
         */
        assertThrows(
                UnauthorizedException.class,
                () -> authService.changePassword(999, "oldPass", "newPass")
        );
    }
}