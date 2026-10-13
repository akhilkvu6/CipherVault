package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.activity.ActivityService;
import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorageQuotaBoundaryTest {

    private static final long TEN_GIGABYTES = 10L * 1024L * 1024L * 1024L; // 10,737,418,240 bytes
    private static final long ONE_GIGABYTE = 1024L * 1024L * 1024L; // 1,073,741,824 bytes

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private MediaPreviewService mediaPreviewService;

    @Mock
    private KeyManagementService keyManagementService;

    @Mock
    private MetadataExtractionService metadataExtractionService;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileCategoryService fileCategoryService;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private FileStorageService fileStorageService;

    private User testUser;
    private SecretKey mockKey;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(testUser, "id", 100L);
        testUser.setEmail("quota_test@ciphervault.local");
        testUser.setUsername("quota_user");
        testUser.setStorageLimit(TEN_GIGABYTES);
        testUser.setUsedStorage(0L);

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        mockKey = keyGen.generateKey();
    }

    @Test
    @DisplayName("Empty file upload throws IllegalArgumentException")
    void testEmptyFileUploadRejected() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.storeFile(testUser, emptyFile)
        );
        assertEquals("File cannot be empty", ex.getMessage());
    }

    @Test
    @DisplayName("Upload succeeding within 10 GB capacity updates atomic storage")
    void testUploadWithinCapacitySucceeds() throws Exception {
        byte[] content = "Hello Secure World".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", content);

        when(fileRepository.findByUserAndSha256Hash(eq(testUser), anyString()))
                .thenReturn(Optional.empty());
        when(keyManagementService.getOrGenerateUserKey(testUser))
                .thenReturn(mockKey);
        when(userRepository.incrementStorageUsedAtomic(eq(100L), eq((long) content.length)))
                .thenReturn(1);
        when(fileRepository.save(any(StoredFile.class)))
                .thenAnswer(inv -> {
                    StoredFile sf = inv.getArgument(0);
                    org.springframework.test.util.ReflectionTestUtils.setField(sf, "id", 555L);
                    return sf;
                });

        StoredFile stored = fileStorageService.storeFile(testUser, file);

        assertNotNull(stored);
        assertEquals("doc.txt", stored.getOriginalFilename());
        assertEquals(content.length, stored.getFileSize());
        verify(userRepository, times(1)).incrementStorageUsedAtomic(eq(100L), eq((long) content.length));
    }

    @Test
    @DisplayName("Upload rejected when usedStorage + fileSize strictly exceeds user storage limit")
    void testUploadExceedingPreCheckQuota() {
        // Set user currently at 10 GB minus 5 bytes
        testUser.setUsedStorage(TEN_GIGABYTES - 5);

        // Uploading 10 bytes -> exceeds 10 GB limit by 5 bytes
        byte[] content = new byte[10];
        MockMultipartFile file = new MockMultipartFile("file", "overflow.bin", "application/octet-stream", content);

        StorageQuotaExceededException ex = assertThrows(StorageQuotaExceededException.class, () ->
                fileStorageService.storeFile(testUser, file)
        );
        assertEquals("Storage quota exceeded", ex.getMessage());
        verify(userRepository, never()).incrementStorageUsedAtomic(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Upload rejected when atomic SQL constraint triggers rowsUpdated == 0")
    void testUploadAtomicReservationRaceConditionFails() throws Exception {
        byte[] content = new byte[100];
        MockMultipartFile file = new MockMultipartFile("file", "race.bin", "application/octet-stream", content);

        when(fileRepository.findByUserAndSha256Hash(eq(testUser), anyString()))
                .thenReturn(Optional.empty());
        when(keyManagementService.getOrGenerateUserKey(testUser))
                .thenReturn(mockKey);

        // Simulate concurrent upload consuming the remaining bytes right before atomic update
        when(userRepository.incrementStorageUsedAtomic(eq(100L), eq(100L)))
                .thenReturn(0);

        StorageQuotaExceededException ex = assertThrows(StorageQuotaExceededException.class, () ->
                fileStorageService.storeFile(testUser, file)
        );
        assertEquals("Storage quota exceeded", ex.getMessage());
    }

    @Test
    @DisplayName("File deletion atomically reclaims storage quota")
    void testFileDeleteReclaimsQuota() {
        StoredFile storedFile = new StoredFile();
        org.springframework.test.util.ReflectionTestUtils.setField(storedFile, "id", 777L);
        storedFile.setUser(testUser);
        storedFile.setOriginalFilename("video.mp4");
        storedFile.setStoragePath("encrypted/fake-path.enc");
        storedFile.setFileSize(50_000_000L); // 50 MB

        testUser.setUsedStorage(100_000_000L);

        when(fileRepository.findByIdAndUser(777L, testUser))
                .thenReturn(Optional.of(storedFile));

        boolean deleted = fileStorageService.deleteFile(testUser, 777L);

        assertTrue(deleted);
        verify(userRepository, times(1)).decrementStorageUsedAtomic(100L, 50_000_000L);
        assertEquals(50_000_000L, testUser.getUsedStorage());
        verify(fileRepository, times(1)).delete(storedFile);
    }

    @Test
    @DisplayName("Duplicate upload attempt triggers DuplicateFileException and does not allocate quota")
    void testDuplicateUploadDoesNotConsumeQuota() {
        byte[] content = "Duplicate bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "dup.txt", "text/plain", content);

        StoredFile existing = new StoredFile();
        existing.setOriginalFilename("original_dup.txt");
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 888L);

        when(fileRepository.findByUserAndSha256Hash(eq(testUser), anyString()))
                .thenReturn(Optional.of(existing));

        DuplicateFileException ex = assertThrows(DuplicateFileException.class, () ->
                fileStorageService.storeFile(testUser, file)
        );
        assertEquals("Duplicate file already exists", ex.getMessage());
        verify(userRepository, never()).incrementStorageUsedAtomic(anyLong(), anyLong());
    }
}
