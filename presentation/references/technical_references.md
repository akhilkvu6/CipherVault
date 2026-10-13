# CipherVault — Technical References & Cryptographic Standards

This document establishes the verified technical specifications, cryptographic standards, security RFCs, and framework documentation governing the architecture and implementation of CipherVault.

---

## 1. Cryptographic and Security Standards

### 1. NIST SP 800-38D — Galois/Counter Mode (AES-GCM)
* **Citation:** Dworkin, M. (2007). *Recommendation for Block Cipher Modes of Operation: Galois/Counter Mode (GCM) and GMAC.* NIST Special Publication 800-38D, National Institute of Standards and Technology, U.S. Department of Commerce.
* **Standard Specification:** Defines Authenticated Encryption with Associated Data (AEAD) combining counter-mode encryption with Galois field $GF(2^{128})$ authentication.
* **CipherVault Application:** Used for all stored file encryption and key wrapping:
  - Cipher: `AES/GCM/NoPadding`
  - Key Length: 256 bits (32 bytes)
  - Nonce / IV: 96 bits (12 bytes), generated via `SecureRandom` per file operation
  - Authentication Tag: 128 bits (16 bytes) GHASH tag appended to ciphertext
  - Tamper Detection: Automatically throws `javax.crypto.AEADBadTagException` if even a single ciphertext or tag byte is modified.
* **Source Reference:** [`EncryptionService.java#L25-L60`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/EncryptionService.java).

### 2. FIPS PUB 180-4 — Secure Hash Standard (SHA-256)
* **Citation:** National Institute of Standards and Technology (2015). *Secure Hash Standard (SHS).* Federal Information Processing Standards Publication (FIPS PUB 180-4), National Institute of Standards and Technology, Gaithersburg, MD.
* **Standard Specification:** Defines cryptographic hash functions producing a fixed 256-bit digest with collision resistance ($2^{128}$ operations).
* **CipherVault Application:**
  - File integrity calculation and verification prior to encryption.
  - Per-user duplicate file detection: enforces database unique constraint `uq_stored_files_user_sha256 (user_id, sha256_hash)`.
* **Source Reference:** [`FileStorageService.java#L208-L230`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java).

### 3. RFC 2898 / RFC 8018 — Password-Based Cryptography (PBKDF2)
* **Citation:** Kaliski, B. (2000). *PKCS #5: Password-Based Cryptography Specification Version 2.0.* RFC 2898 / RFC 8018, Internet Engineering Task Force (IETF).
* **Standard Specification:** Pseudorandom function iteration mechanism for deriving cryptographic keys from master passphrases and cryptographic salts.
* **CipherVault Application:**
  - Algorithm: `PBKDF2WithHmacSHA256`
  - Iterations: 65,536 rounds
  - Derived Key Length: 256 bits
  - Operational Scope: Derives the Server Key Encryption Key (KEK) used to wrap individual User Data Encryption Keys (UDEKs).
* **Source Reference:** [`KeyManagementService.java#L125-L149`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/security/KeyManagementService.java).

### 4. RFC 7519 — JSON Web Token (JWT)
* **Citation:** Jones, M., Bradley, J., & Sakimura, N. (2015). *JSON Web Token (JWT).* RFC 7519, Standards Track, Internet Engineering Task Force (IETF).
* **Standard Specification:** Compact, URL-safe means of representing claims to be transferred between two parties, digitally signed using HMAC or public/private key pairs.
* **CipherVault Application:**
  - Signature Algorithm: `HMAC-SHA256` (HS256) with a 256-bit secret.
  - Claims Encoded: `sub` (user email), `userId`, `username`, `tokenVersion`, `iat` (issued-at), `exp` (expiration, 24-hour lifetime).
  - Revocation Mechanism: Token version matching against database `users.token_version`. Invalidated immediately upon logout or password reset.
* **Source Reference:** [`JwtService.java#L30-L90`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/security/JwtService.java), [`JwtAuthenticationFilter.java#L45-L95`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/security/JwtAuthenticationFilter.java).

### 5. Provos & Mazières (1999) — BCrypt Password Hashing
* **Citation:** Provos, N., & Mazières, D. (1999). *A Future-Adaptable Password Scheme.* Proceedings of the FREENIX Track: 1999 USENIX Annual Technical Conference, Monterey, CA.
* **Standard Specification:** Adaptive password hashing function based on the Blowfish cipher with configurable cost factor and embedded 128-bit salt.
* **CipherVault Application:**
  - Implementation: `BCryptPasswordEncoder`
  - Cost Factor: 10 (1,024 key expansion rounds)
  - Scope: User account passwords stored in `users.password`.
* **Source Reference:** [`PasswordConfig.java#L15-L25`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/config/PasswordConfig.java).

---

## 2. Platform, Framework, and Tool Specifications

### 1. Google Android Jetpack & Material Design 3
* **Citation:** Google LLC (2025). *Android Jetpack Architecture & Material Design 3 Guidelines.* Google Open Source.
* **CipherVault Application:**
  - Client Language: Java 17 / XML Views.
  - UI Components: Material 3 Cards (`ShapeAppearance.Corner.Large`, 16dp radii), BottomSheetDialogs, FloatingActionButtons, dynamic Monet wallpaper extraction with brand fallback.
  - Hardware Security: Android Keystore StrongBox/TEE, `EncryptedSharedPreferences` (AES-256-SIV key + AES-256-GCM value encryption), `BiometricPrompt` authentication.
  - Direct Action Accessibility: Two-column grid with dedicated 40dp action touch targets (`[Download]`, `[Delete]`).

### 2. Spring Boot 3.3.4 & Spring Security 6.3
* **Citation:** VMware Tanzu / Broadcom (2024). *Spring Boot Reference Documentation (v3.3.4) & Spring Security 6.3 Architecture.* Pivotal Software, Inc.
* **CipherVault Application:**
  - Runtime: Java 21 LTS with Virtual Threads.
  - Security Pipeline: Stateless `SecurityFilterChain` with `SessionCreationPolicy.STATELESS`.
  - Multipart Streaming Limits: Maximum single file limit of 1 GB (`1,073,741,824 bytes`) and 10 GB total quota enforcement.
  - Streaming I/O: Constant-memory 16 KB buffer piped directly through `CipherOutputStream` and `CipherInputStream`.

### 3. Oracle MySQL 8.0 Community Server
* **Citation:** Oracle Corporation (2024). *MySQL 8.0 Reference Manual: InnoDB Storage Engine Architecture and B-Tree Index Optimization.* Oracle Corporation.
* **CipherVault Application:**
  - Storage Engine: InnoDB with `utf8mb4` character set and `utf8mb4_unicode_ci` collation.
  - Relational Integrity: Enforces foreign key constraints with `ON DELETE CASCADE` across `stored_files`, `file_metadata`, `activity_events`, and `transfers`.
  - Cryptographic Deduplication: Composite index `uq_stored_files_user_sha256` on `(user_id, sha256_hash)`.

### 4. ExifTool by Phil Harvey
* **Citation:** Harvey, P. (2025). *ExifTool by Phil Harvey: Image-ExifTool Platform-Independent Command-Line Application and Perl Library.*
* **CipherVault Application:**
  - Forensic Extraction: Reads camera make, camera model, lens optics, focal length, ISO, exposure time, aperture (f-number), image resolution, audio/video codecs (H.264, AAC, ProRes), frame rate, and embedded document author metadata during the pre-encryption upload stage.
  - Storage: Populates the dedicated relational table `file_metadata`.
* **Source Reference:** [`MetadataExtractionService.java#L40-L120`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/MetadataExtractionService.java).
