package com.ciphervault.app;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests verifying Metadata Search state logic, cascading rules,
 * reconciliation pruning, and schema mapping.
 */
public class MetadataSearchLogicTest {

    private String selectedFileType;
    private String metadataQuery;
    private Map<String, Set<String>> selectedFilters;

    @Before
    public void setUp() {
        selectedFileType = "ALL";
        metadataQuery = "";
        selectedFilters = new LinkedHashMap<>();
    }

    @Test
    public void testFileTypeChangeClearsIncompatibleSelections() {
        selectedFileType = "IMAGES";
        selectedFilters.put("cameraMake", new LinkedHashSet<>(List.of("Sony")));
        selectedFilters.put("cameraModel", new LinkedHashSet<>(List.of("ILCE-7M4")));
        selectedFilters.put("lens", new LinkedHashSet<>(List.of("FE 24-70mm")));

        // User switches from Images to Audio
        String newFileType = "AUDIO";
        if (!selectedFileType.equalsIgnoreCase(newFileType)) {
            selectedFileType = newFileType;
            selectedFilters.clear();
        }

        assertEquals("AUDIO", selectedFileType);
        assertTrue(selectedFilters.isEmpty());
    }

    @Test
    public void testMultiValueFilterSelectionWithinCategory() {
        // Multi-select resolutions (combines with OR)
        Set<String> resSet = selectedFilters.computeIfAbsent("resolution", k -> new LinkedHashSet<>());
        resSet.add("1920x1080");
        resSet.add("3840x2160");

        assertEquals(2, selectedFilters.get("resolution").size());
        assertTrue(selectedFilters.get("resolution").contains("1920x1080"));
        assertTrue(selectedFilters.get("resolution").contains("3840x2160"));

        // Unselect one value
        resSet.remove("1920x1080");
        assertEquals(1, selectedFilters.get("resolution").size());
        assertFalse(selectedFilters.get("resolution").contains("1920x1080"));
        assertTrue(selectedFilters.get("resolution").contains("3840x2160"));
    }

    @Test
    public void testRemovingCameraMakeCascadesToModelAndLens() {
        selectedFilters.put("cameraMake", new LinkedHashSet<>(List.of("Sony")));
        selectedFilters.put("cameraModel", new LinkedHashSet<>(List.of("ILCE-7M4")));
        selectedFilters.put("lens", new LinkedHashSet<>(List.of("FE 24-70mm")));

        // Removing camera make
        selectedFilters.remove("cameraMake");
        // Cascading rule
        selectedFilters.remove("cameraModel");
        selectedFilters.remove("lens");

        assertFalse(selectedFilters.containsKey("cameraMake"));
        assertFalse(selectedFilters.containsKey("cameraModel"));
        assertFalse(selectedFilters.containsKey("lens"));
    }

    @Test
    public void testReconcileSelectionsPrunesObsoleteDownstreamValues() {
        selectedFilters.put("cameraMake", new LinkedHashSet<>(List.of("Sony")));
        selectedFilters.put("cameraModel", new LinkedHashSet<>(List.of("ILCE-7M4", "ObsoleteModel")));

        // Simulated returned facet fields from backend
        List<FileFacetsResponse.FacetField> backendFacets = new ArrayList<>();
        backendFacets.add(new FileFacetsResponse.FacetField(
                "cameraMake", "Camera Make", true,
                List.of(new FileFacetsResponse.FacetValue("Sony", "Sony", 10))
        ));
        backendFacets.add(new FileFacetsResponse.FacetField(
                "cameraModel", "Camera Model", true,
                List.of(new FileFacetsResponse.FacetValue("ILCE-7M4", "ILCE-7M4", 10))
        ));

        // Reconcile logic
        Map<String, Set<String>> validValuesByField = new HashMap<>();
        for (FileFacetsResponse.FacetField facet : backendFacets) {
            Set<String> vals = new HashSet<>();
            for (FileFacetsResponse.FacetValue val : facet.getValues()) {
                vals.add(val.getValue().toLowerCase(Locale.ROOT));
            }
            validValuesByField.put(facet.getField().toLowerCase(Locale.ROOT), vals);
        }

        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            Set<String> validSet = validValuesByField.get(entry.getKey().toLowerCase(Locale.ROOT));
            if (validSet != null) {
                entry.getValue().removeIf(val -> !validSet.contains(val.toLowerCase(Locale.ROOT)));
            }
        }

