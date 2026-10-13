# CipherVault — Implementation Findings & Verified Capabilities

This document records the exact, verified technical capabilities implemented in the CipherVault codebase. Every finding is supported by concrete source code references, database constraints, automated test suites, or live emulator runtime evidence.

---

## 1. Summary of Implemented vs. Historical Claims

| Functional Domain | First-Review Presentation Claim | Current Verified Implementation | Status in Final Presentation |
| :--- | :--- | :--- | :---: |
| **Download Decryption** | *"Secure decryption and original-file download are planned as a future enhancement."* (Slide 4, 15) | **Fully Implemented**. Backend provides streaming AES-256-GCM decryption via `CipherInputStream` when `decrypt=true` (default). Android UI features a toggle switch: *"Decrypt on download"*. | **Implemented & Verified** |
| **ExifTool Metadata** | *"ExifTool integration (planned)"* (Slide 5, 6, 9) | **Fully Implemented**. `MetadataExtractionService` executes ExifTool on uploaded files, extracting camera optics, resolution, and codecs into `file_metadata`. | **Implemented & Verified** |
| **Metadata Search** | *"Complete metadata search is planned/future."* (Slide 8, 18) | **Fully Implemented**. `FileSearchService` supports keyword, category, date range, size range, and dynamic facet filters (`/api/files/search` and `/api/files/suggestions`). | **Implemented & Verified** |
| **Thumbnail Previews** | *"Thumbnail generation (future enhancement)"* (Slide 16, 18) | **Fully Implemented**. `MediaPreviewService` generates JPEG previews for images and media, stored in `storage/previews/` and served via `/api/files/{id}/preview`. | **Implemented & Verified** |
| **Database Schema** | Showed tables `users`, `files`, `file_metadata`, `activity_log`. | Verified schema uses `users`, `stored_files`, `file_metadata`, `activity_events`, and `transfers`. | **Corrected to Authoritative** |
| **File Deletion** | *"File deletion is currently planned/future."* (Slide 8, 18) | **Fully Implemented**. `DELETE /api/files/{id}` cascades to metadata and previews, decrements user storage quota atomically, and unlinks disk files. | **Implemented & Verified** |
| **Deduplication** | Described as SHA-256 duplicate detection. | Implemented as **Per-User SHA-256** enforced via database composite constraint `UNIQUE (user_id, sha256_hash)`. | **Verified & Clarified** |
| **Storage Quotas** | Described as storage quota monitoring. | Enforced at **10 GB** user vault limit and **1 GB** single upload/batch limit on both Android client and Spring Boot server. | **Verified & Benchmarked** |

---

## 2. Detailed Capability Verification

### 2.1 Authentication & Session Management
* **Actual Code Implementation:**
  - Registration: `POST /api/auth/register` creates account, validates password complexity, and hashes password using `BCryptPasswordEncoder` (cost factor 10).
  - Login: `POST /api/auth/login` verifies credentials, enforces sliding-window brute-force rate limiting (5 attempts = 15 min lockout), and issues a stateless 24-hour JWT signed with HMAC-SHA256.
  - Session Revocation: Incrementing `users.token_version` invalidates all previously issued JWTs.
  - Android Client: JWT tokens are stored in hardware-backed `EncryptedSharedPreferences` via MasterKey. Logout completely clears active and legacy preferences, synchronizing via multi-target `.commit()`.
  - AppLock: Cold-start and background-resume challenge via `BiometricPrompt` and cryptographic PIN overlay.
