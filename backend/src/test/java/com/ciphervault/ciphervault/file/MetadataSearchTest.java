package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MetadataSearchTest {

    private FileRepository fileRepository;
    private UserRepository userRepository;
    private EncryptionService encryptionService;
    private MediaPreviewService mediaPreviewService;
    private KeyManagementService keyManagementService;
    private MetadataExtractionService metadataExtractionService;
    private FileMetadataRepository fileMetadataRepository;
    private FileController fileController;
    private Authentication authentication;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        userRepository = mock(UserRepository.class);
        encryptionService = mock(EncryptionService.class);
        mediaPreviewService = mock(MediaPreviewService.class);
        keyManagementService = mock(KeyManagementService.class);
        metadataExtractionService = new MetadataExtractionService();
        fileMetadataRepository = mock(FileMetadataRepository.class);

        FileCategoryService fileCategoryService = new FileCategoryService();
        FileSearchService fileSearchService = new FileSearchService(fileRepository, fileMetadataRepository, fileCategoryService);

        fileController = new FileController(
                fileRepository,
                userRepository,
                encryptionService,
                mediaPreviewService,
                keyManagementService,
                metadataExtractionService,
                fileMetadataRepository,
                fileCategoryService,
                fileSearchService
        );

        alice = new User();
        alice.setEmail("alice@example.com");

        bob = new User();
        bob.setEmail("bob@example.com");

        authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(bob));
    }

    @Test
    void extractMetadataShouldSafelyHandleNonExistentFile() {
        Path fake = Path.of("non_existent_file.xyz");
        FileMetadata meta = metadataExtractionService.extractMetadata(fake, "fake.xyz", "application/octet-stream");
        assertNotNull(meta);
        assertNull(meta.getCameraMake());
        assertNull(meta.getResolution());
    }

    @Test
    void extractMetadataShouldExtractDimensionsFromImage() throws Exception {
        Path tempImg = Files.createTempFile("test_img_", ".png");
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(640, 480, java.awt.image.BufferedImage.TYPE_INT_RGB);
            javax.imageio.ImageIO.write(img, "png", tempImg.toFile());

            FileMetadata meta = metadataExtractionService.extractMetadata(tempImg, "test.png", "image/png");
            assertNotNull(meta);
            assertEquals(640, meta.getWidth());
            assertEquals(480, meta.getHeight());
            assertEquals("640x480", meta.getResolution());
        } finally {
            Files.deleteIfExists(tempImg);
        }
    }

    @Test
    void searchFilesShouldReturnMetadataInResponse() {
        StoredFile file = new StoredFile();
        file.setOriginalFilename("galaxy_photo.jpg");
        file.setContentType("image/jpeg");
        file.setFileSize(2048L);
        file.setUser(alice);

        FileMetadata meta = new FileMetadata();
        meta.setFile(file);
        meta.setCameraMake("Samsung");
        meta.setCameraModel("Galaxy S24 Ultra");
        meta.setResolution("4000x3000");
        file.setMetadata(meta);

        when(fileRepository.findAll(any(Specification.class))).thenReturn(List.of(file));

        ResponseEntity<List<FileController.FileResponse>> response = fileController.searchFiles(
                "Samsung", "Images", "Samsung", "Galaxy S24 Ultra", "4000x3000", null, null, null, null, authentication
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        FileController.FileResponse item = response.getBody().get(0);
        assertEquals("galaxy_photo.jpg", item.getFilename());
        assertNotNull(item.getMetadata());
        assertEquals("Samsung", item.getMetadata().getCameraMake());
        assertEquals("Galaxy S24 Ultra", item.getMetadata().getCameraModel());
        assertEquals("4000x3000", item.getMetadata().getResolution());
    }

    @Test
    void getSuggestionsShouldReturnTopSuggestionsForUser() {
        List<Object[]> mockRows = List.<Object[]>of(
            new Object[]{"Samsung", "Galaxy S24 Ultra", "3840x2160", "HEVC", "Arijit Singh", "CipherVault", null, "Acoustic", null},
            new Object[]{"Sony", "Alpha 7", "1920x1080", "H.264", null, null, null, null, null}
        );
        when(fileMetadataRepository.findSuggestions(alice, "")).thenReturn(mockRows);

        ResponseEntity<List<String>> response = fileController.getSuggestions(null, authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() <= 10);
        assertTrue(response.getBody().contains("Galaxy S24 Ultra"));
        assertTrue(response.getBody().contains("Samsung"));
        assertTrue(response.getBody().contains("HEVC"));
    }

    @Test
    void getSuggestionsShouldFilterByPrefixForAutocomplete() {
        List<Object[]> mockRows = List.<Object[]>of(
            new Object[]{"Samsung", null, null, null, null, null, null, null, null}
        );
        when(fileMetadataRepository.findSuggestions(alice, "sam")).thenReturn(mockRows);

        ResponseEntity<List<String>> response = fileController.getSuggestions("Sam", authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Samsung", response.getBody().get(0));
    }
}
