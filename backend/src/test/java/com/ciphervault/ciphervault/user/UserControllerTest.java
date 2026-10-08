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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        user.setName("Tester Display");
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
        assertEquals("Tester Display", response.getBody().getName());
        assertEquals("tester", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        assertEquals(2, response.getBody().getFileCount());
        assertEquals(1, response.getBody().getEncryptedCount());
        assertFalse(response.getBody().isProfilePhotoAvailable());
        assertEquals(20_000L, response.getBody().getCategoryBytes().get("Images"));
        assertEquals(15_000L, response.getBody().getCategoryBytes().get("Documents"));
        assertEquals(1L, response.getBody().getCategoryCounts().get("Images"));
        assertEquals(1L, response.getBody().getCategoryCounts().get("Documents"));
    }

    @Test
    void updateUsernameShouldValidateAndPersist() {
        User user = new User();
        user.setId(1L);
        user.setUsername("oldname");
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newname")).thenReturn(false);

        // Fail short name
        ResponseEntity<Map<String, Object>> shortResp = userController.updateUsername(Map.of("username", "ab"), authentication);
        assertEquals(HttpStatus.BAD_REQUEST, shortResp.getStatusCode());

        // Fail duplicate name
        when(userRepository.existsByUsername("taken")).thenReturn(true);
        ResponseEntity<Map<String, Object>> dupResp = userController.updateUsername(Map.of("username", "taken"), authentication);
        assertEquals(HttpStatus.BAD_REQUEST, dupResp.getStatusCode());

        // Success
        ResponseEntity<Map<String, Object>> okResp = userController.updateUsername(Map.of("username", "newname"), authentication);
        assertEquals(HttpStatus.OK, okResp.getStatusCode());
        assertEquals("newname", user.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
    }

    @Test
    void deleteAccountShouldVerifyPasswordIfProvided() {
        org.springframework.security.crypto.password.PasswordEncoder encoder =
                Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        userController.setPasswordEncoder(encoder);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrongpass", "encodedPassword")).thenReturn(false);
        when(encoder.matches("correctpass", "encodedPassword")).thenReturn(true);

        // Wrong password fails
        ResponseEntity<Map<String, Object>> failResp = userController.deleteAccount(Map.of("password", "wrongpass"), authentication);
        assertEquals(HttpStatus.BAD_REQUEST, failResp.getStatusCode());
        assertEquals("Incorrect password", failResp.getBody().get("message"));
        Mockito.verify(userRepository, Mockito.never()).delete(any());

        // Correct password succeeds
        ResponseEntity<Map<String, Object>> okResp = userController.deleteAccount(Map.of("password", "correctpass"), authentication);
        assertEquals(HttpStatus.OK, okResp.getStatusCode());
        Mockito.verify(userRepository, Mockito.times(1)).delete(user);
    }

    @Test
    void updateNameShouldValidateAndPersist() {
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Blank name fails
        ResponseEntity<Map<String, Object>> blankResp = userController.updateName(Map.of("name", "   "), authentication);
        assertEquals(HttpStatus.BAD_REQUEST, blankResp.getStatusCode());

        // Valid name succeeds
        ResponseEntity<Map<String, Object>> okResp = userController.updateName(Map.of("name", "New John"), authentication);
        assertEquals(HttpStatus.OK, okResp.getStatusCode());
        assertEquals("New John", user.getName());
        Mockito.verify(userRepository, Mockito.atLeastOnce()).save(user);
    }

    @Test
    void updatePasswordShouldValidateAndPersist() {
        org.springframework.security.crypto.password.PasswordEncoder encoder =
                Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        userController.setPasswordEncoder(encoder);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("oldEncoded");
        user.setTokenVersion(1);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("OldPass123!", "oldEncoded")).thenReturn(true);
        when(encoder.matches("WrongPass!", "oldEncoded")).thenReturn(false);
        when(encoder.encode("NewPass456!")).thenReturn("newEncoded");

        // Wrong current password fails
        Map<String, String> wrongReq = Map.of(
                "currentPassword", "WrongPass!",
                "newPassword", "NewPass456!",
                "confirmPassword", "NewPass456!"
        );
        ResponseEntity<Map<String, Object>> wrongResp = userController.updatePassword(wrongReq, authentication);
        assertEquals(HttpStatus.BAD_REQUEST, wrongResp.getStatusCode());

        // Correct password succeeds
        Map<String, String> validReq = Map.of(
                "currentPassword", "OldPass123!",
                "newPassword", "NewPass456!",
                "confirmPassword", "NewPass456!"
        );
        ResponseEntity<Map<String, Object>> okResp = userController.updatePassword(validReq, authentication);
        assertEquals(HttpStatus.OK, okResp.getStatusCode());
        assertEquals(2, user.getTokenVersion());
        Mockito.verify(userRepository, Mockito.atLeastOnce()).save(user);
    }
}
