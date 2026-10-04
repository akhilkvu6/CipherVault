package com.ciphervault.ciphervault.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileCategoryServiceTest {

    private FileCategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new FileCategoryService();
    }

    @Test
    void getStandardCategoriesShouldReturnFiveCategories() {
        List<String> categories = categoryService.getStandardCategories();
        assertEquals(5, categories.size());
        assertTrue(categories.contains("Images"));
        assertTrue(categories.contains("Documents"));
        assertTrue(categories.contains("Media"));
        assertTrue(categories.contains("Archives"));
        assertTrue(categories.contains("Other"));
    }

    @Test
    void determineCategoryShouldAccuratelyClassifyExtensions() {
        assertEquals("Images", categoryService.determineCategory("vacation.JPG", "application/octet-stream"));
        assertEquals("Images", categoryService.determineCategory("banner.png", "image/png"));
        assertEquals("Documents", categoryService.determineCategory("invoice.pdf", "application/pdf"));
        assertEquals("Documents", categoryService.determineCategory("report.docx", "application/octet-stream"));
        assertEquals("Documents", categoryService.determineCategory("sheet.xlsx", null));
        assertEquals("Media", categoryService.determineCategory("song.mp3", "audio/mpeg"));
        assertEquals("Media", categoryService.determineCategory("clip.mp4", "video/mp4"));
        assertEquals("Archives", categoryService.determineCategory("backup.zip", "application/zip"));
        assertEquals("Archives", categoryService.determineCategory("bundle.tar.gz", "application/gzip"));
        assertEquals("Other", categoryService.determineCategory("program.exe", "application/octet-stream"));
        assertEquals("Other", categoryService.determineCategory("binary.dat", null));
    }
}
