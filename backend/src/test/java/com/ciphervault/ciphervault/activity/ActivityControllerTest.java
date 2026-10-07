package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ActivityControllerTest {

    private ActivityEventRepository repository;
    private UserRepository userRepository;
    private ActivityController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ActivityEventRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        controller = new ActivityController(repository, userRepository);

        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
    }

    @Test
    void listActivityShouldReturnPaginatedEvents() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        ActivityEvent event1 = new ActivityEvent();
        event1.setId(101L);
        event1.setEventType(EventType.UPLOAD);
        event1.setSummary("Uploaded test.pdf");

        ActivityEvent event2 = new ActivityEvent();
        event2.setId(102L);
        event2.setEventType(EventType.DOWNLOAD);
        event2.setSummary("Downloaded test.pdf");

        Page<ActivityEvent> page = new PageImpl<>(List.of(event1, event2));
        when(repository.findByUser(eq(user), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<ActivityEvent>> response = controller.listActivity(0, 20, null, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertEquals(EventType.UPLOAD, response.getBody().getContent().get(0).getEventType());
    }

    @Test
    void listActivityWithAllPeriodShouldNotThrow() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        Page<ActivityEvent> page = new PageImpl<>(List.of());
        when(repository.findByUser(eq(user), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<ActivityEvent>> response = controller.listActivity(0, 20, null, "all", authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getActivityDetailShouldReturnEvent() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        ActivityEvent event = new ActivityEvent();
        event.setId(200L);
        event.setEventType(EventType.DUPLICATE_DETECTED);
        event.setSummary("Duplicate detected: photo.jpg");

        when(repository.findByIdAndUser(200L, user)).thenReturn(Optional.of(event));

        ResponseEntity<ActivityEvent> response = controller.getActivityDetail(200L, authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200L, response.getBody().getId());
        assertEquals(EventType.DUPLICATE_DETECTED, response.getBody().getEventType());
    }
}
