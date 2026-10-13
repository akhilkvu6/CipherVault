# CIPHERVAULT: A SECURE PRIVATE CLOUD STORAGE ANDROID APPLICATION WITH ZERO-KNOWLEDGE ARCHITECTURE

**Academic Degree:** Master of Computer Applications (MCA)  
**Academic Year:** 2025&ndash;2026  
**Document ID:** `CV-MCA-REP-2026-FINAL`  
**Target Platform:** Native Android (Material 3 / Java 21 / SDK 37) &bull; Spring Boot 3.3.4 &bull; MySQL 8.0  
**Repository:** `https://github.com/akhilkvu6/CipherVault`  
**Total Target Pages:** 112 Pages  

---

## Preliminary Documentation
- **Cover Page:** Formal MCA Final Project Report Submission
- **Certificate:** Bonafide institutional certificate with Guide, HOD, and Viva-Voce placeholders
- **Declaration:** Student declaration of originality and academic authenticity
- **Acknowledgements:** Acknowledgements of faculty, institutional management, and technical contributors
- **Abstract:** Comprehensive technical abstract detailing zero-knowledge envelope encryption, chunked streaming, and forensic search
- **Executive Summary:** Core value proposition, architectural highlights, and verification audit summary
- **Table of Contents:** Chapters 1 through 17 and Appendices A through F
- **List of Figures:** 24 technical architecture diagrams and 32 genuine application screenshots
- **List of Tables:** Comparative evaluation, requirements matrices, data dictionaries, and test registers

---

## Chapter 1: Introduction
- **1.1 Project Background and Context:** Shift toward mobile data custody and the privacy risks of centralized hyperscale storage.
- **1.2 Motivation:** Mitigating surveillance capitalism, warrantless third-party subpoenas, and server-side data breaches.
- **1.3 Problem Statement:** The trade-off between cryptographic privacy and mobile usability; memory exhaustion on large media; lack of metadata search.
- **1.4 Objectives:** Implementing NIST AES-256-GCM authenticated encryption, 2 GB chunked streaming, Material Design 3 client, and forensic EXIF indexing.
- **1.5 Scope:** Demarcation of in-scope capabilities versus future roadmap features.
- **1.6 User Personas:** Privacy-conscious professionals, journalists, photographers, and forensic researchers.
- **1.7 Report Organization:** Structural overview of the 17 chapters and 6 appendices.

---

## Chapter 2: Problem Analysis and Existing Systems
- **2.1 Traditional Cloud Storage Dilemma:** Server-side encryption illusion and shared multi-tenant attack surfaces.
- **2.2 Existing Solutions Survey:** Audits of Google Drive, Dropbox, Nextcloud, and Cryptomator.
- **2.3 Comparative Evaluation Matrix:** Detailed comparison table across custody, encryption, streaming, metadata, and UI design.
- **2.4 Structural Gaps:** The metadata paradox, rigid mobile UIs, fragile transfer lifecycles, and missing pre-upload controls.
- **2.5 Proposed CipherVault Paradigm:** Envelope encryption, decoupled forensic metadata, and foreground notification transfers.
- **2.6 Feasibility Analysis:** Technical, economic, and operational feasibility validations.

---

## Chapter 3: Literature and Technology Review
- **3.1 Authenticated Cryptography Standards:** NIST SP 800-38D AES-GCM, Galois field $GF(2^128)$ arithmetic, and 12-byte IV uniqueness requirements.
- **3.2 Key Derivation Standards:** PBKDF2-HMAC-SHA256 (65,536 iterations), BCrypt cost 10, and SHA-256 integrity digests.
- **3.3 Modern Android Client Ecosystem:** Android 12&ndash;17, Material Design 3, Monet dynamic theming, and Android Keystore StrongBox binding.
- **3.4 Enterprise Backend Architecture:** Spring Boot 3.3.4, Java 21 LTS virtual threads, and Spring Security 6 stateless JWT pipelines.
- **3.5 Relational Storage & Indexing:** MySQL 8.0 InnoDB engine, B-Tree indexes on hashes, and ExifTool forensic metadata pipelines.

---

## Chapter 4: Requirements Specification
- **4.1 Functional Requirements Specification (FRS):** FR-01 through FR-20 covering authentication, connection, staging, streaming upload, deduplication, vault browsing, bottom sheet details, decrypted viewing, and desktop control.
- **4.2 Non-Functional Requirements (NFR):** Rigorous benchmarks for security strength, tamper detection, constant-memory I/O, REST latency, and accessibility.
- **4.3 Use Case Modeling:** Comprehensive UML Use Case diagram mapping Mobile User and Host Administrator boundaries.
- **4.4 User Stories & Acceptance Criteria:** Pre-upload inspection, forensic asset discovery, and biometric app lock stories.
- **4.5 Operational Constraints:** Hardware, OS, and network transport boundaries.

---

