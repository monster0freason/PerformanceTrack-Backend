# Pagination Implementation Guide

Here's every change, layer by layer, with before/after and explanations.

## 1. NEW FILE: PageResponse.java
This doesn't exist yet. It's a simple DTO that wraps Spring's Page object into a clean JSON response.

```java
// NEW FILE: src/main/java/com/project/performanceTrack/dto/PageResponse.java

package com.project.performanceTrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    // Takes Spring's Page object and extracts the fields we care about
    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
    }
}
```

What this means: When the frontend calls GET /api/v1/goals?page=0&size=10, instead of getting a raw list, they get:

```json
{
  "status": "success",
  "msg": "Goals retrieved",
  "data": {
    "content": [ ...10 goals... ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 47,
    "totalPages": 5,
    "last": false
  }
}
```

## 2. CHANGE: ApiResponse.java -- add one helper method

NOW:

```java
public static <T> ApiResponse<T> success(String msg, T data) {
    return new ApiResponse<>("success", msg, data);
}
```

AFTER -- add this method below the existing ones:

```java
// Existing methods stay as-is, just add this:

public static <T> ApiResponse<PageResponse<T>> successPage(String msg, Page<T> page) {
    return new ApiResponse<>("success", msg, new PageResponse<>(page));
}
```

You'll also need to add `import org.springframework.data.domain.Page;` at the top.

What this means: A convenience method so controllers can do `ApiResponse.successPage("Goals retrieved", page)` instead of manually creating the PageResponse every time.

## 3. CHANGE: Repositories -- add paginated method signatures
The key thing: keep existing methods, add new overloaded ones alongside them. Spring Data auto-implements both.

### GoalRepository
NOW:

```java
List<Goal> findByAssignedToUser_UserId(Integer userId);
List<Goal> findByAssignedManager_UserId(Integer managerId);
```

AFTER:

```java
// Existing - keep these (used internally by other service methods)
List<Goal> findByAssignedToUser_UserId(Integer userId);
List<Goal> findByAssignedManager_UserId(Integer managerId);

// New - paginated versions (used by controllers)
Page<Goal> findByAssignedToUser_UserId(Integer userId, Pageable pageable);
Page<Goal> findByAssignedManager_UserId(Integer managerId, Pageable pageable);
```

You'll also need `import org.springframework.data.domain.Page;` and `import org.springframework.data.domain.Pageable;` at the top.

What this means: Same method name, but when you pass a Pageable parameter, Spring Data automatically adds LIMIT and OFFSET to the SQL query and returns a Page object with total count. The old List versions still work for internal use (like in deleteGoal or markAllAsRead).

### NotificationRepository
NOW:

```java
List<Notification> findByUser_UserIdOrderByCreatedDateDesc(Integer userId);
List<Notification> findByUser_UserIdAndStatusOrderByCreatedDateDesc(Integer userId, NotificationStatus status);
```

AFTER:

```java
// Existing - keep (used by markAllAsRead internally)
List<Notification> findByUser_UserIdOrderByCreatedDateDesc(Integer userId);
List<Notification> findByUser_UserIdAndStatusOrderByCreatedDateDesc(Integer userId, NotificationStatus status);

// New - paginated versions
Page<Notification> findByUser_UserId(Integer userId, Pageable pageable);
Page<Notification> findByUser_UserIdAndStatus(Integer userId, NotificationStatus status, Pageable pageable);
```

Note: The paginated versions drop OrderByCreatedDateDesc from the method name because the sort order will come from the Pageable object instead. No duplication of sort logic.

### AuditLogRepository
NOW:

```java
List<AuditLog> findByUser_UserIdOrderByTimestampDesc(Integer userId);
List<AuditLog> findByActionOrderByTimestampDesc(String action);
List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
```

AFTER:

```java
// Existing - keep
List<AuditLog> findByUser_UserIdOrderByTimestampDesc(Integer userId);
List<AuditLog> findByActionOrderByTimestampDesc(String action);
List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

// New - paginated versions
Page<AuditLog> findByUser_UserId(Integer userId, Pageable pageable);
Page<AuditLog> findByAction(String action, Pageable pageable);
Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
```

## 4. CHANGE: Services -- add paginated methods
Again, keep existing methods, add new ones. This way nothing breaks.

### GoalService
NOW:

```java
public List<Goal> getGoalsByUser(Integer userId) {
    return goalRepo.findByAssignedToUser_UserId(userId);
}

public List<Goal> getGoalsByManager(Integer mgrId) {
    return goalRepo.findByAssignedManager_UserId(mgrId);
}
```

