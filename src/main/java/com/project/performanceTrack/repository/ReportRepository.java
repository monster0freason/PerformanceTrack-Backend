package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Report repository
@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {

    // Find reports by scope
    List<Report> findByScope(String scope);

    // Find reports by generated user
    List<Report> findByGeneratedBy_UserIdOrderByGeneratedDateDesc(Integer userId);
}