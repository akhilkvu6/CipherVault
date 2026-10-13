# CIPHERVAULT — COMPREHENSIVE REPOSITORY FILE INVENTORY

**Document ID:** `CIPHERVAULT_FILE_INVENTORY.md`  
**Location:** `docs/audits/CIPHERVAULT_FILE_INVENTORY.md`  
**Execution Timestamp:** 2026-10-13  
**Audit Scope:** Full repository forensic inventory covering Android, Backend, Database, Server Control, Scripts, and Documentation.  
**Inspection Mode:** Directly inspected against filesystem contents and abstract syntax trees.  

---

## 1. Inventory Summary by Module

| Module / Category | File Types | Count | Primary Responsibility |
|---|---|---|---|
| **Android Core App** | `.java` | 54 | Activities, UI Fragments, Adapters, Network Clients, Session & Security |
| **Android Architecture Packages** | `.java` | 36 | Auth, Core, Motion, Navigation, Onboarding, Theme, Transfer |
| **Android Unit Tests** | `.java` | 15 | Validation, Motion, Navigation, Session, Connection, Category Tests |
| **Android Layouts** | `.xml` | 42 | Screens, Bottom Sheets, Dialogs, Recycler List & Card Items |
| **Android Resources** | `.xml` | 85 | Drawables, Values, Colors, Themes, Styles, Menus, Insets |
| **Android Build & Manifest** | `.kts`, `.xml`, `.properties` | 6 | AndroidManifest.xml, Gradle build scripts, properties |
| **Backend Core** | `.java` | 61 | Controllers, Services, Repositories, Entities, DTOs, Security, Logging |
| **Backend Unit & Integration Tests** | `.java` | 30 | Auth, Crypto, Storage, Quota, Category, Facets, Controller Tests |
| **Backend Config & Resources** | `.properties`, `.xml`, `.cmd` | 4 | application.properties, logback-spring.xml, Maven wrapper & pom.xml |
| **Database** | `.sql` | 1 | Complete database DDL schema and indexes |
| **Server Control** | `.py`, `.bat`, `.json` | 11 | Desktop Server Manager (PySide6), lifecycle and QR services |
| **Report Generator** | `.py` | 8 | MCA Project report generation and styling |
| **Root & Documentation** | `.md`, `.txt`, `.py` | 23 | Build scripts, architecture guides, reports, README |
| **Evidence & Test Assets** | `.png`, `.xml` | 10 | Staging screenshots, hierarchy dumps |
| **Total Core Project Files** | | **386+** | (Excluding `.git`, build directories, and storage blobs) |

---

## 2. Android Source Files (`android/app/src/main/java`)

### 2.1 Activities & Screens (`com.ciphervault.app`)

1. **`AboutActivity.java`**
   - *Type:* Android Activity (`BaseActivity`)
   - *Purpose:* Renders application metadata, architecture specs, encryption standards, developer credits, and GitHub links.
   - *Dependencies:* `BaseActivity`, `R.layout.activity_about`, `MaterialToolbar`.
   - *Status:* Current. Retain.
2. **`AppLockActivity.java`**
   - *Type:* Android Activity (`BaseActivity`)
   - *Purpose:* Enforces biometric (fingerprint/face) or PIN re-authentication when resuming the application after timeout.
   - *Dependencies:* `BiometricPrompt`, `CipherVaultPreferences`, `R.layout.activity_app_lock`.
   - *Status:* Current. Retain.
3. **`BaseActivity.java`**
   - *Type:* Abstract Base Activity
   - *Purpose:* Provides common lifecycle behavior, dynamic theme application (Warm cream/dark mode), and system bar inset management across all screens.
   - *Dependencies:* `ThemeManager`, `SessionManager`, `AppCompatActivity`.
   - *Status:* Current. Retain.