## Chapter 5: System Architecture and Design
- **5.1 System Context Diagram (Level 0):** Entity boundaries across user, admin, mobile client, backend core, database, and filesystem.
- **5.2 Multi-Tier System Architecture:** Three-tier decomposition (Presentation, Application Core, Persistence).
- **5.3 Communication Protocol Flow:** HTTP/1.1 REST over port 8080, Bearer token injection, and multipart/octet-stream pipelines.
- **5.4 Data Flow Diagrams:** DFD Level 0 Context model and DFD Level 1 functional process decomposition.
- **5.5 Physical Deployment Topology:** Hardware nodes, execution environments, socket bindings, and network links.
- **5.6 Architectural Trade-offs:** In-depth evaluation of AES-GCM vs ChaCha20, envelope vs client-only KDFs, and streaming vs memory buffering.

---

## Chapter 6: Android Application Design
- **6.1 Component Architecture:** UI Activities, ViewModels/Adapters, Domain Managers, and Security/Networking components.
- **6.2 Application Entry Point & Lifecycle:** `CipherVaultApplication` cold start, `MainActivity` fragment caching, and lifecycle safety.
- **6.3 Dynamic Theming (Monet) vs Heritage Identity:** Material You wallpaper extraction engine paired with warm beige/peach brand fallback.
- **6.4 Navigation Architecture & State Machine:** Deterministic screen graph across Splash, Onboarding, Auth, AppLock, and Main tabs.
- **6.5 ViewHolder Lifecycle Safety:** Dynamic `getBindingAdapterPosition()` guards and checkbox listener recycling detachment.
- **6.6 Onboarding Experience:** Complete 5-screen onboarding flow and persistent state transitions.
- **6.7 AppLock Subsystem:** Hardware biometric authentication, cold-start lock overlay, and TEE Keymaster binding.

---

## Chapter 7: Backend Design and Implementation
- **7.1 Backend Layered Architecture:** Four-tier server decomposition across Controllers, Security, Services, and Repositories.
- **7.2 Bootstrapping & Configuration:** Spring Boot 3.3.4 parameters, Tomcat 10.1 multipart limits, and production secret enforcement.
- **7.3 Spring Security 6 & JWT Filter:** Stateless filter chain, Bearer token verification, and security context population.
- **7.4 REST Controllers:** Complete endpoint mappings across `AuthController`, `FileController`, `MetadataController`, and `UserController`.
- **7.5 Service Layer Business Logic:** Transactional execution inside `KeyManagementService`, `FileStorageService`, and `EncryptionService`.
- **7.6 2GB Streaming I/O Engine:** Constant-memory 16 KB chunked streaming pipeline using `CipherOutputStream` and `CipherInputStream`.
- **7.7 Global Exception Handling:** Controller advice converting runtime exceptions into uniform JSON error payloads.

---

## Chapter 8: Database Design
- **8.1 Schema Overview:** Relational design across `users`, `stored_files`, and `file_metadata` tables.
- **8.2 Entity-Relationship (ER) Diagram:** Complete relational model with 1:N and 1:1 cardinalities.
- **8.3 Table Specifications:** Column definitions, data types, constraints, and operational purposes for `users` and `stored_files`.
- **8.4 Forensic Metadata Table:** Column definitions for camera make, model, ISO, shutter, resolution, codecs, and raw JSON.
- **8.5 Indexing & Atomic Quotas:** B-Tree unique hash constraints and atomic SQL quota update expressions.
- **8.6 Data Lifecycle:** Cascading deletion invariants and physical disk unlinking sequences.

---

## Chapter 9: Security and Cryptography
- **9.1 NIST Authenticated Encryption:** AES-256-GCM parameterization, 12-byte IVs, and 128-bit GHASH authentication tags.
- **9.2 Authentication & JWT Lifecycle:** Token issuance, signature verification, expiry handling, and version revocation.
- **9.3 Envelope Encryption Hierarchy:** PBKDF2-derived Server KEK wrapping per-user UDEKs, storing blobs formatted as `[12B Nonce] + [Ciphertext] + [16B Tag]`.
- **9.4 Key Management:** Memory-only KEK residence and isolated user key spaces.
- **9.5 Password Hashing & Rate Limiting:** BCrypt cost factor 10 and 15-minute lockout after 5 consecutive failed logins.
- **9.6 Threat Model (STRIDE):** Trust boundary delineation across Client, Network, and Server.
- **9.7 Risk Assessment Matrix:** Comprehensive STRIDE threat-to-mitigation mapping table.
- **9.8 Android Hardening:** Keystore MasterKey, `FLAG_SECURE` window defense, and ADB backup suppression.

---

## Chapter 10: Core Feature Workflows
- **10.1 Login Sequence:** Step-by-step authentication sequence diagram.
- **10.2 File Upload Sequence:** End-to-end streaming upload and encryption sequence diagram.
- **10.3 File Download Sequence:** Streaming decryption and GHASH tamper verification sequence diagram.
- **10.4 Duplicate Detection Workflow:** SHA-256 block hash computation and unique database constraint verification.
- **10.5 Metadata Search Workflow:** ExifTool extraction, dynamic suggestion chips, and JPA Criteria queries.
- **10.6 File Preview Workflow:** In-memory preview dialogs and decrypted full-screen viewing.
- **10.7 File Deletion Workflow:** Cascading database deletion, quota decrement, and disk unlinking.
- **10.8 Staging & Error Recovery Workflows:** Branching decision activity workflows and fault-tolerance error recovery trees.

