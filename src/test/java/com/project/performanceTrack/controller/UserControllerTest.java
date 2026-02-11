package com.project.performanceTrack.controller;

/*
 * This file tests the UserController class.
 *
 * It checks that all user management endpoints (get all users, get by ID,
 * create, update, get team) work correctly from the controller's perspective.
 *
 * Same approach as AuthControllerTest - we fake the service layer
 * and test only the controller logic.
 */

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

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    /*
     * Fake version of UserService - won't touch the real database.
     */
    @Mock
    private UserService userSvc;

    /*
     * Real UserController with the fake service injected into it.
     */
    @InjectMocks
    private UserController userController;

    private User testUser;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {

        /*
         * Create a sample user that represents a typical employee in the system.
         * We reuse this across all tests in this file.
         */
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setRole(UserRole.EMPLOYEE);
        testUser.setDepartment("Engineering");
        testUser.setStatus(UserStatus.ACTIVE);

        /*
         * Simulate an admin making the HTTP request.
         * userId=100 represents the admin who is performing operations like creating/updating users.
         */
        mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("userId", 100);
    }

    /*
     * ==================== TESTS FOR getAllUsers() ====================
     */

    @Test
    @DisplayName("getAllUsers() should return success with list of users")
    void getAllUsers_ShouldReturnUsers() {

        /*
         * ARRANGE: Make the fake service return a list containing our test user.
         * Arrays.asList() is just a quick way to create a list with one item.
         */
        when(userSvc.getAllUsers()).thenReturn(Arrays.asList(testUser));

        /*
         * ACT: Call the controller method.
         */
        ApiResponse<List<User>> response = userController.getAllUsers();

        /*
         * ASSERT: Check status message and that we got exactly 1 user back.
         */
        assertEquals("success", response.getStatus());
        assertEquals("Users retrieved", response.getMsg());
        assertEquals(1, response.getData().size());
    }

    /*
     * ==================== TESTS FOR getUserById() ====================
     */

    @Test
    @DisplayName("getUserById() should return success with user data")
    void getUserById_ShouldReturnUser() {

        /*
         * ARRANGE: When someone asks for user with ID 1, return our test user.
         */
        when(userSvc.getUserById(1)).thenReturn(testUser);

        ApiResponse<User> response = userController.getUserById(1);

        /*
         * ASSERT: Check we got the right user back.
         */
        assertEquals("success", response.getStatus());
        assertEquals("John Doe", response.getData().getName());
    }

    @Test
    @DisplayName("getUserById() should propagate exception when user not found")
    void getUserById_NotFound_ShouldPropagateException() {

        /*
         * ARRANGE: When someone asks for user ID 999 (doesn't exist),
         * throw a ResourceNotFoundException.
         */
        when(userSvc.getUserById(999)).thenThrow(new ResourceNotFoundException("User not found"));

        /*
         * ASSERT: The controller should NOT catch this exception - it should let it
         * bubble up to the GlobalExceptionHandler which converts it to a 404 response.
         */
        assertThrows(ResourceNotFoundException.class, () -> userController.getUserById(999));
    }

    /*
     * ==================== TESTS FOR createUser() ====================
     */

    @Test
    @DisplayName("createUser() should return success with created user")
    void createUser_ShouldReturnCreatedUser() {

        /*
         * ARRANGE: Build the request to create a new user.
         */
        CreateUserRequest req = new CreateUserRequest();
        req.setName("New User");
        req.setEmail("new@example.com");

        /*
         * When createUser is called with:
         * - any CreateUserRequest (any() matcher)
         * - userId exactly 100 (eq() matcher - the admin's ID from mockRequest)
         *
         * Return our testUser.
         */
        when(userSvc.createUser(any(CreateUserRequest.class), eq(100))).thenReturn(testUser);

        ApiResponse<User> response = userController.createUser(req, mockRequest);

        assertEquals("success", response.getStatus());
        assertEquals("User created", response.getMsg());
        assertNotNull(response.getData());
    }

    /*
     * ==================== TESTS FOR updateUser() ====================
     */

    @Test
    @DisplayName("updateUser() should return success with updated user")
    void updateUser_ShouldReturnUpdatedUser() {

        /*
         * ARRANGE: Build an update request with new name.
         */
        CreateUserRequest req = new CreateUserRequest();
        req.setName("Updated Name");

        /*
         * eq(1) = must be exactly userId 1 (the user being updated)
         * any(CreateUserRequest.class) = any update data
         * eq(100) = must be exactly admin userId 100 (who's making the update)
         */
        when(userSvc.updateUser(eq(1), any(CreateUserRequest.class), eq(100))).thenReturn(testUser);

        ApiResponse<User> response = userController.updateUser(1, req, mockRequest);

        assertEquals("success", response.getStatus());
        assertEquals("User updated", response.getMsg());
    }

    /*
     * ==================== TESTS FOR getTeam() ====================
     */

    @Test
    @DisplayName("getTeam() should return team members")
    void getTeam_ShouldReturnTeamMembers() {

        /*
         * ARRANGE: When someone asks for manager 50's team, return a list with our test user.
         */
        when(userSvc.getTeamMembers(50)).thenReturn(Arrays.asList(testUser));

        ApiResponse<List<User>> response = userController.getTeam(50);

        assertEquals("success", response.getStatus());
        assertEquals("Team members retrieved", response.getMsg());
        assertEquals(1, response.getData().size());
    }
}