4. **`CameraActivity.java`**
   - *Type:* Android Activity (`BaseActivity`)
   - *Purpose:* Provides in-app camera capture for immediate encrypted photo/video staging without leaving unencrypted copies in the device gallery.
   - *Dependencies:* CameraX / Camera API, `R.layout.activity_camera`, `FileUtils`.
   - *Status:* Current. Retain.
5. **`ConnectionActivity.java`**
   - *Type:* Android Activity (`BaseActivity`)
   - *Purpose:* Configures and validates backend server connectivity. Supports ADB Reverse, Emulator Host (`10.0.2.2`), Localhost, Wi-Fi LAN IP, QR code scanning, and manual IP entry with instant health check.
   - *Dependencies:* `ServerConnectionManager`, `ApiClient`, `QrScanActivity`, `R.layout.activity_connection`.
   - *Status:* Current. Retain.
6. **`FileViewerActivity.java`**
   - *Type:* Android Activity (`BaseActivity`)
   - *Purpose:* Provides secure, in-memory preview of decrypted files (images, PDFs, text, videos) with playback controls and metadata viewing.
   - *Dependencies:* `ThumbnailLoader`, `FileUtils`, `R.layout.activity_file_viewer`.
   - *Status:* Current. Retain.
7. **`LoginActivity.java`**
   - *Type:* Android Activity (`BaseActivity`)
   - *Purpose:* Handles user sign-in via **email or username**, enforces input validation, handles 401/429 lockout errors, and saves JWT session.
   - *Dependencies:* `ApiClient`, `SessionManager`, `LoginRequest`, `LoginResponse`, `AuditLogger`, `R.layout.activity_login`.
   - *Status:* Current. Retain.
8. **`MainActivity.java`**
   - *Type:* Root Navigation Activity (`BaseActivity`)
   - *Purpose:* Hosts bottom navigation managing all five primary destinations: Home, Vault, Search, Transfers/Log, and Settings. Handles intent dispatch and system bar insets.
   - *Dependencies:* `FragmentHome`, `FragmentVault`, `FragmentMetadataSearch`, `FragmentTransfers`, `FragmentSettings`, `UploadStagingActivity`, `R.layout.activity_main`.
   - *Status:* Current. Retain.
9. **`MetadataSearchActivity.java`**
   - *Type:* Standalone Activity wrapper for metadata search
   - *Purpose:* Secondary search intent host that delegates to metadata search queries.
   - *Dependencies:* `FragmentMetadataSearch`, `R.layout.activity_metadata_search`.
   - *Status:* Current. Retain.
10. **`QrScanActivity.java`**
    - *Type:* Android Activity (`BaseActivity`)
    - *Purpose:* Scans QR codes generated by Desktop Server Manager to configure server base URL instantly.
    - *Dependencies:* CameraX, Barcode scanner / ML Kit, `R.layout.activity_qr_scan`.
    - *Status:* Current. Retain.
11. **`SignupActivity.java`**
    - *Type:* Android Activity (`BaseActivity`)
    - *Purpose:* Registers new accounts with full validation (password complexity, email regex, unique username) and auto-routes to Login.
    - *Dependencies:* `ApiClient`, `RegisterRequest`, `RegisterResponse`, `R.layout.activity_signup`.
    - *Status:* Current. Retain.
12. **`SplashActivity.java`**
    - *Type:* Launcher Activity
    - *Purpose:* First launch router: checks onboarding status, session validity, server connectivity, and routes to Onboarding, Connection, Login, or Main.
    - *Dependencies:* `OnboardingPreferences`, `SessionManager`, `ServerConnectionManager`, `R.layout.activity_splash`.
    - *Status:* Current. Retain.
13. **`UiShowcaseActivity.java`**
    - *Type:* Developer / Showcase Activity
    - *Purpose:* Displays all Material 3 components, colors, and motion tokens in light and dark mode.
    - *Dependencies:* `R.layout.activity_ui_showcase`.
    - *Status:* Current. Retain.