AFTER -- add these new methods (keep old ones too):

```java
// Existing - keep
public List<Goal> getGoalsByUser(Integer userId) {
    return goalRepo.findByAssignedToUser_UserId(userId);
}

public List<Goal> getGoalsByManager(Integer mgrId) {
    return goalRepo.findByAssignedManager_UserId(mgrId);
}

// New - paginated
public Page<Goal> getGoalsByUser(Integer userId, Pageable pageable) {
    return goalRepo.findByAssignedToUser_UserId(userId, pageable);
}

public Page<Goal> getGoalsByManager(Integer mgrId, Pageable pageable) {
    return goalRepo.findByAssignedManager_UserId(mgrId, pageable);
}
```

You'll need `import org.springframework.data.domain.Page;` and `import org.springframework.data.domain.Pageable;`.

What this means: Method overloading. When the controller passes a Pageable, it calls the new version. Internal code that doesn't need pagination still calls the old List version.

### NotificationService
NOW:

```java
public List<Notification> getNotifications(Integer userId, String status) {
    if (status != null) {
        NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
        return notifRepo.findByUser_UserIdAndStatusOrderByCreatedDateDesc(userId, notifStatus);
    }
    return notifRepo.findByUser_UserIdOrderByCreatedDateDesc(userId);
}
```

AFTER -- add new method, keep old one:

```java
// Existing - keep (used by markAllAsRead)
public List<Notification> getNotifications(Integer userId, String status) {
    if (status != null) {
        NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
        return notifRepo.findByUser_UserIdAndStatusOrderByCreatedDateDesc(userId, notifStatus);
    }
    return notifRepo.findByUser_UserIdOrderByCreatedDateDesc(userId);
}

// New - paginated
public Page<Notification> getNotifications(Integer userId, String status, Pageable pageable) {
    if (status != null) {
        NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
        return notifRepo.findByUser_UserIdAndStatus(userId, notifStatus, pageable);
    }
    return notifRepo.findByUser_UserId(userId, pageable);
}
```

### AuditLogService
NOW:

```java
public List<AuditLog> getAuditLogs(Integer userId, String action, 
                                    LocalDateTime startDt, LocalDateTime endDt) {
    if (userId != null) {
        return auditRepo.findByUser_UserIdOrderByTimestampDesc(userId);
    } else if (action != null) {
        return auditRepo.findByActionOrderByTimestampDesc(action);
    } else if (startDt != null && endDt != null) {
        return auditRepo.findByTimestampBetweenOrderByTimestampDesc(startDt, endDt);
    } else {
        return auditRepo.findAll();
    }
}
```

AFTER -- add new method, keep old one:

```java
// Existing - keep
public List<AuditLog> getAuditLogs(Integer userId, String action,
                                    LocalDateTime startDt, LocalDateTime endDt) {
    // ... same as before
}

// New - paginated
public Page<AuditLog> getAuditLogs(Integer userId, String action,
                                    LocalDateTime startDt, LocalDateTime endDt,
                                    Pageable pageable) {
    if (userId != null) {
        return auditRepo.findByUser_UserId(userId, pageable);
    } else if (action != null) {
        return auditRepo.findByAction(action, pageable);
    } else if (startDt != null && endDt != null) {
        return auditRepo.findByTimestampBetween(startDt, endDt, pageable);
    } else {
        return auditRepo.findAll(pageable);  // JpaRepository already has this
    }
}
```

Note: findAll(pageable) comes for free from JpaRepository -- no need to define it in the repository.

## 5. CHANGE: Controllers -- switch to paginated responses
This is where the API actually changes for the frontend.

### GoalController
NOW:

```java
@GetMapping
public ApiResponse<List<Goal>> getGoals(HttpServletRequest httpReq,
                                        @RequestParam(required = false) Integer userId,
                                        @RequestParam(required = false) Integer mgrId) {
    String role = (String) httpReq.getAttribute("userRole");
    Integer currentUserId = (Integer) httpReq.getAttribute("userId");

    List<Goal> goals;
    if (role.equals("EMPLOYEE")) {
        goals = goalSvc.getGoalsByUser(currentUserId);
    } else if (role.equals("MANAGER")) {
        if (userId != null) {
            goals = goalSvc.getGoalsByUser(userId);
        } else {
            goals = goalSvc.getGoalsByManager(currentUserId);
        }
    } else {
        goals = userId != null ? goalSvc.getGoalsByUser(userId) :
                mgrId != null ? goalSvc.getGoalsByManager(mgrId) :
                        goalSvc.getGoalsByUser(currentUserId);
    }

    return ApiResponse.success("Goals retrieved", goals);
}
```

