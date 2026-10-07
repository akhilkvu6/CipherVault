package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class StorageControllerTest {

    private UserRepository userRepository;
    private FileRepository fileRepository;
    private StorageController storageController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        fileRepository = Mockito.mock(FileRepository.class);
        storageController = new StorageController(userRepository, fileRepository, new FileCategoryService());
        
        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
    }

    @Test
    void getSummaryShouldReturnAccurateStorageData() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setStorageLimit(100_000_000L);
        user.setUsedStorage(25_000_000L);
        
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.countByUser(user)).thenReturn(42L);
        
        ResponseEntity<Map<String, Object>> response = storageController.getSummary(authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100_000_000L, response.getBody().get("totalBytes"));
        assertEquals(25_000_000L, response.getBody().get("usedBytes"));
        assertEquals(75_000_000L, response.getBody().get("availableBytes"));
        assertEquals(25.0, response.getBody().get("usagePercentage"));
        assertEquals(42L, response.getBody().get("fileCount"));
    }

    @Test
    void getCategoriesShouldReturnCategoryBreakdown() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        
        List<Object[]> stats = new java.util.ArrayList<>();
        stats.add(new Object[]{"image.jpg", "image/jpeg", 1000L});
        stats.add(new Object[]{"doc.pdf", "application/pdf", 2000L});
        when(fileRepository.findFileStatsByUser(user)).thenReturn(stats);
        
        ResponseEntity<List<Map<String, Object>>> response = storageController.getCategories(authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().size()); // 5 standard categories
    }



    @Test
    void getLargeFilesShouldReturnFilesOrderedBySize() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        
        StoredFile f1 = new StoredFile(); f1.setOriginalFilename("large.bin"); f1.setFileSize(500000L);
        when(fileRepository.findByUserAndFileSizeGreaterThanEqualOrderByFileSizeDesc(any(), anyLong(), any())).thenReturn(new PageImpl<>(List.of(f1)));
        
        ResponseEntity<List<Map<String, Object>>> response = storageController.getLargeFiles(authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("large.bin", response.getBody().get(0).get("filename"));
    }

    @Test
    void getDuplicatesShouldReturnGroupedDuplicateFilesAndSavings() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        StoredFile f1 = new StoredFile();
        f1.setId(101L);
        f1.setOriginalFilename("photo1.jpg");
        f1.setSha256Hash("aabbcc123");
        f1.setFileSize(2048L);
        f1.setContentType("image/jpeg");

        StoredFile f2 = new StoredFile();
        f2.setId(102L);
        f2.setOriginalFilename("photo2.jpg");
        f2.setSha256Hash("aabbcc123");
        f2.setFileSize(2048L);
        f2.setContentType("image/jpeg");

        StoredFile f3 = new StoredFile();
        f3.setId(103L);
        f3.setOriginalFilename("unique.txt");
        f3.setSha256Hash("uniquerecord");
        f3.setFileSize(512L);
        f3.setContentType("text/plain");

        when(fileRepository.findByUser(user)).thenReturn(List.of(f1, f2, f3));

        ResponseEntity<List<Map<String, Object>>> response = storageController.getDuplicates(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        Map<String, Object> group = response.getBody().get(0);
        assertEquals("aabbcc123", group.get("hash"));
        assertEquals(2L, ((Number) group.get("fileCount")).longValue());
        assertEquals(2048L, ((Number) group.get("fileSize")).longValue());
        assertEquals(4096L, ((Number) group.get("totalOccupied")).longValue());
        assertEquals(2048L, ((Number) group.get("potentialSaving")).longValue());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) group.get("files");
        assertEquals(2, files.size());
    }

    @Test
    void getDuplicatesShouldReturnEmptyListWhenNoDuplicates() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        StoredFile f1 = new StoredFile();
        f1.setId(101L);
        f1.setOriginalFilename("file1.pdf");
        f1.setSha256Hash("hash_one");
        f1.setFileSize(1000L);

        when(fileRepository.findByUser(user)).thenReturn(List.of(f1));

        ResponseEntity<List<Map<String, Object>>> response = storageController.getDuplicates(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }
}