14. **`UploadStagingActivity.java`**
    - *Type:* Android Activity (`BaseActivity`)
    - *Purpose:* Pre-upload staging pipeline. Performs local SHA-256 duplicate checking, enforces **10 GB vault capacity** and **1 GB batch / single file limits**, generates previews, and triggers background encrypted streaming upload.
    - *Dependencies:* `TransferManager`, `ThumbnailLoader`, `FileUtils`, `AuditLogger`, `R.layout.activity_upload_staging`.
    - *Status:* Current. Retain.

### 2.2 Fragments & Bottom Sheets (`com.ciphervault.app`)

15. **`AdvancedSearchFiltersBottomSheet.java`**
    - *Type:* `BottomSheetDialogFragment`
    - *Purpose:* Advanced filter selection dialog supporting camera make/model, codec, date ranges, and resolution.
    - *Dependencies:* `R.layout.bottom_sheet_advanced_search_filters`.
    - *Status:* Current. Retain.
16. **`FileDetailsBottomSheet.java`**
    - *Type:* `BottomSheetDialogFragment`
    - *Purpose:* Displays exhaustive technical metadata (camera make, model, ISO, shutter speed, focal length, resolution, audio/video codecs, Exif dates, hash, file size) with Download and Delete actions.
    - *Dependencies:* `StoredFile`, `FileMetadataDTO`, `ThumbnailLoader`, `R.layout.bottom_sheet_file_details`.
    - *Status:* Current. Retain.
17. **`FragmentDiagnostics.java`**
    - *Type:* UI Fragment
    - *Purpose:* Diagnostic panel for testing network latency, API endpoints, cipher parameters, and local database cache.
    - *Dependencies:* `ApiClient`, `ServerConnectionManager`, `R.layout.fragment_diagnostics`.
    - *Status:* Current. Retain.
18. **`FragmentHome.java`**
    - *Type:* Primary Destination Fragment
    - *Purpose:* Dashboard destination showing storage consumption widget (used vs 10 GB limit), quick category shortcuts (Photos, Videos, Documents, Audio), recent file cards, and system status.
    - *Dependencies:* `ApiService`, `StoredFile`, `FilesAdapter`, `UploadStagingActivity`, `R.layout.fragment_home`.
    - *Status:* Current. Retain.
19. **`FragmentMetadataSearch.java`**
    - *Type:* Primary Destination Fragment
    - *Purpose:* Unified cascading metadata search screen. Renders search query input, category pills, dynamic server facets (camera makes, models, resolutions, dates), inline "+ More" expansion, active filters bar, and sorted file results.
    - *Dependencies:* `ApiService`, `FileFacetsResponse`, `FilesAdapter`, `R.layout.fragment_metadata_search`.
    - *Status:* Current. Retain.
20. **`FragmentSettings.java`**
    - *Type:* Primary Destination Fragment
    - *Purpose:* Settings destination managing Theme (Light, Dark, System Default, Dynamic Warm), Biometric App Lock, Server Connection selection, Profile details, Storage breakdown, and Secure Logout.
    - *Dependencies:* `ThemeManager`, `SessionManager`, `ConnectionActivity`, `AboutActivity`, `R.layout.fragment_settings`.
    - *Status:* Current. Retain.
21. **`FragmentTransfers.java`**
    - *Type:* Primary Destination Fragment
    - *Purpose:* Dual-mode destination displaying live upload/download transfer progress with pause/resume/cancel controls and historical audit event log with filtering by status and date.
    - *Dependencies:* `TransferManager`, `AuditLogger`, `R.layout.fragment_transfers`.
    - *Status:* Current. Retain.
