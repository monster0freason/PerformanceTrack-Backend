package com.project.performanceTrack.controller;

import com.project.performanceTrack.dto.ApiResponse;
import com.project.performanceTrack.dto.CreateUserRequest;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.enums.UserStatus;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userSvc;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setRole(UserRole.EMPLOYEE);
        testUser.setDepartment("Engineering");
        testUser.setStatus(UserStatus.ACTIVE);

        mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("userId", 100); // Admin user making the request
    }

    // ==================== getAllUsers() ====================

    @Test
    @DisplayName("getAllUsers() should return success with list of users")
    void getAllUsers_ShouldReturnUsers() {
        when(userSvc.getAllUsers()).thenReturn(Arrays.asList(testUser));

        ApiResponse<List<User>> response = userController.getAllUsers();

        assertEquals("success", response.getStatus());
        assertEquals("Users retrieved", response.getMsg());
        assertEquals(1, response.getData().size());
    }

    // ==================== getUserById() ====================

    @Test
    @DisplayName("getUserById() should return success with user data")
    void getUserById_ShouldReturnUser() {
        when(userSvc.getUserById(1)).thenReturn(testUser);

        ApiResponse<User> response = userController.getUserById(1);

        assertEquals("success", response.getStatus());
        assertEquals("John Doe", response.getData().getName());
    }

    @Test
    @DisplayName("getUserById() should propagate exception when user not found")
    void getUserById_NotFound_ShouldPropagateException() {
        when(userSvc.getUserById(999)).thenThrow(new ResourceNotFoundException("User not found"));

        assertThrows(ResourceNotFoundException.class, () -> userController.getUserById(999));
    }

    // ==================== createUser() ====================

    @Test
    @DisplayName("createUser() should return success with created user")
    void createUser_ShouldReturnCreatedUser() {
        CreateUserRequest req = new CreateUserRequest();
        req.setName("New User");
        req.setEmail("new@example.com");

        when(userSvc.createUser(any(CreateUserRequest.class), eq(100))).thenReturn(testUser);

        ApiResponse<User> response = userController.createUser(req, mockRequest);

        assertEquals("success", response.getStatus());
        assertEquals("User created", response.getMsg());
        assertNotNull(response.getData());
    }

    // ==================== updateUser() ====================

    @Test
    @DisplayName("updateUser() should return success with updated user")
    void updateUser_ShouldReturnUpdatedUser() {
        CreateUserRequest req = new CreateUserRequest();
        req.setName("Updated Name");

        when(userSvc.updateUser(eq(1), any(CreateUserRequest.class), eq(100))).thenReturn(testUser);

        ApiResponse<User> response = userController.updateUser(1, req, mockRequest);

        assertEquals("success", response.getStatus());
        assertEquals("User updated", response.getMsg());
    }

    // ==================== getTeam() ====================

    @Test
    @DisplayName("getTeam() should return team members")
    void getTeam_ShouldReturnTeamMembers() {
        when(userSvc.getTeamMembers(50)).thenReturn(Arrays.asList(testUser));

        ApiResponse<List<User>> response = userController.getTeam(50);

        assertEquals("success", response.getStatus());
        assertEquals("Team members retrieved", response.getMsg());
        assertEquals(1, response.getData().size());
    }
}
