# CipherVault — 16-Slide Presentation Content Blueprint

**Project Title:** CipherVault — Private Cloud Storage with AES Encryption, SHA-256 Duplicate Detection, and Metadata-Based Search  
**Presenter:** Akhil Krishna V U (Reg No: TVE25MCA-2013)  
**Project Guide:** Prof. Deepa S.  
**Academic Programme:** Master of Computer Applications (MCA)  
**Date:** October 2026  

---

## Slide 1: Title Slide

### Slide Content
* **Title:** **CipherVault**
* **Subtitle:** Private Cloud Storage with AES Encryption, SHA-256 Duplicate Detection, and Metadata-Based Search
* **Academic Credentials:**
  * **Presenter:** Akhil Krishna V U (Reg No: TVE25MCA-2013)
  * **Project Guide:** Prof. Deepa S.
  * **Department:** Department of Computer Applications (MCA)
* **Tagline Badge:** *Secure. Private. Intelligent File Storage.*

### Key Takeaway
CipherVault is a sovereign, self-hosted private cloud storage system designed to return cryptographic custody and forensic file intelligence to the individual user.

### Recommended Asset
* [`presentation/assets/branding/ciphervault_icon.png`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/branding/ciphervault_icon.png)
* [`presentation/assets/screenshots/01_splash_screen.png`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/screenshots/01_splash_screen.png)

### Presenter Notes
Introduce the project title with emphasis on the three core pillars: zero-knowledge AES encryption, privacy-preserving SHA-256 duplicate detection, and rich metadata-based file management. Emphasize that the project combines an Android client with a Spring Boot backend.

### Technical Source Evidence
[`README.md#L1-L15`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/README.md).

---

## Slide 2: Abstract / Project Overview

### Slide Content
* **Core Paradigm:** Replaces third-party commercial cloud dependence with a private, sovereign Android and Spring Boot microservice architecture.
* **Cryptographic Foundation:** Implements NIST SP 800-38D authenticated AES-256-GCM envelope encryption with hardware-backed PBKDF2 key management.
* **Storage Optimization:** Features atomic per-user SHA-256 duplicate detection, strictly isolating tenants to prevent side-channel leakage.
* **Forensic Intelligence:** Extracts 29 forensic media attributes via ExifTool (camera optics, resolution, audio/video codecs) into a decoupled relational catalog.
* **Production Integrity:** Tested with 211 passing automated tests and verified on Android 17 / API 37 with a 10 GB quota ceiling and 1 GB upload limits.

### Key Takeaway
CipherVault solves the "metadata paradox" in secure cloud storage: files remain strongly encrypted while their decoupled forensic metadata enables instant, rich searching.

### Recommended Asset
* [`presentation/assets/diagrams/system_architecture.svg`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/diagrams/system_architecture.svg) / `.png`

### Presenter Notes
Briefly state that while ordinary cloud vaults turn uploaded files into opaque blobs that cannot be previewed or searched, CipherVault safely parses metadata prior to encryption, indexing it in MySQL while encrypting the actual payload on the private filesystem.

### Technical Source Evidence
[`docs/architecture/CIPHERVAULT_SYSTEM_ARCHITECTURE.md#L15-L45`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/architecture/CIPHERVAULT_SYSTEM_ARCHITECTURE.md).

---

## Slide 3: Problem Statement

### Slide Content
* **Loss of Cryptographic Sovereignty:** Commercial public clouds (Google Drive, Dropbox) hold encryption keys server-side, enabling corporate data mining and exposure to third-party data breaches.
* **The Metadata Paradox:** Client-side encryption utilities (Cryptomator) turn files into opaque blobs, making media previews and metadata searching impossible without insecure local exports.
* **Side-Channel Deduplication Hazards:** Cross-user cloud deduplication allows malicious attackers to probe whether sensitive files already exist in other users' vaults.
* **Mobile Ingestion Blindness:** Conventional apps lack pre-upload inspection, pushing files blindly to the network without verifying batch size quotas or forensic content.

### Key Takeaway
Modern users are forced to choose between the convenience of commercial clouds (which lack true privacy) and the extreme friction of client-side encryption utilities (which destroy searchability).

