# CipherVault — Application Screenshot Manifest

This document catalogs the genuine, high-resolution application screenshots curated for the final MCA project presentation. Every screenshot was captured from a live test runtime on an Android emulator (Google Pixel 10 Pro XL, API 37 / Android 17) connected to the local CipherVault Spring Boot 3.3.4 backend. No screenshots are synthetic, mockups, or fabricated.

---

## 1. Summary of Screenshots

| Index | Presentation Asset | Original Source Path | Functional Category | Intended Slide |
| :---: | :--- | :--- | :--- | :---: |
| **01** | `01_splash_screen.png` | `evidence/screenshots/onboarding/fig_01_splash.png` | Cold Start & Branding | Slide 1, 12 |
| **02** | `02_onboarding_welcome.png` | `evidence/screenshots/onboarding/fig_02_onboarding_1.png` | Onboarding Walkthrough | Slide 12 |
| **03** | `03_server_connection.png` | `evidence/screenshots/onboarding/fig_07_server_connection.png` | Backend Network Setup | Slide 8, 12 |
| **04** | `04_connection_verified.png` | `evidence/screenshots/onboarding/fig_08_connection_healthy.png` | Health Probe (200 OK) | Slide 8, 13 |
| **05** | `05_login_screen.png` | `evidence/screenshots/authentication/fig_10_login.png` | JWT Authentication | Slide 6, 12 |
| **06** | `06_signup_screen.png` | `evidence/screenshots/authentication/fig_11_signup.png` | User Registration & Validation | Slide 9, 12 |
| **07** | `07_app_lock_security.png` | `evidence/screenshots/authentication/fig_12_app_lock.png` | Biometric/PIN AppLock | Slide 10, 12 |
| **08** | `08_home_dashboard.png` | `evidence/screenshots/home-and-vault/fig_14_home.png` | 10 GB Storage Dashboard | Slide 9, 12 |
| **09** | `09_vault_file_grid.png` | `evidence/screenshots/home-and-vault/fig_15_vault_grid.png` | Vault File Explorer (2-Col) | Slide 9, 12 |
| **10** | `10_upload_staging_queue.png` | `evidence/screenshots/upload-and-download/fig_20_upload_staging.png` | Staging Queue (1 GB Limit) | Slide 6, 12 |
| **11** | `11_staged_preview_modal.png` | `evidence/screenshots/upload-and-download/fig_22_staged_preview_dialog.png` | Pre-Upload Inspection Modal | Slide 6, 9 |
| **12** | `12_transfers_monitor.png` | `evidence/screenshots/upload-and-download/fig_25_transfers.png` | Foreground Transfer Monitor | Slide 9, 12 |
| **13** | `13_download_decryption_dialog.png` | `evidence/screenshots/upload-and-download/download_dialog_decrypt_on_light.png` | Decryption on Download | Slide 6, 10 |
| **14** | `14_file_details_bottom_sheet.png` | `evidence/screenshots/file-details-and-metadata/fig_18_file_details_sheet.png` | Expandable Forensic Details | Slide 9, 12 |
| **15** | `15_metadata_search_chips.png` | `evidence/screenshots/search/fig_27_metadata_search.png` | Search & Dynamic Filter Chips | Slide 9, 12 |
| **16** | `16_metadata_search_results.png` | `evidence/screenshots/search/fig_28_metadata_results.png` | Filtered Facet Search Results | Slide 9, 12 |
| **17** | `17_user_profile_settings.png` | `evidence/screenshots/settings-and-profile/fig_29_profile.png` | Profile & Storage Quota | Slide 12 |
| **18** | `18_settings_and_diagnostics.png` | `evidence/screenshots/settings-and-profile/fig_31_settings_about.png` | Diagnostics & Session Info | Slide 12, 13 |
| **19** | `19_material3_ui_showcase.png` | `evidence/screenshots/settings-and-profile/fig_32_ui_showcase.png` | Material 3 Design Tokens | Slide 8, 12 |
| **20** | `20_deletion_staging_update.png` | `evidence/screenshots/deletion/fig_24_staged_after_delete.png` | Cascading Deletion & Quota | Slide 9, 13 |

---

## 2. Detailed Screen Descriptions and Presenter Guidance

### Screenshot 01: `01_splash_screen.png`
- **Functional Context:** Displays cold-start initialization, asset preloading, and active session check.
- **Visual Design:** Centered CipherVault padlock brand icon against the warm cream background with the slogan *"Secure. Private. Intelligent File Storage."*
- **Presentation Use:** Ideal for Slide 1 (Title) or Slide 12 (App Showcase) to introduce the system identity.
- **Implementation Evidence:** [`SplashActivity.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/SplashActivity.java).

### Screenshot 02: `02_onboarding_welcome.png`
- **Functional Context:** Introductory walkthrough page outlining local cryptographic sovereignty and private cloud architecture.
- **Visual Design:** Material 3 card container, stylized vector illustration, and smooth page indicators.
- **Presentation Use:** Slide 12 (User Interface) to demonstrate user experience design.
- **Implementation Evidence:** [`OnboardingActivity.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/onboarding/OnboardingActivity.java).

