# CipherVault — Test-Data Reset and Clean State Audit Report

**Date:** October 13, 2026  
**Auditor / Role:** Senior Database & Backend Systems Engineer  
**Target Repository:** `C:\Users\akhil\OneDrive\Desktop\CipherVault`  
**Target Database:** MySQL 8.0 `ciphervault` on port 3306  
**Status:** **SUCCESS / ALL OBJECTIVES VERIFIED**

---

## 1. Executive Summary & Objective

A controlled, comprehensive, and non-destructive reset of all test data in CipherVault has been successfully performed. The objective was to return both the MySQL database and the physical storage layer to a clean, fresh-installation state with **zero test users, zero test file records, and zero physical test uploads**, while rigorously preserving:
- All database schema definitions, columns, data types, indexes, unique constraints, and foreign keys.
- All application source code, resources, assets, build scripts, tests, and configuration files.
- The authoritative 10 GB vault user storage limit and 1 GB upload limit.
- All historical database backups and forensic evidence.
- No plaintext credentials, tokens, or encryption keys in console logs or reports.

Following the purge, end-to-end live lifecycle verification confirmed that fresh accounts can register, authenticate, upload encrypted files, download/decrypt payloads with byte parity, and delete files cleanly.

---

## 2. Pre-Reset Database Backup & Verification

Prior to any modification or deletion, a complete, timestamped SQL backup was generated using a secure temporary MySQL options configuration file to prevent credentials from being exposed in process arguments or logs.

| Backup File | Path | Size | Creation Timestamp | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Fresh Pre-Reset Backup** | `database/backups/ciphervault_backup_20261013_232658.sql` | 549,803 bytes | 2026-10-13 23:26:58 | Verified valid SQL dump (DDL + DML) |
| **Historical Backup** | `database/backups/ciphervault_backup_20261013_153755.sql` | 548,270 bytes | 2026-10-13 15:37:55 | Preserved untouched |

### Backup Content Validation:
- Contains full `CREATE TABLE` definitions for all 5 application tables (`users`, `stored_files`, `file_metadata`, `transfers`, `activity_events`).
- Contains all table constraints, foreign key definitions, and table option parameters.
- Contains complete `INSERT INTO` statements dumping the state of all 25 test users, 89 stored files, 89 metadata records, 130 transfers, and 6 activity events.

---

## 3. Pre-Reset Inventory & Cross-Referencing

A comprehensive audit was conducted across the MySQL database and the filesystem storage structures.

### 3.1 Database Inventory Before Reset

| Table Name | Initial Record Count | Foreign Key Dependencies | Description |
| :--- | :--- | :--- | :--- |
| `users` | **25** | Parent of `stored_files`, `transfers`, `activity_events` | Test user credentials, hashed passwords, storage usage |
| `stored_files` | **89** | References `users(id)`, Parent of `file_metadata` | File records, SHA-256 hashes, physical storage paths |
| `file_metadata` | **89** | References `stored_files(id)` | Extracted EXIF/ID3/PDF metadata, facets |
| `transfers` | **130** | References `users(id)` | Upload and download audit log entries |
| `activity_events`| **6** | References `users(id)` | Security and user activity history entries |
| **Total DB Records** | **339** | | |

### 3.2 Physical Storage Inventory & Discrepancy Analysis

The application storage path was analyzed. In `FileStorageConfig.java`, storage is anchored relative to the application's working directory (`Paths.get("storage")`).
- When the backend runs from `C:\Users\akhil\OneDrive\Desktop\CipherVault\backend`, physical files are stored in `backend/storage/`.
- The repository root `storage/` directory contains an empty directory scaffolding mirrored during tooling execution.

A file-by-file audit in `backend/storage/` revealed:

