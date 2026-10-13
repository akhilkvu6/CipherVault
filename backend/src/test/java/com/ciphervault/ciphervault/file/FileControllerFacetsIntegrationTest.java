package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class FileControllerFacetsIntegrationTest {

    @Autowired
    private FileController fileController;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        testUser = userRepository.findByEmail("user@gmail.com").orElse(null);
        if (testUser == null) {
            testUser = userRepository.findAll().stream().findFirst().orElse(null);
        }
        assertNotNull(testUser, "Test user must exist");

        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(testUser.getEmail());
    }

    @Test
    public void testGetFacetsUnauthorizedWhenNoAuth() {
        ResponseEntity<FileFacetsResponse> response = fileController.getFacets(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testGetFacetsReturns200WithFullSchemaForAuthenticatedUser() {
        ResponseEntity<FileFacetsResponse> response = fileController.getFacets(
                null, "ALL", null, null, null, null, null, null, null, null, null, null, null, authentication
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FileFacetsResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.getTotalMatchingFiles() > 0);
        assertNotNull(body.getFacets());
        assertFalse(body.getFacets().isEmpty());

        boolean hasCategory = body.getFacets().stream().anyMatch(f -> "category".equals(f.getField()));
        boolean hasMake = body.getFacets().stream().anyMatch(f -> "cameraMake".equals(f.getField()));
        boolean hasModel = body.getFacets().stream().anyMatch(f -> "cameraModel".equals(f.getField()));
        boolean hasRes = body.getFacets().stream().anyMatch(f -> "resolution".equals(f.getField()));

        assertTrue(hasCategory, "Must return category facet");
        assertTrue(hasMake, "Must return cameraMake facet");
        assertTrue(hasModel, "Must return cameraModel facet");
        assertTrue(hasRes, "Must return resolution facet");
    }

    @Test
    public void testGetFacetsCascadingEndpoint() {
        ResponseEntity<FileFacetsResponse> response = fileController.getFacets(
                null, "IMAGES", "Canon", null, null, null, null, null, null, null, null, null, null, authentication
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FileFacetsResponse body = response.getBody();
        assertNotNull(body);

        FileFacetsResponse.FacetField modelField = body.getFacets().stream()
                .filter(f -> "cameraModel".equals(f.getField()))
                .findFirst()
                .orElse(null);
        assertNotNull(modelField);
        assertFalse(modelField.getValues().isEmpty());
    }
}