### Screenshot 03: `03_server_connection.png` & Screenshot 04: `04_connection_verified.png`
- **Functional Context:** Configures backend host IP, port (8080), and SSL toggles. Performs active network health probe against `/api/health` or `/actuator/health`.
- **Key Evidence:** In Screenshot 04, status indicates **"Connected"** with real latency readout (`226 ms`), proving live network communication before user login.
- **Presentation Use:** Slide 8 (Technology Stack) or Slide 12/13 (Testing & Setup).

### Screenshot 05: `05_login_screen.png` & Screenshot 06: `06_signup_screen.png`
- **Functional Context:** Registration and authentication endpoints. Registration validates username uniqueness, email format, and password complexity (uppercase, lowercase, number, special character, min 8 chars).
- **Key Evidence:** Login issues a stateless JWT stored in hardware-backed `EncryptedSharedPreferences`.
- **Presentation Use:** Slide 10 (Security & Authentication) and Slide 12 (User Interface).

### Screenshot 07: `07_app_lock_security.png`
- **Functional Context:** Cold-start and background-resume app lock overlay requiring biometric fingerprint or PIN challenge.
- **Key Evidence:** Binds to Android BiometricPrompt and KeyStore StrongBox/TEE.
- **Presentation Use:** Slide 10 (Security Architecture).

### Screenshot 08: `08_home_dashboard.png`
- **Functional Context:** Central hub displaying the 10 GB storage quota progress bar, category breakdown (Photos, Videos, Documents, Audio, Other), and quick-action shortcuts.
- **Presentation Use:** Slide 9 (Core Features) and Slide 12 (UI Walkthrough).

### Screenshot 09: `09_vault_file_grid.png`
- **Functional Context:** Two-column responsive card grid displaying stored vault items, encryption badge indicators, file size, and direct action buttons (Download, Delete).
- **Key Evidence:** Demonstrates that files have direct action buttons rather than hidden nested menus.
- **Presentation Use:** Slide 9 (Core Features).

### Screenshot 10: `10_upload_staging_queue.png` & Screenshot 11: `11_staged_preview_modal.png`
- **Functional Context:** Pre-upload staging queue allowing batch file selection, total batch size calculation against the 1 GB batch limit, and modal image preview before triggering upload.
- **Key Evidence:** Addresses the critical "metadata paradox" and user inspection gap identified in existing cloud storage systems.
- **Presentation Use:** Slide 5/6 (Proposed System) and Slide 9 (Features).

### Screenshot 12: `12_transfers_monitor.png`
- **Functional Context:** Live transfer tab tracking active uploads, transferred byte counts, streaming progress, and completed historical transfer batches.
- **Key Evidence:** Enforces constant-memory streaming I/O with foreground service notification.
- **Presentation Use:** Slide 9 (Features) and Slide 13 (Results).

### Screenshot 13: `13_download_decryption_dialog.png`
- **Functional Context:** Download dialog featuring the toggle switch **"Decrypt on download"**.
- **Key Evidence:** Proves that AES-256-GCM client-side/streaming decryption is fully implemented and operational, superseding older first-review slides that noted decryption as planned.
- **Presentation Use:** Slide 6 (Proposed System), Slide 10 (Security & Encryption).

### Screenshot 14: `14_file_details_bottom_sheet.png`
- **Functional Context:** Expandable Material 3 bottom sheet showing comprehensive metadata: original filename, content type, SHA-256 integrity hash, storage timestamp, camera make/model, ISO, aperture, resolution, and audio/video codecs.
- **Presentation Use:** Slide 9 (Core Features) and Slide 11 (Database Design).

### Screenshot 15: `15_metadata_search_chips.png` & Screenshot 16: `16_metadata_search_results.png`
- **Functional Context:** Search interface showing real-time suggestion chips derived dynamically from stored files (e.g., "Pixel 8", "1920x1080", "H.264", "PDF") and faceted search results.
- **Presentation Use:** Slide 9 (Core Features) and Slide 12 (UI Showcase).

### Screenshot 17: `17_user_profile_settings.png` & Screenshot 18: `18_settings_and_diagnostics.png`
- **Functional Context:** Profile photo customization, used storage quota metrics, theme mode selection (System/Light/Dark), server configuration, and secure session sign out.
- **Presentation Use:** Slide 12 (UI & Settings).

### Screenshot 19: `19_material3_ui_showcase.png`
- **Functional Context:** Live UI component showcase demonstrating Material 3 dynamic color tokens, surface elevations, rounded container radii, and buttons.
- **Presentation Use:** Slide 8 (Technology Stack) and Slide 12 (Design System).

### Screenshot 20: `20_deletion_staging_update.png`
- **Functional Context:** Cascading deletion confirmation dialog and real-time storage quota decrement upon file deletion.
- **Presentation Use:** Slide 9 (File Lifecycle) and Slide 13 (Testing).
