package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
    
    Page<ActivityEvent> findByUser(User user, Pageable pageable);
    
    Page<ActivityEvent> findByUserAndEventType(User user, EventType eventType, Pageable pageable);
    
    Page<ActivityEvent> findByUserAndTimestampAfter(User user, LocalDateTime after, Pageable pageable);
    
    Page<ActivityEvent> findByUserAndEventTypeAndTimestampAfter(User user, EventType eventType, LocalDateTime after, Pageable pageable);
    
    Optional<ActivityEvent> findByIdAndUser(Long id, User user);
    
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ActivityEvent a WHERE a.user = :user")
    void deleteByUser(@Param("user") User user);

    // Analytics queries
    @Query("SELECT COUNT(a) FROM ActivityEvent a WHERE a.user = :user AND a.eventType = :eventType AND a.timestamp >= :since")
    long countEventsSince(@Param("user") User user, @Param("eventType") EventType eventType, @Param("since") LocalDateTime since);

    @Query("SELECT SUM(a.totalBytes) FROM ActivityEvent a WHERE a.user = :user AND a.eventType = :eventType AND a.timestamp >= :since AND a.totalBytes IS NOT NULL")
    Long sumBytesSince(@Param("user") User user, @Param("eventType") EventType eventType, @Param("since") LocalDateTime since);

    @Query("SELECT SUM(a.fileCount) FROM ActivityEvent a WHERE a.user = :user AND a.eventType = :eventType AND a.timestamp >= :since AND a.fileCount IS NOT NULL")
    Long sumFilesSince(@Param("user") User user, @Param("eventType") EventType eventType, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM ActivityEvent a WHERE a.user = :user AND a.eventType IN :types AND a.timestamp >= :since")
    List<ActivityEvent> findEventsSince(@Param("user") User user, @Param("types") List<EventType> types, @Param("since") LocalDateTime since);
}