22. **`FragmentVault.java`**
    - *Type:* Primary Destination Fragment
    - *Purpose:* Comprehensive encrypted file repository. Supports List and Grid modes, category tabs, real-time filename search, multi-sort (Name, Size, Date), multi-file batch selection, and direct visible [Download] and [Delete] buttons.
    - *Dependencies:* `ApiService`, `StoredFile`, `FilesAdapter`, `FileDetailsBottomSheet`, `R.layout.fragment_vault`.
    - *Status:* Current. Retain.
23. **`MetadataSearchBottomSheet.java`**
    - *Type:* `BottomSheetDialogFragment`
    - *Purpose:* Compact metadata filter picker.
    - *Dependencies:* `R.layout.bottom_sheet_metadata_search`.
    - *Status:* Current. Retain.
24. **`StorageDetailsBottomSheet.java`**
    - *Type:* `BottomSheetDialogFragment`
    - *Purpose:* Visual breakdown of the **10 GB vault capacity** by category (Photos, Videos, Documents, Audio, Other) and duplicate space reclaim suggestions.
    - *Dependencies:* `ApiService`, `R.layout.bottom_sheet_storage_details`.
    - *Status:* Current. Retain.

### 2.3 Data Models, Adapters, Network & Helpers (`com.ciphervault.app`)

25. **`ApiClient.java`**: Retrofit 2 builder with OkHttp, logging interceptor, auth token injection, and timeout management.
26. **`ApiService.java`**: Retrofit interface defining REST contracts: auth, storage summary, files, upload, download, preview, search, facets, delete.
27. **`AuditLogger.java`**: Persists client audit logs with SQLite / SharedPreferences for transfer and security operations.
28. **`AuthInterceptor.java`**: OkHttp interceptor injecting `Authorization: Bearer <token>` into outgoing requests.
29. **`CipherVaultApplication.java`**: Application class initializing Timber, theme preferences, and transfer notification channels.
30. **`CipherVaultPreferences.java`**: General shared preferences wrapper for client settings.
31. **`DuplicateCheckResponse.java`**: DTO mapping `{ isDuplicate, existingFileName, fileId }`.
32. **`FileComparator.java`**: Sort comparator implementing all sorting rules (Date, Name, Size asc/desc).
33. **`FileFacetsResponse.java`**: DTO mapping dynamic server metadata facets `{ totalMatchingFiles, facets: [field, label, available, values] }`.
34. **`FileMetadataDTO.java`**: Client DTO holding complete camera, lens, codec, resolution, and document metadata.
35. **`FilesAdapter.java`**: RecyclerView adapter for file list and grid cards, handling thumbnails, video badges, encryption chips, download, and delete.
36. **`FileSortOption.java`**: Enum defining sorting modes (`DATE_NEW_OLD`, `NAME_ASC`, `SIZE_HIGH_LOW`, etc.).
37. **`FileUtils.java`**: File helper for MIME types, size formatting, SHA-256 hashing, and URI resolution.
38. **`LoginRequest.java`**: DTO supporting login by identifier, email, or username.
39. **`LoginResponse.java`**: DTO parsing server login response.
40. **`NetworkPreferences.java`**: Persists active backend server URL and connection modes.
41. **`ProfilePhotoHelper.java`**: Manages profile picture caching and upload.
42. **`RegisterRequest.java` / `RegisterResponse.java`**: Registration DTOs.
43. **`SavedServer.java`**: Model for remembered server configurations.
44. **`SearchHistoryManager.java`**: Manages popular and recent search query history.
45. **`ServerConnectionManager.java`**: Network probing engine supporting ADB Reverse, Emulator Host, and Wi-Fi LAN modes.
46. **`SessionManager.java`**: Secure session token storage using `EncryptedSharedPreferences`, master key, and default 10 GB quota limit.
47. **`StoredFile.java`**: Client entity representing vault items.
48. **`StreamingProgressRequestBody.java`**: OkHttp request body calculating streaming upload progress.
49. **`ThemeManager.java`**: Applies Material 3 warm cream and dynamic palettes.
50. **`ThumbnailLoader.java`**: Generates and caches image thumbnails, video keyframes, audio album art, and PDF first pages.
51. **`UploadResponse.java`**: DTO mapping server file upload confirmation.
52. **`UserProfileResponse.java`**: DTO mapping user profile and quota usage.

