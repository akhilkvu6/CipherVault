package com.ciphervault.ciphervault.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MetadataExtractionServiceTest {

    @InjectMocks
    private MetadataExtractionService metadataExtractionService;

    @Test
    void testLargeMetadata() throws Exception {
        // We want to test the storeRawMetadata method which was modified to remove the 65k limit
        Method storeRawMetadataMethod = MetadataExtractionService.class.getDeclaredMethod("storeRawMetadata", FileMetadata.class, Map.class);
        storeRawMetadataMethod.setAccessible(true);

        FileMetadata metadata = new FileMetadata();
        Map<String, Object> tags = new HashMap<>();
        
        // Generate a large string > 65k
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 70_000; i++) {
            sb.append("A");
        }
        tags.put("LargeTag", sb.toString());

        storeRawMetadataMethod.invoke(metadataExtractionService, metadata, tags);

        String rawJson = metadata.getRawMetadataJson();
        assertTrue(rawJson.length() > 65_000, "Raw metadata JSON should not be truncated");
        
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> parsed = mapper.readValue(rawJson, Map.class);
        assertEquals(sb.toString(), parsed.get("LargeTag"));
    }
}
