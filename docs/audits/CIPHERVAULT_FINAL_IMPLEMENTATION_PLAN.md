# CIPHERVAULT — COMPREHENSIVE PROJECT RECOVERY & IMPLEMENTATION PLAN

**Document ID:** `CIPHERVAULT_FINAL_IMPLEMENTATION_PLAN.md`  
**Location:** `docs/audits/CIPHERVAULT_FINAL_IMPLEMENTATION_PLAN.md`  
**Execution Timestamp:** 2026-10-13  
**Target Repository:** `C:\Users\akhil\OneDrive\Desktop\CipherVault`  
**Architectural Baseline:** Android Java/XML Views (Material 3) + Spring Boot 3.3.4 (Java 21 LTS) + MySQL 8.0  

---

## 1. Executive Summary & Current Project State

### 1.1 Baseline System State
The CipherVault application is a self-hosted private cloud storage system with client-side and server-side encryption, duplicate detection, cascading metadata extraction, and storage accounting.

- **Android Client:**
  - Build Variant: `debug` (minSdk 26, targetSdk 34/35)
  - UI Toolkit: Native Java and XML Views adhering strictly to Material 3 Design Tokens (Warm cream, peach, brown color scheme).
  - Current APK Assembly: Built successfully at `android/app/build/outputs/apk/debug/app-debug.apk` (31,495,370 bytes / ~30.04 MB).
  - Existing Android Test Suite: 15 test suites, 58 tests passing (0 failures, 0 errors, 0 skipped).
- **Backend Core:**
  - Framework: Spring Boot 3.3.4 on Java 21 LTS, Spring Security 6.3.3, Hibernate 6.5.3 (DDL-auto: update), HikariCP.
  - Server Port: `8080`, binding to `0.0.0.0`.
  - Database: MySQL Community Server 8.0.46 running locally on port `3306` (`ciphervault` database).
  - Storage Root: `backend/storage/` with `encrypted/`, `previews/`, `uploads/`, `profiles/`.
  - Existing Backend Test Suite: 132 tests passing (0 failures, 0 errors, 0 skipped).
- **Desktop Server Manager:**
  - Tooling: Python 3 / PySide6 (Qt) in `server-control/` for server monitoring, service lifecycle, QR code generation, and diagnostic logging.
- **Git Working Tree:**
  - Active Branch: `master`
  - Uncommitted Work: 121 modified files and 25 untracked files reflecting recent logging upgrades, cascading metadata facets UI, and unit test expansions.
  - Policy: Protect working tree; zero destructive commands (`git reset --hard` or blanket `git clean` prohibited); no commits or pushes (owner manages Git history).

---

## 2. File Inventory Summary & Cross-Reference Findings

### 2.1 File Counts and Distribution
Total core repository files (excluding `.git`, build directories, and physical storage blobs): **461 files**.
- **Java Source Files (196):**
  - Android Main Java: 54 files in `com.ciphervault.app` (Activities, Fragments, Adapters, Network Clients, Utilities).
  - Android Architecture Packages: 36 files across `auth/`, `core/`, `motion/`, `navigation/`, `onboarding/`, `theme/`, `transfer/`.
  - Android Unit Tests: 15 files in `android/app/src/test/java`.
  - Backend Main Java: 61 files in `com.ciphervault.ciphervault` across `activity/`, `analytics/`, `auth/`, `config/`, `exception/`, `file/`, `health/`, `logging/`, `security/`, `transfer/`, `user/`.
  - Backend Unit/Integration Tests: 30 files in `backend/src/test/java`.
- **XML Resource Files (157):**
  - Android Layouts: 42 XML files in `res/layout` (all 14 Activities, 10 Fragments/Bottom Sheets, and 18 item/dialog views).
  - Android Drawables & Vectors: 79 XML files (Lucide icon set, status badges, backgrounds).
  - Android Values: 6 XML files (`colors.xml`, `strings.xml`, `styles.xml`, `themes.xml`, etc.).
  - Android Navigation & Menus: 7 XML files (`bottom_nav_menu.xml`, etc.).
  - Configuration: `network_security_config.xml`, `logback-spring.xml`, `pom.xml`.
