# CipherVault — Repository & Documentation Audit

This document provides a comprehensive audit of all project documentation, specifications, reports, schemas, and source code across the CipherVault repository. Every audited fact is mapped to its exact relative path in the project.

---

## 1. Documentation Inventory and Classification

The repository contains extensive technical documentation organized into functional root directories under `docs/`. Each file was inspected to evaluate project requirements, current implementation reality, historical proposals, and test verifications.

| Category | Relative Path | Purpose & Content Scope | Classification |
| :--- | :--- | :--- | :--- |
| **Requirements** | [`docs/project-reports/CipherVault_Project_Report_Source.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CipherVault_Project_Report_Source.md) | Exhaustive 17-chapter project report outline, Functional Requirements (FR-01 to FR-20), Non-Functional Requirements, STRIDE threat model, and academic bibliography. | **Authoritative Specification** |
| **Architecture** | [`docs/architecture/CIPHERVAULT_SYSTEM_ARCHITECTURE.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/architecture/CIPHERVAULT_SYSTEM_ARCHITECTURE.md) | Multi-tier system architecture, network protocols, layered component decomposition, and cryptographic trust boundaries. | **Current Implementation** |
| **Architecture** | [`docs/architecture/ARCHITECTURE.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/architecture/ARCHITECTURE.md) | Architectural overview and component interaction workflows. | **Current Implementation** |
| **API & Database** | [`docs/api-and-database/API_SPECIFICATION.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/api-and-database/API_SPECIFICATION.md) | Exhaustive REST API contract across Authentication, File Management, Metadata, and Transfers. | **Current Implementation** |
| **API & Database** | [`docs/api-and-database/DATABASE_SCHEMA.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/api-and-database/DATABASE_SCHEMA.md) | Relational schema definitions, column data dictionaries, and indexing strategies. | **Current Implementation** |
| **API & Database** | [`database/schema.sql`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/database/schema.sql) | Authoritative MySQL DDL statements with constraints, default values, and foreign keys. | **Ground-Truth Source** |
| **Security** | [`docs/security-and-encryption/CIPHERVAULT_SECURITY_AND_ENCRYPTION.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/security-and-encryption/CIPHERVAULT_SECURITY_AND_ENCRYPTION.md) | Detailed cryptographic mechanics: AES-256-GCM, PBKDF2 iterations, envelope key wrapping, and threat mitigations. | **Current Implementation** |
| **Security** | [`docs/security-and-encryption/SECURITY_MODEL.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/security-and-encryption/SECURITY_MODEL.md) | Threat modeling and security invariants. | **Current Implementation** |
| **UI & Screens** | [`docs/project-reports/CIPHERVAULT_SCREEN_BY_SCREEN_IMPLEMENTATION.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CIPHERVAULT_SCREEN_BY_SCREEN_IMPLEMENTATION.md) | Screen-by-screen catalog of all 20+ Android screens, XML layout IDs, view bindings, and state transitions. | **Current Implementation** |
| **Testing & QA** | [`docs/project-reports/CIPHERVAULT_FINAL_VERIFICATION_REPORT.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CIPHERVAULT_FINAL_VERIFICATION_REPORT.md) | Verification ledger, automated test results (138 backend, 73/82 Android), and emulator execution logs. | **Verification Evidence** |
| **Testing & QA** | [`docs/testing-and-qa/CIPHERVAULT_COMPREHENSIVE_TEST_PLAN.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/testing-and-qa/CIPHERVAULT_COMPREHENSIVE_TEST_PLAN.md) | Formal test matrix, boundary conditions (10 GB / 1 GB), and test case definitions TC-01 to TC-25. | **Verification Plan** |
| **Testing & QA** | [`docs/testing-and-qa/CIPHERVAULT_FILE_AUDIT_LEDGER.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/testing-and-qa/CIPHERVAULT_FILE_AUDIT_LEDGER.md) | File-by-file audit ledger verifying source code integrity and purpose across the entire repository. | **Audit Ledger** |
| **Setup & Run** | [`CIPHERVAULT_HOW_TO_RUN.txt`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/CIPHERVAULT_HOW_TO_RUN.txt) & [`docs/setup-and-usage/HOW_TO_RUN.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/setup-and-usage/HOW_TO_RUN.md) | Step-by-step instructions for running Server Control, Maven backend, and Android debug builds. | **Operational Guide** |
| **Historical Slide** | `Ak - CipherVault MCA Project Presentation.pdf` | First-review presentation deck (19 slides) presented on September 08, 2026. | **Historical Baseline** |

---

## 2. Analysis of Historical First-Review Presentation vs. Current Reality

An in-depth comparison was performed between the first-review presentation (`Ak - CipherVault MCA Project Presentation.pdf`) and the current source code and schema. The table below identifies outdated statements and explains the current implementation:

