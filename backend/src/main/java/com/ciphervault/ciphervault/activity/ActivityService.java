package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ActivityService {

    private final ActivityEventRepository repository;

    public ActivityService(ActivityEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logEvent(User user, EventType type, String summary, Integer fileCount, Long totalBytes, String affectedFiles, String status, String errorMessage) {
        if (user == null) return;
        
        ActivityEvent event = new ActivityEvent();
        event.setUser(user);
        event.setEventType(type);
        event.setSummary(summary);
        event.setFileCount(fileCount);
        event.setTotalBytes(totalBytes);
        event.setAffectedFiles(affectedFiles);
        event.setStatus(status);
        event.setErrorMessage(errorMessage);
        
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventDto> listActivity(User user, EventType type, LocalDateTime after, Pageable pageable) {
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
        return events.map(ActivityEventDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<ActivityEventDto> getActivityDetail(Long id, User user) {
        return repository.findByIdAndUser(id, user).map(ActivityEventDto::fromEntity);
    }
}