### Recommended Asset
* [`presentation/assets/tables/existing_vs_proposed.svg`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/tables/existing_vs_proposed.svg) / `.png`

### Presenter Notes
Highlight the trade-off. Mention that students, journalists, and privacy-conscious professionals need a cloud system they can host on their own machine, where they can search camera EXIF and video codecs without surrendering plaintext to a third party.

### Technical Source Evidence
[`presentation/references/existing_systems_review.md#L68-L115`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/references/existing_systems_review.md).

---

## Slide 4: Objectives

### Slide Content
* **Primary Objective:** Develop a secure, self-hosted private cloud storage system providing authenticated encryption, deduplication, and metadata indexing across Android and Spring Boot.
* **Specific Objective 1 — Sovereign Ingestion:** Enforce constant-memory chunked streaming (16 KB buffers, up to 2 GB payloads) with strict 10 GB user quotas and 1 GB batch limits.
* **Specific Objective 2 — Authenticated Cryptography:** Implement AES-256-GCM envelope encryption with 128-bit GHASH authentication tags and PBKDF2 (65,536 iterations).
* **Specific Objective 3 — Side-Channel-Resistant Deduplication:** Restrict SHA-256 duplicate detection to the authenticated user's scope via database unique constraints.
* **Specific Objective 4 — Decoupled Metadata & Search:** Extract rich EXIF and codec attributes via ExifTool and provide dynamic suggestion chips and faceted search.
* **Specific Objective 5 — Dual-Mode Streaming Download:** Provide authenticated streaming on-the-fly decryption or raw `.enc` ciphertext export.

### Key Takeaway
CipherVault achieves a balanced security architecture: sovereign custody, authenticated encryption, high-throughput streaming, and forensic searchability.

### Recommended Asset
* [`presentation/assets/diagrams/module_map.svg`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/diagrams/module_map.svg) / `.png`

### Presenter Notes
Emphasize that every objective listed here is fully implemented in the current code and verified through automated test suites.

### Technical Source Evidence
[`docs/project-reports/CipherVault_Project_Report_Source.md#L55-L62`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CipherVault_Project_Report_Source.md).

---

## Slide 5: Existing System and Proposed System

### Slide Content
* **Commercial Clouds (Google Drive, Dropbox):**
  * *Studied Limitation:* Keys held by vendor; data scanned server-side; cross-user deduplication leaks ownership.
* **Self-Hosted Monoliths (Nextcloud):**
  * *Studied Limitation:* Heavy LAMP stack; resource-intensive; mobile client lacks pre-upload staging and forensic search.
* **Client Encryption Wrappers (Cryptomator):**
  * *Studied Limitation:* Opaque virtual drives; impossible to search EXIF or preview images without external app intents.
* **CipherVault Solution:**
  * Combines self-hosted sovereignty (Ubuntu/Linux or local host) with decoupled forensic metadata indexing and zero-knowledge AES-256-GCM streaming.

### Key Takeaway
CipherVault bridges the gap between privacy and usability by decoupling metadata indexing from physical ciphertext storage.

### Recommended Asset
* Table: **Existing System vs. Proposed CipherVault** (`presentation/assets/tables/existing_vs_proposed.svg` / `.png`)

### Presenter Notes
Direct the audience's attention to the comparison table. Mention that research into Yang et al. (2017) and Liu et al. (2015) highlighted the risks of cross-user deduplication, which CipherVault solves by binding deduplication strictly to the authenticated user.

### Technical Source Evidence
[`presentation/references/existing_systems_review.md#L118-L135`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/references/existing_systems_review.md).

---

## Slide 6: Proposed System and Methodology

### Slide Content
* **Architecture Pipeline:** Android Client (Java 17, Material 3) &rarr; HTTP/1.1 REST API &rarr; Spring Boot 3.3.4 &rarr; MySQL 8.0 & Filesystem.
* **Pre-Upload Staging Queue:** Android client validates batch size against 1 GB limit, allows modal preview inspection, and enables item removal before upload.
* **Ingestion Pipeline:** File streams into memory buffer &rarr; SHA-256 computed &rarr; per-user duplicate check &rarr; ExifTool metadata extracted &rarr; AES-256-GCM encrypted &rarr; saved to `storage/encrypted/`.
* **Decoupled Relational Persistence:** File record saved to `stored_files`; 29 forensic fields saved to `file_metadata`; quota updated atomically in `users`.
* **Dual-Mode Retrieval:** Downloads stream decrypted bytes using `CipherInputStream` (default) or raw `.enc` ciphertext if decryption is toggled off.

