package com.ciphervault.ciphervault.user;

import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.file.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserRepository userRepository;
    private FileRepository fileRepository;
    private UserController userController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        fileRepository = Mockito.mock(FileRepository.class);
        userController = new UserController(userRepository, fileRepository, new com.ciphervault.ciphervault.file.FileCategoryService());
        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    void getUserProfileShouldReturnAccurateMetrics() {
        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setEmail("test@example.com");
        user.setStorageLimit(100_000_000L);
        user.setUsedStorage(35_000L);

        StoredFile f1 = new StoredFile();
        f1.setOriginalFilename("photo.jpg");
        f1.setContentType("image/jpeg");
        f1.setFileSize(20_000L);
        f1.setEncrypted(true);

        StoredFile f2 = new StoredFile();
        f2.setOriginalFilename("doc.pdf");
        f2.setContentType("application/pdf");
        f2.setFileSize(15_000L);
        f2.setEncrypted(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        List<Object[]> mockStats = List.of(
            new Object[]{"photo.jpg", "image/jpeg", 20000L, true},
            new Object[]{"doc.pdf", "application/pdf", 15000L, false}
        );
        when(fileRepository.findFileStatsByUser(user)).thenReturn(mockStats);

        ResponseEntity<UserController.UserProfileResponse> response = userController.getUserProfile(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("tester", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        assertEquals(2, response.getBody().getFileCount());
        assertEquals(1, response.getBody().getEncryptedCount());
        assertEquals(20_000L, response.getBody().getCategoryBytes().get("Images"));
        assertEquals(15_000L, response.getBody().getCategoryBytes().get("Documents"));
        assertEquals(1L, response.getBody().getCategoryCounts().get("Images"));
        assertEquals(1L, response.getBody().getCategoryCounts().get("Documents"));
    }
}
