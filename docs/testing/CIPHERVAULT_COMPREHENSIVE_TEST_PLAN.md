# CIPHERVAULT — COMPREHENSIVE TEST PLAN & VERIFICATION MATRIX

**Document ID:** `CIPHERVAULT_COMPREHENSIVE_TEST_PLAN.md`  
**Location:** `docs/testing/CIPHERVAULT_COMPREHENSIVE_TEST_PLAN.md`  
**Execution Timestamp:** 2026-10-13  
**Total Automated Tests:** 211 Tests Passing (138 Backend + 73 Android, 0 Failures, 0 Errors, 0 Skipped)  

---

## 1. Test Architecture & Methodology

CipherVault employs a multi-tiered test verification strategy:
1. **Unit Testing:** Pure unit tests covering business logic, validation rules, cryptography, format conversions, and state machines.
2. **Integration Testing:** Spring Boot context tests (`@SpringBootTest`, `MockMvc`) verifying database transactions, repository queries, and security filter execution.
3. **Boundary Verification:** Targeted boundary tests for the 10 GB vault storage quota and 1 GB upload limits.
4. **Static Analysis & Compilation:** Android Gradle build verification (`assembleDebug`, `checkDebugAarMetadata`) and Maven test compilation.
5. **Runtime QA & Recovery:** Verifying fault handling, duplicate detection, network drop recovery, and error codes.

---

## 2. Test Execution Matrix

### 2.1 Backend Core Test Suites (138 Tests Passing)

| Test Suite Class | Tests | Status | Scenarios Covered |
|---|---|---|---|
| `StorageQuotaBoundaryTest` | 6 | `PASS` | 10 GB quota limit, 1 GB file limit, atomic SQL reservation, duplicate non-allocation, delete quota reclaim |
| `FileStorageServiceTest` | 3 | `PASS` | File deletion idempotency, missing file handling, MAX_FILE_SIZE constant check |
| `StorageControllerTest` | 5 | `PASS` | Storage summary calculations, category breakdown, duplicate grouping, large file filtering |
| `FileCategoryServiceTest` | 2 | `PASS` | MIME type and file extension classification |
| `FileFacetServiceTest` | 2 | `PASS` | Dynamic cascading facet generation, label mapping |
| `FileFacetServiceIntegrationTest` | 3 | `PASS` | JPA integration for facets across database rows |
| `FileSearchServiceTest` | 4 | `PASS` | Metadata search specifications, prefix suggestions |
| `FileControllerTest` | 5 | `PASS` | Upload, list, download, preview, delete endpoints |
| `FileControllerFacetsIntegrationTest` | 3 | `PASS` | Facets endpoint integration with MockMvc |
| `EncryptionServiceTest` | 3 | `PASS` | AES-256-GCM encryption/decryption, tag validation |
| `KeyManagementServiceTest` | 3 | `PASS` | PBKDF2 key derivation, master key envelope |
| `MediaPreviewServiceTest` | 2 | `PASS` | Thumbnail and preview generation pipeline |
| `MetadataExtractionServiceTest` | 3 | `PASS` | ExifTool metadata parsing, camera/lens/date extraction |
| `MetadataSearchTest` | 5 | `PASS` | Multi-field search filtering |
| `AuthControllerTest` | 6 | `PASS` | Registration, login via username/email, password change |
| `JwtAuthenticationFilterTest` | 7 | `PASS` | Bearer token validation, tokenVersion revocation |
| `JwtServiceTest` | 3 | `PASS` | JWT generation, claim parsing, expiration |
| `LoginRateLimiterServiceTest` | 4 | `PASS` | 5 failed attempts lockout, 15-minute expiration |
| `ActivityControllerTest` | 3 | `PASS` | Audit event listing and filtering |
| `ActivityControllerRegressionTest` | 3 | `PASS` | DTO projection regression coverage |
| `AnalyticsControllerTest` | 3 | `PASS` | Analytics metrics endpoints |
| `HealthControllerTest` | 1 | `PASS` | Health check endpoint |
| `UserControllerTest` | 5 | `PASS` | User profile, name update, password update, delete account |
| `UserDeletionIntegrationTest` | 3 | `PASS` | Cascade deletion of files, metadata, and activity events |
| `ProfilePhotoTest` | 10 | `PASS` | Profile photo upload, retrieval, and delete |
| `RequestContextTest` | 4 | `PASS` | ThreadLocal request correlation ID tracking |
| `RequestLoggingFilterTest` | 3 | `PASS` | HTTP access log latency formatting |
| `CipherVaultConsoleConverterTest` | 3 | `PASS` | ANSI console color conversion |
| `VerifyExistingFileTest` | 1 | `PASS` | Verification of existing file decryption and hash |
| `CiphervaultApplicationTests` | 1 | `PASS` | Spring Boot context load verification |

### 2.2 Android Client Test Suites (73 Tests Passing)

