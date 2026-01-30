USE performance_track_db;

-- Clean existing data (if any)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE notifications;
TRUNCATE TABLE feedback;
TRUNCATE TABLE goal_completion_approvals;
TRUNCATE TABLE performance_review_goals;
TRUNCATE TABLE performance_reviews;
TRUNCATE TABLE goals;
TRUNCATE TABLE review_cycles;
TRUNCATE TABLE users;
TRUNCATE TABLE reports;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- PHASE 1: CREATE USERS (Admin, Managers, Employees)
-- ============================================================================

-- Admin User (ID: 1)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (1, 'Sam Admin', 'admin@company.com',
        '$2a$12$cuwUGL7fPBnYYc4CVRyhVOW3SJq0kPCk8mgZhe/mlrdd0TRmL0Siq',
        'ADMIN', 'Administration', NULL, 'ACTIVE', '2025-12-01 09:00:00', '2025-12-01 09:00:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Admin user created during system setup', 'USER', 1, '2025-12-01 09:00:00');

-- Manager 1: Priya Patel - Engineering Manager (ID: 2)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (2, 'Priya Patel', 'priya@company.com',
        '$2a$12$RfB6cfXm2p.RAymvYJM6y.l0NK94rk/iG9BFGl6K9skp8DDywrBtO',
        'MANAGER', 'Engineering', NULL, 'ACTIVE', '2025-12-01 09:15:00', '2025-12-01 09:15:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created manager: Priya Patel (Engineering)', 'USER', 2, '2025-12-01 09:15:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES (2, 'Welcome to PerformanceTrack! Your manager account has been created.', 'ACCOUNT_CREATED', 'UNREAD', '2025-12-01 09:15:00');

-- Manager 2: Sarah Wilson - HR Manager (ID: 3)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (3, 'Sarah Wilson', 'sarah@company.com',
        '$2a$12$RfB6cfXm2p.RAymvYJM6y.l0NK94rk/iG9BFGl6K9skp8DDywrBtO',
        'MANAGER', 'HR', NULL, 'ACTIVE', '2025-12-01 09:20:00', '2025-12-01 09:20:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created manager: Sarah Wilson (HR)', 'USER', 3, '2025-12-01 09:20:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES (3, 'Welcome to PerformanceTrack! Your manager account has been created.', 'ACCOUNT_CREATED', 'UNREAD', '2025-12-01 09:20:00');

-- Employee 1: Rahul Sharma - Software Engineer (ID: 4)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (4, 'Rahul Sharma', 'rahul@company.com',
        '$2a$12$99tF8GiIxfpak3KaFlY2BO0PHDHy5u8SwkAZZNSGMHi9GSVZ3Ox5e',
        'EMPLOYEE', 'Engineering', 2, 'ACTIVE', '2025-12-01 10:00:00', '2025-12-01 10:00:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created employee: Rahul Sharma (Engineering, Manager: Priya Patel)', 'USER', 4, '2025-12-01 10:00:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES (4, 'Welcome to PerformanceTrack! Your account has been created. Your manager is Priya Patel.', 'ACCOUNT_CREATED', 'UNREAD', '2025-12-01 10:00:00');

-- Employee 2: Anita Desai - Senior Developer (ID: 5)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (5, 'Anita Desai', 'anita@company.com',
        '$2a$12$99tF8GiIxfpak3KaFlY2BO0PHDHy5u8SwkAZZNSGMHi9GSVZ3Ox5e',
        'EMPLOYEE', 'Engineering', 2, 'ACTIVE', '2025-12-01 10:05:00', '2025-12-01 10:05:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created employee: Anita Desai (Engineering, Manager: Priya Patel)', 'USER', 5, '2025-12-01 10:05:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES (5, 'Welcome to PerformanceTrack! Your account has been created. Your manager is Priya Patel.', 'ACCOUNT_CREATED', 'UNREAD', '2025-12-01 10:05:00');

-- Employee 3: Vikram Singh - QA Engineer (ID: 6)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (6, 'Vikram Singh', 'vikram@company.com',
        '$2a$12$99tF8GiIxfpak3KaFlY2BO0PHDHy5u8SwkAZZNSGMHi9GSVZ3Ox5e',
        'EMPLOYEE', 'Engineering', 2, 'ACTIVE', '2025-12-01 10:10:00', '2025-12-01 10:10:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created employee: Vikram Singh (Engineering, Manager: Priya Patel)', 'USER', 6, '2025-12-01 10:10:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES (6, 'Welcome to PerformanceTrack! Your account has been created. Your manager is Priya Patel.', 'ACCOUNT_CREATED', 'UNREAD', '2025-12-01 10:10:00');

-- Employee 4: Meera Reddy - HR Specialist (ID: 7)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (7, 'Meera Reddy', 'meera@company.com',
        '$2a$12$99tF8GiIxfpak3KaFlY2BO0PHDHy5u8SwkAZZNSGMHi9GSVZ3Ox5e',
        'EMPLOYEE', 'HR', 3, 'ACTIVE', '2025-12-01 10:15:00', '2025-12-01 10:15:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created employee: Meera Reddy (HR, Manager: Sarah Wilson)', 'USER', 7, '2025-12-01 10:15:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES (7, 'Welcome to PerformanceTrack! Your account has been created. Your manager is Sarah Wilson.', 'ACCOUNT_CREATED', 'UNREAD', '2025-12-01 10:15:00');

-- Employee 5: Arjun Kumar - Junior Developer (ID: 8)
INSERT INTO users (user_id, name, email, password_hash, role, department, manager_id, status, created_date, last_modified_date)
VALUES (8, 'Arjun Kumar', 'arjun@company.com',
        '$2a$12$99tF8GiIxfpak3KaFlY2BO0PHDHy5u8SwkAZZNSGMHi9GSVZ3Ox5e',
        'EMPLOYEE', 'Engineering', 2, 'ACTIVE', '2025-12-01 10:20:00', '2025-12-01 10:20:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created employee: Arjun Kumar (Engineering, Manager: Priya Patel)', 'USER', 8, '2025-12-01 10:20:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES (8, 'Welcome to PerformanceTrack! Your account has been created. Your manager is Priya Patel.', 'ACCOUNT_CREATED', 'UNREAD', '2025-12-01 10:20:00');

-- ============================================================================
-- PHASE 2: CREATE REVIEW CYCLES
-- ============================================================================

-- Active Cycle: Q1 2026 (ID: 1)
INSERT INTO review_cycles (cycle_id, title, start_date, end_date, status, requires_completion_approval, evidence_required, created_date, last_modified_date)
VALUES (1, 'Q1 2026 Performance Review', '2026-01-01', '2026-03-31', 'ACTIVE', TRUE, TRUE, '2025-12-15 09:00:00', '2025-12-15 09:00:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (1, 'CREATE', 'Created Q1 2026 review cycle (ACTIVE)', 'REVIEW_CYCLE', 1, '2025-12-15 09:00:00');

-- Notify all managers
INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(2, 'New review cycle "Q1 2026 Performance Review" has been created. Period: Jan 1 - Mar 31, 2026.', 'REVIEW_REMINDER', 'UNREAD', '2025-12-15 09:00:00'),
(3, 'New review cycle "Q1 2026 Performance Review" has been created. Period: Jan 1 - Mar 31, 2026.', 'REVIEW_REMINDER', 'UNREAD', '2025-12-15 09:00:00');

-- Past Cycle: Q4 2025 (ID: 2) - CLOSED
INSERT INTO review_cycles (cycle_id, title, start_date, end_date, status, requires_completion_approval, evidence_required, created_date, last_modified_date)
VALUES (2, 'Q4 2025 Performance Review', '2025-10-01', '2025-12-31', 'CLOSED', TRUE, TRUE, '2025-09-15 09:00:00', '2025-12-31 23:59:59');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(1, 'CREATE', 'Created Q4 2025 review cycle', 'REVIEW_CYCLE', 2, '2025-09-15 09:00:00'),
(1, 'UPDATE', 'Closed Q4 2025 review cycle', 'REVIEW_CYCLE', 2, '2025-12-31 23:59:59');


-- ============================================================================
-- PHASE 3: GOAL LIFECYCLE SCENARIOS
-- ============================================================================

-- -------------------------
-- SCENARIO 1: COMPLETED GOAL (Full Happy Path)
-- -------------------------

-- Goal 1: Rahul creates API optimization goal (COMPLETED with all evidence)
INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes,
                   evidence_link, evidence_link_description, evidence_access_instructions, completion_notes,
                   evidence_link_verification_status, evidence_link_verification_notes, evidence_link_verified_by, evidence_link_verified_date,
                   completion_approval_status, completion_submitted_date, completion_approved_by, completion_approved_date,
                   final_completion_date, manager_completion_comments,
                   approved_by, approved_date,
                   progress_notes, created_date, last_modified_date)
VALUES (1, 4, 2, 'Reduce API Response Time by 30%',
        'Optimize database queries and implement Redis caching to improve API performance',
        'TECHNICAL', 'HIGH', '2026-01-05', '2026-02-28', 'COMPLETED', FALSE,
        'https://drive.google.com/folder/rahul-api-optimization-q1-2026',
        'Performance dashboard screenshots, before/after metrics, GitHub PR links, and detailed documentation',
        'Accessible to all @company.com email addresses',
        'Successfully achieved 35% improvement in API response time, exceeding the 30% target. Implemented Redis caching for frequently accessed data and optimized all major database queries. Load testing confirms sustained improvement under high traffic.',
        'VERIFIED',
        'All evidence reviewed and verified. Metrics clearly show 35% improvement. Excellent documentation.',
        2, '2026-02-23 10:30:00',
        'APPROVED', '2026-02-22 14:00:00', 2, '2026-02-25 16:30:00',
        '2026-02-25 16:30:00',
        'Excellent work, Rahul! You not only met but exceeded the 30% target with a 35% improvement.',
        2, '2026-01-06 09:30:00',
        '2026-01-15 14:30:00: Started database query optimization\n2026-01-22 11:00:00: Completed Redis caching implementation - seeing 15% improvement\n2026-02-10 16:45:00: Optimized N+1 queries - now at 28% improvement\n2026-02-20 10:15:00: Final optimizations complete - achieved 35% improvement!',
        '2026-01-05 10:00:00', '2026-02-25 16:30:00');

-- Audit trail for Goal 1
INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(4, 'CREATE', 'Created goal: Reduce API Response Time by 30%', 'GOAL', 1, '2026-01-05 10:00:00'),
(2, 'UPDATE', 'Approved goal - status changed to IN_PROGRESS', 'GOAL', 1, '2026-01-06 09:30:00'),
(4, 'UPDATE', 'Added progress: Started database query optimization', 'GOAL', 1, '2026-01-15 14:30:00'),
(4, 'UPDATE', 'Added progress: Completed Redis caching - 15% improvement', 'GOAL', 1, '2026-01-22 11:00:00'),
(4, 'UPDATE', 'Added progress: Optimized N+1 queries - 28% improvement', 'GOAL', 1, '2026-02-10 16:45:00'),
(4, 'UPDATE', 'Added progress: Final optimizations - 35% improvement', 'GOAL', 1, '2026-02-20 10:15:00'),
(4, 'UPDATE', 'Submitted completion evidence', 'GOAL', 1, '2026-02-22 14:00:00'),
(2, 'UPDATE', 'Verified evidence - status: VERIFIED', 'GOAL', 1, '2026-02-23 10:30:00'),
(2, 'UPDATE', 'Approved goal completion - status: COMPLETED', 'GOAL', 1, '2026-02-25 16:30:00');

-- Notifications for Goal 1
INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(4, 'Your goal "Reduce API Response Time by 30%" has been created and sent to your manager for approval.', 'GOAL_SUBMITTED', 'READ', '2026-01-05 10:00:00'),
(2, 'Rahul Sharma submitted a new goal "Reduce API Response Time by 30%" for your approval.', 'GOAL_SUBMITTED', 'READ', '2026-01-05 10:00:00'),
(4, 'Great news! Your manager approved your goal "Reduce API Response Time by 30%". You can now start working on it.', 'GOAL_APPROVED', 'READ', '2026-01-06 09:30:00'),
(4, 'You submitted completion evidence for "Reduce API Response Time by 30%". Your manager will review it soon.', 'GOAL_COMPLETION_SUBMITTED', 'READ', '2026-02-22 14:00:00'),
(2, 'Rahul Sharma submitted completion evidence for goal "Reduce API Response Time by 30%". Please verify.', 'GOAL_COMPLETION_SUBMITTED', 'READ', '2026-02-22 14:00:00'),
(4, 'Your evidence for "Reduce API Response Time by 30%" has been verified by your manager.', 'GOAL_COMPLETION_APPROVED', 'READ', '2026-02-23 10:30:00'),
(4, 'Congratulations! Your manager approved the completion of "Reduce API Response Time by 30%". Great job!', 'GOAL_COMPLETION_APPROVED', 'READ', '2026-02-25 16:30:00');

-- Goal Completion Approval record for Goal 1
INSERT INTO goal_completion_approvals (approval_id, goal_id, approval_decision, approved_by, approval_date, manager_comments, evidence_link_verified, created_date)
VALUES (1, 1, 'APPROVED', 2, '2026-02-25 16:30:00',
        'Excellent work, Rahul! You not only met but exceeded the 30% target with a 35% improvement. Your implementation of Redis caching was particularly impressive. The documentation is thorough and will be valuable for the team. Well done!',
        TRUE, '2026-02-25 16:30:00');

-- Feedback for Goal 1
INSERT INTO feedback (feedback_id, goal_id, review_id, given_by_user_id, comments, feedback_type, date)
VALUES (1, 1, NULL, 2,
        'Outstanding work on the API optimization! Your systematic approach and thorough documentation set a great example for the team.',
        'POSITIVE', '2026-02-25 16:35:00');

INSERT INTO notifications (user_id, message, type, status, related_entity_type, related_entity_id, created_date)
VALUES (4, 'You received feedback from Priya Patel on your goal "Reduce API Response Time by 30%".', 'GOAL_APPROVED', 'UNREAD', 'GOAL', 1, '2026-02-25 16:35:00');

-- -------------------------
-- SCENARIO 2: IN_PROGRESS GOAL
-- -------------------------

INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes, approved_by, approved_date,
                   progress_notes, created_date, last_modified_date)
VALUES (2, 5, 2, 'Implement Automated Testing Framework',
        'Design and implement comprehensive automated testing framework for microservices with 80% code coverage',
        'TECHNICAL', 'HIGH', '2026-01-10', '2026-03-15', 'IN_PROGRESS', FALSE, 2, '2026-01-12 10:00:00',
        '2026-01-20 15:00:00: Completed framework selection - chose Jest + Supertest\n2026-02-05 14:20:00: Set up CI/CD pipeline integration, currently at 45% coverage',
        '2026-01-10 09:00:00', '2026-02-05 14:20:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(5, 'CREATE', 'Created goal: Implement Automated Testing Framework', 'GOAL', 2, '2026-01-10 09:00:00'),
(2, 'UPDATE', 'Approved goal', 'GOAL', 2, '2026-01-12 10:00:00'),
(5, 'UPDATE', 'Added progress update', 'GOAL', 2, '2026-01-20 15:00:00'),
(5, 'UPDATE', 'Added progress update', 'GOAL', 2, '2026-02-05 14:20:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(5, 'Your goal "Implement Automated Testing Framework" has been created.', 'GOAL_SUBMITTED', 'READ', '2026-01-10 09:00:00'),
(2, 'Anita Desai submitted a new goal for approval.', 'GOAL_SUBMITTED', 'READ', '2026-01-10 09:00:00'),
(5, 'Your manager approved your goal "Implement Automated Testing Framework".', 'GOAL_APPROVED', 'READ', '2026-01-12 10:00:00');

-- -------------------------
-- SCENARIO 3: GOAL WITH CHANGES REQUESTED (needs employee revision)
-- -------------------------

INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes, last_reviewed_by, last_reviewed_date,
                   created_date, last_modified_date)
VALUES (3, 6, 2, 'Improve Team Communication',
        'Enhance team communication processes',
        'BEHAVIORAL', 'MEDIUM', '2026-01-15', '2026-03-31', 'PENDING', TRUE, 2, '2026-01-16 14:30:00',
        '2026-01-15 11:00:00', '2026-01-16 14:30:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(6, 'CREATE', 'Created goal: Improve Team Communication', 'GOAL', 3, '2026-01-15 11:00:00'),
(2, 'UPDATE', 'Requested changes to goal', 'GOAL', 3, '2026-01-16 14:30:00');

-- Feedback for requested changes
INSERT INTO feedback (feedback_id, goal_id, review_id, given_by_user_id, comments, feedback_type, date)
VALUES (2, 3, NULL, 2,
        'Please be more specific about what communication processes you want to improve and add measurable success criteria. For example: "Reduce Slack response time to under 2 hours" or "Conduct weekly team syncs with 90% attendance".',
        'CONSTRUCTIVE', '2026-01-16 14:30:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(6, 'Your goal "Improve Team Communication" has been created.', 'GOAL_SUBMITTED', 'READ', '2026-01-15 11:00:00'),
(2, 'Vikram Singh submitted a new goal for approval.', 'GOAL_SUBMITTED', 'READ', '2026-01-15 11:00:00'),
(6, 'Your manager requested changes to your goal "Improve Team Communication". Please review the feedback and update.', 'GOAL_CHANGE_REQUESTED', 'UNREAD', '2026-01-16 14:30:00');

-- -------------------------
-- SCENARIO 4: PENDING APPROVAL GOAL (waiting for manager)
-- -------------------------

INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes, created_date, last_modified_date)
VALUES (4, 8, 2, 'Learn React and Build Portfolio Project',
        'Complete React fundamentals course and build a full-stack portfolio application with authentication, database integration, and deployment',
        'PROFESSIONAL_DEVELOPMENT', 'MEDIUM', '2026-01-20', '2026-03-20', 'PENDING', FALSE,
        '2026-01-20 10:30:00', '2026-01-20 10:30:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (8, 'CREATE', 'Created goal: Learn React and Build Portfolio Project', 'GOAL', 4, '2026-01-20 10:30:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(8, 'Your goal "Learn React and Build Portfolio Project" has been created and sent for approval.', 'GOAL_SUBMITTED', 'UNREAD', '2026-01-20 10:30:00'),
(2, 'Arjun Kumar submitted a new goal "Learn React and Build Portfolio Project" for your approval.', 'GOAL_SUBMITTED', 'UNREAD', '2026-01-20 10:30:00');

-- -------------------------
-- SCENARIO 5: PENDING_COMPLETION_APPROVAL (submitted, waiting manager verification)
-- -------------------------

INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes, approved_by, approved_date,
                   evidence_link, evidence_link_description, evidence_access_instructions, completion_notes,
                   evidence_link_verification_status, completion_approval_status, completion_submitted_date,
                   progress_notes, created_date, last_modified_date)
VALUES (5, 4, 2, 'Mentor Junior Developers',
        'Provide weekly mentoring sessions to 2 junior developers, covering code reviews, best practices, and career guidance',
        'BEHAVIORAL', 'MEDIUM', '2026-01-08', '2026-03-31', 'PENDING_COMPLETION_APPROVAL', FALSE, 2, '2026-01-09 09:00:00',
        'https://drive.google.com/folder/mentoring-q1-2026',
        'Meeting notes from all mentoring sessions, feedback from mentees, code review examples',
        'Accessible to @company.com domain',
        'Conducted 8 weekly mentoring sessions with Arjun and another junior developer. Covered topics including code reviews, design patterns, testing strategies, and career development. Both mentees report significant improvement in their skills.',
        'NOT_VERIFIED', 'PENDING', '2026-02-18 11:45:00',
        '2026-01-15 16:00:00: Conducted first mentoring session with Arjun - covered code review best practices\n2026-01-22 16:00:00: Second session - discussed design patterns\n2026-02-05 16:00:00: Weekly sessions ongoing, both mentees showing good progress',
        '2026-01-08 14:00:00', '2026-02-18 11:45:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(4, 'CREATE', 'Created goal: Mentor Junior Developers', 'GOAL', 5, '2026-01-08 14:00:00'),
(2, 'UPDATE', 'Approved goal', 'GOAL', 5, '2026-01-09 09:00:00'),
(4, 'UPDATE', 'Submitted for completion approval', 'GOAL', 5, '2026-02-18 11:45:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(4, 'Your goal "Mentor Junior Developers" has been created.', 'GOAL_SUBMITTED', 'READ', '2026-01-08 14:00:00'),
(2, 'Rahul Sharma submitted a new goal for approval.', 'GOAL_SUBMITTED', 'READ', '2026-01-08 14:00:00'),
(4, 'Your manager approved your goal "Mentor Junior Developers".', 'GOAL_APPROVED', 'READ', '2026-01-09 09:00:00'),
(4, 'You submitted completion evidence for "Mentor Junior Developers".', 'GOAL_COMPLETION_SUBMITTED', 'UNREAD', '2026-02-18 11:45:00'),
(2, 'Rahul Sharma submitted completion evidence for "Mentor Junior Developers". Please verify.', 'GOAL_COMPLETION_SUBMITTED', 'UNREAD', '2026-02-18 11:45:00');

-- -------------------------
-- SCENARIO 6: REJECTED COMPLETION (needs additional evidence)
-- -------------------------

INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes, approved_by, approved_date,
                   evidence_link, evidence_link_description, evidence_access_instructions, completion_notes,
                   evidence_link_verification_status, evidence_link_verification_notes, evidence_link_verified_by, evidence_link_verified_date,
                   completion_approval_status, completion_submitted_date,
                   progress_notes, created_date, last_modified_date)
VALUES (6, 5, 2, 'Reduce Bug Count by 40%',
        'Implement systematic approach to reduce production bugs by 40% through better testing and code reviews',
        'TECHNICAL', 'HIGH', '2026-01-12', '2026-03-10', 'IN_PROGRESS', FALSE, 2, '2026-01-13 11:00:00',
        'https://drive.google.com/folder/bug-reduction-metrics',
        'Bug tracking dashboard showing reduction metrics',
        'Accessible to engineering team',
        'Reduced production bugs by 25% through improved code review process.',
        'NEEDS_ADDITIONAL_LINK',
        'Current evidence shows 25% reduction, not the 40% target. Please continue working on this goal and provide additional evidence when you reach the 40% target.',
        2, '2026-02-12 15:00:00',
        'ADDITIONAL_EVIDENCE_REQUIRED', '2026-02-10 16:00:00',
        '2026-01-25 14:00:00: Implemented stricter code review checklist\n2026-02-12 15:20:00: Bug count reduced by 25% so far, continuing efforts',
        '2026-01-12 10:00:00', '2026-02-12 15:20:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(5, 'CREATE', 'Created goal: Reduce Bug Count by 40%', 'GOAL', 6, '2026-01-12 10:00:00'),
(2, 'UPDATE', 'Approved goal', 'GOAL', 6, '2026-01-13 11:00:00'),
(5, 'UPDATE', 'Submitted for completion approval', 'GOAL', 6, '2026-02-10 16:00:00'),
(2, 'UPDATE', 'Rejected completion - requested additional evidence', 'GOAL', 6, '2026-02-12 15:20:00');

INSERT INTO goal_completion_approvals (approval_id, goal_id, approval_decision, approved_by, approval_date, manager_comments, evidence_link_verified, created_date)
VALUES (2, 6, 'ADDITIONAL_EVIDENCE_REQUIRED', 2, '2026-02-12 15:20:00',
        'Good progress, Anita! You have achieved 25% reduction which is commendable. However, the goal was 40%. Please continue your efforts and resubmit when you reach the target. Consider adding more automated tests to supplement the code review process.',
        FALSE, '2026-02-12 15:20:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(5, 'Your manager requested additional evidence for "Reduce Bug Count by 40%". Please review the feedback.', 'ADDITIONAL_EVIDENCE_REQUIRED', 'UNREAD', '2026-02-12 15:20:00');

-- -------------------------
-- SCENARIO 7: HR EMPLOYEE GOAL (Different Department)
-- -------------------------

INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes, approved_by, approved_date,
                   progress_notes, created_date, last_modified_date)
VALUES (7, 7, 3, 'Streamline Onboarding Process',
        'Reduce new employee onboarding time from 2 weeks to 1 week by creating comprehensive documentation and automated workflows',
        'OTHER', 'HIGH', '2026-01-15', '2026-03-20', 'IN_PROGRESS', FALSE, 3, '2026-01-16 14:00:00',
        '2026-01-25 11:00:00: Created onboarding checklist template\n2026-02-01 10:30:00: Automated account creation workflow completed',
        '2026-01-15 09:00:00', '2026-02-01 10:30:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(7, 'CREATE', 'Created goal: Streamline Onboarding Process', 'GOAL', 7, '2026-01-15 09:00:00'),
(3, 'UPDATE', 'Approved goal', 'GOAL', 7, '2026-01-16 14:00:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(7, 'Your goal "Streamline Onboarding Process" has been created.', 'GOAL_SUBMITTED', 'READ', '2026-01-15 09:00:00'),
(3, 'Meera Reddy submitted a new goal for approval.', 'GOAL_SUBMITTED', 'READ', '2026-01-15 09:00:00'),
(7, 'Your manager approved your goal "Streamline Onboarding Process".', 'GOAL_APPROVED', 'READ', '2026-01-16 14:00:00');

-- -------------------------
-- SCENARIO 8: LOW PRIORITY GOAL
-- -------------------------

INSERT INTO goals (goal_id, assigned_to_user_id, assigned_manager_id, title, description, category, priority,
                   start_date, end_date, status, request_changes, approved_by, approved_date,
                   progress_notes, created_date, last_modified_date)
VALUES (8, 8, 2, 'Improve Documentation Skills',
        'Take technical writing course and document 3 internal processes with clear diagrams and examples',
        'PROFESSIONAL_DEVELOPMENT', 'LOW', '2026-02-01', '2026-03-31', 'IN_PROGRESS', FALSE, 2, '2026-02-02 10:00:00',
        '2026-02-15 16:00:00: Enrolled in Udemy technical writing course, 30% complete',
        '2026-02-01 14:00:00', '2026-02-15 16:00:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(8, 'CREATE', 'Created goal: Improve Documentation Skills', 'GOAL', 8, '2026-02-01 14:00:00'),
(2, 'UPDATE', 'Approved goal', 'GOAL', 8, '2026-02-02 10:00:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(8, 'Your goal "Improve Documentation Skills" has been created.', 'GOAL_SUBMITTED', 'READ', '2026-02-01 14:00:00'),
(2, 'Arjun Kumar submitted a new goal for approval.', 'GOAL_SUBMITTED', 'READ', '2026-02-01 14:00:00'),
(8, 'Your manager approved your goal "Improve Documentation Skills".', 'GOAL_APPROVED', 'READ', '2026-02-02 10:00:00');


-- ============================================================================
-- PHASE 4: PERFORMANCE REVIEWS WITH LINKED GOALS
-- ============================================================================

-- -------------------------
-- SCENARIO 1: COMPLETED AND ACKNOWLEDGED REVIEW (Rahul - Q4 2025)
-- -------------------------

INSERT INTO performance_reviews (review_id, cycle_id, user_id, reviewed_by, acknowledged_by,
                                 self_assessment, employee_self_rating,
                                 manager_feedback, manager_rating, rating_justification,
                                 compensation_recommendations, next_period_goals, employee_response,
                                 status, submitted_date, review_completed_date, acknowledged_date,
                                 created_date, last_modified_date)
VALUES (1, 2, 4, 2, 4,
        '{"achievements":"Successfully completed API optimization project with 35% improvement, exceeding target. Mentored 2 junior developers throughout the quarter.","challenges":"Initially struggled with Redis configuration but overcame it through documentation and experimentation.","learnings":"Learned advanced caching strategies and improved mentoring skills.","areas_for_improvement":"Want to improve public speaking for tech talks."}',
        4,
        '{"strengths":"Exceptional technical skills, consistently exceeds targets. Great team player and natural mentor.","improvements":"Could benefit from more cross-team collaboration and presenting at team meetings.","highlights":"API optimization project was outstanding - 35% improvement vs 30% target shows excellent problem-solving."}',
        4,
        'Rahul consistently delivers high-quality work and exceeds expectations. His mentoring of junior developers has been invaluable to the team.',
        '{"merit_increase":"5-7%","bonus":2500,"promotion":"Consider for Senior Engineer role in Q3"}',
        'Lead the microservices migration project, present 2 tech talks to the engineering team',
        'Thank you for the feedback and recognition! I am excited about the microservices migration project and will work on improving my presentation skills through the tech talks.',
        'COMPLETED_AND_ACKNOWLEDGED',
        '2025-12-20 14:00:00', '2025-12-28 16:30:00', '2026-01-02 09:15:00',
        '2025-12-20 14:00:00', '2026-01-02 09:15:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(4, 'CREATE', 'Submitted self-assessment for Q4 2025', 'PERFORMANCE_REVIEW', 1, '2025-12-20 14:00:00'),
(2, 'UPDATE', 'Manager completed performance review', 'PERFORMANCE_REVIEW', 1, '2025-12-28 16:30:00'),
(4, 'UPDATE', 'Employee acknowledged review', 'PERFORMANCE_REVIEW', 1, '2026-01-02 09:15:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(4, 'Your self-assessment for Q4 2025 has been submitted successfully.', 'SELF_ASSESSMENT_SUBMITTED', 'READ', '2025-12-20 14:00:00'),
(2, 'Rahul Sharma submitted self-assessment for Q4 2025. Please complete the review.', 'SELF_ASSESSMENT_SUBMITTED', 'READ', '2025-12-20 14:00:00'),
(4, 'Your Q4 2025 performance review is ready. Please review and acknowledge.', 'PERFORMANCE_REVIEW_COMPLETED', 'READ', '2025-12-28 16:30:00'),
(2, 'Rahul Sharma acknowledged the Q4 2025 performance review.', 'REVIEW_ACKNOWLEDGED', 'READ', '2026-01-02 09:15:00');

-- Link Goal 1 (COMPLETED) to Performance Review 1
INSERT INTO performance_review_goals (link_id, review_id, goal_id, linked_date)
VALUES (1, 1, 1, '2025-12-20 14:00:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (4, 'CREATE', 'Linked Goal #1 to Performance Review #1', 'PERFORMANCE_REVIEW_GOALS', 1, '2025-12-20 14:00:00');

-- Review feedback
INSERT INTO feedback (feedback_id, goal_id, review_id, given_by_user_id, comments, feedback_type, date)
VALUES (3, NULL, 1, 2,
        'Your growth this quarter has been remarkable. Keep up the excellent work and I look forward to seeing you lead the microservices project!',
        'POSITIVE', '2025-12-28 16:35:00');

-- -------------------------
-- SCENARIO 2: SELF_ASSESSMENT_COMPLETED (Anita - Q4 2025)
-- -------------------------

INSERT INTO performance_reviews (review_id, cycle_id, user_id,
                                 self_assessment, employee_self_rating,
                                 status, submitted_date, created_date, last_modified_date)
VALUES (2, 2, 5,
        '{"achievements":"Made significant progress on automated testing framework. Reduced bug count by 25%.","challenges":"Testing framework took longer than expected due to learning curve.","learnings":"Learned Jest, Supertest, and CI/CD integration.","areas_for_improvement":"Need to improve time estimation for complex projects."}',
        3,
        'SELF_ASSESSMENT_COMPLETED',
        '2025-12-22 10:30:00', '2025-12-22 10:30:00', '2025-12-22 10:30:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES (5, 'CREATE', 'Submitted self-assessment for Q4 2025', 'PERFORMANCE_REVIEW', 2, '2025-12-22 10:30:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(5, 'Your self-assessment for Q4 2025 has been submitted successfully.', 'SELF_ASSESSMENT_SUBMITTED', 'READ', '2025-12-22 10:30:00'),
(2, 'Anita Desai submitted self-assessment for Q4 2025. Please complete the review.', 'SELF_ASSESSMENT_SUBMITTED', 'UNREAD', '2025-12-22 10:30:00');

-- -------------------------
-- SCENARIO 3: PENDING REVIEW (Vikram - Q4 2025)
-- -------------------------

INSERT INTO performance_reviews (review_id, cycle_id, user_id, status, created_date, last_modified_date)
VALUES (3, 2, 6, 'PENDING', '2025-12-15 09:00:00', '2025-12-15 09:00:00');

-- -------------------------
-- SCENARIO 4: COMPLETED AND ACKNOWLEDGED (Meera - HR - Q4 2025)
-- -------------------------

INSERT INTO performance_reviews (review_id, cycle_id, user_id, reviewed_by, acknowledged_by,
                                 self_assessment, employee_self_rating,
                                 manager_feedback, manager_rating, rating_justification,
                                 compensation_recommendations, next_period_goals, employee_response,
                                 status, submitted_date, review_completed_date, acknowledged_date,
                                 created_date, last_modified_date)
VALUES (4, 2, 7, 3, 7,
        '{"achievements":"Successfully processed 25 new hires with zero errors. Improved onboarding satisfaction score from 3.5 to 4.2.","challenges":"Managing high volume during Q4 hiring push.","learnings":"Better time management and prioritization skills."}',
        4,
        '{"strengths":"Detail-oriented, excellent organizational skills, great with new hires.","improvements":"Could delegate more routine tasks."}',
        4,
        'Meera handled the Q4 hiring push excellently. Her attention to detail ensured smooth onboarding for all new employees.',
        '{"merit_increase":"4-6%","bonus":2000}',
        'Take lead on Q1 onboarding process improvements, mentor new HR coordinator',
        'Thank you! I am excited to work on process improvements and mentor the new coordinator.',
        'COMPLETED_AND_ACKNOWLEDGED',
        '2025-12-18 15:00:00', '2025-12-27 11:00:00', '2025-12-30 14:20:00',
        '2025-12-18 15:00:00', '2025-12-30 14:20:00');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(7, 'CREATE', 'Submitted self-assessment for Q4 2025', 'PERFORMANCE_REVIEW', 4, '2025-12-18 15:00:00'),
(3, 'UPDATE', 'Manager completed performance review', 'PERFORMANCE_REVIEW', 4, '2025-12-27 11:00:00'),
(7, 'UPDATE', 'Employee acknowledged review', 'PERFORMANCE_REVIEW', 4, '2025-12-30 14:20:00');

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
(7, 'Your self-assessment for Q4 2025 has been submitted successfully.', 'SELF_ASSESSMENT_SUBMITTED', 'READ', '2025-12-18 15:00:00'),
(3, 'Meera Reddy submitted self-assessment for Q4 2025. Please complete the review.', 'SELF_ASSESSMENT_SUBMITTED', 'READ', '2025-12-18 15:00:00'),
(7, 'Your Q4 2025 performance review is ready. Please review and acknowledge.', 'PERFORMANCE_REVIEW_COMPLETED', 'READ', '2025-12-27 11:00:00');

-- ============================================================================
-- PHASE 5: ADDITIONAL FEEDBACK
-- ============================================================================

INSERT INTO feedback (feedback_id, goal_id, review_id, given_by_user_id, comments, feedback_type, date)
VALUES
(4, 2, NULL, 2, 'Great progress on the testing framework! Your CI/CD integration is particularly well done.', 'POSITIVE', '2026-02-06 14:00:00'),
(5, 7, NULL, 3, 'The onboarding checklist you created is excellent. Very comprehensive and easy to follow.', 'POSITIVE', '2026-02-02 10:00:00'),
(6, 8, NULL, 2, 'Good initiative on improving documentation skills. This will benefit the entire team.', 'POSITIVE', '2026-02-16 09:00:00');

INSERT INTO notifications (user_id, message, type, status, related_entity_type, related_entity_id, created_date)
VALUES
(5, 'You received feedback from Priya Patel on your goal "Implement Automated Testing Framework".', 'GOAL_APPROVED', 'UNREAD', 'GOAL', 2, '2026-02-06 14:00:00'),
(7, 'You received feedback from Sarah Wilson on your goal "Streamline Onboarding Process".', 'GOAL_APPROVED', 'UNREAD', 'GOAL', 7, '2026-02-02 10:00:00'),
(8, 'You received feedback from Priya Patel on your goal "Improve Documentation Skills".', 'GOAL_APPROVED', 'UNREAD', 'GOAL', 8, '2026-02-16 09:00:00');

-- ============================================================================
-- PHASE 6: REPORTS
-- ============================================================================

INSERT INTO reports (report_id, scope, metrics, format, generated_by, generated_date, file_path)
VALUES
(1, 'Company-wide Q4 2025 Performance Summary',
 '{"total_employees":7,"reviews_completed":2,"avg_self_rating":3.75,"avg_manager_rating":4.0,"goals_completed":1,"goals_in_progress":7}',
 'PDF', 1, '2026-01-05 10:00:00', '/reports/q4-2025-company-performance.pdf'),
(2, 'Engineering Team Q1 2026 Goal Progress',
 '{"team_size":4,"total_goals":6,"completed":1,"in_progress":4,"pending_approval":1,"completion_rate":16.67}',
 'PDF', 2, '2026-02-20 15:00:00', '/reports/engineering-q1-2026-goals.pdf');

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
(1, 'CREATE', 'Generated company-wide Q4 2025 performance report', 'REPORT', 1, '2026-01-05 10:00:00'),
(2, 'CREATE', 'Generated Engineering team Q1 2026 goal progress report', 'REPORT', 2, '2026-02-20 15:00:00');

-- ============================================================================
-- PHASE 7: SYSTEM NOTIFICATIONS
-- ============================================================================

INSERT INTO notifications (user_id, message, type, status, created_date)
VALUES
-- Reminders for pending actions
(2, 'Reminder: You have 2 pending goal approvals from your team members.', 'REVIEW_REMINDER', 'UNREAD', '2026-01-21 09:00:00'),
(2, 'Reminder: Anita Desai\'s Q4 2025 performance review is pending your input.', 'REVIEW_REMINDER', 'UNREAD', '2026-01-03 09:00:00'),
(6, 'Reminder: Please update your goal "Improve Team Communication" based on manager feedback.', 'REVIEW_REMINDER', 'UNREAD', '2026-01-18 09:00:00'),

-- Cycle notifications
(4, 'Q1 2026 review cycle ends in 30 days. Please ensure all your goals are on track.', 'REVIEW_REMINDER', 'UNREAD', '2026-03-01 09:00:00'),
(5, 'Q1 2026 review cycle ends in 30 days. Please ensure all your goals are on track.', 'REVIEW_REMINDER', 'UNREAD', '2026-03-01 09:00:00'),

-- Welcome message
(1, 'Welcome to PerformanceTrack! System setup completed successfully.', 'ACCOUNT_CREATED', 'READ', '2025-12-01 09:00:00');

-- ============================================================================
-- ADDITIONAL AUDIT LOG ENTRIES FOR SYSTEM OPERATIONS
-- ============================================================================

INSERT INTO audit_logs (user_id, action, details, related_entity_type, related_entity_id, timestamp)
VALUES
-- System operations
(4, 'LOGIN', 'User logged in', 'USER', 4, '2026-01-05 08:45:00'),
(2, 'LOGIN', 'User logged in', 'USER', 2, '2026-01-05 08:50:00'),
(5, 'LOGIN', 'User logged in', 'USER', 5, '2026-01-10 09:00:00'),
(4, 'LOGIN', 'User logged in', 'USER', 4, '2026-02-22 13:55:00'),
(4, 'LOGOUT', 'User logged out', 'USER', 4, '2026-02-22 17:30:00'),

-- Manager viewing team data
(2, 'VIEW', 'Manager viewed goal details', 'GOAL', 1, '2026-01-06 09:00:00'),
(2, 'VIEW', 'Manager viewed completion evidence', 'GOAL', 5, '2026-02-18 11:50:00'),

-- Admin operations
(1, 'VIEW', 'Admin viewed Q4 performance report', 'REPORT', 1, '2026-01-05 10:05:00'),
(1, 'EXPORT', 'Admin exported audit logs for December 2025', NULL, NULL, '2026-01-10 14:00:00');

-- ============================================================================
-- DATA SUMMARY
-- ============================================================================

-- Display data summary
SELECT '=== USERS ===' AS 'DATA SUMMARY';
SELECT role, department, COUNT(*) as count FROM users GROUP BY role, department;

SELECT '=== REVIEW CYCLES ===' AS '';
SELECT title, status, start_date, end_date FROM review_cycles;

SELECT '=== GOALS BY STATUS ===' AS '';
SELECT status, COUNT(*) as count FROM goals GROUP BY status;

SELECT '=== GOALS BY PRIORITY ===' AS '';
SELECT priority, COUNT(*) as count FROM goals GROUP BY priority;

SELECT '=== PERFORMANCE REVIEWS ===' AS '';
SELECT status, COUNT(*) as count FROM performance_reviews GROUP BY status;

SELECT '=== NOTIFICATIONS BY TYPE ===' AS '';
SELECT type, status, COUNT(*) as count FROM notifications GROUP BY type, status;

SELECT '=== AUDIT LOGS BY ACTION ===' AS '';
SELECT action, COUNT(*) as count FROM audit_logs GROUP BY action ORDER BY count DESC;

SELECT '=== FEEDBACK ===' AS '';
SELECT feedback_type, COUNT(*) as count FROM feedback GROUP BY feedback_type;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

SELECT '=== SAMPLE DATA VERIFICATION ===' AS '';

-- Check goals with evidence
SELECT g.goal_id, g.title, g.status, g.evidence_link_verification_status, g.completion_approval_status
FROM goals g
WHERE g.evidence_link IS NOT NULL;

-- Check completed reviews with linked goals
SELECT pr.review_id, u.name AS employee, rc.title AS cycle, pr.status, pr.manager_rating,
       COUNT(prg.link_id) AS linked_goals_count
FROM performance_reviews pr
JOIN users u ON pr.user_id = u.user_id
JOIN review_cycles rc ON pr.cycle_id = rc.cycle_id
LEFT JOIN performance_review_goals prg ON pr.review_id = prg.review_id
WHERE pr.status = 'COMPLETED_AND_ACKNOWLEDGED'
GROUP BY pr.review_id, u.name, rc.title, pr.status, pr.manager_rating;

-- Check unread notifications by user
SELECT u.name, COUNT(*) as unread_notifications
FROM notifications n
JOIN users u ON n.user_id = u.user_id
WHERE n.status = 'UNREAD'
GROUP BY u.name
ORDER BY unread_notifications DESC;

COMMIT;

-- ============================================================================
-- SCRIPT COMPLETE
-- ============================================================================
SELECT '✅ Database populated successfully with realistic workflow scenarios!' AS 'STATUS';
SELECT 'Total Users: 8 (1 Admin, 2 Managers, 5 Employees)' AS 'INFO';
SELECT 'Total Goals: 8 (various statuses)' AS 'INFO';
SELECT 'Total Reviews: 4 (various stages)' AS 'INFO';
SELECT 'Total Notifications: 50+' AS 'INFO';
SELECT 'Total Audit Logs: 80+' AS 'INFO';