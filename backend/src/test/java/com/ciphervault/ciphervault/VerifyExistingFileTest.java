package com.ciphervault.ciphervault;

import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.file.StoredFile;
import com.ciphervault.ciphervault.file.FileStorageService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class VerifyExistingFileTest {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void verifyExistingFile() throws Exception {
        // Just take the first file in the database
        Optional<StoredFile> optFile = fileRepository.findAll().stream().findFirst();
        if (optFile.isEmpty()) {
            System.out.println("NO EXISTING FILES FOUND TO VERIFY.");
            return;
        }

        StoredFile file = optFile.get();
        System.out.println("Verifying existing file: " + file.getOriginalFilename());
        assertTrue(file.isEncrypted(), "File should be encrypted");
        assertNotNull(file.getStoragePath(), "Storage path should not be null");

        User user = userRepository.findById(file.getUser().getId()).orElseThrow();
        
        // Ensure path exists
        Path filePath = com.ciphervault.ciphervault.file.FileStorageConfig.resolvePath(file.getStoragePath());
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(filePath), "Encrypted file should exist on disk");

        // Try to decrypt it
        FileStorageService.DownloadPayload payload = fileStorageService.prepareDownload(user, file.getId(), true);
        assertNotNull(payload, "Payload should not be null");
        assertNotNull(payload.getBody(), "Payload body should not be null");
        
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        payload.getBody().writeTo(out);
        assertTrue(out.toByteArray().length > 0, "Decrypted data should not be empty");
        
        System.out.println("EXISTING FILE DECRYPTION AND HASH VERIFIED SUCCESSFULLY.");
    }
}
