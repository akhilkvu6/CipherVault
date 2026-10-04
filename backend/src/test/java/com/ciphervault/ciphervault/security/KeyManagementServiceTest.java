package com.ciphervault.ciphervault.security;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeyManagementServiceTest {

    private UserRepository userRepository;
    private KeyManagementService keyManagementService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        keyManagementService = new KeyManagementService(userRepository);

        ReflectionTestUtils.setField(keyManagementService, "masterPassphrase", "TestMasterPassphrase2026!");
        ReflectionTestUtils.setField(keyManagementService, "pbkdf2Salt", "TestSalt2026!");
        ReflectionTestUtils.setField(keyManagementService, "pbkdf2Iterations", 1000); // lower iterations for quick unit tests

        keyManagementService.init();
    }

    @Test
    void shouldGenerateAndDecryptUserKey() {
        String encryptedKey = keyManagementService.generateAndEncryptUserKey();
        assertNotNull(encryptedKey);
        assertFalse(encryptedKey.isBlank());

        SecretKey decryptedKey = keyManagementService.decryptKeyWithKek(encryptedKey);
        assertNotNull(decryptedKey);
        assertEquals("AES", decryptedKey.getAlgorithm());
        assertEquals(32, decryptedKey.getEncoded().length); // 256-bit key
    }

    @Test
    void shouldRetrieveExistingUserKey() {
        String encryptedKey = keyManagementService.generateAndEncryptUserKey();
        User user = new User();
        user.setId(10L);
        user.setEmail("bob@example.com");
        user.setUserKey(encryptedKey);

        SecretKey key = keyManagementService.getOrGenerateUserKey(user);
        assertNotNull(key);
        assertEquals(32, key.getEncoded().length);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLazilyGenerateKeyForUserWithoutKey() {
        User user = new User();
        user.setId(20L);
        user.setEmail("carol@example.com");
        user.setUserKey(null);

        SecretKey key = keyManagementService.getOrGenerateUserKey(user);
        assertNotNull(key);
        assertNotNull(user.getUserKey());
        verify(userRepository, times(1)).save(user);
    }
}
