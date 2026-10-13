# CIPHERVAULT — REPOSITORY CLEANUP & FILE REORGANIZATION PLAN

**Date:** October 13, 2026  
**Author:** Antigravity Senior Software Engineer & Autonomous Systems Lead  
**Objective:** Reorganize the entire CipherVault repository into a clean, professional, and maintainable project structure conforming to the target schema.

---

## 1. Baseline State & Findings

### 1.1 Git Status & Safety Verification
- Working tree: clean on branch `main`.
- Ignored paths: `.idea/`, `.gradle/`, `build/`, `target/`, `storage/`, `logs/`.
- No user work will be discarded or overwritten.

### 1.2 Inventory of Clutter & Misplaced Items
1. **Root Directory Clutter:**
   - Report generation scripts: `build_full_report.py`, `generate_diagrams.py`.
   - Report generator package: `report_generator/` directory.
   - Server control desktop utility: `server-control/` directory.
   - Runtime logs: `logs/` directory (`ciphervault.log`, `ciphervault-error.log`).
   - Legacy test folders: `CipherVault_Final_QA_Evidence/`, `CipherVault_Final_UI_Regression_2/`.
2. **Scattered Screenshots & Evidence:**
   - 243 screenshots spread across 4 distinct locations:
     - `CipherVault_Final_QA_Evidence/screenshots/` (23 files)
     - `CipherVault_Final_UI_Regression_2/` (157 files in 13 subfolders)
     - `docs/archive/historical_evidence/` (31 files)
     - `docs/project-report/screenshots/` (32 files)
   - Redundant duplicate screenshots detected across inventory folders sharing identical SHA-256 hashes.
3. **Fragmented Documentation Folders:**
   - `docs/` has multiple disjointed folders: `api/`, `architecture/`, `archive/`, `audits/`, `database/`, `project-report/`, `reference/`, `reports/`, `security/`, `setup/`, `testing/`, plus loose root files (`CONNECTION.md`, `HOW_TO_RUN.md`).

---

## 2. Target Directory Hierarchy

```text
CipherVault/
├── README.md
├── .gitignore
├── CIPHERVAULT_HOW_TO_RUN.txt
├── android/
│   ├── app/
│   │   └── src/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── gradlew / gradlew.bat
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── required configuration files
├── database/
│   ├── schema.sql
│   ├── migrations/
│   └── backups/
├── scripts/
│   └── documentation/
├── server-control/
│   ├── main.py
│   ├── requirements.txt
│   ├── settings.json
│   ├── app/
│   └── tests/
├── docs/
│   ├── architecture/
│   ├── setup-and-usage/
│   ├── api-and-database/
│   ├── security-and-encryption/
│   ├── testing-and-qa/
│   └── project-reports/
└── evidence/
    ├── screenshots/
    │   ├── onboarding/
    │   ├── authentication/
    │   ├── home-and-vault/
    │   ├── upload-and-download/
    │   ├── file-details-and-metadata/
    │   ├── search/
    │   ├── settings-and-profile/
    │   ├── deletion/
    │   └── errors-and-diagnostics/
    ├── logs/
    ├── test-results/
    └── test-data/
```

---

## 3. Detailed Action & Movement Plan

### Phase 1: Establish Target Directory Layout
Create:
- `scripts/build/`, `scripts/testing/`, `scripts/documentation/`
- `database/migrations/`
- `docs/setup-and-usage/`, `docs/api-and-database/`, `docs/security-and-encryption/`, `docs/testing-and-qa/`, `docs/project-reports/`
- `evidence/screenshots/onboarding/`
- `evidence/screenshots/authentication/`
- `evidence/screenshots/home-and-vault/`
- `evidence/screenshots/upload-and-download/`
- `evidence/screenshots/file-details-and-metadata/`
- `evidence/screenshots/search/`
- `evidence/screenshots/settings-and-profile/`
- `evidence/screenshots/deletion/`
- `evidence/screenshots/errors-and-diagnostics/`
- `evidence/logs/`, `evidence/test-results/`, `evidence/test-data/`

### Phase 2: Consolidate Screenshots into `evidence/screenshots/`
- Compare SHA-256 hashes to retain canonical descriptive filenames.
- Categorize each unique screenshot into one of the 9 defined subfolders.
- Deduplicate exact clones between inventory folders and document them.

### Phase 3: Relocate Evidence Artifacts, Dumps, and Logs
- Move `logs/` at root to `evidence/logs/`.
- Move `CipherVault_Final_QA_Evidence/logs/` and `CIPHERVAULT_TEST_EVENT_LOG.txt` to `evidence/logs/`.
- Move `CipherVault_Final_QA_Evidence/test-data/` to `evidence/test-data/`.
- Move `CipherVault_Final_QA_Evidence/final-debug-apk/` and `*.xml` layout dumps to `evidence/test-results/`.
- Safely remove now-empty legacy staging folders (`CipherVault_Final_QA_Evidence/`, `CipherVault_Final_UI_Regression_2/`).

### Phase 4: Consolidate Documentation under `docs/`
- Move setup docs to `docs/setup-and-usage/` (`SETUP_GUIDE.md`, `CONNECTION.md`, `HOW_TO_RUN.md`, `CIPHERVAULT_SETUP_CONNECTION_AND_TROUBLESHOOTING.md`).
- Move API & DB docs to `docs/api-and-database/` (`API_SPECIFICATION.md`, `DATABASE_SCHEMA.md`, `CIPHERVAULT_BACKEND_API_DATABASE_REFERENCE.md`).
- Move security docs to `docs/security-and-encryption/` (`SECURITY_MODEL.md`, `CIPHERVAULT_SECURITY_AND_ENCRYPTION.md`).
- Move testing docs to `docs/testing-and-qa/` (`CIPHERVAULT_COMPREHENSIVE_TEST_PLAN.md`, `final_detailed_forensic_debug_report.md`, audit ledger, inventory, implementation plan).
- Move report docs to `docs/project-reports/` (`CIPHERVAULT_FINAL_VERIFICATION_REPORT.md`, `CIPHERVAULT_SCREEN_BY_SCREEN_IMPLEMENTATION.md`, `CipherVault_Project_Report_Source.md`, `report_print.html`, `REPORT_VERIFICATION.md`, `diagrams/`).
- Remove empty legacy topic directories (`docs/api/`, `docs/setup/`, `docs/security/`, `docs/database/`, `docs/reports/`, `docs/reference/`, `docs/audits/`, `docs/testing/`).

### Phase 5: Reorganize Scripts
- Move `build_full_report.py`, `generate_diagrams.py`, and `report_generator/` into `scripts/documentation/`.
- Maintain `server-control/` at the repository root directly beside `run.bat` for convenient 1-click execution.

### Phase 6: Update References and Authoritative Documentation
- Update `scripts/documentation/build_full_report.py` and `scripts/documentation/report_generator/` paths to reflect new locations.
- Update `README.md` and `CIPHERVAULT_HOW_TO_RUN.txt` with updated paths.
- Update `report_print.html` and markdown references to screenshots.

### Phase 7: Verification & Build Validation
- Run `.\gradlew.bat testDebugUnitTest` in `android/`.
- Run `.\mvnw.cmd test` in `backend/`.
- Verify all references resolve.
