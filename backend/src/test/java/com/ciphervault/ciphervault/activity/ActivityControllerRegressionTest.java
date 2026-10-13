package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ActivityControllerRegressionTest {

    @Autowired
    private ActivityController activityController;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES);

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        String suffixB = UUID.randomUUID().toString().substring(0, 8);

        userA = new User();
        userA.setName("Alice Reg");
        userA.setUsername("alice_" + suffixA);
        userA.setEmail("alice_" + suffixA + "@test.ciphervault.local");
        userA.setPassword("$2a$10$hashedpasswordA");
        userA = userRepository.save(userA);

        userB = new User();
        userB.setName("Bob Reg");
        userB.setUsername("bob_" + suffixB);
        userB.setEmail("bob_" + suffixB + "@test.ciphervault.local");
        userB.setPassword("$2a$10$hashedpasswordB");
        userB = userRepository.save(userB);
    }

    @AfterEach
    void tearDown() {
        if (userA != null && userA.getId() != null) {
            activityEventRepository.deleteAll(activityEventRepository.findByUser(userA, Pageable.unpaged()).getContent());
            userRepository.delete(userA);
        }
        if (userB != null && userB.getId() != null) {
            activityEventRepository.deleteAll(activityEventRepository.findByUser(userB, Pageable.unpaged()).getContent());
            userRepository.delete(userB);
        }
    }

    @Test
    void testSerializationWithoutLazyInitializationException() throws Exception {
        // Log events for User A
        activityService.logEvent(userA, EventType.UPLOAD, "Uploaded secret.pdf", 1, 1024L, "secret.pdf", "SUCCESS", null);
        activityService.logEvent(userA, EventType.DOWNLOAD, "Downloaded secret.pdf", 1, 1024L, "secret.pdf", "SUCCESS", null);

        Authentication authA = new UsernamePasswordAuthenticationToken(userA.getEmail(), null, java.util.Collections.emptyList());

        // Fetch activity page as User A
        ResponseEntity<Page<ActivityEventDto>> response = activityController.listActivity(0, 10, null, null, authA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getTotalElements());

        // CRITICAL REGRESSION CHECK:
        // Serialize the Page<ActivityEventDto> to JSON with Jackson
        // In previous buggy version, this threw LazyInitializationException due to lazy User proxy!
        String json = objectMapper.writeValueAsString(response.getBody());
        assertNotNull(json);
        assertTrue(json.contains("Uploaded secret.pdf"));
        assertTrue(json.contains("secret.pdf"));

        // Ensure user sensitive data is NOT leaked into JSON
        assertFalse(json.contains("hashedpasswordA"), "Password hash must never leak in activity response");
    }

    @Test
    void testUserIsolation() {
        // Log event for User A and User B
        activityService.logEvent(userA, EventType.UPLOAD, "Alice secret photo", 1, 5000L, "alice.jpg", "SUCCESS", null);
        activityService.logEvent(userB, EventType.UPLOAD, "Bob confidential doc", 1, 3000L, "bob.doc", "SUCCESS", null);

        Authentication authA = new UsernamePasswordAuthenticationToken(userA.getEmail(), null, java.util.Collections.emptyList());
        Authentication authB = new UsernamePasswordAuthenticationToken(userB.getEmail(), null, java.util.Collections.emptyList());

        // User A should only see Alice's event
        ResponseEntity<Page<ActivityEventDto>> responseA = activityController.listActivity(0, 10, null, null, authA);
        assertEquals(1, responseA.getBody().getTotalElements());
        assertEquals("Alice secret photo", responseA.getBody().getContent().get(0).getSummary());

        // User B should only see Bob's event
        ResponseEntity<Page<ActivityEventDto>> responseB = activityController.listActivity(0, 10, null, null, authB);
        assertEquals(1, responseB.getBody().getTotalElements());
        assertEquals("Bob confidential doc", responseB.getBody().getContent().get(0).getSummary());
    }

    @Test
    void testActivityDetailSecurityAndIsolation() {
        activityService.logEvent(userA, EventType.SIGN_IN, "Login attempt", 0, 0L, null, "WARNING", "Bad password");
        ActivityEvent eventA = activityEventRepository.findByUser(userA, Pageable.unpaged()).getContent().get(0);

        Authentication authA = new UsernamePasswordAuthenticationToken(userA.getEmail(), null, java.util.Collections.emptyList());
        Authentication authB = new UsernamePasswordAuthenticationToken(userB.getEmail(), null, java.util.Collections.emptyList());

        // User A can view own event
        ResponseEntity<ActivityEventDto> detailA = activityController.getActivityDetail(eventA.getId(), authA);
        assertEquals(HttpStatus.OK, detailA.getStatusCode());
        assertEquals(eventA.getId(), detailA.getBody().getId());
        assertEquals("Bad password", detailA.getBody().getErrorMessage());

        // User B CANNOT view User A's event -> 404 NOT_FOUND
        assertThrows(Exception.class, () -> {
            activityController.getActivityDetail(eventA.getId(), authB);
        });
    }
}