### Key Takeaway
A multi-tier pipeline guarantees that files are never stored unencrypted, duplicates are rejected before writing, and metadata is indexed without compromising payload security.

### Recommended Asset
* Flowchart: **Upload & Ingestion Workflow** (`presentation/assets/diagrams/upload_workflow.svg` / `.png`)
* Screenshot: **Download Decryption Dialog** (`presentation/assets/screenshots/13_download_decryption_dialog.png`)

### Presenter Notes
Clarify that in our first review, decryption on download was noted as planned. It is now completely implemented in the code with on-the-fly streaming decryption.

### Technical Source Evidence
[`FileStorageService.java#L100-L460`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java).

---

## Slide 7: System Architecture

### Slide Content
* **Tier 1 — Presentation Layer:** Android Native Client built with Java Views, Material Design 3, `EncryptedSharedPreferences`, and OkHttp/Retrofit REST pipelines.
* **Tier 2 — Security & Transport Layer:** Spring Security 6 stateless filter chain, JWT Bearer token authentication, and sliding-window brute-force rate limiter.
* **Tier 3 — Application Services:** Java 21 LTS Spring Boot services (`FileStorageService`, `KeyManagementService`, `MetadataExtractionService`, `FileSearchService`).
* **Tier 4 — Persistence Layer:** MySQL 8.0 InnoDB database for user accounts and metadata + Local Filesystem directory tree for encrypted binary blobs.
* **Clear Storage Distinction:** MySQL stores **zero** file binary bytes; actual encrypted bytes reside strictly on the server filesystem.

### Key Takeaway
Clean architectural separation ensures that database queries remain microsecond-fast while high-throughput file streaming never blocks database transactions.

### Recommended Asset
* Diagram: **System Architecture** (`presentation/assets/diagrams/system_architecture.svg` / `.png`)

### Presenter Notes
Walk through the diagram from left to right: User on Android phone &rarr; REST call over port 8080 &rarr; Spring Security JWT verification &rarr; Service Layer &rarr; Split persistence (metadata to MySQL, encrypted bytes to filesystem).

### Technical Source Evidence
[`docs/architecture/CIPHERVAULT_SYSTEM_ARCHITECTURE.md#L48-L95`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/architecture/CIPHERVAULT_SYSTEM_ARCHITECTURE.md).

---

## Slide 8: Technology Stack

### Slide Content
* **Mobile Client:** Android Native (Java 17 / XML), Google Material Design 3, Android Jetpack, Retrofit 2, OkHttp 4.12, Android Keystore.
* **Backend Microservice:** Spring Boot 3.3.4, Java 21 LTS (Virtual Threads), Spring Security 6.3, Spring Data JPA / Hibernate.
* **Security & Cryptography:** NIST SP 800-38D AES-256-GCM, PBKDF2-HMAC-SHA256 (65,536 rounds), FIPS 180-4 SHA-256, BCrypt, JJWT (0.12.5).
* **Database & Persistence:** Oracle MySQL 8.0 Community Server, InnoDB Engine, B-Tree Indexes, Cascading Foreign Keys.
* **External Tooling:** Phil Harvey's ExifTool (forensic metadata parsing), PySide6 / Python (desktop Server Control utility).

### Key Takeaway
Every layer of the technology stack relies exclusively on enterprise-grade, battle-tested, open-standard components.

### Recommended Asset
* Table: **Technology Stack** (`presentation/assets/tables/technology_stack.svg` / `.png`)
* Screenshot: **Server Connection & Health** (`presentation/assets/screenshots/04_connection_verified.png`)

### Presenter Notes
Mention that Java 21 LTS virtual threads ensure high concurrent request handling during large multi-gigabyte file transfers without thread-pool starvation.

