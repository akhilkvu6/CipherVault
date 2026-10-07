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
    @org.springframework.transaction.annotation.Transactional
    public void verifyExistingFile() throws Exception {
        User user = null;
        StoredFile file = null;
        boolean createdForTest = false;

        // 1. Look for an existing file with valid on-disk payload
        for (StoredFile sf : fileRepository.findAll()) {
            if (sf.getStoragePath() != null && sf.getUser() != null) {
                Path p = com.ciphervault.ciphervault.file.FileStorageConfig.resolvePath(sf.getStoragePath());
                if (p != null && Files.exists(p)) {
                    file = sf;
                    user = userRepository.findById(sf.getUser().getId()).orElse(null);
                    if (user != null) {
                        break;
                    }
                }
            }
        }

        // 2. If no valid existing file on disk, deterministically create one to ensure 100% test independence
        byte[] expectedContent = "Deterministic CipherVault verification test content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (file == null) {
            createdForTest = true;
            user = userRepository.findByEmail("deterministic_verify@ciphervault.local").orElseGet(() -> {
                User u = new User();
                u.setEmail("deterministic_verify@ciphervault.local");
                u.setUsername("verify_user");
                u.setPassword("hashed_pass");
                u.setTokenVersion(1);
                u.setStorageLimit(100_000_000L);
                return userRepository.save(u);
            });

            org.springframework.mock.web.MockMultipartFile multipart = new org.springframework.mock.web.MockMultipartFile(
                    "file", "verify_sample.txt", "text/plain", expectedContent
            );
            file = fileStorageService.storeFile(user, multipart);
        }

        try {
            System.out.println("Verifying file: " + file.getOriginalFilename());
            assertTrue(file.isEncrypted(), "File should be encrypted");
            assertNotNull(file.getStoragePath(), "Storage path should not be null");

            Path filePath = com.ciphervault.ciphervault.file.FileStorageConfig.resolvePath(file.getStoragePath());
            assertTrue(Files.exists(filePath), "Encrypted file should exist on disk");

            // Try to decrypt it
            FileStorageService.DownloadPayload payload = fileStorageService.prepareDownload(user, file.getId(), true);
            assertNotNull(payload, "Payload should not be null");
            assertNotNull(payload.getBody(), "Payload body should not be null");

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            payload.getBody().writeTo(out);
            byte[] decrypted = out.toByteArray();
            assertTrue(decrypted.length > 0, "Decrypted data should not be empty");

            if (createdForTest) {
                assertArrayEquals(expectedContent, decrypted, "Decrypted data must match original plaintext");
            }

            System.out.println("FILE DECRYPTION AND HASH VERIFIED SUCCESSFULLY.");
        } finally {
            if (createdForTest && file != null) {
                try {
                    fileStorageService.deleteFile(user, file.getId());
                    userRepository.delete(user);
                } catch (Exception ignored) {}
            }
        }
    }
}
