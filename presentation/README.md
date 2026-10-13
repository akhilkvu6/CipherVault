# CipherVault — Final MCA Presentation Package & Slide Deck

Welcome to the presentation package for **CipherVault — Private Cloud Storage with AES Encryption, SHA-256 Duplicate Detection, and Metadata-Based Search**.

This package contains the complete, authoritative, and verified 16-slide presentation deck:
- [`CipherVault_Final_MCA_Presentation.pptx`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/CipherVault_Final_MCA_Presentation.pptx) — Fully editable 16-slide presentation (16:9, Times New Roman, native tables, complete notes, Google Slides compatible).
- [`CipherVault_Final_MCA_Presentation.pdf`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/CipherVault_Final_MCA_Presentation.pdf) — High-fidelity 16-page PDF export generated via headless Impress.
- The supporting asset library: diagrams, tables, screenshots, academic references, data dictionaries, and editable source files.

> [!IMPORTANT]
> **Google Slides Direct Import Guide:**
> Because programmatic access to the user's private Google account requires an interactive authenticated OAuth session (no Google credentials/plugins are connected in this local environment), `CipherVault_Final_MCA_Presentation.pptx` was engineered with 100% native shapes, selectable text, and structured tables.
> **To open in Google Slides:**
> 1. Open [Google Drive](https://drive.google.com) or [Google Slides](https://slides.google.com).
> 2. Click **New (+)** &rarr; **File upload** (or File &rarr; Open &rarr; Upload in Google Slides).
> 3. Select [`CipherVault_Final_MCA_Presentation.pptx`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/CipherVault_Final_MCA_Presentation.pptx).
> 4. Google Slides converts the deck seamlessly into a live Google Slides presentation with every shape, card, text box, table cell, and speaker note remaining 100% editable.

---

## 1. Project & Presenter Metadata

- **Project Title:** CipherVault: Private Cloud Storage with AES Encryption, SHA-256 Duplicate Detection, and Metadata-Based Search
- **Degree:** Master of Computer Applications (MCA)
- **Presenter:** Akhil Krishna V U
- **Register Number:** TVE25MCA-2013
- **Project Guide:** Prof. Deepa S.
- **Academic Term / Evaluation Date:** 14 October 2026
- **Target Presentation Format:** 16:9 Widescreen (1920 × 1080 / 13.333 in × 7.5 in)
- **Primary Typography:** Times New Roman (Serif, 100% enforced across all 308 text runs)
- **Color Palette:**
  - Background: Near-White Warm Cream (`#FAF9F6`)
  - Text & Accents: Near-Black (`#171513`), Dark Brown (`#332B27`)
  - Accent / Borders / Headers: Warm Muted Brown (`#65483A`)
  - Container / Card Fill: Warm Taupe / Cream (`#F4EFEA`, `#E4DCD1`)

---

## 2. Directory Structure

```text
presentation/
├── CipherVault_Final_MCA_Presentation.pptx <- Fully editable 16-slide presentation (Google Slides compatible)
├── CipherVault_Final_MCA_Presentation.pdf  <- High-fidelity 16-page PDF export
├── README.md                           <- This documentation & assembly guide
├── QA_CHECKLIST.md                     <- Visual QA, XML verification, & accuracy audit
├── asset_manifest.csv                  <- Complete catalog of all 62 presentation assets
├── slide_content.md                    <- 16-slide blueprints: bullets, notes, and pairings
├── repository_audit.md                 <- Codebase audit resolving all documentation gaps
├── implementation_findings.md          <- Verified technical facts, quotas, and crypto paths
├── database_schema_reference.md        <- Complete data dictionary of the 5 MySQL tables
├── screenshot_manifest.md              <- Functional guide to the 20 runtime screenshots
│
├── assets/
│   ├── branding/
│   │   └── ciphervault_icon.png        <- Official app icon (cloud, folder & padlock)
│   ├── diagrams/                       <- 11 diagrams (both vector SVG and 1920x1080 PNG)
│   │   ├── system_architecture.[svg|png]
│   │   ├── dfd_level0.[svg|png]
│   │   ├── dfd_level1.[svg|png]
│   │   ├── er_diagram.[svg|png]
│   │   ├── upload_workflow.[svg|png]
│   │   ├── download_workflow.[svg|png]
│   │   ├── authentication_flow.[svg|png]
│   │   ├── security_pipeline.[svg|png]
│   │   ├── use_case_diagram.[svg|png]
│   │   ├── module_map.[svg|png]
│   │   └── storage_layout.[svg|png]
│   ├── tables/                         <- 5 comparative & summary tables (SVG and PNG)
│   │   ├── existing_vs_proposed.[svg|png]
│   │   ├── technology_stack.[svg|png]
│   │   ├── database_summary.[svg|png]
│   │   ├── feature_matrix.[svg|png]
│   │   └── testing_results.[svg|png]
│   └── screenshots/                    <- 20 authentic runtime Android client screenshots
│       ├── 01_splash_screen.png
│       ├── 02_onboarding_welcome.png
│       ├── 03_server_connection.png
│       ├── 04_connection_verified.png
│       ├── 05_login_screen.png
│       ├── 06_signup_screen.png
│       ├── 07_app_lock_security.png
│       ├── 08_home_dashboard.png
│       ├── 09_vault_file_grid.png
│       ├── 10_upload_staging_queue.png
│       ├── 11_staged_preview_modal.png
│       ├── 12_transfers_monitor.png
│       ├── 13_download_decryption_dialog.png
│       ├── 14_file_details_bottom_sheet.png
│       ├── 15_metadata_search_chips.png
│       ├── 16_metadata_search_results.png
│       ├── 17_user_profile_settings.png
│       ├── 18_settings_and_diagnostics.png
│       ├── 19_material3_ui_showcase.png
│       └── 20_deletion_staging_update.png
│
├── references/
│   ├── existing_systems_review.md      <- Academic literature review & comparison
│   └── technical_references.md         <- Standards (NIST, FIPS, RFCs) & crypto specs
│
└── sources/                            <- Editable source representations
    ├── authentication_flow.mmd
    ├── dfd_level0.mmd
    ├── dfd_level1.mmd
    ├── download_workflow.mmd
    ├── er_diagram.mmd
    ├── module_map.mmd
    ├── security_pipeline.mmd
    ├── storage_layout.mmd
    ├── system_architecture.mmd
    ├── upload_workflow.mmd
    ├── use_case_diagram.puml
    └── tables/
        ├── database_summary.csv
        ├── existing_vs_proposed.csv
        ├── feature_matrix.csv
        ├── technology_stack.csv
        └── testing_results.csv
```

---

## 3. Slide-by-Slide Assembly Mapping (16 Slides)

Refer to [`slide_content.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/slide_content.md) for the exact bullet text, oral script, and presenter notes for each slide. Below is the asset pairing blueprint:

| Slide # | Slide Title | Recommended Visual Asset(s) | Slide Content File Section |
| :---: | :--- | :--- | :--- |
| **1** | Title & Project Overview | `assets/branding/ciphervault_icon.png` | Slide 1: Project Title & Introduction |
| **2** | Problem Statement & Motivation | `assets/branding/ciphervault_icon.png` (corner watermark/logo) | Slide 2: Problem Statement & Motivation |
| **3** | Literature Survey & Existing Systems | `assets/tables/existing_vs_proposed.png` | Slide 3: Literature Survey & Existing Systems |
| **4** | Objectives & Proposed Scope | Custom 3-column objective cards | Slide 4: Objectives & Proposed Scope |
| **5** | System Architecture & Technology Stack | `assets/diagrams/system_architecture.png` + `assets/tables/technology_stack.png` | Slide 5: System Architecture & Technology Stack |
| **6** | Data Flow Architecture (DFD L0 & L1) | `assets/diagrams/dfd_level0.png` + `assets/diagrams/dfd_level1.png` | Slide 6: Data Flow Architecture |
| **7** | Database Design & ER Model | `assets/diagrams/er_diagram.png` + `assets/tables/database_summary.png` | Slide 7: Database Design & ER Model |
| **8** | Storage Pipeline & Deduplication | `assets/diagrams/upload_workflow.png` + `assets/diagrams/storage_layout.png` | Slide 8: Storage Pipeline & Deduplication |
| **9** | Retrieval & Streaming Decryption | `assets/diagrams/download_workflow.png` + `assets/screenshots/13_download_decryption_dialog.png` | Slide 9: Retrieval & Streaming Decryption |
| **10** | Security & Cryptographic Model | `assets/diagrams/security_pipeline.png` + `assets/diagrams/authentication_flow.png` | Slide 10: Security & Cryptographic Model |
| **11** | Modules & Functional Architecture | `assets/diagrams/use_case_diagram.png` + `assets/tables/feature_matrix.png` | Slide 11: Modules & Functional Architecture |
| **12** | Live Application Walkthrough — Part 1 | `assets/screenshots/01_splash_screen.png`, `03_server_connection.png`, `08_home_dashboard.png`, `10_upload_staging_queue.png` | Slide 12: Application Walkthrough Part 1 |
| **13** | Live Application Walkthrough — Part 2 | `assets/screenshots/12_transfers_monitor.png`, `14_file_details_bottom_sheet.png`, `15_metadata_search_chips.png`, `17_user_profile_settings.png` | Slide 13: Application Walkthrough Part 2 |
| **14** | Testing, Verification & Results | `assets/tables/testing_results.png` | Slide 14: Testing, Verification & Results |
| **15** | Limitations & Future Enhancements | Custom 2-column roadmap cards | Slide 15: Limitations & Future Scope |
| **16** | Conclusion & References | `assets/branding/ciphervault_icon.png` | Slide 16: Conclusion & References |

---

## 4. Key Ground-Truth Architectural Clarifications

During the comprehensive audit of the CipherVault codebase, three key areas where previous review documents had become outdated were verified and corrected:

1. **Download Decryption is Fully Operational:**
   - *Previous Review Document Claim:* Download decryption was listed as a "planned future enhancement".
   - *Codebase Truth:* The backend actively implements streaming decryption in `FileStorageService.java` (`GET /api/files/{id}/download?decrypt=true`). It uses `CipherInputStream` with AES-256-GCM and verifies the 128-bit GHASH tag in real time. The Android app features an explicit toggle switch (*"Decrypt on download"* in `FileDetailBottomSheet.java`).
2. **Metadata Extraction is Fully Operational:**
   - *Previous Review Document Claim:* ExifTool metadata extraction was listed as "future work".
   - *Codebase Truth:* `MetadataExtractionService.java` actively extracts up to 29 technical and EXIF metadata attributes upon upload and persists them into the `file_metadata` MySQL table. The Android search interface includes instant search chips for Camera, ISO, and Resolution.
3. **Per-User Deduplication Eliminates Side Channels:**
   - *Security Design:* Deduplication is strictly scoped per user via a composite unique constraint `UNIQUE(user_id, sha256_hash)` on `stored_files`. Cross-user deduplication is deliberately avoided to prevent side-channel file-probing attacks.
4. **Binary Separation:**
   - Zero binary file bytes are stored in MySQL. Ciphertext blobs reside in `storage/encrypted/{uuid}` on disk. The database stores only metadata, wrapped per-user keys, hashes, and foreign keys.

---

## 5. How to Regenerate or Modify Assets

- **Editable Sources:**
  - Mermaid diagram sources are in `presentation/sources/*.mmd`.
  - PlantUML use-case diagram source is in `presentation/sources/use_case_diagram.puml`.
  - Table sources are in `presentation/sources/tables/*.csv`.
- **SVG & PNG Rendering:**
  - All diagrams and tables are rendered using SVG vector markup adhering strictly to the **Times New Roman** and **Warm Cream** theme.
  - High-resolution 1920x1080 PNG files are rendered via Google Chrome headless (`--headless=new --screenshot=... --window-size=1920,1080`).
  - To regenerate any diagram or table, edit the corresponding SVG or run the Python generation scripts located in the agent scratchpad.

---

## 6. Checklist Before Presenting

- [x] Confirm all 20 screenshots are copied into `presentation/assets/screenshots/`.
- [x] Confirm all 11 diagram PNGs and 5 table PNGs are rendered at 1920x1080.
- [x] Confirm `slide_content.md` has been reviewed for presentation speech timing (15–20 minutes).
- [x] Verify that presenter details (Akhil Krishna V U, TVE25MCA-2013, Prof. Deepa S.) are populated on Title Slide 1.
- [x] Use [`asset_manifest.csv`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/asset_manifest.csv) as an inventory index when importing slides into PowerPoint or Google Slides.