### Technical Source Evidence
[`presentation/references/technical_references.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/references/technical_references.md).

---

## Slide 9: Core Features and Modules

### Slide Content
* **Authentication & Access Control:** User registration, password complexity validation, stateless JWT auth, and biometric AppLock.
* **Pre-Upload Staging & Quotas:** Multi-file picker, 1 GB batch calculation, 10 GB user quota progress bar, and modal thumbnail preview.
* **Duplicate Detection:** Instant SHA-256 hashing; atomic database rejection if identical file exists for that user (`409 Conflict`).
* **Vault Browsing & File Management:** Two-column responsive card grid with direct 40dp action touch targets (`[Download]`, `[Delete]`).
* **Forensic Metadata & Search:** Expandable BottomSheet displaying 29 EXIF attributes; dynamic search suggestion chips and multi-facet filtering.
* **Live Transfer Monitor:** Foreground service with real-time transfer progress, byte counters, and historical batch logs.

### Key Takeaway
CipherVault delivers a complete, end-to-end mobile file management suite with direct action controls and forensic intelligence.

### Recommended Asset
* Diagram: **Module & Component Map** (`presentation/assets/diagrams/module_map.svg` / `.png`)
* Screenshots: **Vault Grid** (`presentation/assets/screenshots/09_vault_file_grid.png`) and **File Details** (`presentation/assets/screenshots/14_file_details_bottom_sheet.png`)

### Presenter Notes
Point out that the vault cards feature direct, prominent Download and Delete buttons rather than hiding essential actions behind three-dot menus.

### Technical Source Evidence
[`docs/project-reports/CIPHERVAULT_SCREEN_BY_SCREEN_IMPLEMENTATION.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CipherVault_Screen_By_Screen_Implementation.md).

---

## Slide 10: Security and Encryption Mechanism

### Slide Content
* **Envelope Encryption Hierarchy:**
  * Server Key Encryption Key (KEK): Derived from Master Passphrase via PBKDF2 (HMAC-SHA256, 65,536 iterations).
  * User Data Encryption Key (UDEK): Ephemeral 256-bit AES key generated per user, wrapped by Server KEK, and stored in `users.user_key`.
* **Streaming AES-256-GCM:** Files encrypted in 16 KB chunks; format: `[12-byte Nonce] + [Ciphertext] + [16-byte GHASH Tag]`.
* **Tamper Verification:** Any modification to stored ciphertext or tag immediately triggers an `AEADBadTagException` on download.
* **Isolated Deduplication:** SHA-256 digest computed before encryption; per-user uniqueness eliminates cross-user side-channel probing.
* **Android Client Hardening:** Credentials stored in `EncryptedSharedPreferences`; BiometricPrompt challenge on cold start.

### Key Takeaway
Multi-tier envelope encryption guarantees cryptographic isolation: compromising one user's key never exposes another user's vault data.

### Recommended Asset
* Diagram: **Security & Cryptographic Pipeline** (`presentation/assets/diagrams/security_pipeline.svg` / `.png`)

### Presenter Notes
Explain the distinction clearly: SHA-256 is used for file integrity and duplicate checking; AES-256-GCM is used for authenticated encryption; PBKDF2 is used for master key derivation; BCrypt is used for user passwords. They are distinct, specialized algorithms.

### Technical Source Evidence
[`docs/security-and-encryption/CIPHERVAULT_SECURITY_AND_ENCRYPTION.md#L15-L80`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/security-and-encryption/CIPHERVAULT_SECURITY_AND_ENCRYPTION.md).

---

## Slide 11: Database Design

### Slide Content
* **Authoritative Schema Tables:**
  1. `users`: Credentials, PBKDF2 wrapped UDEK, token version, and 10 GB quota tracking.
  2. `stored_files`: Filenames, sizes, SHA-256 hash, physical storage path, and preview flags.
  3. `file_metadata`: 29 decoupled forensic columns (camera make, model, lens, ISO, codecs, duration).
  4. `activity_events`: Audit trail for login, upload, download, duplicate rejection, and deletion.
  5. `transfers`: Foreground transfer batch tracking, byte counts, and throughput metrics.
