package com.ciphervault.ciphervault.user;

import com.ciphervault.ciphervault.file.FileCategoryService;
import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.file.FileStorageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfilePhotoTest {

    private UserRepository userRepository;
    private FileRepository fileRepository;
    private UserController userController;
    private Authentication authentication;

    private User testUser;
    private Path createdPhotoPath;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(FileStorageConfig.PROFILE_PHOTO_STORAGE);

        userRepository = Mockito.mock(UserRepository.class);
        fileRepository = Mockito.mock(FileRepository.class);
        userController = new UserController(userRepository, fileRepository, new FileCategoryService());

        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");

        testUser = new User();
        testUser.setId(10L);
        testUser.setName("Alice");
        testUser.setUsername("alice");
        testUser.setEmail("alice@example.com");
        testUser.setUsedStorage(1024L);
        testUser.setStorageLimit(10L * 1024L * 1024L * 1024L);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        if (createdPhotoPath != null) {
            try {
                Files.deleteIfExists(createdPhotoPath);
            } catch (Exception ignored) {}
        }
    }

    @Test
    void testUploadPhotoSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "photo",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4, 5}
        );

        ResponseEntity<Map<String, Object>> response = userController.uploadProfilePhoto(file, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertNotNull(testUser.getProfilePhotoPath());
        assertTrue(testUser.getProfilePhotoPath().startsWith("profile_photos/profile_"));
        assertTrue(testUser.getProfilePhotoPath().endsWith(".jpg"));

        createdPhotoPath = FileStorageConfig.resolvePath(testUser.getProfilePhotoPath());
        assertNotNull(createdPhotoPath);
        assertTrue(Files.exists(createdPhotoPath));

        verify(userRepository).save(testUser);
    }

    @Test
    void testUploadWithoutAuthenticationRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "photo",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        // Unauthenticated call
        ResponseEntity<Map<String, Object>> response = userController.uploadProfilePhoto(file, null, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testReplaceExistingPhotoRemovesOldPhysicalFile() throws IOException {
        // Prepare old photo on disk
        Path oldFile = FileStorageConfig.PROFILE_PHOTO_STORAGE.resolve("profile_old_test.jpg");
        Files.write(oldFile, new byte[]{1, 1, 1});
        testUser.setProfilePhotoPath("profile_photos/profile_old_test.jpg");

        assertTrue(Files.exists(oldFile), "Old photo should exist before replacement");

        MockMultipartFile newFile = new MockMultipartFile(
                "photo",
                "new_avatar.png",
                "image/png",
                new byte[]{2, 2, 2, 2}
        );

        ResponseEntity<Map<String, Object>> response = userController.uploadProfilePhoto(newFile, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(Files.exists(oldFile), "Old physical photo should be deleted on replacement");

        createdPhotoPath = FileStorageConfig.resolvePath(testUser.getProfilePhotoPath());
        assertNotNull(createdPhotoPath);
        assertTrue(Files.exists(createdPhotoPath), "New physical photo should exist");
        assertTrue(testUser.getProfilePhotoPath().endsWith(".png"));
    }

    @Test
    void testGetOwnPhotoSuccess() throws IOException {
        Path photo = FileStorageConfig.PROFILE_PHOTO_STORAGE.resolve("profile_get_test.jpg");
        byte[] expectedBytes = new byte[]{9, 8, 7, 6};
        Files.write(photo, expectedBytes);
        createdPhotoPath = photo;

        testUser.setProfilePhotoPath("profile_photos/profile_get_test.jpg");

        ResponseEntity<?> response = userController.getProfilePhoto(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertArrayEquals(expectedBytes, (byte[]) response.getBody());
    }

    @Test
    void testDeleteOwnPhotoSuccess() throws IOException {
        Path photo = FileStorageConfig.PROFILE_PHOTO_STORAGE.resolve("profile_del_test.jpg");
        Files.write(photo, new byte[]{5, 5, 5});
        testUser.setProfilePhotoPath("profile_photos/profile_del_test.jpg");

        assertTrue(Files.exists(photo));

        ResponseEntity<Map<String, Object>> response = userController.deleteProfilePhoto(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(testUser.getProfilePhotoPath());
        assertFalse(Files.exists(photo), "Physical photo must be deleted from disk");
        verify(userRepository).save(testUser);
    }

    @Test
    void testRequestWhenNoPhotoExistsReturns404() {
        testUser.setProfilePhotoPath(null);

        ResponseEntity<?> response = userController.getProfilePhoto(authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUnsupportedFileTypeRejected() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "photo",
                "document.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        ResponseEntity<Map<String, Object>> response = userController.uploadProfilePhoto(pdfFile, null, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Unsupported image format. Allowed formats: JPG, PNG, WebP", response.getBody().get("message"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testOversizedProfilePhotoRejected() {
        // 5 MB + 1 byte
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile(
                "photo",
                "large.jpg",
                "image/jpeg",
                largeBytes
        );

        ResponseEntity<Map<String, Object>> response = userController.uploadProfilePhoto(largeFile, null, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("5 MB"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testProfilePhotoDoesNotAlterVaultUsedStorage() {
        long initialUsedStorage = testUser.getUsedStorage();

        MockMultipartFile file = new MockMultipartFile(
                "photo",
                "avatar.jpg",
                "image/jpeg",
                new byte[2048]
        );

        userController.uploadProfilePhoto(file, null, authentication);

        assertEquals(initialUsedStorage, testUser.getUsedStorage(), "Vault usedStorage must NOT be altered by profile photo");
        createdPhotoPath = FileStorageConfig.resolvePath(testUser.getProfilePhotoPath());
    }

    @Test
    void testCrossUserAccessIsolation() throws IOException {
        // Alice has a photo
        Path alicePhoto = FileStorageConfig.PROFILE_PHOTO_STORAGE.resolve("profile_alice_iso.jpg");
        Files.write(alicePhoto, new byte[]{1, 2, 3});
        createdPhotoPath = alicePhoto;
        testUser.setProfilePhotoPath("profile_photos/profile_alice_iso.jpg");

        // Bob has no photo
        User bob = new User();
        bob.setId(20L);
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setProfilePhotoPath(null);

        Authentication bobAuth = Mockito.mock(Authentication.class);
        when(bobAuth.isAuthenticated()).thenReturn(true);
        when(bobAuth.getName()).thenReturn("bob@example.com");
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(bob));

        // When Bob requests photo, he gets 404, not Alice's photo
        ResponseEntity<?> bobPhotoResponse = userController.getProfilePhoto(bobAuth);
        assertEquals(HttpStatus.NOT_FOUND, bobPhotoResponse.getStatusCode());

        // When Bob deletes photo, Alice's photo remains intact
        userController.deleteProfilePhoto(bobAuth);
        assertTrue(Files.exists(alicePhoto), "Alice's photo must not be deleted by Bob");
        assertEquals("profile_photos/profile_alice_iso.jpg", testUser.getProfilePhotoPath());
    }
}