| Storage Subdirectory | Total Files | Disk Usage | DB-Referenced Files | Orphaned Test Files |
| :--- | :--- | :--- | :--- | :--- |
| `backend/storage/encrypted/` | 183 | ~1.35 GB | 89 | **94** (legacy uploads from interrupted test runs) |
| `backend/storage/previews/` | 164 | ~1.28 MB | 70 | **94** (legacy thumbnails from interrupted test runs) |
| `backend/storage/profile_photos/`| 3 | ~7.62 MB | 3 | 0 |
| `backend/storage/uploads/` | 0 | 0 B | 0 | 0 |
| Root `backend/storage/*.tmp` | 2 | ~12 KB | 0 | 2 (transient upload artifacts) |
| **Total Physical Assets** | **352** | **~1.36 GB** | **162** | **190** |

All 190 orphaned files were cross-referenced against the `stored_files` table; zero orphaned files had active database mappings.

---

## 4. Backend Process Coordination & Execution

To avoid database locking, file handle contention, and concurrent write race conditions:
1. The running Spring Boot backend process (PID 6656 / 6120) was gracefully terminated via task tree signal.
2. Verified port 8080 was fully released.
3. Verified MySQL service on port 3306 remained active and healthy.

---

## 5. Test-Data Purge Execution

### 5.1 Transactional Database Reset
The purge was performed inside an ACID SQL transaction. To preserve referential integrity without temporarily disabling foreign key checks (`FOREIGN_KEY_CHECKS=1` maintained throughout), tables were purged in strict leaf-to-root order:

```sql
START TRANSACTION;
DELETE FROM file_metadata;
DELETE FROM stored_files;
DELETE FROM activity_events;
DELETE FROM transfers;
DELETE FROM users;
COMMIT;
```

**Result:** All 5 tables were cleanly emptied with zero foreign key violations.

### 5.2 Physical Storage Cleanup
All physical test assets and orphaned files were removed from `backend/storage/`:
- Removed 183 encrypted file blobs (`encrypted/`).
- Removed 164 thumbnail previews (`previews/`).
- Removed 3 test profile photos (`profile_photos/`).
- Removed 2 transient `.tmp` files.
- Preserved all directory structures intact (`encrypted/`, `previews/`, `profile_photos/`, `uploads/`).
- Preserved root `storage/` scaffolding intact.

---

## 6. Post-Reset State & Schema Verification

### 6.1 Record & File Counts After Reset

| Component / Table | Target State | Actual State | Result |
| :--- | :--- | :--- | :--- |
| `users` | 0 rows | **0 rows** | **PASS** |
| `stored_files` | 0 rows | **0 rows** | **PASS** |
| `file_metadata` | 0 rows | **0 rows** | **PASS** |
| `transfers` | 0 rows | **0 rows** | **PASS** |
| `activity_events` | 0 rows | **0 rows** | **PASS** |
| `backend/storage/encrypted/` | 0 files | **0 files** | **PASS** |
| `backend/storage/previews/` | 0 files | **0 files** | **PASS** |
| `backend/storage/profile_photos/`| 0 files | **0 files** | **PASS** |
| `backend/storage/uploads/` | 0 files | **0 files** | **PASS** |
| `storage/` (all subdirectories) | 0 files | **0 files** | **PASS** |

### 6.2 Schema & Constraint Integrity Audit
Schema definitions and constraints were queried from `information_schema.TABLE_CONSTRAINTS` to confirm that DDL structure was unchanged:

| Constraint Name | Table | Referenced Table | Status |
| :--- | :--- | :--- | :--- |
| `FK3w8bqvmn8ghb4bm40e8tm866v` | `activity_events` | `users` | **Intact** |
| `FKn613w9asl808q1elcbvukyqo5` | `file_metadata` | `stored_files` | **Intact** |
| `FKc0pxhjsng9h58ifnju7u0hol3` | `stored_files` | `users` | **Intact** |
| `FKcvgnuaekhfu7jkgfdjr2rsy6w` | `transfers` | `users` | **Intact** |

Column definitions, indexes, nullability, and primary key auto-increment behaviors remain identical to production specification.

---

## 7. Storage Limit & System Invariant Verification

Application configuration and source constants were inspected to verify that system invariants were preserved without modification:

