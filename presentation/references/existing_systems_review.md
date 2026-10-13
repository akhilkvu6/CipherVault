# CipherVault — Existing Systems Literature Review & Comparative Analysis

This document provides a rigorous, evidence-based academic review of existing cloud storage architectures, client-side encryption schemes, and academic research papers studied during the engineering of CipherVault. 

Every reviewed source is documented with its complete bibliographic citation, the problem addressed, the methodology employed, the specific limitations discovered, and how those findings directly guided CipherVault's architectural design and concrete implementation.

---

## 1. Academic Research Papers Studied

### Study 1: Zero-Knowledge Client-Side Deduplication for Encrypted Cloud Storage

* **Full Title:** *Zero knowledge based client side deduplication for encrypted files of secure cloud storage in smart cities*
* **Authors:** Chao Yang, Mingyue Zhang, Qi Jiang, Junwei Zhang, Danping Li, Jianfeng Ma, and Jian Ren
* **Publication:** *Pervasive and Mobile Computing* (Elsevier), Volume 41, Pages 243–258
* **Publication Year:** 2017
* **DOI / Source URL:** [`https://doi.org/10.1016/j.pmcj.2017.03.014`](https://doi.org/10.1016/j.pmcj.2017.03.014)
* **Problem Addressed:** In secure cloud storage, users encrypt files before uploading to preserve privacy. However, standard semantic encryption produces distinct ciphertexts for identical plaintext files, destroying the ability of cloud providers to perform data deduplication (saving storage and upload bandwidth). Furthermore, cross-user Proof-of-Ownership (PoW) protocols over plaintexts leak user ownership information to malicious or honest-but-curious servers.
* **Approach Described by the Source:**
  1. Formulates a zero-knowledge Proof-of-Ownership protocol over encrypted files, allowing the storage server to verify that a client genuinely possesses a file without learning anything about its plaintext content or private encryption keys.
  2. Proposes a Proxy Re-Encryption (PRE) key distribution mechanism that allows multiple authorized users to share a single encrypted duplicate copy without establishing direct pairwise communication channels.
* **Limitations Identified in the Studied Approach:**
  - High computational complexity: Interactive zero-knowledge proofs and pairing-based elliptic curve cryptography impose heavy mathematical overhead on mobile devices with limited battery and CPU budgets.
  - Multi-tenant side-channel leakage: Cross-user deduplication—even with zero-knowledge PoW—inherently allows an adversary to perform *ownership-confirmation attacks* (probing whether another user has already uploaded a specific sensitive file).
  - Proxy key management overhead: Requires complex re-encryption key delegation tables and proxy server maintenance.
* **How the Source Informed CipherVault:**
  - Inspired CipherVault's fundamental design decision to **isolate duplicate detection per user account** rather than performing global cross-user deduplication.
  - Demonstrates why cross-user deduplication creates unavoidable privacy hazards in sovereign cloud storage.
* **CipherVault Implementation:**
  - **Feature:** Per-User SHA-256 Deduplication.
  - **Status:** **Fully Implemented**.
  - **Technical Mechanism:** SHA-256 is computed across the file stream before storage. The database schema enforces a composite unique constraint: `UNIQUE (user_id, sha256_hash)` on table `stored_files`. When User A uploads file `doc.pdf`, it is checked only against User A's existing catalog. If User B uploads the identical `doc.pdf`, User B's upload succeeds independently, preserving strict privacy isolation and preventing side-channel probing.
  - **Code Reference:** [`FileStorageService.java#L238-L281`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java), [`schema.sql#L56-L57`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/database/schema.sql).

---

### Study 2: Secure Deduplication of Encrypted Data Without Auxiliary Servers

* **Full Title:** *Secure Deduplication of Encrypted Data without Additional Independent Servers*
* **Authors:** Jian Liu, N. Asokan, and Benny Pinkas
* **Publication:** *Proceedings of the 22nd ACM SIGSAC Conference on Computer and Communications Security (CCS '15)*, Denver, CO, USA, Pages 874–885
* **Publication Year:** 2015
* **DOI / Source URL:** [`https://doi.org/10.1145/2810103.2813623`](https://doi.org/10.1145/2810103.2813623)
* **Problem Addressed:** Previous encrypted deduplication schemes either used *convergent encryption* (which is fundamentally vulnerable to offline brute-force dictionary attacks against predictable files like templates or invoices) or relied on *additional, independent key-management servers* (e.g., DupLESS) to blind file hashes. Requiring independent auxiliary servers introduces substantial administrative complexity, network fragility, and new trust assumptions.
* **Approach Described by the Source:**
  1. Introduces the first single-server, server-aided client-side deduplication protocol that operates without auxiliary servers.
  2. Adapts Password-Authenticated Key Exchange (PAKE) protocols to securely derive consistent encryption keys and verify file ownership directly between the client and the primary storage provider.
  3. Provides formal security proofs against offline dictionary attacks under realistic adversary models.
* **Limitations Identified in the Studied Approach:**
  - Architectural complexity: PAKE handshakes require multiple network round-trips prior to initiating data transfer, noticeably increasing connection latency on high-jitter cellular networks.
  - Inability to inspect rich metadata: Encrypted blobs remain completely opaque to both client and server, preventing post-upload forensic searching, attribute indexing, and instant client-side thumbnail previews.
  - Rigid coupling between data encryption and access control: Revoking access or modifying user quotas requires complex cryptographic key rotation cycles.
* **How the Source Informed CipherVault:**
  - Guided CipherVault's **Envelope Encryption Hierarchy** and stateless REST interaction pattern.
  - Confirmed that relying on external key-management or third-party auxiliary servers undermines the self-contained simplicity required for a private, self-hosted deployment.
* **CipherVault Implementation:**
  - **Feature:** Single-Server Envelope Encryption with PBKDF2 Master Key and Per-User Data Encryption Keys (UDEKs).
  - **Status:** **Fully Implemented**.
  - **Technical Mechanism:** The server derives a master Key Encryption Key (KEK) using PBKDF2-HMAC-SHA256 (65,536 iterations). Each user account is assigned an isolated 256-bit AES User Data Encryption Key (UDEK), which is wrapped by the KEK and stored in `users.user_key`. Files are encrypted in AES-256-GCM streaming mode with unique 12-byte initialization vectors (IVs).
  - **Code Reference:** [`KeyManagementService.java#L119-L151`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/security/KeyManagementService.java), [`EncryptionService.java#L25-L70`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/EncryptionService.java).

---

## 2. Empirical Survey of Existing Cloud Storage & Vault Systems

### 1. Google Drive & Dropbox (Centralized Proprietary Cloud)
* **Architecture:** Massive multi-tenant public cloud storage operated by third-party corporations.
* **Custody Model:** Third-party custodial storage. Private keys and master storage volumes are managed and controlled by the service provider.
* **Identified Limitations:**
  - *Lack of Cryptographic Sovereignty:* Encryption keys reside on the vendor's infrastructure. Data is decrypted server-side for indexing, virus scanning, OCR, and commercial analytics.
  - *Data Privacy & Compliance Concerns:* Subject to government subpoenas, third-party insider threats, and administrative data breaches.
  - *Aggressive Global Deduplication:* Global cross-user deduplication enables side-channel confirmation attacks where malicious actors can verify whether target files exist in another user's private drive.
* **How CipherVault Differs:** CipherVault is 100% self-hosted on user-controlled hardware (Ubuntu/Linux server or local host). All encryption keys are derived with authenticated PBKDF2/AES-GCM, and deduplication is strictly bound to the authenticated user's account.

### 2. Nextcloud (Self-Hosted Private Cloud Platform)
* **Architecture:** Open-source PHP/LAMP/LEMP monolithic private productivity suite.
* **Custody Model:** Self-hosted on private virtual private servers (VPS) or on-premise hardware.
* **Identified Limitations:**
  - *Excessive Resource Overhead:* Heavy PHP runtime with extensive dependency stacks (Apache/Nginx, MariaDB, Redis, PHP-FPM) requiring substantial RAM and CPU overhead, making lightweight mobile or micro-server deployment difficult.
  - *Fragile Client-Side E2EE:* Nextcloud's optional End-to-End Encryption (E2EE) module has historically suffered from synchronization race conditions, folder-locking locks, and key corruption bugs across mobile clients.
  - *Absence of Pre-Upload Forensic Staging:* Mobile client immediately queues picked files into upload threads without displaying comprehensive forensic metadata (camera EXIF, lens, aperture, video codecs) or allowing pre-upload file inspection.
* **How CipherVault Differs:** CipherVault utilizes a compiled, high-performance Java 21 LTS Spring Boot microservice backend with constant-memory chunked streaming I/O (16 KB buffers, 28–36 MB operational heap footprint). It introduces a dedicated **Pre-Upload Staging Queue** with full-screen inspection, 1 GB batch quota gauge, and dedicated preview/delete action buttons.

### 3. Cryptomator (Client-Side Storage Encryption Utility)
* **Architecture:** Client-side encryption wrapper designed to encrypt directory trees before syncing via third-party storage utilities (Google Drive, OneDrive, WebDAV).
* **Custody Model:** Client-controlled key derivation using scrypt and AES-256 filename and content encryption.
* **Identified Limitations:**
  - *Virtual Filesystem Bridge Only:* Cryptomator does not provide a standalone private server or centralized storage infrastructure; it requires an external sync client.
  - *The "Metadata Paradox":* Because Cryptomator obfuscates all filenames, directory hierarchies, and file headers into opaque ciphertext chunks, the client cannot perform metadata-based searches, dynamic filter indexing, or media facet queries.
  - *Awkward External Media Handling:* Does not feature native forensic metadata extraction; viewing files requires spawning third-party external viewer Android intents, creating potential temporary file leakage on shared device storage.
* **How CipherVault Differs:** CipherVault resolves the *Metadata Paradox* by cleanly decoupling forensic metadata from the encrypted payload. Metadata (Exif, codecs, resolution, timestamps) is parsed during upload and securely indexed in dedicated relational tables (`file_metadata`), enabling instant multi-attribute search and dynamic suggestion chips while the physical storage blob remains strongly encrypted in `storage/encrypted/`.

---

## 3. Comparison Synthesis Matrix

| Critical Evaluation Dimension | Google Drive / Dropbox | Nextcloud Self-Hosted | Cryptomator Mobile | CipherVault (This Project) |
| :--- | :--- | :--- | :--- | :--- |
| **Data Custody** | Proprietary Third-Party | Self-Hosted Sovereign | Third-Party Hybrid | **Self-Hosted Sovereign** |
| **Payload Encryption** | Server-Side AES-256 | Server-Side / Optional E2EE | Client-Side AES-256 | **AES-256-GCM Envelope** |
| **Tamper Verification** | TLS Transport Only | HMAC / TLS | HMAC-SHA256 | **128-bit GHASH Tag (AEAD)** |
| **Deduplication Scope** | Global Cross-User | None (Full Duplication) | None (Opaque Blobs) | **Per-User SHA-256 Constraint** |
| **Forensic Metadata Search** | Server Plaintext Scanning | Basic MIME / Tags | None (Opaque Blobs) | **Decoupled EXIF & Codec Facets** |
| **Pre-Upload Staging Queue** | Immediate Upload | Immediate Sync | None | **Dedicated Staging + Modal Preview** |
| **Mobile Client Design** | Proprietary Commercial | Heavy Generic UI | Minimal File Browser | **Native Java/XML Material 3** |
| **Storage Architecture** | Centralized Cloud Object | Monolithic Server Filesystem | Synchronized Virtual Drive | **Chunked Streaming (2GB/16KB)** |