* **Cryptographic Deduplication Constraint:** `UNIQUE (user_id, sha256_hash)` on `stored_files`.
* **Relational Integrity:** Foreign keys enforce `ON DELETE CASCADE`; deleting a file automatically cleans up metadata and decrements user quota.

### Key Takeaway
The normalized schema cleanly decouples queryable forensic metadata from opaque storage blobs while enforcing atomic quota integrity.

### Recommended Asset
* Diagram: **Entity-Relationship (ER) Diagram** (`presentation/assets/diagrams/er_diagram.svg` / `.png`)
* Table: **Database Summary** (`presentation/assets/tables/database_summary.svg` / `.png`)

### Presenter Notes
Highlight that this diagram reflects the real MySQL 8.0 schema (`stored_files`, `activity_events`), correcting earlier draft slides that used generic names like `files` or `activity_log`.

### Technical Source Evidence
[`database/schema.sql`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/database/schema.sql).

---

## Slide 12: Application Screenshots and User Interface

### Slide Content
* **Design Identity:** Clean academic styling inspired by Material Design 3, featuring warm cream backgrounds, rounded taupe cards, and high contrast.
* **Onboarding & Connection:** Guided 5-screen intro followed by live server health probe with latency readout (`226 ms`).
* **Storage Dashboard:** Real-time visual progress gauge enforcing 10 GB total vault capacity and category breakdown.
* **Pre-Upload Staging:** Dedicated file review queue with modal image preview and batch quota enforcement.
* **Forensic Metadata Inspection:** Expandable BottomSheet displaying camera optics, resolution, and codecs directly in the app.

### Key Takeaway
The native Android interface combines modern Material 3 aesthetics with rigorous pre-upload inspection and accessible touch targets.

### Recommended Asset
* Multi-Image Layout:
  * [`presentation/assets/screenshots/08_home_dashboard.png`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/screenshots/08_home_dashboard.png)
  * [`presentation/assets/screenshots/10_upload_staging_queue.png`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/screenshots/10_upload_staging_queue.png)
  * [`presentation/assets/screenshots/14_file_details_bottom_sheet.png`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/screenshots/14_file_details_bottom_sheet.png)
  * [`presentation/assets/screenshots/15_metadata_search_chips.png`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/screenshots/15_metadata_search_chips.png)

### Presenter Notes
Explain that every screenshot shown was captured from a live test session on an Android emulator connected to the Spring Boot server. Point out the warm cream palette matching the presentation theme.