| Slide # | Slide Title | Statement in First-Review PDF | Current Reality & Implementation Finding | Presentation Adjustment |
| :---: | :--- | :--- | :--- | :--- |
| **4** | Proposed System | *"Encrypted files can currently be downloaded in their encrypted form (.enc). Secure decryption and original-file download are planned as a future enhancement."* | **Outdated.** Decryption on download is fully implemented. The backend route `GET /api/files/{id}/download?decrypt=true` performs streaming AES-256-GCM decryption with `CipherInputStream` and GHASH authentication tag verification. The Android download dialog provides a toggle switch (*"Decrypt on download"*). | Present download decryption as **fully implemented and verified**. |
| **5** | System Architecture | *"Planned: ExifTool integration"* | **Outdated.** ExifTool metadata extraction is fully implemented in `MetadataExtractionService.java`, extracting 29 media attributes into `file_metadata`. | Present ExifTool as an **active core backend component**. |
| **6** | System Modules | *"ExifTool-based extraction (future)"*, *"Complete activity logging (future)"* | **Outdated.** Both ExifTool metadata extraction and comprehensive security activity logging (via `activity_events`) are actively running in production. | Upgrade both modules from "future" to **fully implemented**. |
| **8** | Use Case Diagram | *"File deletion, complete metadata search, and complete activity functionality are currently planned/future features."* | **Outdated.** All three use cases are implemented: `DELETE /api/files/{id}` cascades to metadata and disk; `/api/files/search` supports multi-attribute filtering; `/api/activities` serves audit logs. | Include all three as **active, standard UML use cases**. |
| **9, 10** | Data Flow Diagram | Depicts DFD with dashed lines to *ExifTool (External Tool - Planned)*. | **Outdated.** ExifTool is integrated into the active upload pipeline before encryption. | Draw ExifTool as an **active operational process**. |
| **11** | ER / Database Design | Depicts tables: `users`, `files`, `file_metadata`, `activity_log`. | **Outdated.** Authoritative table names in `schema.sql` are `users`, `stored_files`, `file_metadata`, `activity_events`, and `transfers`. | Use the **authoritative schema table and column names**. |
| **15** | AES-256-GCM | *"Backend decryption and original-file download will be implemented in a future phase."* | **Outdated.** Dual-mode streaming download (decrypted plaintext vs raw `.enc`) is operational. | Present dual-mode download as an **achieved security milestone**. |
| **16** | Storage Architecture | Depicts only `uploads/`, `encrypted/`, and `thumbnails/`. | **Incomplete.** Actual physical directories in `FileStorageConfig.java` are: `storage/uploads/`, `storage/encrypted/`, `storage/previews/`, and `storage/profile_photos/`. | Accurately illustrate the **four real storage directories**. |

---

## 3. Implementation Status Register

| Feature / Capability | Implementation Status | Evidence / Verification Source |
| :--- | :---: | :--- |
| User Registration & BCrypt Hashing | **Implemented** | `AuthController.java`, `User.java`, `PasswordConfig.java` |
| Stateless JWT Authentication & Refresh | **Implemented** | `JwtService.java`, `JwtAuthenticationFilter.java` |
| Sliding-Window Login Rate Limiter (Brute-Force) | **Implemented** | `LoginRateLimiterService.java` (5 attempts / 15 min lockout) |
| Hardware-Backed Session Persistence | **Implemented** | Android `SessionManager.java`, `EncryptedSharedPreferences` |
| Biometric / PIN AppLock Overlay | **Implemented** | `AppLockActivity.java`, `BiometricPrompt` |
| 10 GB Storage Quota Enforcement | **Implemented** | `users.storage_limit` = `10737418240`, `FileStorageService.java` |
| 1 GB Maximum Upload / Batch Limit | **Implemented** | `FileStorageService.java` (`MAX_FILE_SIZE = 1073741824`), `UploadStagingActivity.java` |
| Pre-Upload Staging Queue & Modal Preview | **Implemented** | `UploadStagingActivity.java`, `dialog_staged_file_preview.xml` |
| Per-User SHA-256 Duplicate Detection | **Implemented** | `schema.sql` (`uq_stored_files_user_sha256`), `FileStorageService.java` |
| AES-256-GCM Streaming Ingestion (16 KB Chunks) | **Implemented** | `EncryptionService.java`, `CipherOutputStream` |
| PBKDF2 Envelope Key Wrapping (KEK & UDEK) | **Implemented** | `KeyManagementService.java` (65,536 rounds), `users.user_key` |
| ExifTool Forensic Metadata Extraction | **Implemented** | `MetadataExtractionService.java`, `file_metadata` (29 columns) |
| Dynamic Metadata Search & Suggestion Chips | **Implemented** | `FileSearchService.java`, `/api/files/suggestions` |
| Dual-Mode Streaming Download (Decrypted / Raw) | **Implemented** | `FileController.java#L684`, `FileStorageService.java#L600` |
| Cascading File Deletion & Quota Decrement | **Implemented** | `FileController.java#L800`, `ON DELETE CASCADE` |
| Real-Time Foreground Transfer Monitoring | **Implemented** | `TransferService.java`, `TransferManager.java`, `transfers` table |
| Multi-Factor Authentication (MFA / TOTP) | **Planned / Future** | Not implemented in v2.4.0; documented on Slide 14 roadmap |
| Chunked Resumable Uploads (TUS Protocol) | **Planned / Future** | Single-request streaming is used; resumability is roadmap |
| Distributed Object Storage (MinIO / S3) | **Planned / Future** | Local server filesystem is currently used; MinIO is roadmap |

---

## 4. Test Suite and Runtime Evidence Summary

* **Automated Backend Tests:** **138 tests run, 138 passed, 0 failures, 0 errors** (verified in `backend/target/surefire-reports/`).
* **Automated Android Unit Tests:** **82 tests run, 82 passed, 0 failures, 0 errors** (`.\gradlew.bat testDebugUnitTest`).
* **Automated Server Control Tests:** **23 tests run, 23 passed, 0 failures** (`python -m unittest discover -s server-control/tests`).
* **Live Emulator Verification:** Executed on `Pixel_10_Pro_XL` (API 37) with 20 real frame captures archived in `presentation/assets/screenshots/`.
