package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileControllerTest {

    private FileRepository fileRepository;
    private UserRepository userRepository;
    private EncryptionService encryptionService;
    private MediaPreviewService mediaPreviewService;
    private FileController fileController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        encryptionService = Mockito.mock(EncryptionService.class);
        mediaPreviewService = Mockito.mock(MediaPreviewService.class);
        fileController = new FileController(fileRepository, userRepository, encryptionService, mediaPreviewService);
        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
    }

    @Test
    void deleteFileShouldReturnUnauthorizedWhenNotAuthenticated() {
        ResponseEntity<Map<String, Object>> response = fileController.deleteFile(1L, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deleteFileShouldReturnNotFoundWhenFileNotOwnedByUser() {
        User user = new User();
        user.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.findByIdAndUser(1L, user)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = fileController.deleteFile(1L, authentication);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteFileShouldDeleteRecordAndReduceStorageQuota() throws Exception {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setUsedStorage(5000L);

        Path tempFile = Files.createTempFile("ciphervault_test", ".tmp");
        Path tempPreview = Files.createTempFile("ciphervault_prev", ".jpg");
        Files.writeString(tempFile, "sample data");
        Files.writeString(tempPreview, "sample preview");

        StoredFile storedFile = new StoredFile();
        storedFile.setFileSize(2000L);
        storedFile.setStoragePath(tempFile.toString());
        storedFile.setHasPreview(true);
        storedFile.setPreviewPath(tempPreview.toString());
        storedFile.setUser(user);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(storedFile));

        ResponseEntity<Map<String, Object>> response = fileController.deleteFile(10L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("File deleted successfully", response.getBody().get("message"));

        assertEquals(3000L, user.getUsedStorage());
        verify(userRepository, times(1)).save(user);
        verify(fileRepository, times(1)).delete(storedFile);

        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempPreview);
    }

    @Test
    void getFilePreviewShouldReturnUnauthorizedWhenNotAuthenticated() {
        ResponseEntity<byte[]> response = fileController.getFilePreview(1L, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getFilePreviewShouldReturnNotFoundWhenNoPreview() {
        User user = new User();
        user.setEmail("alice@example.com");

        StoredFile storedFile = new StoredFile();
        storedFile.setHasPreview(false);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(storedFile));

        ResponseEntity<byte[]> response = fileController.getFilePreview(1L, authentication);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getFilePreviewShouldReturnPreviewBytesWhenAvailable() throws Exception {
        User user = new User();
        user.setEmail("alice@example.com");

        Path previewDir = FileStorageConfig.STORAGE_ROOT.resolve("previews").toAbsolutePath().normalize();
        Files.createDirectories(previewDir);
        Path tempPreview = previewDir.resolve("test_preview.jpg");
        Files.write(tempPreview, new byte[]{1, 2, 3, 4});

        StoredFile storedFile = new StoredFile();
        storedFile.setHasPreview(true);
        storedFile.setPreviewPath(tempPreview.toString());
        storedFile.setPreviewMimeType("image/jpeg");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(storedFile));

        ResponseEntity<byte[]> response = fileController.getFilePreview(1L, authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, response.getBody());

        Files.deleteIfExists(tempPreview);
    }

    @Test
    void uploadFileShouldGeneratePreviewAndSaveWhenSupported() throws Exception {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setUsedStorage(0L);
        user.setStorageLimit(10_000_000L);

        MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
        byte[] originalContent = new byte[]{10, 20, 30, 40};
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenAnswer(invocation -> new java.io.ByteArrayInputStream(originalContent));
        when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.existsByUserAndSha256Hash(any(), any())).thenReturn(false);
        when(encryptionService.encrypt(any())).thenReturn(new byte[]{99, 88, 77});

        byte[] fakePreview = new byte[]{1, 2, 3};
        when(mediaPreviewService.generatePreview(any(Path.class), eq("photo.jpg"), eq("image/jpeg"))).thenReturn(fakePreview);

        when(fileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile file = invocation.getArgument(0);
            return file;
        });

        ResponseEntity<FileController.FileUploadResponse> response =
                fileController.uploadFile(multipartFile, true, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());

        verify(mediaPreviewService, times(1)).generatePreview(any(Path.class), eq("photo.jpg"), eq("image/jpeg"));
        verify(fileRepository, times(1)).save(argThat(StoredFile::isHasPreview));
    }
}
