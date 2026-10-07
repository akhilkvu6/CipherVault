package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
}
