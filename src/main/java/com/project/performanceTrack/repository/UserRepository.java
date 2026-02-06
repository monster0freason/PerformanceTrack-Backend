package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.User;
import com.project.performanceTrack.enums.UserRole;
import com.project.performanceTrack.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// User repository for database operations
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Find users by role
    List<User> findByRole(UserRole role);

    // Find users by department
    List<User> findByDepartment(String department);

    // Find users by status
    List<User> findByStatus(UserStatus status);

    // Find team members by manager ID
    List<User> findByManager_UserId(Integer managerId);
}

