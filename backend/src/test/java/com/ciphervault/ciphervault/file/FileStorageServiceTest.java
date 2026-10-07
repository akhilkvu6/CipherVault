package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private KeyManagementService keyManagementService;

    @Mock
    private FileCategoryService fileCategoryService;

    @InjectMocks
    private FileStorageService fileStorageService;

    private User testUser;
    private StoredFile testFile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(testUser, "id", 1L);
        testUser.setEmail("test@example.com");
        testUser.setUsedStorage(0L);
        testUser.setStorageLimit(1000L);

        testFile = new StoredFile();
        org.springframework.test.util.ReflectionTestUtils.setField(testFile, "id", 10L);
        testFile.setUser(testUser);
        testFile.setStoragePath("test-path");
        testFile.setFileSize(500L);
    }

    @Test
    void testDeleteFileIdempotency() {
        when(fileRepository.findByIdAndUser(10L, testUser))
                .thenReturn(Optional.of(testFile));

        // When physical file is not found (deleted manually), deleteFile should still return true and delete from DB
        boolean result = fileStorageService.deleteFile(testUser, 10L);

        assertTrue(result, "Should return true even if physical file is missing");
        verify(fileRepository, times(1)).delete(testFile);
        verify(userRepository, times(1)).decrementStorageUsedAtomic(1L, 500L);
    }

    @Test
    void testDeleteFileNotFound() {
        when(fileRepository.findByIdAndUser(99L, testUser))
                .thenReturn(Optional.empty());

        boolean result = fileStorageService.deleteFile(testUser, 99L);

        assertFalse(result, "Should return false for non-existent file");
        verify(fileRepository, never()).delete(any(StoredFile.class));
    }

    @Test
    void testMaxUploadSizeConstantEquals1GB() {
        assertEquals(1073741824L, FileStorageService.MAX_FILE_SIZE, "MAX_FILE_SIZE must equal exactly 1,073,741,824 bytes (1 GB)");
    }
}
