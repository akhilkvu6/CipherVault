package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FileFacetServiceIntegrationTest {

    @Autowired
    private FileFacetService fileFacetService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void setUp() {
        user = userRepository.findByEmail("user@gmail.com").orElse(null);
        if (user == null) {
            user = userRepository.findAll().stream().findFirst().orElse(null);
        }
        assertNotNull(user, "Test user must exist");
    }

    @Test
    public void testGetFacetsWithRealDatabase() {
        FileFacetsResponse response = fileFacetService.getFacets(
                user, null, "ALL", null, null, null, null, null, null, null, null, null, null, null
        );

        assertNotNull(response);
        assertTrue(response.getTotalMatchingFiles() > 0);
        assertFalse(response.getFacets().isEmpty());

        boolean hasCategory = response.getFacets().stream().anyMatch(f -> "category".equals(f.getField()));
        boolean hasMake = response.getFacets().stream().anyMatch(f -> "cameraMake".equals(f.getField()));
        boolean hasModel = response.getFacets().stream().anyMatch(f -> "cameraModel".equals(f.getField()));
        boolean hasRes = response.getFacets().stream().anyMatch(f -> "resolution".equals(f.getField()));

        assertTrue(hasCategory, "Must return category facet");
        assertTrue(hasMake, "Must return cameraMake facet");
        assertTrue(hasModel, "Must return cameraModel facet");
        assertTrue(hasRes, "Must return resolution facet");
    }

    @Test
    public void testCascadingPhotosToCameraMakeToModelToResolution() {
        // Step 1: Filter to IMAGES (Photos)
        FileFacetsResponse photoFacets = fileFacetService.getFacets(
                user, null, "IMAGES", null, null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(photoFacets);
        assertTrue(photoFacets.getTotalMatchingFiles() > 0);

        FileFacetsResponse.FacetField makeField = photoFacets.getFacets().stream()
                .filter(f -> "cameraMake".equals(f.getField()))
                .findFirst()
                .orElse(null);
        assertNotNull(makeField, "Camera Make must be present for photos");
        assertTrue(makeField.getValues().size() > 0);

        // Step 2: Select Camera Make = "Canon"
        FileFacetsResponse canonFacets = fileFacetService.getFacets(
                user, null, "IMAGES", "Canon", null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(canonFacets);

        FileFacetsResponse.FacetField canonModels = canonFacets.getFacets().stream()
                .filter(f -> "cameraModel".equals(f.getField()))
                .findFirst()
                .orElse(null);
        assertNotNull(canonModels, "Camera models must be returned for Canon");
        assertFalse(canonModels.getValues().isEmpty());

        // All returned models should be Canon models
        for (FileFacetsResponse.FacetValue modelVal : canonModels.getValues()) {
            assertTrue(modelVal.getValue().toLowerCase().contains("canon")
                            || modelVal.getValue().toLowerCase().contains("eos"),
                    "Model should be a Canon model: " + modelVal.getValue());
        }

        // Step 3: Select Camera Model = "Canon EOS 5D Mark III"
        FileFacetsResponse modelFacets = fileFacetService.getFacets(
                user, null, "IMAGES", "Canon", "Canon EOS 5D Mark III", null, null, null, null, null, null, null, null, null
        );
        assertNotNull(modelFacets);

        FileFacetsResponse.FacetField resField = modelFacets.getFacets().stream()
                .filter(f -> "resolution".equals(f.getField()))
                .findFirst()
                .orElse(null);
        assertNotNull(resField, "Resolution facet must be returned");
        assertFalse(resField.getValues().isEmpty());

        // Step 4: Multi-value selection within Make (Canon OR SONY)
        FileFacetsResponse multiMakeFacets = fileFacetService.getFacets(
                user, null, "IMAGES", "Canon,SONY", null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(multiMakeFacets);
        FileFacetsResponse.FacetField multiModels = multiMakeFacets.getFacets().stream()
                .filter(f -> "cameraModel".equals(f.getField()))
                .findFirst()
                .orElse(null);
        assertNotNull(multiModels);
        assertTrue(multiModels.getValues().stream().anyMatch(m -> m.getValue().toLowerCase().contains("canon") || m.getValue().toLowerCase().contains("eos")));
        assertTrue(multiModels.getValues().stream().anyMatch(m -> m.getValue().toLowerCase().contains("ilce") || m.getValue().toLowerCase().contains("dsc")));
    }
}
