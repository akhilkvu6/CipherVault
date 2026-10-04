package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileControllerTest {

    private FileRepository fileRepository;
    private UserRepository userRepository;
    private EncryptionService encryptionService;
    private MediaPreviewService mediaPreviewService;
    private KeyManagementService keyManagementService;
    private FileController fileController;
    private Authentication authentication;

    private Path createdStorageFile;
    private Path createdPreviewFile;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        encryptionService = Mockito.mock(EncryptionService.class);
        mediaPreviewService = Mockito.mock(MediaPreviewService.class);
        keyManagementService = Mockito.mock(KeyManagementService.class);

        FileCategoryService fileCategoryService = new FileCategoryService();
        FileSearchService fileSearchService = new FileSearchService(fileRepository, null, fileCategoryService);
        MetadataExtractionService metadataExtractionService = Mockito.mock(MetadataExtractionService.class);

        fileController = new FileController(
                fileRepository,
                userRepository,
                encryptionService,
                mediaPreviewService,
                keyManagementService,
                metadataExtractionService,
                null,
                fileCategoryService,
                fileSearchService
        );
        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");

        when(userRepository.incrementStorageUsedAtomic(any(), anyLong())).thenReturn(1);
        when(userRepository.decrementStorageUsedAtomic(any(), anyLong())).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        if (createdStorageFile != null) {
            try { Files.deleteIfExists(createdStorageFile); } catch (Exception ignored) {}
        }
        if (createdPreviewFile != null) {
            try { Files.deleteIfExists(createdPreviewFile); } catch (Exception ignored) {}
        }
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

        Path tempFile = Files.createTempFile(FileStorageConfig.STORAGE_ROOT, "ciphervault_test", ".tmp");
        Path tempPreview = Files.createTempFile(FileStorageConfig.STORAGE_ROOT, "ciphervault_prev", ".jpg");
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
        verify(userRepository, times(1)).decrementStorageUsedAtomic(user.getId(), 2000L);
        verify(userRepository, never()).save(user);
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
        when(fileRepository.findByUserAndSha256Hash(any(), any())).thenReturn(Optional.empty());

        byte[] fakePreview = new byte[]{1, 2, 3};
        when(mediaPreviewService.generatePreview(any(Path.class), eq("photo.jpg"), eq("image/jpeg"))).thenReturn(fakePreview);

        when(fileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile file = invocation.getArgument(0);
            if (file.getStoragePath() != null) {
                createdStorageFile = Path.of(file.getStoragePath());
            }
            if (file.getPreviewPath() != null) {
                createdPreviewFile = Path.of(file.getPreviewPath());
            }
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

    @Test
    void checkDuplicateShouldReturnTrueWhenHashExists() {
        User user = new User();
        user.setEmail("alice@example.com");

        StoredFile existing = new StoredFile();
        existing.setOriginalFilename("test.pdf");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.findByUserAndSha256Hash(user, "testhash")).thenReturn(Optional.of(existing));

        ResponseEntity<Map<String, Object>> response = fileController.checkDuplicate("testhash", authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("isDuplicate"));
        assertEquals("test.pdf", response.getBody().get("existingFileName"));
    }

    @Test
    void searchFilesShouldReturnMatchingFiles() {
        User user = new User();
        user.setEmail("alice@example.com");

        StoredFile file1 = new StoredFile();
        file1.setOriginalFilename("contract.pdf");
        file1.setContentType("application/pdf");
        file1.setFileSize(1024L);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(user, "contract"))
                .thenReturn(List.of(file1));

        ResponseEntity<List<FileController.FileResponse>> response =
                fileController.searchFiles("contract", null, null, null, null, null, null, null, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("contract.pdf", response.getBody().get(0).getFilename());
        assertEquals("Documents", response.getBody().get(0).getCategory());
    }

    @Test
    void downloadFileShouldStreamDecryptedContent() throws Exception {
        User user = new User();
        user.setEmail("alice@example.com");

        Path tempFile = Files.createTempFile(FileStorageConfig.STORAGE_ROOT, "ciphervault_dl_test", ".tmp");
        Files.write(tempFile, new byte[]{11, 22, 33});

        StoredFile storedFile = new StoredFile();
        storedFile.setOriginalFilename("secret.txt");
        storedFile.setStoragePath(tempFile.toString());
        storedFile.setContentType("text/plain");
        storedFile.setFileSize(3L);
        storedFile.setEncrypted(true);
        storedFile.setSha256Hash("fakehash");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(storedFile));

        ResponseEntity<StreamingResponseBody> response = fileController.downloadFile(5L, true, authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);

        verify(encryptionService, times(1)).decryptStream(any(), any(), any());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void downloadFileShouldReturnNotFoundWhenFileOwnedByDifferentUser() {
        User alice = new User();
        alice.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(fileRepository.findByIdAndUser(99L, alice)).thenReturn(Optional.empty());

        ResponseEntity<StreamingResponseBody> response = fileController.downloadFile(99L, true, authentication);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getFilePreviewShouldReturnNotFoundWhenFileOwnedByDifferentUser() {
        User alice = new User();
        alice.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(fileRepository.findByIdAndUser(99L, alice)).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = fileController.getFilePreview(99L, authentication);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void searchFilesShouldIsolatePerUser() {
        User alice = new User();
        alice.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(fileRepository.findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(alice, "tax"))
                .thenReturn(List.of());

        ResponseEntity<List<FileController.FileResponse>> response =
                fileController.searchFiles("tax", null, null, null, null, null, null, null, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(fileRepository, times(1)).findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(alice, "tax");
    }

    @Test
    void checkDuplicateShouldIsolatePerUser() {
        User alice = new User();
        alice.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(fileRepository.findByUserAndSha256Hash(alice, "cross_user_hash")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = fileController.checkDuplicate("cross_user_hash", authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().get("isDuplicate"));
        verify(fileRepository, times(1)).findByUserAndSha256Hash(alice, "cross_user_hash");
    }
}