AFTER:

```java
@GetMapping
public ApiResponse<PageResponse<Goal>> getGoals(
        HttpServletRequest httpReq,
        @RequestParam(required = false) Integer userId,
        @RequestParam(required = false) Integer mgrId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    String role = (String) httpReq.getAttribute("userRole");
    Integer currentUserId = (Integer) httpReq.getAttribute("userId");

    // Cap page size at 100 to prevent abuse
    Pageable pageable = PageRequest.of(page, Math.min(size, 100),
            Sort.by("createdDate").descending());

    Page<Goal> goals;
    if (role.equals("EMPLOYEE")) {
        goals = goalSvc.getGoalsByUser(currentUserId, pageable);
    } else if (role.equals("MANAGER")) {
        if (userId != null) {
            goals = goalSvc.getGoalsByUser(userId, pageable);
        } else {
            goals = goalSvc.getGoalsByManager(currentUserId, pageable);
        }
    } else {
        goals = userId != null ? goalSvc.getGoalsByUser(userId, pageable) :
                mgrId != null ? goalSvc.getGoalsByManager(mgrId, pageable) :
                        goalSvc.getGoalsByUser(currentUserId, pageable);
    }

    return ApiResponse.successPage("Goals retrieved", goals);
}
```

You'll need these imports:

```java
import com.project.performanceTrack.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
```

What changed:

- Return type: `ApiResponse<List<Goal>>` --> `ApiResponse<PageResponse<Goal>>`
- Added page and size params with defaults (page 0, size 20)
- Created a Pageable with hardcoded sort createdDate DESC (no custom sort param -- keeps it simple)
- Math.min(size, 100) caps max page size
- Uses `ApiResponse.successPage()` instead of `ApiResponse.success()`
- The business logic (role-based filtering) is exactly the same -- just passes pageable as extra argument

### NotificationController
NOW:

```java
@GetMapping
public ApiResponse<List<Notification>> getNotifications(HttpServletRequest httpReq,
                                                        @RequestParam(required = false) String status) {
    Integer userId = (Integer) httpReq.getAttribute("userId");
    List<Notification> notifications = notificationService.getNotifications(userId, status);
    return ApiResponse.success("Notifications retrieved", notifications);
}
```

AFTER:

```java
@GetMapping
public ApiResponse<PageResponse<Notification>> getNotifications(
        HttpServletRequest httpReq,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Integer userId = (Integer) httpReq.getAttribute("userId");
    Pageable pageable = PageRequest.of(page, Math.min(size, 100),
            Sort.by("createdDate").descending());

    Page<Notification> notifications = notificationService.getNotifications(userId, status, pageable);
    return ApiResponse.successPage("Notifications retrieved", notifications);
}
```

Same pattern: Add page/size params, create Pageable, pass it through, use successPage.

### AuditLogController
NOW:

```java
@GetMapping
public ApiResponse<List<AuditLog>> getAuditLogs(
        @RequestParam(required = false) Integer userId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDt) {

    List<AuditLog> logs = auditLogService.getAuditLogs(userId, action, startDt, endDt);
    return ApiResponse.success("Audit logs retrieved", logs);
}
```

AFTER:

```java
@GetMapping
public ApiResponse<PageResponse<AuditLog>> getAuditLogs(
        @RequestParam(required = false) Integer userId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, Math.min(size, 100),
            Sort.by("timestamp").descending());

    Page<AuditLog> logs = auditLogService.getAuditLogs(userId, action, startDt, endDt, pageable);
    return ApiResponse.successPage("Audit logs retrieved", logs);
}
```

## Summary of all changes

| Layer | File | What changes |
|-------|------|--------------|
| New file | dto/PageResponse.java | Wrapper DTO for paginated responses |
| DTO | dto/ApiResponse.java | Add 1 method: successPage() |
| Repository | GoalRepository.java | Add 2 methods (paginated overloads) |
| Repository | NotificationRepository.java | Add 2 methods |
| Repository | AuditLogRepository.java | Add 3 methods |
| Service | GoalService.java | Add 2 methods |
| Service | NotificationService.java | Add 1 method |
| Service | AuditLogService.java | Add 1 method |
| Controller | GoalController.java | Change getGoals() -- add page/size params, return PageResponse |
| Controller | NotificationController.java | Change getNotifications() -- same pattern |
| Controller | AuditLogController.java | Change getAuditLogs() -- same pattern |

