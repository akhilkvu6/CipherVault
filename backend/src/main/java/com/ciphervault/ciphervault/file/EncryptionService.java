package com.ciphervault.ciphervault.file;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int BUFFER_SIZE = 16 * 1024;

    private final SecureRandom secureRandom = new SecureRandom();

    // Encrypt a file stream with AES-256-GCM.
    public void encryptStream(
            InputStream input,
            OutputStream output,
            SecretKey key) throws IOException, GeneralSecurityException {

        requireKey(key);

        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        output.write(iv);

        Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, key, iv);

        try (CipherOutputStream encrypted = new CipherOutputStream(output, cipher)) {
            copy(input, encrypted);
        }
    }

    // Decrypt a file stream with the IV stored at the beginning of the file.
    public void decryptStream(
            InputStream input,
            OutputStream output,
            SecretKey key) throws IOException, GeneralSecurityException {

        requireKey(key);

        byte[] iv = input.readNBytes(IV_LENGTH);
        if (iv.length != IV_LENGTH) {
            throw new IOException("Invalid encrypted file: missing GCM IV.");
        }

        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, key, iv);

        try (CipherInputStream decrypted = new CipherInputStream(input, cipher)) {
            copy(decrypted, output);
        }
    }

    // Encrypt small in-memory data with AES-256-GCM.
    public byte[] encrypt(byte[] data, SecretKey key) {
        requireKey(key);

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            byte[] ciphertext = createCipher(
                    Cipher.ENCRYPT_MODE, key, iv).doFinal(data);

            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed.", e);
        }
    }

    // Decrypt small in-memory data containing IV + ciphertext.
    public byte[] decrypt(byte[] encryptedData, SecretKey key) {
        requireKey(key);

        if (encryptedData == null || encryptedData.length <= IV_LENGTH + 16) {
            throw new IllegalArgumentException("Invalid encrypted data.");
        }

        try {
            byte[] iv = Arrays.copyOf(encryptedData, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(
                    encryptedData, IV_LENGTH, encryptedData.length);

            return createCipher(
                    Cipher.DECRYPT_MODE, key, iv).doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed.", e);
        }
    }

    private Cipher createCipher(int mode, SecretKey key, byte[] iv)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(
                mode,
                key,
                new GCMParameterSpec(TAG_LENGTH, iv)
        );
        return cipher;
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    private void requireKey(SecretKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Encryption key is required.");
        }
    }
}