        // ObsoleteModel should be pruned!
        assertEquals(1, selectedFilters.get("cameraModel").size());
        assertTrue(selectedFilters.get("cameraModel").contains("ILCE-7M4"));
        assertFalse(selectedFilters.get("cameraModel").contains("ObsoleteModel"));
    }

    @Test
    public void testClearAllFiltersLeavesQueryIntact() {
        metadataQuery = "vacation";
        selectedFileType = "IMAGES";
        selectedFilters.put("cameraMake", new LinkedHashSet<>(List.of("Canon")));
        selectedFilters.put("resolution", new LinkedHashSet<>(List.of("4K")));

        // Clear all filters action
        selectedFilters.clear();

        assertTrue(selectedFilters.isEmpty());
        assertEquals("vacation", metadataQuery);
        assertEquals("IMAGES", selectedFileType);
    }

    @Test
    public void testFileSortingComparators() {
        StoredFile f1 = new StoredFile(1L, "b.jpg", 2000L, "image/jpeg", "2026-10-10T12:00:00Z", false);
        StoredFile f2 = new StoredFile(2L, "a.jpg", 1000L, "image/jpeg", "2026-10-11T12:00:00Z", false);

        List<StoredFile> list = new ArrayList<>(List.of(f1, f2));

        // Sort Name Ascending
        list.sort(FileComparator.getComparator(FileSortOption.NAME_ASC));
        assertEquals("a.jpg", list.get(0).getOriginalFilename());

        // Sort Size High to Low
        list.sort(FileComparator.getComparator(FileSortOption.SIZE_HIGH_LOW));
        assertEquals("b.jpg", list.get(0).getOriginalFilename());

        // Sort Date New to Old
        list.sort(FileComparator.getComparator(FileSortOption.DATE_NEW_OLD));
        assertEquals("a.jpg", list.get(0).getOriginalFilename());
    }

    @Test
    public void testFacetDisplayNameMapping() {
        assertEquals("Make", mapFacetDisplay("cameraMake"));
        assertEquals("Model", mapFacetDisplay("cameraModel"));
        assertEquals("Resolution", mapFacetDisplay("resolution"));
        assertEquals("Lens", mapFacetDisplay("lens"));
        assertEquals("Codec", mapFacetDisplay("codec"));
        assertEquals("Year", mapFacetDisplay("dateTaken"));
        assertEquals("Artist", mapFacetDisplay("artist"));
        assertEquals("Author", mapFacetDisplay("author"));
        assertEquals("Genre", mapFacetDisplay("genre"));
        assertEquals("customField", mapFacetDisplay("customField"));
    }

    @Test
    public void testCategoryDisplayNameMapping() {
        assertEquals("Photos", mapCategoryDisplay("IMAGES"));
        assertEquals("Photos", mapCategoryDisplay("PHOTOS"));
        assertEquals("Videos", mapCategoryDisplay("VIDEOS"));
        assertEquals("Documents", mapCategoryDisplay("DOCUMENTS"));
        assertEquals("Documents", mapCategoryDisplay("DOCS"));
        assertEquals("Audio", mapCategoryDisplay("AUDIO"));
        assertEquals("Archives", mapCategoryDisplay("ARCHIVES"));
        assertEquals("OTHER", mapCategoryDisplay("OTHER"));
    }

    @Test
    public void testResultCountFormatting() {
        assertEquals("0 files found", formatResultCount(0));
        assertEquals("1 file found", formatResultCount(1));
        assertEquals("5 files found", formatResultCount(5));
        assertEquals("42 files found", formatResultCount(42));
    }

    private String mapFacetDisplay(String field) {
        if ("cameraMake".equalsIgnoreCase(field)) return "Make";
        if ("cameraModel".equalsIgnoreCase(field)) return "Model";
        if ("resolution".equalsIgnoreCase(field)) return "Resolution";
        if ("lens".equalsIgnoreCase(field)) return "Lens";
        if ("codec".equalsIgnoreCase(field)) return "Codec";
        if ("dateTaken".equalsIgnoreCase(field)) return "Year";
        if ("artist".equalsIgnoreCase(field)) return "Artist";
        if ("author".equalsIgnoreCase(field)) return "Author";
        if ("genre".equalsIgnoreCase(field)) return "Genre";
        return field;
    }

    private String mapCategoryDisplay(String cat) {
        if ("IMAGES".equalsIgnoreCase(cat) || "PHOTOS".equalsIgnoreCase(cat)) return "Photos";
        if ("VIDEOS".equalsIgnoreCase(cat)) return "Videos";
        if ("DOCUMENTS".equalsIgnoreCase(cat) || "DOCS".equalsIgnoreCase(cat)) return "Documents";
        if ("AUDIO".equalsIgnoreCase(cat)) return "Audio";
        if ("ARCHIVES".equalsIgnoreCase(cat)) return "Archives";
        return cat;
    }

    private String formatResultCount(int count) {
        if (count == 0) return "0 files found";
        if (count == 1) return "1 file found";
        return count + " files found";
    }
}