1. **Vault User Storage Limit:**
   - Source: `backend/src/main/java/com/ciphervault/ciphervault/user/User.java` (Line 17)
   - Value: `DEFAULT_STORAGE_LIMIT = 10L * 1024L * 1024L * 1024L;` (**10 GB / 10,737,418,240 bytes**)
   - Status: **Verified Unchanged**

2. **Single / Batch Upload Limit:**
   - Source: `backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java` (Line 37)
   - Value: `MAX_FILE_SIZE = 1024L * 1024L * 1024L;` (**1 GB / 1,073,741,824 bytes**)
   - Multipart Max Request Size: `spring.servlet.multipart.max-request-size=2GB`
   - Status: **Verified Unchanged**

3. **Historical Backups & Evidence:**
   - `database/backups/ciphervault_backup_20261013_153755.sql` (548,270 bytes): **Verified Intact**
   - `evidence/logs/`, `evidence/screenshots/`, `evidence/test-results/`: **Verified Intact**

---

## 8. Live Application Lifecycle Smoke Verification

A full automated end-to-end integration test was executed against the clean environment using `verify_live_lifecycle.py`:

```text
=== LIVE BACKEND SMOKE VERIFICATION (CLEAN ENVIRONMENT) ===
1. Backend Startup: Started via mvnw.cmd spring-boot:run
   [OK] Backend health UP in 3s: components={backend: HEALTHY, database: HEALTHY, 
        storage: HEALTHY, encryption: HEALTHY, metadata: HEALTHY, transfers: HEALTHY}
2. Registration: POST /api/auth/register (clean_verifier_1791914809)
   [OK] Registration HTTP 200 in 0.32s
3. Authentication: POST /api/auth/login
   [OK] JWT Bearer token acquired successfully
4. File Upload: POST /api/files/upload (51-byte payload)
   [OK] Upload HTTP 200 -> Stored file ID 292, encrypted=True, SHA-256 computed
5. File Download: GET /api/files/292/download
   [OK] Download HTTP 200 -> Plaintext payload matches byte-for-byte
6. File Deletion: DELETE /api/files/292
   [OK] Deletion HTTP 200 -> Physical encrypted blob removed
7. Shutdown & Cleanup:
   [OK] Backend terminated cleanly
   [OK] Transactional deletion of verification account
   [OK] Database and storage confirmed: 0 records, 0 files
```

---

## 9. Python Server-Control Test Suite

The server control management test suite was executed to ensure the root desktop launcher remains healthy:

```text
python -m unittest discover -s server-control/tests -v
Ran 18 tests in 0.581s

OK (18/18 tests passed)
```

---

## 10. Git Status & Repository Cleanliness

A full Git status check was performed:

```text
git status --short --untracked-files=all
?? database/backups/ciphervault_backup_20261013_232658.sql
```

- **Zero modified tracked files.**
- **Zero deleted tracked files.**
- **No Git commits, resets, or rebases performed.**
- The only untracked file is the newly created pre-reset database backup.

---

## 11. Final Verification Checklist

- [x] Pre-reset timestamped backup created and verified (`database/backups/ciphervault_backup_20261013_232658.sql`).
- [x] Pre-existing backup preserved (`ciphervault_backup_20261013_153755.sql`).
- [x] All 25 test users and credentials removed from `users`.
- [x] All 89 stored file records removed from `stored_files`.
- [x] All 89 metadata entries removed from `file_metadata`.
- [x] All 130 transfer records removed from `transfers`.
- [x] All 6 activity log events removed from `activity_events`.
- [x] All 183 encrypted blobs removed from physical storage.
- [x] All 164 thumbnail previews removed from physical storage.
- [x] All 3 profile photos removed from physical storage.
- [x] All 190 orphaned files and temporary artifacts purged.
- [x] Directory hierarchy in `backend/storage/` and `storage/` preserved intact.
- [x] Database schema, tables, column types, and foreign key constraints 100% intact.
- [x] Vault storage limit (10 GB) and batch limit (1 GB) verified intact.
- [x] QA evidence preserved in `evidence/`.
- [x] Fresh application lifecycle verified via live backend test.
- [x] Python server-control suite passes 18/18 tests.
- [x] Repository clean state verified ready for new testing.