---

## Chapter 11: User Interface and Screen Documentation
- **11.1 Onboarding Experience:** Splash screen and Onboarding Pages 1&ndash;2 with genuine application screenshots.
- **11.2 Onboarding Completion:** Onboarding Pages 3&ndash;5 illustrated and documented.
- **11.3 Server Connection & Health:** Connection settings, health probe confirmation, and QR scanner viewfinder.
- **11.4 Authentication & AppLock:** Login screen, registration screen, and cold-start biometric lock overlay.
- **11.5 Home & Vault Explorer:** Storage quota dashboard, two-column vault grid, and sorting options menu.
- **11.6 Vault Multi-Select & Details Sheet:** Batch selection mode, expandable `FileDetailsBottomSheet`, and decrypted viewer.
- **11.7 Pre-Upload Staging & Actions:** Staged file queue, dedicated preview/delete action buttons, and 240dp preview modal.
- **11.8 Full-Screen Preview & Transfers:** Pinch-to-zoom staged preview, post-delete queue state, and transfer monitor.
- **11.9 Metadata Search, Settings & UI Showcase:** Dynamic search suggestion chips, search results, settings, and UI showcase.

---

## Chapter 12: Implementation Details
- **12.1 Source Tree Topology:** Complete project directory and package structure.
- **12.2 Core UML Class Diagram:** Structural class diagram linking Android controllers, adapters, and backend services.
- **12.3 Android Code Highlights:** BottomSheet expansion logic and foreground transfer service implementations.
- **12.4 Backend Code Highlights:** PBKDF2 key derivation and streaming decryption with tamper verification.
- **12.5 Server Control & Build:** PySide6 desktop manager implementation and Maven/Gradle toolchains.

---

## Chapter 13: Testing and Verification
- **13.1 Testing Methodology:** Verification tiers (Source-Verified, Build-Verified, Runtime-Verified, Partially Implemented, Future).
- **13.2 Automated Backend Test Suite:** 108/108 Maven tests passing (JUnit 5, Mockito, SpringBootTest).
- **13.3 Automated Android Unit Tests:** 22 Gradle unit test tasks passing cleanly.
- **13.4 UI Regression Pass:** 155 independent frame captures on Google Pixel 10 Pro XL (Android 17 / API 37).
- **13.5 Master Test Execution Matrix:** TC-01 through TC-25 test execution register.

---

## Chapter 14: Results and Discussion
- **14.1 Capabilities Summary:** Comprehensive table of implemented and verified capabilities.
- **14.2 Performance & Bandwidth:** Memory benchmarking (28&ndash;36 MB RAM during 1.5 GB transfer) and throughput analysis.
- **14.3 Comparison Against Objectives:** Validation of initial success criteria against observed outcomes.
- **14.4 Engineering Challenges:** Lessons learned in dynamic theming, adapter closures, and cryptographic stream flushing.

---

## Chapter 15: Limitations and Future Roadmap
- **15.1 Architectural Limitations:** Non-resumable transfers, single-node host deployment, and flat vault hierarchy.
- **15.2 Security Review & Gaps:** Lack of MFA, memory zeroization gaps, and duress PIN vulnerabilities.
- **15.3 Recommendations Register:** Prioritized improvement matrix (P0 to P3) across complexity and impact.
- **15.4 Phased Development Roadmap:** Phase 1 (Immediate UX), Phase 2 (Crypto Recovery), Phase 3 (Automation), Phase 4 (Clustering).
- **15.5 Future Architecture:** Comparative architecture diagram contrasting v2.4.0 with proposed MinIO/BIP-39 architecture.

---

## Chapter 16: Deployment and Operations Manual
- **16.1 Prerequisites & DB Setup:** Host environment prerequisites and MySQL database schema initialization.
- **16.2 Backend & Server Control Startup:** Graphical PySide6 launcher and direct Maven CLI instructions.
- **16.3 Android Compilation & Troubleshooting:** Gradle debug APK packaging, installation, and operational troubleshooting table.

---

## Chapter 17: Conclusion
- **17.1 Concluding Summary:** Synthesis of CipherVault's engineering achievements and sovereign cloud paradigm.
- **17.2 Academic Contributions:** Contributions to applied mobile cryptography and zero-knowledge architectures.
- **17.3 Final Remarks:** Concluding evaluation of project success and future research directions.

---

## Appendices
- **Appendix A: Technical References:** Comprehensive IEEE, NIST, and IETF bibliography.
- **Appendix B: Complete REST API Reference:** Exhaustive documentation of all endpoints, request bodies, and response schemas.
- **Appendix C: Source File Traceability Matrix:** Traceability table linking features to concrete Java and XML source files.
- **Appendix D: Database Data Dictionary:** Complete schema dictionary covering all tables, column types, and constraints.
- **Appendix E: Requirements Traceability Matrix (RTM):** End-to-end matrix linking functional requirements to test cases.
- **Appendix F: Sample Payloads & Forensic JSON:** Sanitized JSON responses, ExifTool metadata dumps, and sample code snippets.
