package com.ciphervault.ciphervault.security;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

@Service
public class KeyManagementService {

    private static final Logger log =
            LoggerFactory.getLogger(KeyManagementService.class);

    private static final String AES_ALGORITHM = "AES";
    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8;
    private static final int AES_KEY_BYTES = 32;

    public static final String DEFAULT_DEV_MASTER_KEY =
            "CipherVaultMasterPassphraseKEK2026!";

    public static final String DEFAULT_DEV_SALT =
            "CipherVaultSecureSalt2026DefaultSalt!";

    @Value("${ciphervault.security.master-key:}")
    private String masterPassphrase;

    @Value("${ciphervault.security.pbkdf2.salt:}")
    private String pbkdf2Salt;

    @Value("${ciphervault.security.pbkdf2.iterations:65536}")
    private int pbkdf2Iterations;

    @Value("${ciphervault.environment:development}")
    private String environment;

    @Value("${ciphervault.security.dev-defaults-enabled:true}")
    private boolean devDefaultsEnabled;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom;

    private SecretKey serverKek;

    public KeyManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.secureRandom = new SecureRandom();
    }

    @PostConstruct
    public void init() {
        try {
            boolean productionMode =
                    "production".equalsIgnoreCase(environment)
                            || !devDefaultsEnabled;

            if (masterPassphrase == null
                    || masterPassphrase.trim().isEmpty()
                    || (productionMode
                    && DEFAULT_DEV_MASTER_KEY.equals(masterPassphrase))) {

                if (productionMode) {
                    throw new IllegalStateException(
                            "CRITICAL SECURITY ERROR: "
                                    + "CIPHERVAULT_MASTER_KEY environment variable "
                                    + "is required in production and cannot use "
                                    + "development default."
                    );
                }

                log.warn(
                        "SECURITY WARNING: CIPHERVAULT_MASTER_KEY is unset. "
                                + "Running with development master passphrase fallback."
                );

                masterPassphrase = DEFAULT_DEV_MASTER_KEY;
            }

            if (pbkdf2Salt == null
                    || pbkdf2Salt.trim().isEmpty()
                    || (productionMode
                    && DEFAULT_DEV_SALT.equals(pbkdf2Salt))) {

                if (productionMode) {
                    throw new IllegalStateException(
                            "CRITICAL SECURITY ERROR: "
                                    + "CIPHERVAULT_PBKDF2_SALT environment variable "
                                    + "is required in production and cannot use "
                                    + "development default."
                    );
                }

                log.warn(
                        "SECURITY WARNING: CIPHERVAULT_PBKDF2_SALT is unset. "
                                + "Running with development PBKDF2 salt fallback."
                );

                pbkdf2Salt = DEFAULT_DEV_SALT;
            }

            log.debug(
                    "Deriving Server Key Encryption Key (KEK) using "
                            + "PBKDF2 (HMAC-SHA256, {} iterations)...",
                    pbkdf2Iterations
            );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            char[] password = masterPassphrase.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(
                    password,
                    pbkdf2Salt.getBytes(StandardCharsets.UTF_8),
                    pbkdf2Iterations,
                    256
            );

            try {
                byte[] kekBytes =
                        factory.generateSecret(spec).getEncoded();

                try {
                    this.serverKek =
                            new SecretKeySpec(kekBytes, AES_ALGORITHM);
                } finally {
                    Arrays.fill(kekBytes, (byte) 0);
                }
            } finally {
                spec.clearPassword();
                Arrays.fill(password, '\0');
            }

            log.debug("Server KEK derived successfully (AES-256).");

        } catch (Exception e) {
            log.error(
                    "Failed to derive Server KEK: {}",
                    e.getMessage(),
                    e
            );

            throw new IllegalStateException(
                    "Failed to initialize cryptographic KeyManagementService",
                    e
            );
        }
    }

    /**
     * Generates a new cryptographically random 256-bit AES user data
     * encryption key, encrypts it with the server KEK using AES-256-GCM,
     * and returns its Base64 representation.
     */
    public String generateAndEncryptUserKey() {
        byte[] rawKey = new byte[AES_KEY_BYTES];

        secureRandom.nextBytes(rawKey);

        try {
            return encryptKeyWithKek(rawKey);
        } finally {
            Arrays.fill(rawKey, (byte) 0);
        }
    }

    /**
     * Encrypts a 32-byte AES-256 key with the server KEK using AES-256-GCM.
     */
    public String encryptKeyWithKek(byte[] rawKeyBytes) {
        if (rawKeyBytes == null || rawKeyBytes.length != AES_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "Key must be exactly 32 bytes for AES-256."
            );
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(GCM_TRANSFORMATION);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    serverKek,
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH_BITS,
                            iv
                    )
            );

            byte[] ciphertext = cipher.doFinal(rawKeyBytes);

            byte[] envelope =
                    new byte[GCM_IV_LENGTH + ciphertext.length];

            System.arraycopy(
                    iv,
                    0,
                    envelope,
                    0,
                    GCM_IV_LENGTH
            );

            System.arraycopy(
                    ciphertext,
                    0,
                    envelope,
                    GCM_IV_LENGTH,
                    ciphertext.length
            );

            return Base64.getEncoder().encodeToString(envelope);

        } catch (Exception e) {
            log.error(
                    "Failed to encrypt key with Server KEK: {}",
                    e.getMessage()
            );

            throw new RuntimeException(
                    "Key envelope encryption failed",
                    e
            );
        }
    }

    /**
     * Decrypts a Base64-encoded AES-256-GCM key envelope using the server KEK.
     */
    public SecretKey decryptKeyWithKek(String encryptedBase64) {
        try {
            if (encryptedBase64 == null || encryptedBase64.isBlank()) {
                throw new IllegalArgumentException(
                        "Encrypted key envelope cannot be empty."
                );
            }

            byte[] envelope =
                    Base64.getDecoder().decode(encryptedBase64);

            int expectedEnvelopeLength =
                    GCM_IV_LENGTH
                            + AES_KEY_BYTES
                            + GCM_TAG_LENGTH_BYTES;

            if (envelope.length != expectedEnvelopeLength) {
                throw new IllegalArgumentException(
                        "Invalid key envelope length."
                );
            }

            byte[] iv =
                    Arrays.copyOfRange(
                            envelope,
                            0,
                            GCM_IV_LENGTH
                    );

            byte[] ciphertext =
                    Arrays.copyOfRange(
                            envelope,
                            GCM_IV_LENGTH,
                            envelope.length
                    );

            Cipher cipher =
                    Cipher.getInstance(GCM_TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    serverKek,
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH_BITS,
                            iv
                    )
            );

            byte[] rawKey = cipher.doFinal(ciphertext);

            if (rawKey.length != AES_KEY_BYTES) {
                Arrays.fill(rawKey, (byte) 0);

                throw new IllegalArgumentException(
                        "Decrypted key is not a valid AES-256 key."
                );
            }

            try {
                return new SecretKeySpec(
                        rawKey,
                        AES_ALGORITHM
                );
            } finally {
                Arrays.fill(rawKey, (byte) 0);
            }

        } catch (Exception e) {
            log.error(
                    "Failed to decrypt user key envelope: {}",
                    e.getMessage()
            );

            throw new RuntimeException(
                    "Key envelope decryption failed",
                    e
            );
        }
    }

    /**
     * Returns the user's existing AES-256 key or creates and persists
     * a new encrypted key for accounts that do not have one.
     */
    public SecretKey getOrGenerateUserKey(User user) {
        if (user.getUserKey() != null
                && !user.getUserKey().trim().isEmpty()) {

            return decryptKeyWithKek(user.getUserKey());
        }

        String encryptedKey =
                generateAndEncryptUserKey();

        user.setUserKey(encryptedKey);
        userRepository.save(user);

        return decryptKeyWithKek(encryptedKey);
    }
}