### 2.4 Subpackages (`auth`, `core`, `motion`, `navigation`, `onboarding`, `theme`, `transfer`)
- **`auth/`**: `AuthApi`, `HealthApi`, `AuthRepository`, `AuthViewModel`, `AuthValidator`, `AuthErrorParser`, `PasswordRequirements`.
- **`core/`**: `network/ApiClient`, `ServerConnectionPreferences`, `KeyStoreCipher`, `session/SessionManager`.
- **`motion/`**: `MotionTokens`, `NavigationMotionManager`, `ReducedMotionHelper`.
- **`onboarding/`**: `OnboardingActivity`, `OnboardingAdapter`, `OnboardingPage`, `OnboardingPreferences`, plus 5 animation vector classes.
- **`transfer/`**: `TransferManager`, `TransferItem`, `TransferBatch`, `TransferService`, `TransferNotificationHelper`, `TransferState`, `TransferType`.

---

## 3. Backend Source Files (`backend/src/main/java`)

### 3.1 Controllers & Endpoints
1. **`ActivityController.java`**: REST controller for audit events (`GET /api/activity`, `GET /api/activity/{id}`).
2. **`AnalyticsController.java`**: System metrics and storage utilization analytics.
3. **`AuthController.java`**: Authentication controller (`POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/change-password`, `POST /api/auth/logout`). Supports login by username or email.
4. **`FileController.java`**: File operations (`POST /api/files/upload`, `GET /api/files`, `GET /api/files/{id}/download`, `DELETE /api/files/{id}`, `GET /api/files/{id}/preview`, `GET /api/files/search`, `GET /api/files/facets`, `GET /api/files/check-duplicate`).
5. **`HealthController.java`**: Service health endpoint (`GET /api/health`).
6. **`StorageController.java`**: Storage accounting (`GET /api/storage/summary`, `GET /api/storage/categories`, `GET /api/storage/duplicates`, `GET /api/storage/large-files`).
7. **`TransferController.java`**: Live transfer telemetry endpoints.
8. **`UserController.java`**: Profile management (`GET /api/user/profile`, `PUT /api/user/name`, `POST /api/user/photo`, `DELETE /api/user/delete-account`).

### 3.2 Services & Business Logic
9. **`ActivityService.java`**: Logs user activities to `activity_events` database table.
10. **`EncryptionService.java`**: Core AES-256-GCM streaming encryption and decryption engine with 12-byte IV and 128-bit authentication tags.
11. **`FileCategoryService.java`**: MIME and extension classifier (Images, Videos, Audio, Documents, Archives, Other).
12. **`FileFacetService.java`**: Generates cascading metadata facet counts based on the authorized user's files and current filters.
13. **`FileSearchService.java`**: Dynamic JPA specification search over metadata fields (camera make, model, lens, resolution, codec, artist, author, dates).
14. **`FileStorageService.java`**: Authoritative file lifecycle coordinator: streaming save, 1 GB file validation, 10 GB quota atomic verification, SHA-256 hashing, duplicate rejection, preview creation, Exif extraction, AES-256-GCM encryption, disk persistence, atomic quota reservation, and atomic rollback on failure.
15. **`MediaPreviewService.java`**: Generates thumbnail previews for images, video frames, and documents.
16. **`MetadataExtractionService.java`**: Extracts technical Exif, XMP, ID3, and PDF metadata using ExifTool and Java libraries.
17. **`KeyManagementService.java`**: PBKDF2 with HMAC-SHA256 user key derivation and master-key envelope encryption.
18. **`LoginRateLimiterService.java`**: Sliding-window rate limiter locking accounts after 5 consecutive failures for 15 minutes.