- **Database & Schemas:**
  - `database/schema.sql`: Full DDL defining `users`, `stored_files`, `file_metadata`.
- **Documentation & Reports:**
  - 17 Markdown documents, 6 operational text files, and 12 evidence artifacts.

---

## 3. Critical Findings: Bugs, Security, Quotas & Incomplete Features

### 3.1 Storage Quota and Upload Limits
1. **Total Vault Capacity (10 GB):**
   - Configured in `User.java` as `DEFAULT_STORAGE_LIMIT = 10L * 1024L * 1024L * 1024L` (10,737,418,240 bytes).
   - Configured in `SessionManager.java` as `DEFAULT_LIMIT = 10737418240L`.
   - Backend enforces atomic updates via `userRepository.incrementStorageUsedAtomic` with SQL check `(u.usedStorage + :delta) <= u.storageLimit`.
   - Client displays real-time progress and used/available capacity in `UploadStagingActivity` and `StorageDetailsBottomSheet`.
2. **Maximum Single File & Selected Batch Upload Limit (1 GB):**
   - `FileStorageService.MAX_FILE_SIZE = 1024L * 1024L * 1024L` (1,073,741,824 bytes).
   - In `UploadStagingActivity`: `MAX_SINGLE_FILE_BYTES = 1073741824L` and `MAX_BATCH_BYTES = 1073741824L`.
   - Need comprehensive automated test coverage for exact boundary conditions (e.g., 1 byte over 1 GB, 1 byte over 10 GB, atomic rollback when quota is exceeded, multi-file batch calculation overflow protection).

### 3.2 Authentication & User Isolation
1. **Login with Email or Username:**
   - Client `LoginActivity.java` validates identifier and sends `LoginRequest(identifier, password)`.
   - Backend `AuthController.java` parses `identifier` and queries both `findByEmail` / `findByEmailIgnoreCase` and `findByUsername` / `findByUsernameIgnoreCase`.
   - Token contains email claim and token version; password change and logout bump `tokenVersion`, invalidating stale tokens immediately.
2. **Cross-User Data Isolation:**
   - Every file download, preview, search, delete, and facet query explicitly constrains by authenticated `User` entity (`findByIdAndUser`, `findByUser`).
   - Client-supplied IDs cannot access files belonging to another account.

### 3.3 UI Consistency and Material 3 Visual Identity
1. **Visual Language:**
   - Preserved warm cream, peach, and brown aesthetic (`colorSurface`, `colorSurfaceContainerLow`, `colorSecondaryContainer`, `colorPrimary`).
   - Edge-to-edge layout with system-bar insets in all activities and bottom sheets.
2. **Action Controls:**
   - Direct visible [Download] and [Delete] action buttons are present in `item_file.xml` and `item_file_card.xml` (no three-dot menu, as required).
   - Staging displays two distinct indicators: Vault Capacity (10 GB) and Selected Batch Size (1 GB).

### 3.4 Documentation Conflicts & Redundancies
- Root contains multiple overlapping report files from recent audit passes (`CIPHERVAULT_FINAL_MASTER_IMPLEMENTATION_REPORT.md`, `CIPHERVAULT_METADATA_DATABASE_AUDIT_REPORT.md`, `CIPHERVAULT_BACKEND_LOGGING_IMPLEMENTATION_REPORT.md`, `FINAL_UI_REGRESSION_2_REPORT.md`, etc.).
- Root README needs to be the single authoritative entry point linking to organized, authoritative references under `docs/`.
- No source code, tests, or required assets will be deleted. Only verified redundant documentation will be safely consolidated.

---

## 4. Comprehensive Test and Verification Strategy

### 4.1 Android Client Verification
- Build validation: `assembleDebug` and `checkDebugAarMetadata`.
- Unit tests: Execute full test suite via `gradlew testDebugUnitTest`.
- Add test coverage for quota boundaries: `UploadStagingQuotaTest.java` verifying 1 GB batch limit, 10 GB capacity limit, and duplicate exclusion.

