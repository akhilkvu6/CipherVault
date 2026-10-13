package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private static final Logger log = LoggerFactory.getLogger(ActivityController.class);

    private final ActivityService activityService;
    private final UserRepository userRepository;

    @Autowired
    public ActivityController(ActivityService activityService, UserRepository userRepository) {
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    public ActivityController(ActivityEventRepository repository, UserRepository userRepository) {
        this(new ActivityService(repository), userRepository);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
    }

    @GetMapping
    public ResponseEntity<Page<ActivityEventDto>> listActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) String period,
            Authentication authentication) {
            
        User user = getAuthenticatedUser(authentication);
        size = Math.max(1, Math.min(size, 100));
        page = Math.max(0, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        LocalDateTime after = null;
        if (period != null && !period.isBlank() && !"all".equalsIgnoreCase(period) && !"alltime".equalsIgnoreCase(period)) {
            switch (period.toLowerCase()) {
                case "today": after = LocalDateTime.now().toLocalDate().atStartOfDay(); break;
                case "week": after = LocalDateTime.now().minusDays(7); break;
                case "month": after = LocalDateTime.now().minusDays(30); break;
                default: throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Supported periods: today, week, month, all");
            }
        }
        
        Page<ActivityEventDto> events = activityService.listActivity(user, type, after, pageable);
        log.info("[ACTIVITY] Listed activity events | count={} | total={}", events.getNumberOfElements(), events.getTotalElements());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityEventDto> getActivityDetail(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        ActivityEventDto event = activityService.getActivityDetail(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Activity event not found"));
        log.info("[ACTIVITY] Retrieved activity detail | id={}", id);
        return ResponseEntity.ok(event);
    }
}
