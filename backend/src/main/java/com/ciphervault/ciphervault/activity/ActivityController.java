package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.exception.ApiException;
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

    private final ActivityEventRepository repository;
    private final UserRepository userRepository;

    public ActivityController(ActivityEventRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
    }

    @GetMapping
    public ResponseEntity<Page<ActivityEvent>> listActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) String period,
            Authentication authentication) {
            
        User user = getAuthenticatedUser(authentication);
        size = Math.max(1, Math.min(size, 100)); page = Math.max(0, page);
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
        
        Page<ActivityEvent> events;
        if (type != null && after != null) {
            events = repository.findByUserAndEventTypeAndTimestampAfter(user, type, after, pageable);
        } else if (type != null) {
            events = repository.findByUserAndEventType(user, type, pageable);
        } else if (after != null) {
            events = repository.findByUserAndTimestampAfter(user, after, pageable);
        } else {
            events = repository.findByUser(user, pageable);
        }
        
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityEvent> getActivityDetail(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return repository.findByIdAndUser(id, user)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Activity event not found"));
    }
}