### Technical Source Evidence
[`presentation/screenshot_manifest.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/screenshot_manifest.md).

---

## Slide 13: Testing and Results

### Slide Content
* **Automated Backend Test Suite:** **138 tests run, 138 passed (100% pass rate)** covering quota boundaries, crypto streams, duplicate hashing, and JWT filters.
* **Automated Android Test Suite:** **82 unit tests passed** covering `SessionManager`, token migration isolation, password validation, and staging quotas.
* **Automated Server Control Tests:** **23 desktop utility tests passed** covering process management, socket bindings, and log streaming.
* **Live Runtime Regression:** Complete user journey tested from fresh installation &rarr; onboarding &rarr; server connection &rarr; registration &rarr; login &rarr; staging &rarr; streaming upload &rarr; forensic search &rarr; download decryption &rarr; deletion &rarr; logout session purge.
* **Performance Benchmark:** Streaming I/O maintains a flat heap footprint of **28–36 MB RAM** even during multi-gigabyte file transfers.

### Key Takeaway
Zero test failures across 243 combined automated tests confirm the high stability, security, and quota enforcement of the entire stack.

### Recommended Asset
* Table: **Testing and Verification Results** (`presentation/assets/tables/testing_results.svg` / `.png`)

### Presenter Notes
Distinguish the test tiers: unit tests verify isolated logic; integration tests verify database constraints; emulator regression verifies the real-world end-to-end user experience.

### Technical Source Evidence
[`docs/project-reports/CIPHERVAULT_FINAL_VERIFICATION_REPORT.md#L55-L90`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CIPHERVAULT_FINAL_VERIFICATION_REPORT.md).

---

## Slide 14: Limitations and Future Enhancements

### Slide Content
* **Current Architectural Limitations:**
  * Single-Node Server: Designed for personal and local server hosting; lacks distributed clustering.
  * Non-Resumable Transfers: Uploads stream in a single HTTP request; network interruption requires restarting the transfer.
  * Flat Vault Hierarchy: Files are organized by tags and metadata categories rather than nested directory trees.
* **Future Roadmap Enhancements:**
  * *Phase 1:* Multi-Factor Authentication (TOTP / Hardware Security Keys).
  * *Phase 2:* Chunked Resumable Ingestion using the open TUS protocol.
  * *Phase 3:* Distributed Object Storage integration (MinIO / Ceph S3 backend).
  * *Phase 4:* Automated background client sync and biometric auto-backup.

### Key Takeaway
CipherVault's modular architecture provides a solid, verified foundation ready for enterprise clustering and resumable transfer protocols.

### Recommended Asset
* Diagram: **Current vs. Future Architecture Roadmap** (`presentation/assets/diagrams/storage_layout.svg` / `.png`)

### Presenter Notes
Be honest and academic about limitations. Explain that focusing on single-node self-hosting ensured rock-solid cryptographic and quota correctness, while distributed clustering represents the logical next phase.

### Technical Source Evidence
[`docs/project-reports/CipherVault_Project_Report_Source.md#L169-L175`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CipherVault_Project_Report_Source.md).

---

## Slide 15: References

### Slide Content
* **Academic Literature on Encrypted Deduplication:**
  * C. Yang et al., *"Zero knowledge based client-side deduplication for encrypted files in smart cities,"* *Pervasive & Mobile Computing*, 2017. DOI: 10.1016/j.pmcj.2017.03.014.
  * J. Liu, N. Asokan, & B. Pinkas, *"Secure Deduplication of Encrypted Data without Additional Independent Servers,"* *ACM CCS '15*, 2015. DOI: 10.1145/2810103.2813623.
* **Cryptographic Standards:**
  * NIST SP 800-38D (Galois/Counter Mode — AES-GCM), FIPS PUB 180-4 (SHA-256), RFC 2898 / 8018 (PBKDF2).
* **Industry Frameworks & Specifications:**
  * IETF RFC 7519 (JSON Web Tokens), Google Material Design 3 Guidelines, Spring Boot 3.3.4 & Spring Security 6 Reference, Phil Harvey's ExifTool.

### Key Takeaway
CipherVault's design is firmly grounded in peer-reviewed cryptography research and official international standards.

### Recommended Asset
* Table: **References and Academic Studies** (`presentation/references/existing_systems_review.md` and `presentation/references/technical_references.md`)

### Presenter Notes
Point out that both papers studied directly influenced the decision to avoid third-party key servers and isolate deduplication to the individual user.

### Technical Source Evidence
[`presentation/references/existing_systems_review.md`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/references/existing_systems_review.md).

---

## Slide 16: Conclusion and Q&A

### Slide Content
* **Summary of Achievements:**
  * Successfully developed a sovereign, private cloud storage solution with native Android and Spring Boot microservices.
  * Solved the metadata paradox through decoupled forensic indexing via ExifTool while maintaining AES-256-GCM encryption.
  * Eliminated cross-user deduplication side-channels via atomic per-user SHA-256 unique constraints.
  * Enforced strict 10 GB user storage quotas and 1 GB batch upload boundaries with constant-memory streaming I/O.
* **Final Academic Takeaway:**
  * Cryptographic sovereignty and rich mobile usability can coexist without compromising privacy.
* **Thank You! Questions & Discussion.**

### Key Takeaway
CipherVault proves that self-hosted private storage can provide zero-knowledge security without sacrificing the searchability and smoothness expected of modern mobile apps.

### Recommended Asset
* [`presentation/assets/branding/ciphervault_icon.png`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/presentation/assets/branding/ciphervault_icon.png)

### Presenter Notes
Conclude confidently by summarizing that all planned features from the first review (decryption on download, ExifTool metadata, deletion, search) are now completely implemented, verified with 243 automated tests, and validated on live Android devices. Open the floor for questions.

### Technical Source Evidence
[`docs/project-reports/CipherVault_Project_Report_Source.md#L185-L190`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/docs/project-reports/CipherVault_Project_Report_Source.md).