### 3.3 Repositories & Entities
19. **`User.java`**: JPA entity mapping `users` table with storage quota, used storage, user key, token version, and profile photo.
20. **`UserRepository.java`**: JPA repository with atomic SQL queries: `incrementStorageUsedAtomic` and `decrementStorageUsedAtomic`.
21. **`StoredFile.java`**: JPA entity mapping `stored_files` table with SHA-256 hash, storage path, encryption flag, preview metadata.
22. **`FileRepository.java`**: JPA repository for file queries, sorting, and user constraints.
23. **`FileMetadata.java`**: JPA entity mapping `file_metadata` table holding all Exif / media fields.
24. **`FileMetadataRepository.java`**: JPA repository for metadata entities.
25. **`ActivityEvent.java`**: JPA entity mapping `activity_events` table.
26. **`ActivityEventRepository.java`**: JPA repository for activity logs.
27. **`Transfer.java`**: JPA entity mapping `transfers` table.
28. **`TransferRepository.java`**: JPA repository for transfer records.

### 3.4 Security & Logging Infrastructure
29. **`SecurityConfig.java`**: Spring Security 6 filter chain configuring stateless sessions, CORS, and endpoint rules.
30. **`JwtAuthenticationFilter.java`**: Extracts Bearer token, validates signature and tokenVersion against database, sets security context.
31. **`JwtService.java`**: Generates and verifies HMAC-SHA256 JWT tokens.
32. **`PasswordConfig.java`**: BCryptPasswordEncoder bean (strength 12).
33. **`ConsoleLogger.java`**: Structured console logging formatter with high-readability tags.
34. **`RequestLoggingFilter.java`**: HTTP request/response latency and status logging filter.
35. **`RequestContext.java`**: ThreadLocal storage for request correlation IDs.
36. **`CipherVaultConsoleConverter.java`**: Logback pattern converter for colorized output.
37. **`StartupBannerListener.java`**: Startup banner and environment information printer.

---

## 4. Database Definitions (`database/`)

1. **`database/schema.sql`**
   - *Type:* MySQL 8.0 DDL SQL script
   - *Tables Defined:* `users`, `stored_files`, `file_metadata`
   - *Required Action:* Expand schema to include `activity_events` and `transfers` tables to maintain 100% parity with JPA entities.

---

## 5. Documentation & Technical Guides (`docs/`)

1. **`docs/api/API_SPECIFICATION.md`** -> Consolidating into `docs/reference/CIPHERVAULT_BACKEND_API_DATABASE_REFERENCE.md`.
2. **`docs/architecture/ARCHITECTURE.md`** -> Consolidating into `docs/architecture/CIPHERVAULT_SYSTEM_ARCHITECTURE.md`.
3. **`docs/database/DATABASE_SCHEMA.md`** -> Consolidating into `docs/reference/CIPHERVAULT_BACKEND_API_DATABASE_REFERENCE.md`.
4. **`docs/security/SECURITY_MODEL.md`** -> Consolidating into `docs/reference/CIPHERVAULT_SECURITY_AND_ENCRYPTION.md`.
5. **`docs/setup/SETUP_GUIDE.md`** -> Consolidating into `docs/reference/CIPHERVAULT_SETUP_CONNECTION_AND_TROUBLESHOOTING.md`.
6. **`docs/project-report/`** -> MCA Master project report and SVG architecture diagrams (Preserve all).

---

## 6. Retention and Cleanup Decisions

- **Preserve 100%:** All Java source files, all XML layout and drawable resources, all build configurations (`.gradle`, `.kts`, `pom.xml`), all SQL scripts, all test suites, and all screenshots/evidence.
- **Consolidate:** Dispersed root-level audit reports and text files will be preserved within authoritative reference documents under `docs/` and archived cleanly.
- **Authoritative Root Entry Point:** Exactly one authoritative `README.md` at repository root.

