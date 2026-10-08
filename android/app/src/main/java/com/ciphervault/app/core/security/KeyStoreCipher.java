package com.ciphervault.app.core.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Android Keystore-backed AES-256-GCM encryption, with secure hardware backing when supported by the device.
 * Stores non-exportable AES-256 key inside AndroidKeyStore with fresh random IV per encryption.
 * Software JCE fallback is strictly isolated to host-side JVM unit test environments.
 */
public class KeyStoreCipher {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ciphervault_session_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int KEY_SIZE_BITS = 256;

    private SecretKey jvmFallbackKey = null;

    public KeyStoreCipher() {
    }

    private synchronized SecretKey getOrCreateKey() throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
            );

            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .setRandomizedEncryptionRequired(true)
                    .build();

            keyGenerator.init(spec);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            // Software fallback is strictly isolated to host-side JVM unit testing
            // where the AndroidKeyStore provider is absent. It MUST NOT execute on Android runtime.
            if (isAndroidRuntime()) {
                throw new java.security.KeyStoreException("AndroidKeyStore operation failed on Android runtime", e);
            }
            if (jvmFallbackKey == null) {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE_BITS, new SecureRandom());
                jvmFallbackKey = keyGen.generateKey();
            }
            return jvmFallbackKey;
        }
    }

    private static boolean isAndroidRuntime() {
        String vmName = System.getProperty("java.vm.name");
        return vmName != null && "Dalvik".equalsIgnoreCase(vmName);
    }

    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null) {
            return null;
        }

        SecretKey secretKey = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.allocate(1 + iv.length + ciphertext.length);
        buffer.put((byte) iv.length);
        buffer.put(iv);
        buffer.put(ciphertext);

        return encodeBase64(buffer.array());
    }

    public String decrypt(String encryptedPayload) throws Exception {
        if (encryptedPayload == null || encryptedPayload.trim().isEmpty()) {
            return null;
        }

        byte[] raw = decodeBase64(encryptedPayload);
        if (raw == null || raw.length < 2) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(raw);
        int ivLength = buffer.get() & 0xFF;
        if (ivLength <= 0 || buffer.remaining() < ivLength) {
            return null;
        }

        byte[] iv = new byte[ivLength];
        buffer.get(iv);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        SecretKey secretKey = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] plaintextBytes = cipher.doFinal(ciphertext);
        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }

    private String encodeBase64(byte[] data) {
        try {
            return Base64.encodeToString(data, Base64.NO_WRAP);
        } catch (Throwable t) {
            return java.util.Base64.getEncoder().encodeToString(data);
        }
    }

    private byte[] decodeBase64(String str) {
        try {
            return Base64.decode(str, Base64.NO_WRAP);
        } catch (Throwable t) {
            return java.util.Base64.getDecoder().decode(str);
        }
    }
}
