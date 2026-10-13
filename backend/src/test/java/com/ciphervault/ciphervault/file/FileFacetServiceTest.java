package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileFacetServiceTest {

    private FileRepository fileRepository;
    private FileSearchService fileSearchService;
    private FileCategoryService fileCategoryService;
    private FileFacetService facetService;
    private User testUser;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileRepository.class);
        fileSearchService = Mockito.mock(FileSearchService.class);
        fileCategoryService = new FileCategoryService();
        facetService = new FileFacetService(fileRepository, fileSearchService, fileCategoryService);

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("facetuser@example.com");
    }

    @Test
    void getFacetsShouldReturnEmptyWhenUserIsNull() {
        FileFacetsResponse response = facetService.getFacets(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        assertNotNull(response);
        assertEquals(0, response.getTotalMatchingFiles());
        assertTrue(response.getFacets().isEmpty());
    }

    @Test
    void getFacetsShouldCalculateTotalMatchingFilesAndCategoryFacets() {
        when(fileSearchService.buildSpecification(
                eq(testUser), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn((root, query, cb) -> cb.conjunction());

        when(fileRepository.count(any(Specification.class))).thenReturn(5L);

        FileFacetsResponse response = facetService.getFacets(
                testUser, "photo", "IMAGES", "Sony", null, null, null, null, null, null, null, null, null, null
        );

        assertNotNull(response);
        assertEquals(5L, response.getTotalMatchingFiles());
        assertFalse(response.getFacets().isEmpty());

        FileFacetsResponse.FacetField categoryFacet = response.getFacets().stream()
                .filter(f -> "category".equals(f.getField()))
                .findFirst()
                .orElse(null);

        assertNotNull(categoryFacet);
        assertEquals("File Type", categoryFacet.getLabel());
        assertTrue(categoryFacet.isAvailable());
        assertFalse(categoryFacet.getValues().isEmpty());
    }
}
