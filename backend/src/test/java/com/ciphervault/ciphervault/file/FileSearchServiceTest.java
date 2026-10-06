package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileSearchServiceTest {

    private FileRepository fileRepository;
    private FileMetadataRepository fileMetadataRepository;
    private FileCategoryService fileCategoryService;
    private FileSearchService searchService;
    private User testUser;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileRepository.class);
        fileMetadataRepository = Mockito.mock(FileMetadataRepository.class);
        fileCategoryService = new FileCategoryService();
        searchService = new FileSearchService(fileRepository, fileMetadataRepository, fileCategoryService);

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("test@example.com");
    }

    @Test
    void searchWithMetadataRepoShouldUseSpecification() {
        StoredFile f = new StoredFile();
        f.setOriginalFilename("test.jpg");
        when(fileRepository.findAll(any(Specification.class))).thenReturn(List.of(f));

        List<StoredFile> results = searchService.search(testUser, "test", "Images", "Nikon", null, null, null, null, null, null);
        assertEquals(1, results.size());
        assertEquals("test.jpg", results.get(0).getOriginalFilename());
        verify(fileRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    void searchWithoutMetadataRepoShouldFallbackToSimpleQuery() {
        FileSearchService fallbackService = new FileSearchService(fileRepository, null, fileCategoryService);
        StoredFile f = new StoredFile();
        f.setOriginalFilename("doc.pdf");
        when(fileRepository.findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(testUser, "doc"))
                .thenReturn(List.of(f));

        List<StoredFile> results = fallbackService.search(testUser, "doc", null, null, null, null, null, null, null, null);
        assertEquals(1, results.size());
        assertEquals("doc.pdf", results.get(0).getOriginalFilename());
        verify(fileRepository, times(1))
                .findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(testUser, "doc");
    }

    @Test
    void getSuggestionsShouldAggregateDistinctAttributes() {
        List<Object[]> mockRows = List.<Object[]>of(
            new Object[]{"Canon", "EOS R5", null, null, null, null, null, null, "Sunset"}
        );
        when(fileMetadataRepository.findSuggestions(testUser, "can")).thenReturn(mockRows);

        List<String> suggestions = searchService.getSuggestions(testUser, "can");
        assertEquals(1, suggestions.size());
        assertEquals("Canon", suggestions.get(0));
    }
}
