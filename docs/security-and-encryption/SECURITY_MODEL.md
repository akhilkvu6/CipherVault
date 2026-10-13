# CipherVault Security Architecture & Threat Model

## 1. Cryptographic Foundations

CipherVault rejects weak or outdated cryptographic primitives (such as DES, RC4, or ECB/CBC mode without authenticated MACs) and implements NIST-standard authenticated encryption:

| Layer | Standard / Algorithm | Parameters | Key Length |
|---|---|---|---|
| **Data Encryption** | AES-GCM (Galois/Counter Mode) | 12-byte IV, 128-bit authentication tag | 256 bits |
| **Server KEK Derivation** | PBKDF2-HMAC-SHA256 | 65,536 iterations, 16-byte salt | 256 bits |
| **User Data Key Wrapping** | AES-256-GCM Envelope | Random 12-byte IV per wrap | 256 bits |
| **Password Hashing** | BCrypt | Work factor 10 | 60 chars |
| **Session Integrity** | JWT (JSON Web Token) | HMAC-SHA256 (HS256) | 256 bits |
| **Client Preference Store** | Android Keystore / MasterKey | AES256_GCM (values) / AES256_SIV (keys) | 256 bits |

---

## 2. Envelope Encryption Lifecycle & Secret Management

1. **Production Secret Enforcement**:
   - `KeyManagementService` and `JwtService` enforce strict production guards.
   - When running in production mode (`CIPHERVAULT_ENV=production` or `CIPHERVAULT_DEV_DEFAULTS_ENABLED=false`), if any server secret (`CIPHERVAULT_MASTER_KEY`, `CIPHERVAULT_PBKDF2_SALT`, `CIPHERVAULT_JWT_SECRET`) is missing or set to a development placeholder, the backend halts immediately with a fatal `IllegalStateException`.
   - Hardcoded default keys in cryptographic operations are strictly eliminated.

2. **User Data Encryption Key (UDEK)**:
   - Generated per-user using `KeyGenerator.getInstance("AES").generateKey()` (256-bit).
   - Encrypted in-memory under the Server KEK via AES-256-GCM and persisted in `users.user_key`.
   - Never stored in plaintext on disk, memory dumps, or log output.

3. **File Ingestion**:
   - Unique 12-byte initialization vector (IV) generated via `SecureRandom` per upload.
   - Streaming encryption via `CipherOutputStream` with 16 KB chunks.
   - Output format on disk:
     ```
     [ 12 Bytes: Nonce/IV ] + [ N Bytes: AES-256-GCM Ciphertext ] + [ 16 Bytes: Auth Tag ]
     ```

4. **Tamper Detection (Integrity Verification)**:
   - AES-GCM calculates an algebraic GHASH tag over ciphertext and authenticated additional data.
   - Any single-bit flip or truncation on disk throws `AEADBadTagException` during download, instantly aborting decryption before corrupt bytes can reach the client.

---

## 3. Authentication & Account Hardening

1. **Secure Password Change**:
   - Requires verification of `currentPassword` against BCrypt password hash.
   - Requires `newPassword` of at least 6 characters.
   - Requires confirmation match (`newPassword == confirmPassword`).
   - Prevents reuse of the existing password.

2. **Login Brute-Force Rate Limiting**:
   - `LoginRateLimiterService` tracks failed authentication attempts per IP/username.
   - 5 consecutive failed attempts trigger an automatic 15-minute lockout (`HTTP 429 Too Many Requests`).

3. **Atomic Storage Quota Accounting**:
   - `incrementStorageUsedAtomic` and `decrementStorageUsedAtomic` execute directly in MySQL (`UPDATE users SET used_storage = used_storage + :size WHERE id = :id AND used_storage + :size <= storage_limit`).
   - Prevents race conditions and storage exhaustion attacks.

---

## 4. Android Client Security

1. **Hardware-Backed Credential Storage**:
   - Session tokens, usernames, and email addresses are persisted via `androidx.security.crypto.EncryptedSharedPreferences`.
   - Master encryption key is generated and managed within the Android Hardware Keystore (`MasterKey.KeyScheme.AES256_GCM`).
   - Plaintext keys or tokens are NEVER written to unencrypted `SharedPreferences` or XML files on Android.

2. **Network Security Configuration**:
   - Production (`main/res/xml/network_security_config.xml`): Strictly requires TLS/HTTPS with system trust anchors. No hardcoded LAN IPs. Cleartext is strictly forbidden.
   - Debug (`debug/res/xml/network_security_config.xml`): Permitted only in debug build variant for development and ADB loopback (`cleartextTrafficPermitted="true"`).

3. **Application Hardening**:
   - `android:allowBackup="false"` in `AndroidManifest.xml` prevents extracting sensitive vault cache via ADB backup.
   - Fragment show/hide caching prevents redundant memory allocation and protects in-memory state.
   - Managed background `ExecutorService` in `FileUtils` prevents thread exhaustion during large file I/O.