### 4.2 Backend Core Verification
- Compilation: `mvnw test-compile`.
- Test suite: Execute all 132+ tests via `mvnw test`.
- Add test coverage for quota enforcement: `StorageQuotaBoundaryTest.java` verifying 10 GB limit, 1 GB file limit, atomic database reservation, duplicate conflict race condition, and atomic quota rollback on failure.

### 4.3 Emulator Runtime Validation
- Check available AVD: `Pixel_10_Pro_XL` on Android SDK.
- If authorized and environment permits, launch emulator, install latest APK, verify startup, splash, onboarding, connection, login, vault list, staging, and settings.
- Inspect Logcat for unhandled exceptions or crashes.

---

## 5. Database Reset Safety Procedure (Phase 15 Approval Gate)

1. **Pre-Reset Requirements:**
   - Complete all code audits, fixes, builds, and automated tests.
   - Run read-only verification queries to document affected record counts.
   - Perform full physical SQL dump/backup into an external archive directory (`ciphervault_backup_<timestamp>.sql`).
2. **Explicit User Approval Gate:**
   - Present exact database target (`localhost:3306`, schema: `ciphervault`), affected table counts, and backup verification to the user.
   - **STOP** and wait for explicit confirmation before running any destructive TRUNCATE or DROP operation.
3. **Execution Post-Approval:**
   - Clean user and file records while preserving database schema and migration structure.
   - Restart backend and verify `/api/health` and a clean demo user registration.

---

## 6. Ordered Implementation Phases

| Phase | Description | Deliverable / Output | Acceptance Criteria |
|---|---|---|---|
| **Phase 1** | Complete Repository Inventory | `docs/audits/CIPHERVAULT_FILE_INVENTORY.md` | All 461 core files documented with roles and dependencies |
| **Phase 2** | File-by-File Code Audit & Ledger | `docs/audits/CIPHERVAULT_FILE_AUDIT_LEDGER.md` | Complete status ledger covering all source and resource files |
| **Phase 3** | End-to-End Application Tracing | System trace documentation | Traced auth, connection, staging, crypto, search, delete workflows |
| **Phase 4** | Screen-by-Screen Implementation Ref | `docs/reference/CIPHERVAULT_SCREEN_BY_SCREEN_IMPLEMENTATION.md` | Detailed breakdown of all 14 screens + bottom sheets |
| **Phase 5** | Backend, API & Database Reference | `docs/reference/CIPHERVAULT_BACKEND_API_DATABASE_REFERENCE.md` | Exhaustive REST endpoint and DDL schema documentation |
| **Phase 6** | Documentation Consolidation | Authoritative `README.md` & reference docs | One clean root README, redundant docs consolidated |
| **Phase 7** | Implement & Fix Verified Defects | Source code fixes & refinements | Resolved all discovered edge cases & UI inconsistencies |
| **Phase 8** | Quota & Upload Limit Verification | Automated test suites for 10 GB & 1 GB | Boundary tests for limits, unit conversions, and error states |
| **Phase 9** | Security & Cryptography Audit | `docs/reference/CIPHERVAULT_SECURITY_AND_ENCRYPTION.md` | AES-256-GCM, PBKDF2, JWT token versioning audited |
| **Phase 10** | Automated Test Suites & Build Verification | Build logs, test reports | 100% green tests across Android and Backend |
| **Phase 11** | Emulator / Device Runtime QA | Runtime QA logs and verification status | Verified against Pixel 10 Pro XL emulator if authorized |
| **Phase 12** | Synthetic File & Feature Testing | Test file matrix and validation logs | Multi-format uploads, previews, and metadata validated |
| **Phase 13** | Error Injection & Recovery | Fault recovery verification | App handles network down, 413, 507, 401, 429 safely |
| **Phase 14** | Documentation & Link Integrity | Complete link validation | All cross-references and diagrams verified |
| **Phase 15** | Demo Database Reset Gate | Backup SQL + Approval request | Destructive reset strictly gated on user confirmation |
| **Phase 16** | Final Deliverables & Report | `docs/reports/CIPHERVAULT_FINAL_VERIFICATION_REPORT.md` | Complete evidence-based final report |

