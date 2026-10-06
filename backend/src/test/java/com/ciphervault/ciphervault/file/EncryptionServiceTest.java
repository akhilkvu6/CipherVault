package com.ciphervault.ciphervault.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() throws Exception {
        encryptionService = new EncryptionService();

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        secretKey = keyGenerator.generateKey();
    }

    @Test
    void testStreamEncryptionAndDecryption() throws Exception {
        byte[] plainData = "Hello, CipherVault! This is a test string to be encrypted.".getBytes();

        ByteArrayInputStream input = new ByteArrayInputStream(plainData);
        ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();

        encryptionService.encryptStream(input, encryptedOutput, secretKey);
        byte[] encryptedData = encryptedOutput.toByteArray();

        ByteArrayInputStream encryptedInput = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream decryptedOutput = new ByteArrayOutputStream();

        encryptionService.decryptStream(encryptedInput, decryptedOutput, secretKey);
        byte[] decryptedData = decryptedOutput.toByteArray();

        assertArrayEquals(plainData, decryptedData, "Decrypted data should match original plaintext.");
    }

    @Test
    void testDecryptWithTruncatedIVThrows() {
        byte[] invalidData = new byte[5]; // Too short for IV (12 bytes)
        Arrays.fill(invalidData, (byte) 1);

        ByteArrayInputStream encryptedInput = new ByteArrayInputStream(invalidData);
        ByteArrayOutputStream decryptedOutput = new ByteArrayOutputStream();

        assertThrows(IOException.class, () -> {
            encryptionService.decryptStream(encryptedInput, decryptedOutput, secretKey);
        });
    }

    @Test
    void testDecryptWithCorruptedCiphertextThrows() throws Exception {
        byte[] plainData = "Data to corrupt".getBytes();

        ByteArrayInputStream input = new ByteArrayInputStream(plainData);
        ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();

        encryptionService.encryptStream(input, encryptedOutput, secretKey);
        byte[] encryptedData = encryptedOutput.toByteArray();

        // Corrupt the ciphertext (e.g., alter the last byte which affects the GCM authentication tag)
        encryptedData[encryptedData.length - 1] ^= 0xFF;

        ByteArrayInputStream corruptedInput = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream decryptedOutput = new ByteArrayOutputStream();

        assertThrows(IOException.class, () -> {
            encryptionService.decryptStream(corruptedInput, decryptedOutput, secretKey);
        });
    }
}
