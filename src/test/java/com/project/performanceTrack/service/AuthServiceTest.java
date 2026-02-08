package com.project.performanceTrack.service;

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

/**
 * Unit tests for AuthService.
 *
 * KEY CONCEPTS FOR FRESHERS:
 * --------------------------
 * @ExtendWith(MockitoExtension.class) -> Tells JUnit to use Mockito for this test class.
 * @Mock -> Creates a fake (mock) version of a dependency. It doesn't hit the real database.
 * @InjectMocks -> Creates the real service, but injects all the @Mock objects into it.
 *
 * when(...).thenReturn(...) -> "When this method is called, return this value."
 * verify(...) -> "Check that this method was actually called."
 * assertThrows(...) -> "Expect this code to throw an exception."
 *
 * TEST PATTERN: Arrange -> Act -> Assert
 *   Arrange = set up test data and mock behavior
 *   Act     = call the method you're testing
 *   Assert  = check the result is what you expected
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // These are fake versions of the real dependencies
    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder pwdEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditLogService auditLogService;

    // This is the real service we're testing, with mocks injected into it
    @InjectMocks
    private AuthService authService;

    // Reusable test data
    private User testUser;
    private LoginRequest loginRequest;

    /**
     * @BeforeEach runs before EVERY test method.
     * We set up common test data here so we don't repeat it in every test.
     */
    @BeforeEach
    void setUp() {
        // Create a sample user for our tests
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPasswordHash("encodedPassword123");
        testUser.setRole(UserRole.EMPLOYEE);
        testUser.setDepartment("Engineering");
        testUser.setStatus(UserStatus.ACTIVE);

        // Create a sample login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("login() should return token and user info when credentials are valid")
    void login_WithValidCredentials_ShouldReturnLoginResponse() {
        // Arrange: Set up mock behavior
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
        when(jwtUtil.generateToken("john@example.com", 1, "EMPLOYEE")).thenReturn("fake-jwt-token");

        // Act: Call the method we're testing
        LoginResponse response = authService.login(loginRequest);

        // Assert: Check the result
        assertNotNull(response, "Response should not be null");
        assertEquals("fake-jwt-token", response.getToken(), "Token should match");
        assertEquals(1, response.getUserId(), "User ID should match");
        assertEquals("John Doe", response.getName(), "Name should match");
        assertEquals("john@example.com", response.getEmail(), "Email should match");
        assertEquals(UserRole.EMPLOYEE, response.getRole(), "Role should match");

        // Verify that audit log was called to record the login
        verify(auditLogService).logAudit(eq(testUser), eq("LOGIN"), anyString(), isNull(), isNull(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("login() should throw UnauthorizedException when email is not found")
    void login_WithInvalidEmail_ShouldThrowUnauthorizedException() {
        // Arrange: No user found for this email
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.empty());

        // Act & Assert: Expect an exception
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest),
                "Should throw UnauthorizedException for invalid email"
        );

        assertEquals("Invalid email or password", exception.getMessage());

        // Verify: No token should have been generated
        verify(jwtUtil, never()).generateToken(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("login() should throw UnauthorizedException when password is wrong")
    void login_WithWrongPassword_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("password123", "encodedPassword123")).thenReturn(false);

        // Act & Assert
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("login() should throw UnauthorizedException when account is inactive")
    void login_WithInactiveAccount_ShouldThrowUnauthorizedException() {
        // Arrange: Make the user inactive
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepo.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("password123", "encodedPassword123")).thenReturn(true);

        // Act & Assert
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Account is inactive", exception.getMessage());
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    @DisplayName("logout() should log audit event when user exists")
    void logout_WithExistingUser_ShouldLogAudit() {
        // Arrange
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));

        // Act
        authService.logout(1);

        // Assert: Verify the audit log was created
        verify(auditLogService).logAudit(eq(testUser), eq("LOGOUT"), anyString(), isNull(), isNull(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("logout() should not throw when user doesn't exist")
    void logout_WithNonExistentUser_ShouldNotThrow() {
        // Arrange
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        // Act & Assert: Should not throw any exception
        assertDoesNotThrow(() -> authService.logout(999));

        // Verify: No audit log should be created
        verify(auditLogService, never()).logAudit(any(), anyString(), anyString(), any(), any(), anyString());
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    @DisplayName("changePassword() should update password when old password is correct")
    void changePassword_WithCorrectOldPassword_ShouldSucceed() {
        // Arrange
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("oldPass", "encodedPassword123")).thenReturn(true);
        when(pwdEncoder.encode("newPass")).thenReturn("encodedNewPass");
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.changePassword(1, "oldPass", "newPass");

        // Assert: Verify password was encoded and user was saved
        verify(pwdEncoder).encode("newPass");
        verify(userRepo).save(testUser);
        verify(auditLogService).logAudit(eq(testUser), eq("PASSWORD_CHANGED"), anyString(), eq("User"), eq(1), eq("SUCCESS"));
    }

    @Test
    @DisplayName("changePassword() should throw when old password is wrong")
    void changePassword_WithWrongOldPassword_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(pwdEncoder.matches("wrongOldPass", "encodedPassword123")).thenReturn(false);

        // Act & Assert
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.changePassword(1, "wrongOldPass", "newPass")
        );

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("changePassword() should throw when user is not found")
    void changePassword_WithNonExistentUser_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UnauthorizedException.class,
                () -> authService.changePassword(999, "oldPass", "newPass")
        );
    }
}
