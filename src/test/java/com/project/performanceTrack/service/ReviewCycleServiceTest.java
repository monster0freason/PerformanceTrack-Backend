package com.project.performanceTrack.service;

import com.project.performanceTrack.dto.CreateReviewCycleRequest;
import com.project.performanceTrack.entity.ReviewCycle;
import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.ReviewCycleStatus;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.exception.ResourceNotFoundException;
import com.project.performanceTrack.repository.ReviewCycleRepository;
import com.project.performanceTrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewCycleService.
 *
 * Tests CRUD operations for review cycles (managed by Admin).
 */
@ExtendWith(MockitoExtension.class)
class ReviewCycleServiceTest {

    @Mock
    private ReviewCycleRepository cycleRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReviewCycleService cycleService;

    private ReviewCycle testCycle;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testCycle = new ReviewCycle();
        testCycle.setCycleId(1);
        testCycle.setTitle("Q1 2025 Performance Review");
        testCycle.setStartDate(LocalDate.of(2025, 1, 1));
        testCycle.setEndDate(LocalDate.of(2025, 3, 31));
        testCycle.setStatus(ReviewCycleStatus.ACTIVE);

        adminUser = new User();
        adminUser.setUserId(100);
        adminUser.setName("Admin");
        adminUser.setRole(UserRole.ADMIN);
    }

    // ==================== getAllCycles() ====================

    @Test
    @DisplayName("getAllCycles() should return all review cycles")
    void getAllCycles_ShouldReturnAllCycles() {
        when(cycleRepo.findAll()).thenReturn(Arrays.asList(testCycle));

        List<ReviewCycle> result = cycleService.getAllCycles();

        assertEquals(1, result.size());
        assertEquals("Q1 2025 Performance Review", result.get(0).getTitle());
    }

    // ==================== getCycleById() ====================

    @Test
    @DisplayName("getCycleById() should return cycle when found")
    void getCycleById_WithValidId_ShouldReturnCycle() {
        when(cycleRepo.findById(1)).thenReturn(Optional.of(testCycle));

        ReviewCycle result = cycleService.getCycleById(1);

        assertNotNull(result);
        assertEquals(1, result.getCycleId());
    }

    @Test
    @DisplayName("getCycleById() should throw ResourceNotFoundException when not found")
    void getCycleById_WithInvalidId_ShouldThrowException() {
        when(cycleRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cycleService.getCycleById(999));
    }

    // ==================== getActiveCycle() ====================

    @Test
    @DisplayName("getActiveCycle() should return active cycle")
    void getActiveCycle_WhenActiveExists_ShouldReturnCycle() {
        when(cycleRepo.findFirstByStatusOrderByStartDateDesc(ReviewCycleStatus.ACTIVE))
                .thenReturn(Optional.of(testCycle));

        ReviewCycle result = cycleService.getActiveCycle();

        assertNotNull(result);
        assertEquals(ReviewCycleStatus.ACTIVE, result.getStatus());
    }

    @Test
    @DisplayName("getActiveCycle() should throw when no active cycle exists")
    void getActiveCycle_WhenNoActiveExists_ShouldThrowException() {
        when(cycleRepo.findFirstByStatusOrderByStartDateDesc(ReviewCycleStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cycleService.getActiveCycle());
    }

    // ==================== createCycle() ====================

    @Test
    @DisplayName("createCycle() should create and return a new review cycle")
    void createCycle_ShouldCreateCycleSuccessfully() {
        CreateReviewCycleRequest req = new CreateReviewCycleRequest();
        req.setTitle("Q2 2025 Review");
        req.setStartDt(LocalDate.of(2025, 4, 1));
        req.setEndDt(LocalDate.of(2025, 6, 30));
        req.setStatus(ReviewCycleStatus.ACTIVE);

        when(cycleRepo.save(any(ReviewCycle.class))).thenAnswer(invocation -> {
            ReviewCycle saved = invocation.getArgument(0);
            saved.setCycleId(2);
            return saved;
        });
        when(userRepo.findById(100)).thenReturn(Optional.of(adminUser));

        ReviewCycle result = cycleService.createCycle(req, 100);

        assertNotNull(result);
        assertEquals("Q2 2025 Review", result.getTitle());
        assertEquals(ReviewCycleStatus.ACTIVE, result.getStatus());

        // Verify audit log
        verify(auditLogService).logAudit(eq(adminUser), eq("REVIEW_CYCLE_CREATED"),
                anyString(), eq("ReviewCycle"), anyInt(), eq("SUCCESS"));
    }

    // ==================== updateCycle() ====================

    @Test
    @DisplayName("updateCycle() should update the existing cycle")
    void updateCycle_ShouldUpdateSuccessfully() {
        CreateReviewCycleRequest req = new CreateReviewCycleRequest();
        req.setTitle("Updated Cycle Title");
        req.setStartDt(LocalDate.of(2025, 1, 1));
        req.setEndDt(LocalDate.of(2025, 6, 30));
        req.setStatus(ReviewCycleStatus.CLOSED);

        when(cycleRepo.findById(1)).thenReturn(Optional.of(testCycle));
        when(cycleRepo.save(any(ReviewCycle.class))).thenReturn(testCycle);
        when(userRepo.findById(100)).thenReturn(Optional.of(adminUser));

        ReviewCycle result = cycleService.updateCycle(1, req, 100);

        assertNotNull(result);
        assertEquals("Updated Cycle Title", result.getTitle());
        assertEquals(ReviewCycleStatus.CLOSED, result.getStatus());
    }

    @Test
    @DisplayName("updateCycle() should throw ResourceNotFoundException when cycle not found")
    void updateCycle_WithInvalidId_ShouldThrowException() {
        when(cycleRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cycleService.updateCycle(999, new CreateReviewCycleRequest(), 100));
    }
}
