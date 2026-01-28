package com.project.performanceTrack.enums;

// Performance review status
public enum PerformanceReviewStatus {
    PENDING,                        // Review not started
    SELF_ASSESSMENT_COMPLETED,      // Employee completed self-assessment
    MANAGER_REVIEW_COMPLETED,       // Manager completed review
    COMPLETED,                      // Review fully completed
    COMPLETED_AND_ACKNOWLEDGED      // Review completed and employee acknowledged
}
