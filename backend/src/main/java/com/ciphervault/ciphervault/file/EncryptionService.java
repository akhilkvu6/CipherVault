package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.util.ConsoleLogger;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class EncryptionService {

    private static final String AES_ALGORITHM = "AES";
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";

    private static final int AES_KEY_SIZE = 256;
    private static final int AES_KEY_LENGTH_BYTES = AES_KEY_SIZE / 8;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /*
     * Temporary AES-256 key for the MCA mini project.
     *
     * This value contains exactly 32 ASCII characters
     * which equals 256 bits.
     *
     * For production deployment, this should be loaded
     * from an environment variable or secure key store.
     */
    private static final String SECRET_KEY =
            "CipherVaultAES256Key2026Secure!!";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService() {

        ConsoleLogger.info(
                "Initializing AES-256-GCM encryption service..."
        );

        this.secretKey = createSecretKey();
        this.secureRandom = new SecureRandom();

        ConsoleLogger.success(
                "AES-256-GCM encryption service initialized successfully."
        );
    }

    private SecretKey createSecretKey() {

        byte[] keyBytes = SECRET_KEY.getBytes(
                StandardCharsets.UTF_8
        );

        if (keyBytes.length != AES_KEY_LENGTH_BYTES) {

            ConsoleLogger.error(
                    "Invalid AES key length: "
                            + keyBytes.length
                            + " bytes."
            );

            throw new IllegalStateException(
                    "AES-256 secret key must be exactly 32 bytes."
            );
        }

        return new SecretKeySpec(
                keyBytes,
                AES_ALGORITHM
        );
    }

    public byte[] encrypt(byte[] data) {

        try {

            ConsoleLogger.info(
                    "Encrypting file using AES-256-GCM..."
            );

            byte[] iv = new byte[GCM_IV_LENGTH];

            secureRandom.nextBytes(iv);

            GCMParameterSpec gcmParameterSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            Cipher cipher =
                    Cipher.getInstance(GCM_ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    gcmParameterSpec
            );

            byte[] encryptedData =
                    cipher.doFinal(data);

            /*
             * Store the IV at the beginning of the
             * encrypted file.
             *
             * Format:
             *
             * [12-byte IV][ciphertext + authentication tag]
             */
            byte[] result =
                    new byte[
                            iv.length
                                    + encryptedData.length
                    ];

            System.arraycopy(
                    iv,
                    0,
                    result,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encryptedData,
                    0,
                    result,
                    iv.length,
                    encryptedData.length
            );

            ConsoleLogger.success(
                    "AES-256-GCM encryption completed."
            );

            return result;

        } catch (Exception e) {

            ConsoleLogger.error(
                    "File encryption failed: "
                            + e.getClass().getSimpleName()
            );

            throw new RuntimeException(
                    "File encryption failed.",
                    e
            );
        }
    }

    public byte[] decrypt(byte[] encryptedData) {

        try {

            ConsoleLogger.info(
                    "Decrypting file using AES-256-GCM..."
            );

            /*
             * The encrypted data must contain:
             *
             * 12-byte IV
             * +
             * ciphertext
             * +
             * 16-byte GCM authentication tag
             */
            if (encryptedData == null
                    || encryptedData.length
                    <= GCM_IV_LENGTH + 16) {

                throw new IllegalArgumentException(
                        "Invalid encrypted file data."
                );
            }

            byte[] iv =
                    Arrays.copyOfRange(
                            encryptedData,
                            0,
                            GCM_IV_LENGTH
                    );

            byte[] cipherText =
                    Arrays.copyOfRange(
                            encryptedData,
                            GCM_IV_LENGTH,
                            encryptedData.length
                    );

            GCMParameterSpec gcmParameterSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            Cipher cipher =
                    Cipher.getInstance(GCM_ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    gcmParameterSpec
            );

            byte[] decryptedData =
                    cipher.doFinal(cipherText);

            ConsoleLogger.success(
                    "AES-256-GCM decryption completed."
            );

            return decryptedData;

        } catch (Exception e) {

            ConsoleLogger.error(
                    "File decryption failed: "
                            + e.getClass().getSimpleName()
            );

            throw new RuntimeException(
                    "File decryption failed.",
                    e
            );
        }
    }
}