* **File References:**
  - Backend: [`AuthController.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/auth/AuthController.java), [`JwtService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/security/JwtService.java), [`LoginRateLimiterService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/security/LoginRateLimiterService.java).
  - Android: [`SessionManager.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/SessionManager.java), [`AppLockActivity.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/AppLockActivity.java).
* **Claims for Presentation:** "Stateless JWT authentication with BCrypt hashing, brute-force rate limiting, and hardware-backed biometric AppLock."
* **Limitations:** Does not yet support multi-factor authentication (MFA/TOTP).

---

### 2.2 Client-Side Pre-Upload Staging & Quota Enforcement
* **Actual Code Implementation:**
  - Android client allows multi-file selection from system picker or camera.
  - Displays pre-upload queue in `UploadStagingActivity` with batch byte calculation.
  - Quota Gauge: Visual progress bar enforces **1 GB** batch limit and **10 GB** total vault ceiling.
  - Action Controls: Each staged file has dedicated 40dp touch targets for full-screen preview and queue deletion prior to upload.
* **File References:**
  - Android: [`UploadStagingActivity.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/UploadStagingActivity.java), [`StagedFileAdapter.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/StagedFileAdapter.java).
* **Claims for Presentation:** "Pre-upload staging queue providing modal preview inspection, individual item removal, and strict 1 GB batch quota validation before network transmission."

---

### 2.3 Streaming Upload, SHA-256 Hashing & Deduplication
* **Actual Code Implementation:**
  - Pipeline Sequence:
    1. Stream multipart payload to temporary file.
    2. Validate file size ($\le 1\text{ GB}$) and check user available quota ($\text{used} + \text{size} \le 10\text{ GB}$).
    3. Calculate SHA-256 hash across plaintext bytes using streaming digest.
    4. Query database for duplicate: `fileRepository.findByUserAndSha256Hash(user, sha256)`. If found, reject immediately with `409 Conflict`.
    5. Generate thumbnail preview via `MediaPreviewService` before encryption.
    6. Extract forensic metadata via ExifTool before encryption.
    7. Encrypt stream using AES-256-GCM with user's UDEK into `storage/encrypted/`.
    8. Persist `StoredFile` and `FileMetadata` records; increment user's storage quota atomically.
    9. Log `ActivityEvent` and `Transfer` records.
* **File References:**
  - Backend: [`FileStorageService.java#L100-L460`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java).
* **Claims for Presentation:** "Constant-memory streaming ingestion with atomic per-user SHA-256 deduplication and AES-256-GCM envelope encryption."

---

### 2.4 Streaming Download & Decryption
* **Actual Code Implementation:**
  - Route: `GET /api/files/{id}/download?decrypt={true|false}`
  - Authentication & Authorization: Validates JWT and verifies that `storedFile.user.id == authenticatedUser.id`.
  - Decrypted Path (`decrypt=true`):
    - Retrieves user's UDEK unwrapped by Server KEK.
    - Reads 12-byte IV from file header.
    - Pipes encrypted blob through `CipherInputStream` initialized with AES-256-GCM.
    - Emits `StreamingResponseBody` with original filename, content type, and `X-File-SHA256` header.
    - Throws `AEADBadTagException` if any ciphertext or tag byte was corrupted.
  - Raw Path (`decrypt=false`): Streams ciphertext blob with `.enc` extension as an encrypted artifact.
* **File References:**
  - Backend: [`FileController.java#L684-L745`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileController.java), [`FileStorageService.java#L600-L700`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java).
* **Claims for Presentation:** "Dual-mode streaming download supporting authenticated on-the-fly decryption or raw encrypted blob export."

---

### 2.5 Forensic Metadata Extraction & Faceted Search
* **Actual Code Implementation:**
  - Extraction: Ingestion invokes `MetadataExtractionService`, extracting 29 standard forensic attributes via ExifTool.
  - Search Engine: `FileSearchService` executes Spring Data JPA Criteria queries supporting:
    - Text query matching filename, camera make/model, author, artist, title.
    - Category filtering (Photos, Videos, Documents, Audio, Other).
    - Dynamic facet suggestions: `/api/files/suggestions` extracts distinct camera models, resolutions, and codecs from user's vault.
* **File References:**
  - Backend: [`MetadataExtractionService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/MetadataExtractionService.java), [`FileSearchService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileSearchService.java).
* **Claims for Presentation:** "Decoupled forensic metadata architecture enabling multi-attribute search and dynamic suggestion chips over encrypted vault storage."

---

### 2.6 Cascading File Deletion & Disk Reclamation
* **Actual Code Implementation:**
  - Route: `DELETE /api/files/{id}`
  - Verifies file ownership.
  - Database Deletion: `fileRepository.delete(storedFile)` automatically cascades to `file_metadata` via foreign key `ON DELETE CASCADE`.
  - Quota Update: Decrements `users.used_storage` atomically.
  - Physical Cleanup: Unlinks physical ciphertext blob from `storage/encrypted/` and preview image from `storage/previews/`.
  - Auditing: Records `ActivityEvent` with event type `DELETE`.
* **File References:**
  - Backend: [`FileController.java#L800-L840`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileController.java).
* **Claims for Presentation:** "Atomic cascading deletion guaranteeing database-filesystem consistency and immediate quota reclamation."