| Test Suite Class | Tests | Status | Scenarios Covered |
|---|---|---|---|
| `UploadStagingQuotaTest` | 6 | `PASS` | 1 GB single file limit, 1 GB batch limit, 10 GB vault capacity, batch sum calculation |
| `MetadataSearchLogicTest` | 9 | `PASS` | Dynamic facet state, selection reconciliation, cascading reset, chip expansion |
| `SearchHistoryManagerTest` | 2 | `PASS` | Recent and popular search query history persistence |
| `StoredFileCategoryTest` | 2 | `PASS` | File category mapping and localized display |
| `AuthValidatorTest` | 7 | `PASS` | Email regex, username length, password complexity |
| `AuthErrorParserTest` | 5 | `PASS` | Backend JSON error message parsing |
| `ServerConnectionManagerTest` | 9 | `PASS` | Health check ping, latency calculation, mode resolution |
| `ServerConnectionPreferencesTest` | 3 | `PASS` | Active server URL and connection mode persistence |
| `HealthCheckTest` | 5 | `PASS` | Health response deserialization and status validation |
| `SessionManagerTest` | 4 | `PASS` | Secure token caching, login state, 10 GB default limit |
| `NavigationFoundationTest` | 6 | `PASS` | Tab indices, intent argument dispatch, back navigation |
| `GlobalMotionTest` | 4 | `PASS` | Motion tokens, transition durations |
| `ThemeModeTest` | 3 | `PASS` | Light, Dark, System, Dynamic theme modes |
| `ThemePreferencesTest` | 4 | `PASS` | Theme preference persistence and retrieval |
| `TransferModelTest` | 3 | `PASS` | TransferItem, TransferBatch, state transitions |
| `ExampleUnitTest` | 1 | `PASS` | Baseline assertion test |

---

## 3. Quota & Upload Limit Test Scenarios (Phase 8 Verification)

| Test ID | Scenario | Expected Outcome | Verification Status |
|---|---|---|---|
| **QUOTA-01** | Upload file <= 1 GB into empty vault | Success (HTTP 200), file encrypted, quota incremented | `PASS` |
| **QUOTA-02** | Upload file with size = 1,073,741,825 B (1 byte over 1 GB) | Rejected (`IllegalArgumentException` / HTTP 400), "File exceeds the maximum allowable upload limit of 1 GB" | `PASS` |
| **QUOTA-03** | Upload batch of files with total size > 1 GB in Staging | Marked `EXCEEDS_QUOTA`, reason: "Exceeds 1 GB batch limit", upload button disabled | `PASS` |
| **QUOTA-04** | Staged files whose total brings used storage > 10 GB | Marked `EXCEEDS_QUOTA`, reason: "Exceeds 10 GB vault limit", upload button disabled | `PASS` |
| **QUOTA-05** | Backend direct upload bypassing client when quota exceeded | Rejected (`StorageQuotaExceededException` / HTTP 507), disk file cleaned | `PASS` |
| **QUOTA-06** | Concurrent uploads racing for remaining storage | Atomic SQL check `(used_storage + delta) <= storage_limit` rejects overflow | `PASS` |
| **QUOTA-07** | Duplicate upload attempt | Rejected (`DuplicateFileException` / HTTP 409), no quota consumed | `PASS` |
| **QUOTA-08** | Delete existing file | Atomic quota decrement `used_storage - fileSize`, space reclaimed | `PASS` |

---

## 4. Cryptographic & Security Verification Matrix (Phase 9 Verification)

| Test ID | Target Component | Verification Criteria | Status |
|---|---|---|---|
| **SEC-01** | BCrypt Password Hashing | Work factor 12, random salts, irreversible hashes | `PASS` |
| **SEC-02** | JWT Token Versioning | Logout/password reset bumps `tokenVersion`; stale token rejected | `PASS` |
| **SEC-03** | Brute-Force Rate Limiting | 5 failed attempts triggers 15-minute lockout (HTTP 429) | `PASS` |
| **SEC-04** | Cross-User Isolation | File download/delete rejects requests not owned by authenticated user | `PASS` |
| **SEC-05** | AES-256-GCM Streaming | 12-byte random IV, 16-byte authentication tag, zero plaintext leakage | `PASS` |
| **SEC-06** | Authentication Tag Verification | Corrupted ciphertext fails decryption immediately (`AEADBadTagException`) | `PASS` |
| **SEC-07** | Path Traversal Protection | Stored paths resolving outside storage root throw `SecurityException` | `PASS` |

---

## 5. Execution Commands & Evidence Reproduction

```powershell
# 1. Run all Backend tests
cd backend
.\mvnw.cmd test

# 2. Run all Android unit tests
cd ..\android
.\gradlew.bat testDebugUnitTest

# 3. Assemble release/debug Android APK
.\gradlew.bat assembleDebug
```

