"""
Chapters 1 to 5 for CipherVault Project Report (Pages 11 to 36).
Covers Introduction, Problem Analysis, Literature Review, Requirements, and System Architecture.
Includes Figures 4.1, 5.1, 5.2, 5.3, 5.4, 5.5 and Tables 2.1, 4.1, 4.2.
"""

from .styles import make_page

def generate_chapters_1_to_5(total_pages=112):
    pages = []

    # =========================================================================
    # CHAPTER 1: INTRODUCTION (Pages 11 to 15)
    # =========================================================================

    # Page 11: 1.1 Background, 1.2 Motivation
    p11 = '''
      <h1 class="ch-title">CHAPTER 1: INTRODUCTION</h1>
      
      <h2 class="sec-title">1.1 Project Background and Context</h2>
      <p>
        In an era dominated by pervasive digital mobility, personal and institutional data custody has undergone a fundamental transition from localized physical storage media to centralized hyperscale cloud infrastructures. Modern smartphone users generate gigabytes of sensitive unstructured information weekly, including high-resolution photographic captures, confidential financial paperwork, legal agreements, biometric identifiers, and private personal notes. The ubiquitous adoption of mobile devices running Android and iOS has solidified cloud synchronization as an indispensable component of daily life.
      </p>
      <p>
        However, the convenience of commercial cloud ecosystems is accompanied by a severe, systemic privacy compromise. The overwhelming majority of mainstream cloud storage providers (such as Google Drive, Microsoft OneDrive, and Dropbox) operate on a custodial multi-tenant paradigm. Under this paradigm, user payloads are uploaded in transit over TLS and subsequently encrypted at rest using server-managed cryptographic keys. Because the service provider maintains full custody of both the decryption keys and the plaintext storage volumes, user records remain completely transparent to the host platform.
      </p>

      <h2 class="sec-title">1.2 Industry Motivation and Market Landscape</h2>
      <p>
        This custodial storage architecture presents three catastrophic vulnerabilities to end-user sovereignty:
      </p>
      <ul>
        <li><strong>Automated Forensic Profiling &amp; Data Mining:</strong> Centralized providers routinely subject stored media to automated algorithmic scanning for advertising telemetry, user profiling, and non-consensual artificial intelligence model training datasets.</li>
        <li><strong>Government Subpoenas &amp; Third-Party Compulsion:</strong> Stored documents are subject to administrative surveillance and extraterritorial data seizures without the data owner's knowledge or consent.</li>
        <li><strong>Server-Side Breaches &amp; Insider Threats:</strong> Credential stuffing attacks, misconfigured cloud storage buckets, and compromised administrative credentials regularly expose millions of private customer files on the dark web.</li>
      </ul>
      <p>
        The core motivation of this project is to restore absolute cryptographic ownership to the user through <strong>CipherVault</strong>&mdash;a private, self-hosted cloud vault engineered from the ground up on zero-knowledge cryptographic foundations without compromising native mobile ergonomics or network transfer speeds.
      </p>

      <div class="callout-box">
        <div class="box-title">Defining the Zero-Knowledge Imperative</div>
        In CipherVault, the server is treated as an untrusted storage daemon. Even if the host physical machine running the backend is seized, inspected, or compromised by an adversary, the adversary cannot recover plaintext files without the user's isolated cryptographic credentials.
      </div>
'''
    pages.append(make_page(11, total_pages, "CHAPTER 1 &bull; INTRODUCTION (1/5)", p11))

    # Page 12: 1.3 Problem Statement
    p12 = '''
      <h1 class="ch-title">CHAPTER 1: INTRODUCTION (CONT.)</h1>
      
      <h2 class="sec-title">1.3 Formal Problem Statement</h2>
      <p>
        Existing consumer cloud storage solutions force users into an unacceptable trade-off between <em>ergonomic usability</em> and <em>cryptographic privacy</em>. An in-depth analysis of existing privacy-oriented alternatives reveals deep architectural deficiencies that prevent widespread adoption:
      </p>
      
      <div class="alert-box">
        <div class="box-title">Deficiency 1: Cumbersome Client Tooling and Desktop-Centric Design</div>
        Cryptographic tools such as Cryptomator, VeraCrypt, and GnuPG were originally conceptualized for desktop file managers. When ported to mobile operating systems, their interfaces are notoriously unintuitive, requiring manual virtual drive mounting, manual vault synchronization, and external third-party file managers to inspect documents.
      </div>

      <div class="alert-box">
        <div class="box-title">Deficiency 2: Catastrophic Memory Overhead &amp; OOM Crashes</div>
        Many existing open-source encryption clients read the entirety of a file into the Android JVM heap prior to cipher initialization. On high-end Android smartphones capturing 4K 60FPS video or massive PDF portfolios (exceeding 500 MB to 1 GB), this induces immediate <code class="inline">java.lang.OutOfMemoryError</code> exceptions and application crashes due to strict mobile heap limits.
      </div>

      <div class="alert-box">
        <div class="box-title">Deficiency 3: Absence of Multimedia Forensic Indexing</div>
        Pure zero-knowledge systems typically encrypt both file content and file metadata into opaque blobs. Consequently, users lose all ability to search, sort, or categorize their files by camera make, ISO, resolution, capture date, audio bitrate, or document author without first downloading and decrypting their entire vault library.
      </div>

      <h3 class="subsec-title">Core Research Question</h3>
      <p>
        How can a private cloud storage system achieve <strong>NIST-standard authenticated encryption</strong> (AES-256-GCM), <strong>zero-memory-exhaustion chunked streaming</strong> (handling files up to 2 GB), and <strong>rich multimedia forensic searchability</strong>, while delivering a modern, responsive <strong>Material Design 3</strong> native Android user experience?
      </p>
      <p>
        CipherVault resolves this challenge by establishing an envelope encryption hierarchy and decoupling public forensic indexing from private encrypted payloads.
      </p>
'''
    pages.append(make_page(12, total_pages, "CHAPTER 1 &bull; INTRODUCTION (2/5)", p12))

    # Page 13: 1.4 Objectives, 1.5 Scope
    p13 = '''
      <h1 class="ch-title">CHAPTER 1: INTRODUCTION (CONT.)</h1>
      
      <h2 class="sec-title">1.4 Project Objectives and Success Criteria</h2>
      <p>
        The primary goal of CipherVault is to engineer, implement, audit, and verify a complete, enterprise-grade cloud storage system comprising a native Android mobile client, a resilient Spring Boot micro-backend, and an intuitive desktop server manager. Specific technical objectives include:
      </p>
      <ul>
        <li><strong>O1 &mdash; Zero-Knowledge Authenticated Cryptography:</strong> Implement NIST SP 800-38D AES-256 in Galois/Counter Mode (GCM) with 12-byte initialization vectors and 128-bit GHASH authentication tags, guaranteeing both confidentiality and tamper detection.</li>
        <li><strong>O2 &mdash; Scalable Chunked Streaming Architecture:</strong> Implement a constant-memory 16 KB streaming pipeline (<code class="inline">CipherOutputStream</code> / <code class="inline">CipherInputStream</code>) enabling transfer of files up to 2 GB without memory exhaustion.</li>
        <li><strong>O3 &mdash; Native Material Design 3 Mobile Client:</strong> Develop an edge-to-edge Android application supporting dynamic Monet wallpaper theming, biometric hardware authentication (<code class="inline">BiometricPrompt</code>), and intuitive pre-upload staging.</li>
        <li><strong>O4 &mdash; Forensic Metadata Extraction &amp; Search:</strong> Integrate an automated server-side metadata pipeline parsing EXIF, GPS, resolution, and codecs into indexed relational tables searchable via dynamic suggestion chips.</li>
        <li><strong>O5 &mdash; Storage Integrity &amp; Deduplication:</strong> Implement SHA-256 duplicate avoidance and atomic storage quota accounting to prevent resource exhaustion attacks.</li>
      </ul>

      <h2 class="sec-title">1.5 Scope and Operational Boundaries</h2>
      <p>
        To ensure production-grade depth, the project boundaries are strictly demarcated:
      </p>
      <table class="report-table">
        <tr>
          <th style="width: 25%;">Domain</th>
          <th style="width: 37%;">In Scope (Implemented &amp; Verified)</th>
          <th style="width: 38%;">Out of Scope / Future Roadmap</th>
        </tr>
        <tr>
          <td><strong>Client Platform</strong></td>
          <td>Native Android (Java 21, API 31&ndash;37), Material 3, OkHttp 4.12, Retrofit 2, Android Keystore.</td>
          <td>iOS client, Desktop web client portal, WearOS companion.</td>
        </tr>
        <tr>
          <td><strong>Cryptography</strong></td>
          <td>AES-256-GCM, PBKDF2-HMAC-SHA256, BCrypt, SHA-256, Dual-layer Envelope Encryption.</td>
          <td>Post-quantum lattice cryptography, Homomorphic search over encrypted text.</td>
        </tr>
        <tr>
          <td><strong>Network &amp; Host</strong></td>
          <td>HTTP/1.1 REST over local LAN / Wi-Fi, Reverse ADB port forwarding, Spring Boot 3.3.4, MySQL 8.0.</td>
          <td>Public multi-tenant SaaS hosting, Distributed multi-datacenter consensus.</td>
        </tr>
      </table>
'''
    pages.append(make_page(13, total_pages, "CHAPTER 1 &bull; INTRODUCTION (3/5)", p13))

    # Page 14: 1.6 User Personas
    p14 = '''
      <h1 class="ch-title">CHAPTER 1: INTRODUCTION (CONT.)</h1>
      
      <h2 class="sec-title">1.6 Target User Personas and Real-World Use Cases</h2>
      <p>
        CipherVault is engineered for individuals and institutions who require absolute sovereign control over their sensitive digital footprint:
      </p>

      <div style="display: flex; gap: 10px; margin-top: 8px;">
        <div style="flex: 1; border: 1px solid #E6D7CD; border-radius: 6px; padding: 10px; background-color: #FFF5EE;">
          <div style="font-weight: bold; color: #815621; font-size: 9pt;">PERSONA 1: PRIVACY-CONSCIOUS PROFESSIONAL</div>
          <div style="font-size: 7.8pt; color: #705D53; margin-bottom: 6px;">Journalists, Legal Counsels, Medical Practitioners</div>
          <p style="font-size: 8pt; margin: 0; line-height: 1.45;">
            <strong>Need:</strong> Storing client records, sensitive case files, and privileged medical scans without violating non-disclosure agreements or legal privilege.<br/>
            <strong>CipherVault Fit:</strong> Zero-knowledge AES-256-GCM ensures that even if host storage is inspected, client files cannot be read without cryptographic keys.
          </p>
        </div>

        <div style="flex: 1; border: 1px solid #E6D7CD; border-radius: 6px; padding: 10px; background-color: #FFF5EE;">
          <div style="font-weight: bold; color: #815621; font-size: 9pt;">PERSONA 2: MULTIMEDIA CREATOR &amp; RESEARCHER</div>
          <div style="font-size: 7.8pt; color: #705D53; margin-bottom: 6px;">Photographers, Videographers, Forensic Analysts</div>
          <p style="font-size: 8pt; margin: 0; line-height: 1.45;">
            <strong>Need:</strong> Backing up massive RAW images and 4K footage from mobile devices while retaining the ability to search by camera model and resolution.<br/>
            <strong>CipherVault Fit:</strong> 2 GB streaming chunking prevents OOM crashes; forensic search indexes EXIF tags without exposing plaintext.
          </p>
        </div>
      </div>

      <h2 class="sec-title" style="margin-top: 15px;">Operational Use Cases</h2>
      <ol style="font-size: 8.5pt; line-height: 1.55;">
        <li><strong>Secure Mobile Camera Staging:</strong> The user snaps confidential physical evidence or documents via the in-app camera. Files are immediately staged in memory, previewed, and streamed encrypted to the private host vault.</li>
        <li><strong>Forensic Asset Discovery:</strong> The user filters hundreds of vault assets by tapping dynamic suggestion chips (e.g., <code class="inline">PDF</code>, <code class="inline">1280x960</code>, <code class="inline">Google Pixel</code>), instantly locating matching assets.</li>
        <li><strong>Tamper-Evident Document Verification:</strong> During download, the system computes the GHASH authentication tag; if a single byte has suffered storage degradation or malicious tampering, the download immediately halts.</li>
      </ol>
'''
    pages.append(make_page(14, total_pages, "CHAPTER 1 &bull; INTRODUCTION (4/5)", p14))

    # Page 15: 1.7 Organization of Report
    p15 = '''
      <h1 class="ch-title">CHAPTER 1: INTRODUCTION (CONT.)</h1>
      
      <h2 class="sec-title">1.7 Organization of the Project Report</h2>
      <p>
        This report is structured into 17 technical chapters and 6 supplementary appendices, providing an exhaustive record of CipherVault's engineering lifecycle:
      </p>

      <table class="report-table" style="font-size: 8pt;">
        <tr>
          <th style="width: 25%;">Chapter Range</th>
          <th style="width: 75%;">Thematic Engineering Content</th>
        </tr>
        <tr>
          <td><strong>Chapters 1 &ndash; 3</strong></td>
          <td><strong>Context &amp; Foundations:</strong> Introduction, formal problem statement, comparative survey of existing systems, literature review of cryptography (AES-GCM, PBKDF2), and modern Android/Spring technology stacks.</td>
        </tr>
        <tr>
          <td><strong>Chapters 4 &ndash; 5</strong></td>
          <td><strong>Requirements &amp; Architecture:</strong> Functional and non-functional requirements specifications, use case modeling, multi-tier system architecture, network protocol flows, and deployment topology.</td>
        </tr>
        <tr>
          <td><strong>Chapters 6 &ndash; 8</strong></td>
          <td><strong>Subsystem Design:</strong> Detailed architectural decomposition of the Android client (M3 navigation, theming, adapters), Spring Boot backend services, and MySQL 8.0 relational schema design.</td>
        </tr>
        <tr>
          <td><strong>Chapters 9 &ndash; 10</strong></td>
          <td><strong>Security &amp; Workflows:</strong> NIST authenticated encryption lifecycle, envelope key wrapping, STRIDE threat model, and step-by-step execution workflows for upload, download, and forensic search.</td>
        </tr>
        <tr>
          <td><strong>Chapters 11 &ndash; 12</strong></td>
          <td><strong>UI Documentation &amp; Code:</strong> Screen-by-screen visual documentation with genuine application screenshots, package topology, and critical code highlights across client and server.</td>
        </tr>
        <tr>
          <td><strong>Chapters 13 &ndash; 14</strong></td>
          <td><strong>Verification &amp; Results:</strong> Rigorous automated testing audit (108 backend tests, 22 Android tests), 155-state UI regression on Google Pixel 10 Pro XL, and performance discussion.</td>
        </tr>
        <tr>
          <td><strong>Chapters 15 &ndash; 17</strong></td>
          <td><strong>Roadmap &amp; Conclusion:</strong> Prioritized architectural improvements (P0&ndash;P3), installation and operations manual, and final academic conclusion.</td>
        </tr>
        <tr>
          <td><strong>Appendices A &ndash; F</strong></td>
          <td><strong>Reference Specifications:</strong> Academic bibliography, complete REST API reference, source file traceability, database data dictionary, RTM, and sanitized request payloads.</td>
        </tr>
      </table>
'''
    pages.append(make_page(15, total_pages, "CHAPTER 1 &bull; INTRODUCTION (5/5)", p15))

    # =========================================================================
    # CHAPTER 2: PROBLEM ANALYSIS AND EXISTING SYSTEMS (Pages 16 to 20)
    # =========================================================================

    # Page 16: 2.1 Traditional Cloud Dilemma
    p16 = '''
      <h1 class="ch-title">CHAPTER 2: PROBLEM ANALYSIS &amp; EXISTING SYSTEMS</h1>
      
      <h2 class="sec-title">2.1 The Traditional Cloud Storage Dilemma</h2>
      <p>
        Cloud computing has fundamentally altered software economics by abstracting physical infrastructure into scalable, on-demand virtualized services. However, this centralized multi-tenant architecture introduces an acute structural vulnerability known in cybersecurity literature as the <strong>Third-Party Custody Dilemma</strong>. When an end-user stores unencrypted or server-encrypted data on a cloud platform, the service provider becomes the effective custodian of the data, creating severe legal and operational risks:
      </p>

      <div style="display: flex; gap: 10px; margin: 10px 0;">
        <div style="flex: 1; border: 1px solid #E6D7CD; border-radius: 4px; padding: 8px; background-color: #FFF9F5;">
          <div style="font-weight: bold; color: #815621; font-size: 8.5pt;">SERVER-SIDE ENCRYPTION ILLUSION</div>
          <p style="font-size: 7.8pt; margin-top: 4px; line-height: 1.4;">
            Many commercial platforms advertise "AES-256 Encryption at Rest". However, because the decryption keys are stored in the provider's Key Management Service (KMS), the provider can decrypt and inspect every file at will. This provides security against physical hard drive theft in data centers, but zero protection against server compromise or algorithmic profiling.
          </p>
        </div>
        <div style="flex: 1; border: 1px solid #E6D7CD; border-radius: 4px; padding: 8px; background-color: #FFF9F5;">
          <div style="font-weight: bold; color: #815621; font-size: 8.5pt;">MULTI-TENANCY ATTACK SURFACE</div>
          <p style="font-size: 7.8pt; margin-top: 4px; line-height: 1.4;">
            In shared cloud infrastructures, hypervisor side-channel attacks, IAM policy misconfigurations, and software supply chain vulnerabilities create attack vectors where a compromise in one tenant's perimeter can expose adjacent tenant databases and storage volumes.
          </p>
        </div>
      </div>

      <h2 class="sec-title">The Need for True Cryptographic Isolation</h2>
      <p>
        To neutralize these systemic risks, modern security engineering mandates <strong>Zero-Knowledge Architecture</strong>. Under this paradigm, cryptographic keys are generated, managed, and wrapped such that the storage server possesses only encrypted ciphertext blobs. The server never observes the user's master password, plaintext data encryption keys, or unencrypted byte streams.
      </p>
'''
    pages.append(make_page(16, total_pages, "CHAPTER 2 &bull; PROBLEM ANALYSIS (1/5)", p16))

    # Page 17: 2.2 Survey of Existing Solutions
    p17 = '''
      <h1 class="ch-title">CHAPTER 2: PROBLEM ANALYSIS &amp; EXISTING SYSTEMS (CONT.)</h1>
      
      <h2 class="sec-title">2.2 Survey of Existing Cloud and Cryptographic Solutions</h2>
      <p>
        To establish an empirical baseline, four prominent cloud storage and encryption solutions representing different design philosophies were audited:
      </p>

      <h3 class="subsec-title">1. Google Drive &amp; Dropbox (Centralized Proprietary Cloud)</h3>
      <p>
        These commercial market leaders provide polished cross-platform ergonomics, real-time collaboration, and rapid transfer speeds. However, their underlying security model is strictly custodial. Encryption keys are managed on the server side. Automated classifiers inspect file contents for abuse detection, advertising signals, and indexing. Users have zero cryptographic sovereignty.
      </p>

      <h3 class="subsec-title">2. Nextcloud (Self-Hosted Private Cloud)</h3>
      <p>
        Nextcloud is an open-source, self-hosted productivity platform allowing users to deploy private storage on personal servers. While it eliminates third-party data custody, its native mobile client is notoriously heavy, written as a massive PHP/LAMP monolith. Its optional server-side encryption module incurs substantial CPU overhead, and client-side end-to-end encryption (E2EE) has historically suffered from synchronization race conditions and key loss bugs.
      </p>

      <h3 class="subsec-title">3. Cryptomator (Client-Side Vault Encryption Utility)</h3>
      <p>
        Cryptomator provides client-side file encryption designed to sit on top of third-party cloud folders. It uses AES-256 with scrypt key derivation. While cryptographically sound, its Android application is essentially a virtual file system bridge. It lacks native multimedia forensic indexing, cannot extract camera EXIF or video codecs, and requires awkward external viewer intents to display media.
      </p>

      <h3 class="subsec-title">4. Proton Drive (Commercial Zero-Knowledge Cloud)</h3>
      <p>
        Proton Drive offers client-side OpenPGP-based encrypted cloud storage. However, it is a proprietary, closed-infrastructure SaaS product with high recurring subscription costs. Furthermore, OpenPGP's packet structure incurs significant network and CPU overhead compared to high-throughput authenticated stream ciphers like AES-GCM.
      </p>
'''
    pages.append(make_page(17, total_pages, "CHAPTER 2 &bull; PROBLEM ANALYSIS (2/5)", p17))

    # Page 18: 2.3 Comparative Evaluation Matrix
    p18 = '''
      <h1 class="ch-title">CHAPTER 2: PROBLEM ANALYSIS &amp; EXISTING SYSTEMS (CONT.)</h1>
      
      <h2 class="sec-title">2.3 Comparative Evaluation Matrix</h2>
      <p>
        Table 2.1 presents a comprehensive comparative evaluation across critical security, architectural, and user-experience criteria:
      </p>

      <table class="report-table">
        <tr>
          <th>Evaluation Parameter</th>
          <th>Google Drive / Dropbox</th>
          <th>Nextcloud Self-Hosted</th>
          <th>Cryptomator Mobile</th>
          <th>CipherVault (This Project)</th>
        </tr>
        <tr>
          <td><strong>Data Custody Paradigm</strong></td>
          <td>Third-Party Centralized</td>
          <td>Self-Hosted Private</td>
          <td>Third-Party Hybrid</td>
          <td><span class="badge badge-source">Self-Hosted Sovereign</span></td>
        </tr>
        <tr>
          <td><strong>Encryption Scheme</strong></td>
          <td>Server-Side AES-256</td>
          <td>Server-Side (Optional)</td>
          <td>Client-Side AES-256</td>
          <td><span class="badge badge-source">AES-256-GCM Envelope</span></td>
        </tr>
        <tr>
          <td><strong>Tamper Verification</strong></td>
          <td>TLS Transport Only</td>
          <td>HMAC / TLS</td>
          <td>HMAC-SHA256</td>
          <td><span class="badge badge-source">128-bit GHASH Tag</span></td>
        </tr>
        <tr>
          <td><strong>Large File Streaming</strong></td>
          <td>Proprietary CDN</td>
          <td>Chunked HTTP (Heavy)</td>
          <td>Virtual Drive Buffer</td>
          <td><span class="badge badge-source">16KB Piped Stream (2GB)</span></td>
        </tr>
        <tr>
          <td><strong>Forensic Metadata Search</strong></td>
          <td>Server Plaintext Scan</td>
          <td>Basic MIME / Tagging</td>
          <td>None (Opaque Blobs)</td>
          <td><span class="badge badge-source">Dynamic EXIF/Codec Chips</span></td>
        </tr>
        <tr>
          <td><strong>Client UI Design</strong></td>
          <td>Material / Proprietary</td>
          <td>Generic Mobile UI</td>
          <td>Basic File Browser</td>
          <td><span class="badge badge-source">Material 3 / Monet Theming</span></td>
        </tr>
        <tr>
          <td><strong>Pre-Upload Staging</strong></td>
          <td>None (Immediate Upload)</td>
          <td>None</td>
          <td>None</td>
          <td><span class="badge badge-source">Preview Dialog &amp; Delete</span></td>
        </tr>
        <tr>
          <td><strong>Duplicate Avoidance</strong></td>
          <td>Global Deduplication</td>
          <td>None</td>
          <td>None</td>
          <td><span class="badge badge-source">SHA-256 Atomic Constraint</span></td>
        </tr>
      </table>
      <div class="fig-caption">Table 2.1 &mdash; Comprehensive Comparative Architectural Evaluation</div>
'''
    pages.append(make_page(18, total_pages, "CHAPTER 2 &bull; PROBLEM ANALYSIS (3/5)", p18))

    # Page 19: 2.4 Structural Gaps
    p19 = '''
      <h1 class="ch-title">CHAPTER 2: PROBLEM ANALYSIS &amp; EXISTING SYSTEMS (CONT.)</h1>
      
      <h2 class="sec-title">2.4 Structural Gaps in Existing Mobile-First Systems</h2>
      <p>
        The comparative survey underscores four critical structural gaps that have hindered the adoption of secure, self-hosted mobile cloud storage:
      </p>

      <div class="callout-box">
        <div class="box-title">Gap 1: The "All-or-Nothing" Metadata Paradox</div>
        Traditional encryption tools treat metadata as an all-or-nothing proposition: either metadata is left completely exposed on the server in plaintext, or it is completely encrypted into an opaque ciphertext string. The former violates privacy; the latter renders mobile search impossible, forcing users to manually browse flat lists of random UUIDs. A balanced architectural approach that extracts safe forensic attributes while isolating payload data is missing.
      </div>

      <div class="callout-box">
        <div class="box-title">Gap 2: Inflexible Visual Identity and Outdated UI Paradigms</div>
        Security software has historically neglected user interface design, operating on dated Android Holo or basic Material 1 styles. Modern mobile users expect fluid gesture navigation, Edge-to-Edge display utilization, and dynamic theme harmonization with their operating system wallpaper (Material You Monet engine).
      </div>

      <div class="callout-box">
        <div class="box-title">Gap 3: Fragile Mobile Transfer Lifecycles</div>
        Mobile networks are inherently unstable, characterized by frequent transitions between Wi-Fi and cellular radios, signal dropouts, and aggressive background task pruning by Android OS battery optimization engines. Most open-source mobile tools execute transfers within standard background threads that are abruptly terminated by the operating system, corrupting partially written server files.
      </div>

      <div class="callout-box">
        <div class="box-title">Gap 4: Absence of Pre-Upload Inspection Controls</div>
        Standard cloud clients upload files immediately upon selection. Users cannot inspect the exact staged file, review its integrity hash, preview its content, or delete specific items from a multi-file batch before network transmission begins.
      </div>
'''
    pages.append(make_page(19, total_pages, "CHAPTER 2 &bull; PROBLEM ANALYSIS (4/5)", p19))

    # Page 20: 2.5 Proposed Paradigm, Feasibility
    p20 = '''
      <h1 class="ch-title">CHAPTER 2: PROBLEM ANALYSIS &amp; EXISTING SYSTEMS (CONT.)</h1>
      
      <h2 class="sec-title">2.5 The Proposed CipherVault Solution Paradigm</h2>
      <p>
        CipherVault resolves these structural gaps by introducing a cohesive, high-performance ecosystem designed specifically for modern mobile reality:
      </p>
      <ul>
        <li><strong>Envelope Key Wrapping:</strong> Client credentials and server master secrets derive a dual-layer key hierarchy, isolating user cryptographic domains.</li>
        <li><strong>Decoupled Forensic Metadata:</strong> Files are ingested into a streaming pipeline where EXIF and media headers are parsed into relational tables, while the payload is encrypted with AES-256-GCM.</li>
        <li><strong>Foreground Transfer Service:</strong> Transfers execute inside an Android Foreground Service with explicit notification channels, preventing OS process termination.</li>
      </ul>

      <h2 class="sec-title">2.6 Comprehensive Feasibility Analysis</h2>
      <table class="report-table">
        <tr>
          <th>Feasibility Dimension</th>
          <th>Analysis &amp; Evaluation Findings</th>
          <th>Feasibility Status</th>
        </tr>
        <tr>
          <td><strong>Technical Feasibility</strong></td>
          <td>Leverages proven enterprise frameworks: Spring Boot 3.3.4 (Java 21 LTS), Android Jetpack, Retrofit 2, OkHttp 4.12, and MySQL 8.0. All cryptographic primitives (AES-GCM, PBKDF2) are standard NIST specifications supported natively in the Java Cryptography Architecture (JCA).</td>
          <td><span class="badge badge-build">HIGHLY FEASIBLE</span></td>
        </tr>
        <tr>
          <td><strong>Economic Feasibility</strong></td>
          <td>Built entirely using permissive open-source frameworks (Apache 2.0, MIT, GPL). Eliminates expensive recurring multi-tenant cloud subscription fees by deploying on commodity host PC hardware.</td>
          <td><span class="badge badge-build">HIGHLY FEASIBLE</span></td>
        </tr>
        <tr>
          <td><strong>Operational Feasibility</strong></td>
          <td>The companion PySide6 Server Manager provides a one-click graphical interface for starting, stopping, and monitoring the backend service without requiring command-line Linux administration skills.</td>
          <td><span class="badge badge-build">HIGHLY FEASIBLE</span></td>
        </tr>
      </table>
'''
    pages.append(make_page(20, total_pages, "CHAPTER 2 &bull; PROBLEM ANALYSIS (5/5)", p20))

    # =========================================================================
    # CHAPTER 3: LITERATURE AND TECHNOLOGY REVIEW (Pages 21 to 25)
    # =========================================================================

    # Page 21: 3.1 Authenticated Cryptography
    p21 = '''
      <h1 class="ch-title">CHAPTER 3: LITERATURE &amp; TECHNOLOGY REVIEW</h1>
      
      <h2 class="sec-title">3.1 Authenticated Cryptography Standards (NIST SP 800-38D)</h2>
      <p>
        Traditional symmetric encryption schemes, such as AES in Cipher Block Chaining (CBC) mode or Counter (CTR) mode, provide confidentiality but fail to guarantee data integrity. Under CBC mode, an active network or storage attacker can manipulate ciphertext blocks (e.g., bit-flipping attacks) without detection, potentially leading to catastrophic padding oracle vulnerabilities.
      </p>
      <p>
        To eliminate this flaw, the National Institute of Standards and Technology (NIST) standardized <strong>Galois/Counter Mode (GCM)</strong> in Special Publication 800-38D. AES-GCM is an Authenticated Encryption with Associated Data (AEAD) algorithm that combines standard CTR mode confidentiality with a high-speed polynomial authenticator based on Galois field multiplication over $GF(2^{128})$:
      </p>

      <div class="security-box">
        <div class="box-title">Mathematical Formulation of AES-GCM GHASH Authentication</div>
        Given ciphertext blocks $C_1, C_2, \\dots, C_m$ and optional authenticated data $A$, the GHASH function computes an authentication tag $T$ using a hash subkey $H = E_K(0^{128})$:
        $$\\text{GHASH}_H(A, C) = \\sum_{i=1}^{m} C_i \\cdot H^{m-i+1} \\pmod{x^{128} + x^7 + x^2 + x + 1}$$
        The final 128-bit authentication tag $T$ is obtained by encrypting the pre-counter block with key $K$ and XORing the GHASH result.
      </div>

      <h3 class="subsec-title">Critical Nonce Uniqueness Requirements</h3>
      <p>
        The security of AES-GCM strictly depends on the uniqueness of the Initialization Vector (IV). If a 12-byte IV is ever reused with the same cryptographic key to encrypt two distinct plaintexts, the polynomial authenticator collapses, allowing an adversary to forge authentication tags and recover plaintext XOR differences. CipherVault strictly enforces IV uniqueness by generating a cryptographically secure random 12-byte nonce via <code class="inline">SecureRandom</code> for every single file upload.
      </p>
'''
    pages.append(make_page(21, total_pages, "CHAPTER 3 &bull; LITERATURE REVIEW (1/5)", p21))

    # Page 22: 3.2 Key Derivation & Hashing
    p22 = '''
      <h1 class="ch-title">CHAPTER 3: LITERATURE &amp; TECHNOLOGY REVIEW (CONT.)</h1>
      
      <h2 class="sec-title">3.2 Key Derivation and Password Hashing Standards</h2>
      <p>
        Human-memorable passwords possess low entropy and are vulnerable to dictionary attacks and specialized GPU brute-force clusters. Secure software architectures mandate computationally intensive Key Derivation Functions (KDFs) to convert passwords into cryptographic keys.
      </p>

      <h3 class="subsec-title">1. PBKDF2-HMAC-SHA256 (RFC 8018)</h3>
      <p>
        CipherVault utilizes PBKDF2 to derive the Master Server Key Encryption Key (KEK). PBKDF2 applies a pseudorandom function (HMAC-SHA256) to the input master secret along with a cryptographic salt across tens of thousands of iterations:
        $$DK = \\text{PBKDF2}(PRF, Password, Salt, c, dkLen)$$
        In CipherVault, the iteration count $c$ is configured to <strong>65,536 rounds</strong> with a 16-byte cryptographically secure salt, producing a 256-bit AES master key. This high work factor increases the computational cost of offline dictionary attacks by orders of magnitude.
      </p>

      <h3 class="subsec-title">2. BCrypt Password Hashing</h3>
      <p>
        For user authentication credentials stored in the database, CipherVault uses the BCrypt adaptive hashing function based on the Blowfish cipher. BCrypt incorporates a 128-bit salt and a configurable work factor ($2^{cost}$ iterations). CipherVault enforces a work factor of 10, producing a 60-character hash string (<code class="inline">$2a$10$...</code>). This ensures credential database breaches cannot be exploited via precomputed rainbow tables.
      </p>

      <h3 class="subsec-title">3. SHA-256 Cryptographic Hash (FIPS 180-4)</h3>
      <p>
        To ensure file integrity and implement duplicate detection, CipherVault computes the 256-bit Secure Hash Algorithm (SHA-256) over every file payload. SHA-256 provides strong collision resistance: finding two distinct files with identical digests requires approximately $2^{128}$ operations, making duplicate hash collisions mathematically impossible.
      </p>
'''
    pages.append(make_page(22, total_pages, "CHAPTER 3 &bull; LITERATURE REVIEW (2/5)", p22))

    # Page 23: 3.3 Android Ecosystem
    p23 = '''
      <h1 class="ch-title">CHAPTER 3: LITERATURE &amp; TECHNOLOGY REVIEW (CONT.)</h1>
      
      <h2 class="sec-title">3.3 Modern Android Client Ecosystem (Java 21 / SDK 37)</h2>
      <p>
        The Android client is engineered using modern Android development practices, leveraging the latest Android Jetpack libraries and Material 3 design principles:
      </p>

      <h3 class="subsec-title">1. Material Design 3 (Material You Monet Engine)</h3>
      <p>
        Introduced in Android 12, Material Design 3 (M3) revolutionizes interface styling through dynamic theming. The Monet engine extracts 65 tonal palettes from the user's personal wallpaper, applying harmonized primary, secondary, and surface accents across the application via <code class="inline">DynamicColors.applyToActivitiesIfAvailable()</code>. CipherVault fully embraces this paradigm, providing dynamic wallpaper theming on Android 12+ while maintaining a classic warm heritage beige/peach visual brand identity as a configurable fallback.
      </p>

      <h3 class="subsec-title">2. Hardware-Backed Security: Android Keystore &amp; BiometricPrompt</h3>
      <p>
        To safeguard sensitive tokens and master preferences, modern Android devices provide a hardware-isolated environment known as the <strong>Trusted Execution Environment (TEE)</strong> or <strong>StrongBox Keymaster</strong>. CipherVault integrates <code class="inline">androidx.security.crypto.EncryptedSharedPreferences</code> backed by a Keystore-generated <code class="inline">MasterKey</code> using AES-256-GCM. Plaintext session tokens are never written to unencrypted storage.
      </p>

      <h3 class="subsec-title">3. Networking &amp; HTTP Client: OkHttp 4.12 &amp; Retrofit 2</h3>
      <p>
        OkHttp provides connection pooling, transparent GZIP compression, and HTTP/2 multiplexing. Retrofit turns HTTP REST APIs into type-safe Java interfaces. To support massive multi-gigabyte uploads without network drops, OkHttp timeouts are tuned to 60 seconds, and transfer progress is routed through custom streaming <code class="inline">RequestBody</code> implementations.
      </p>
'''
    pages.append(make_page(23, total_pages, "CHAPTER 3 &bull; LITERATURE REVIEW (3/5)", p23))

    # Page 24: 3.4 Spring Boot Backend
    p24 = '''
      <h1 class="ch-title">CHAPTER 3: LITERATURE &amp; TECHNOLOGY REVIEW (CONT.)</h1>
      
      <h2 class="sec-title">3.4 Enterprise Backend Architecture (Spring Boot 3.3.4 &amp; Java 21)</h2>
      <p>
        The backend is constructed on the modern Spring ecosystem, combining high throughput, declarative security, and enterprise maintainability:
      </p>

      <h3 class="subsec-title">1. Spring Boot 3.3.4 &amp; Embedded Tomcat 10.1</h3>
      <p>
        Spring Boot 3.3.4 adopts Java 21 LTS as its baseline, taking full advantage of modern Java language features (records, pattern matching, enhanced switch expressions) and virtual threads (Project Loom) for lightweight I/O handling. Embedded Apache Tomcat 10.1 provides an optimized HTTP servlet engine with support for multipart chunked request streaming up to 2048 MB.
      </p>

      <h3 class="subsec-title">2. Spring Security 6 &amp; Stateless JWT Authentication</h3>
      <p>
        Spring Security 6 replaces legacy WebSecurityConfigurerAdapter classes with a component-based <code class="inline">SecurityFilterChain</code> bean. CipherVault enforces a purely stateless security architecture:
      </p>
      <ul>
        <li>Sessions are set to <code class="inline">SessionCreationPolicy.STATELESS</code>; no session cookies are stored in server RAM.</li>
        <li>Authentication is verified on every request by <code class="inline">JwtAuthenticationFilter</code>, validating HMAC-SHA256 (HS256) signatures.</li>
        <li>CSRF protection is disabled for stateless REST endpoints, eliminating CSRF token synchronization issues on mobile clients.</li>
      </ul>

      <h3 class="subsec-title">3. Spring Data JPA &amp; Hibernate 6</h3>
      <p>
        Data persistence is abstracted through Spring Data JPA repositories. Hibernate 6 automatically translates entity lifecycle states into optimized SQL queries, supporting atomic quota increments, cascading deletions, and Criteria API multi-attribute forensic queries.
      </p>
'''
    pages.append(make_page(24, total_pages, "CHAPTER 3 &bull; LITERATURE REVIEW (4/5)", p24))

    # Page 25: 3.5 Database & Metadata
    p25 = '''
      <h1 class="ch-title">CHAPTER 3: LITERATURE &amp; TECHNOLOGY REVIEW (CONT.)</h1>
      
      <h2 class="sec-title">3.5 Relational Storage, Indexing &amp; Forensic Metadata Engines</h2>
      <p>
        High-performance file retrieval requires tight coordination between relational database indexing and multimedia metadata extraction pipelines:
      </p>

      <h3 class="subsec-title">1. MySQL 8.0 InnoDB Engine &amp; B-Tree Indexing</h3>
      <p>
        MySQL 8.0 is selected for its robust ACID transactional guarantees, row-level locking, and efficient InnoDB storage engine. To support lightning-fast file lookup and deduplication across millions of records, CipherVault designs composite B-Tree indexes:
      </p>
      <ul>
        <li><code class="inline">uq_stored_files_user_sha256 (user_id, sha256_hash)</code>: Unique composite index enforcing duplicate prevention at the database level with $O(\\log N)$ lookup speed.</li>
        <li><code class="inline">idx_files_user_created (user_id, created_at DESC)</code>: Composite index optimizing chronological vault sorting without expensive temporary disk tables.</li>
      </ul>

      <h3 class="subsec-title">2. Multimedia Forensic Metadata Pipeline (ExifTool / Pure Java)</h3>
      <p>
        Digital media files encapsulate rich metadata inside header segments (EXIF, IPTC, XMP in JPEG/PNG, MOOV atom in MP4). CipherVault implements a hybrid extraction pipeline:
      </p>
      <ul>
        <li><strong>Pure Java Media Parser:</strong> Fast extraction of image dimensions, color space, and basic headers directly from decrypted input streams.</li>
        <li><strong>ExifTool Integration:</strong> Forensic command-line engine parsing over 15 distinct metadata attributes including camera make, camera model, ISO speed, shutter time, GPS latitude/longitude, audio codecs, and document authors.</li>
      </ul>
      <p>
        Extracted attributes are normalized into the <code class="inline">file_metadata</code> relational table, allowing users to execute rich SQL-backed searches without decrypting payloads.
      </p>
'''
    pages.append(make_page(25, total_pages, "CHAPTER 3 &bull; LITERATURE REVIEW (5/5)", p25))

    # =========================================================================
    # CHAPTER 4: REQUIREMENTS SPECIFICATION (Pages 26 to 30)
    # =========================================================================

    # Page 26: 4.1 FRS Part 1
    p26 = '''
      <h1 class="ch-title">CHAPTER 4: REQUIREMENTS SPECIFICATION</h1>
      
      <h2 class="sec-title">4.1 Functional Requirements Specification (FRS)</h2>
      <p>
        The functional requirements define the core operational capabilities implemented and verified in CipherVault. Requirements are categorized into functional modules:
      </p>

      <h3 class="subsec-title">Module 1: User Authentication &amp; Session Management</h3>
      <ul>
        <li><strong>FR-01 (User Registration):</strong> The system shall allow new users to register by supplying a unique username, unique email, and strong password (minimum 6 characters). The server shall hash passwords using BCrypt and generate an isolated 256-bit User Data Encryption Key (UDEK).</li>
        <li><strong>FR-02 (User Login &amp; Rate Limiting):</strong> The system shall authenticate users via email and password, returning a 24-hour stateless HS256 JWT token. The backend shall track failed login attempts, locking accounts for 15 minutes after 5 consecutive failures.</li>
        <li><strong>FR-03 (Hardware-Backed Session Persistence):</strong> The Android client shall securely persist JWT tokens inside Android Keystore-backed <code class="inline">EncryptedSharedPreferences</code>.</li>
        <li><strong>FR-04 (Biometric App Lock):</strong> The client shall enforce an app-lock overlay upon launch and background resume, unlocking only upon successful biometric fingerprint authentication or master PIN entry.</li>
      </ul>

      <h3 class="subsec-title">Module 2: Server Connection &amp; Health Monitoring</h3>
      <ul>
        <li><strong>FR-05 (Server Connection Configuration):</strong> The client shall allow users to configure host IP and port (default 8080), execute instant health probes (<code class="inline">/api/health</code>), and display live connection latency.</li>
        <li><strong>FR-06 (QR Code Quick Pairing):</strong> The client shall provide an integrated camera-based QR code scanner to parse server connection strings automatically.</li>
      </ul>

      <h3 class="subsec-title">Module 3: Pre-Upload Staging &amp; Media Capture</h3>
      <ul>
        <li><strong>FR-07 (Multi-File Selection &amp; Staging):</strong> The client shall support staging multiple files from device storage or camera capture into a dedicated staging queue before upload.</li>
        <li><strong>FR-08 (Pre-Upload Delete &amp; Preview):</strong> Users shall be able to preview staged media in a 240dp modal dialog (or full-screen) and remove specific items from staging before uploading.</li>
      </ul>
'''
    pages.append(make_page(26, total_pages, "CHAPTER 4 &bull; REQUIREMENTS (1/5)", p26))

    # Page 27: 4.1 FRS Part 2 (Table 4.1)
    p27 = '''
      <h1 class="ch-title">CHAPTER 4: REQUIREMENTS SPECIFICATION (CONT.)</h1>
      
      <h2 class="sec-title">Functional Requirements Specification (Cont.)</h2>

      <table class="report-table">
        <tr>
          <th style="width: 12%;">Req ID</th>
          <th style="width: 25%;">Functional Requirement</th>
          <th style="width: 48%;">Verification Criteria &amp; Expected Behavior</th>
          <th style="width: 15%;">Status</th>
        </tr>
        <tr>
          <td><strong>FR-09</strong></td>
          <td>AES-256-GCM Streaming Upload</td>
          <td>Payload streamed via 16KB buffer through CipherOutputStream; unique 12B IV generated; 128B GHASH tag written.</td>
          <td><span class="badge badge-source">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-10</strong></td>
          <td>SHA-256 Deduplication</td>
          <td>Calculates SHA-256 hash; checks DB unique constraint; rejects duplicates with HTTP 409 Conflict.</td>
          <td><span class="badge badge-source">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-11</strong></td>
          <td>Atomic Quota Enforcement</td>
          <td>Enforces 10 GB user ceiling and 1 GB per-batch limit; updates quota atomically in MySQL.</td>
          <td><span class="badge badge-source">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-12</strong></td>
          <td>Vault Browsing &amp; Sorting</td>
          <td>Renders grid/list layouts; supports sort by Date (Desc/Asc), File Size, and Name.</td>
          <td><span class="badge badge-runtime">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-13</strong></td>
          <td>Multi-Select Batch Delete</td>
          <td>Long-press activates selection mode; batch deletion cascades DB records and unlinks disk blobs.</td>
          <td><span class="badge badge-runtime">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-14</strong></td>
          <td>File Details Bottom Sheet</td>
          <td>Single tap opens expandable sheet showing EXIF, SHA-256 copy, Preview, Download, Delete.</td>
          <td><span class="badge badge-build">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-15</strong></td>
          <td>Decrypted In-Memory Viewer</td>
          <td>Streams decrypted media to RAM Bitmap cache with pinch-to-zoom; protected by FLAG_SECURE.</td>
          <td><span class="badge badge-runtime">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-16</strong></td>
          <td>Streaming File Download</td>
          <td>Streams decrypted payload to device DownloadManager; halts on GHASH tag mismatch.</td>
          <td><span class="badge badge-source">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-17</strong></td>
          <td>Forensic Metadata Search</td>
          <td>Dynamic suggestion chips (PDF, JPEG, Camera); multi-field JPA Criteria query across 15+ fields.</td>
          <td><span class="badge badge-runtime">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-18</strong></td>
          <td>Foreground Transfer Engine</td>
          <td>TransferManager executes transfers inside Android Foreground Service with notification progress.</td>
          <td><span class="badge badge-source">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-19</strong></td>
          <td>Dynamic Monet Theming</td>
          <td>Applies Android 12+ wallpaper color palette; allows toggle to warm heritage beige fallback.</td>
          <td><span class="badge badge-build">Verified</span></td>
        </tr>
        <tr>
          <td><strong>FR-20</strong></td>
          <td>Desktop Server Manager</td>
          <td>PySide6 GUI monitors Spring Boot PID, probes port 8080/3306, and tails live server logs.</td>
          <td><span class="badge badge-runtime">Verified</span></td>
        </tr>
      </table>
      <div class="fig-caption">Table 4.1 &mdash; Master Functional Requirements Matrix</div>
'''
    pages.append(make_page(27, total_pages, "CHAPTER 4 &bull; REQUIREMENTS (2/5)", p27))

    # Page 28: 4.2 NFRs
    p28 = '''
      <h1 class="ch-title">CHAPTER 4: REQUIREMENTS SPECIFICATION (CONT.)</h1>
      
      <h2 class="sec-title">4.2 Non-Functional Requirements (NFR)</h2>
      <p>
        Non-functional requirements specify qualitative benchmarks regarding system performance, security rigor, reliability, and usability:
      </p>

      <table class="report-table">
        <tr>
          <th style="width: 20%;">NFR Category</th>
          <th style="width: 25%;">Quality Attribute</th>
          <th style="width: 40%;">Specification Metric &amp; Target Benchmark</th>
          <th style="width: 15%;">Audit Status</th>
        </tr>
        <tr>
          <td><strong>NFR-01 Security</strong></td>
          <td>Cryptographic Strength</td>
          <td>AES-256-GCM authenticated encryption; 65,536-iteration PBKDF2; BCrypt cost 10; zero hardcoded secrets.</td>
          <td><span class="badge badge-source">COMPLIANT</span></td>
        </tr>
        <tr>
          <td><strong>NFR-02 Security</strong></td>
          <td>Tamper Detection</td>
          <td>Immediate abort via AEADBadTagException upon any single-bit ciphertext modification.</td>
          <td><span class="badge badge-source">COMPLIANT</span></td>
        </tr>
        <tr>
          <td><strong>NFR-03 Performance</strong></td>
          <td>Constant-Memory Streaming</td>
          <td>Client and server memory usage during transfer must not exceed 64 MB regardless of file size (up to 2 GB).</td>
          <td><span class="badge badge-source">COMPLIANT</span></td>
        </tr>
        <tr>
          <td><strong>NFR-04 Performance</strong></td>
          <td>Latency &amp; Response Time</td>
          <td>REST API response latency &lt; 150 ms for auth, vault listing, and search queries on LAN.</td>
          <td><span class="badge badge-runtime">COMPLIANT</span></td>
        </tr>
        <tr>
          <td><strong>NFR-05 Reliability</strong></td>
          <td>ACID Data Integrity</td>
          <td>Database operations wrapped in @Transactional; zero orphan files on failed uploads.</td>
          <td><span class="badge badge-source">COMPLIANT</span></td>
        </tr>
        <tr>
          <td><strong>NFR-06 Usability</strong></td>
          <td>Touch Target Accessibility</td>
          <td>All clickable UI controls conform to WCAG 2.1 &ge; 48dp minimum physical touch target size.</td>
          <td><span class="badge badge-runtime">COMPLIANT</span></td>
        </tr>
        <tr>
          <td><strong>NFR-07 Usability</strong></td>
          <td>Visual Contrast Ratio</td>
          <td>Text color contrast exceeds 4.5:1 ratio against background across Light and Dark themes.</td>
          <td><span class="badge badge-runtime">COMPLIANT</span></td>
        </tr>
        <tr>
          <td><strong>NFR-08 Maintainability</strong></td>
          <td>Modular Architecture</td>
          <td>Strict separation of concerns across Controller, Service, Repository, and Presentation layers.</td>
          <td><span class="badge badge-source">COMPLIANT</span></td>
        </tr>
      </table>
      <div class="fig-caption">Table 4.2 &mdash; Non-Functional Requirements Specification &amp; Verification Matrix</div>
'''
    pages.append(make_page(28, total_pages, "CHAPTER 4 &bull; REQUIREMENTS (3/5)", p28))

    # Page 29: 4.3 Use Case Modeling (Figure 4.1)
    p29 = '''
      <h1 class="ch-title">CHAPTER 4: REQUIREMENTS SPECIFICATION (CONT.)</h1>
      
      <h2 class="sec-title">4.3 Use Case Modeling</h2>
      <p>
        The interactions between the two primary system actors&mdash;the <strong>Mobile Vault User</strong> and the <strong>Host Server Administrator</strong>&mdash;and the CipherVault system boundary are formally modeled in Figure 4.1:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_17_use_case.svg" class="figure-img" alt="Use Case Diagram"/>
        <div class="fig-caption">FIGURE 4.1 &mdash; CIPHERVAULT USE CASE MODELING DIAGRAM</div>
        <div class="fig-desc">
          Actor-to-system use case boundaries: Mobile User handles authentication, staging, encryption, vault browsing, forensic search, and downloads; Host Administrator supervises backend processes, network port binding, and log monitoring.
        </div>
      </div>
'''
    pages.append(make_page(29, total_pages, "CHAPTER 4 &bull; REQUIREMENTS (4/5)", p29))

    # Page 30: 4.4 User Stories & Constraints
    p30 = '''
      <h1 class="ch-title">CHAPTER 4: REQUIREMENTS SPECIFICATION (CONT.)</h1>
      
      <h2 class="sec-title">4.4 User Stories and Acceptance Criteria</h2>
      
      <div style="margin-bottom: 8px;">
        <div style="font-weight: bold; color: #815621; font-size: 8.5pt;">US-01: PRE-UPLOAD STAGED INSPECTION</div>
        <div style="font-size: 8pt; font-style: italic; color: #2D241E; margin-bottom: 2px;">
          "As a privacy-conscious user, I want to preview staged files and remove unintended files before uploading, so that I never transmit sensitive files accidentally."
        </div>
        <div style="font-size: 7.8pt; color: #5A4334;">
          <strong>Acceptance Criteria:</strong> Tapping a staged item opens a 240dp modal preview dialog with full-screen option; tapping delete removes the item from the queue without initiating network transmission.
        </div>
      </div>

      <div style="margin-bottom: 8px;">
        <div style="font-weight: bold; color: #815621; font-size: 8.5pt;">US-02: FORENSIC METADATA DISCOVERY</div>
        <div style="font-size: 8pt; font-style: italic; color: #2D241E; margin-bottom: 2px;">
          "As a photographer, I want to filter encrypted vault photos by camera model or resolution chips, so that I can rapidly locate assets without downloading gigabytes of data."
        </div>
        <div style="font-size: 7.8pt; color: #5A4334;">
          <strong>Acceptance Criteria:</strong> Tapping suggestion chips (e.g. 'Google', '1280x960') executes a multi-attribute Criteria query, returning matching cards within 200 ms.
        </div>
      </div>

      <div style="margin-bottom: 8px;">
        <div style="font-weight: bold; color: #815621; font-size: 8.5pt;">US-03: BIOMETRIC APP LOCKING</div>
        <div style="font-size: 8pt; font-style: italic; color: #2D241E; margin-bottom: 2px;">
          "As a mobile user, I want the vault to lock automatically when the app is minimized, so that unauthorized persons cannot browse my decrypted files if I hand over my phone."
        </div>
        <div style="font-size: 7.8pt; color: #5A4334;">
          <strong>Acceptance Criteria:</strong> Cold launches and background resumption display a full-screen locked overlay, prompting for BiometricPrompt fingerprint or PIN unlock.
        </div>
      </div>

      <h2 class="sec-title">4.5 Hardware and Operational Constraints</h2>
      <ul>
        <li><strong>Client Hardware:</strong> Android smartphone running Android 12 (API 31) through Android 17 (API 37) with minimum 3 GB RAM and hardware biometric sensor.</li>
        <li><strong>Host Server Hardware:</strong> Windows/Linux PC running Java 21 LTS, MySQL 8.0, with minimum 4 GB RAM and local SSD storage for encrypted blobs.</li>
        <li><strong>Network Constraints:</strong> Operation over local Wi-Fi LAN or USB reverse ADB port forwarding (<code class="inline">tcp:8080</code>).</li>
      </ul>
'''
    pages.append(make_page(30, total_pages, "CHAPTER 4 &bull; REQUIREMENTS (5/5)", p30))

    # =========================================================================
    # CHAPTER 5: SYSTEM ARCHITECTURE AND DESIGN (Pages 31 to 36)
    # =========================================================================

    # Page 31: 5.1 System Context Diagram (Figure 5.1)
    p31 = '''
      <h1 class="ch-title">CHAPTER 5: SYSTEM ARCHITECTURE &amp; DESIGN</h1>
      
      <h2 class="sec-title">5.1 System Context &amp; Boundary Definition</h2>
      <p>
        The System Context Diagram establishes the highest-level operational boundary of CipherVault, defining external human actors, network boundaries, and persistence data stores:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_01_system_context.svg" class="figure-img" alt="System Context Diagram"/>
        <div class="fig-caption">FIGURE 5.1 &mdash; CIPHERVAULT SYSTEM CONTEXT DIAGRAM (LEVEL 0)</div>
        <div class="fig-desc">
          High-level operational boundaries: Mobile User interacts with Android Client; Host Administrator controls Spring Boot backend via PySide6 Server Manager; Data persistence is separated into MySQL relational metadata and isolated encrypted host filesystem blobs.
        </div>
      </div>

      <h3 class="subsec-title">System Boundary Highlights</h3>
      <p>
        The system isolates client and host responsibilities. The Android Client handles UI rendering, staging, and hardware biometric authentication. The Spring Boot backend exposes stateless REST APIs on port 8080, handling authenticated AES-256-GCM encryption, streaming I/O, and metadata parsing before delegating persistence to MySQL 8.0 and the host filesystem.
      </p>
'''
    pages.append(make_page(31, total_pages, "CHAPTER 5 &bull; ARCHITECTURE (1/6)", p31))

    # Page 32: 5.2 Multi-Tier System Architecture (Figure 5.2)
    p32 = '''
      <h1 class="ch-title">CHAPTER 5: SYSTEM ARCHITECTURE &amp; DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">5.2 Multi-Tier System Architecture</h2>
      <p>
        CipherVault implements a decomposed multi-tier architecture spanning Presentation, Application Core, and Persistence tiers, illustrated in Figure 5.2:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_02_overall_architecture.svg" class="figure-img" alt="Multi-Tier Architecture"/>
        <div class="fig-caption">FIGURE 5.2 &mdash; MULTI-TIER SYSTEM ARCHITECTURE</div>
        <div class="fig-desc">
          Tiered architectural decomposition: Tier 1 (Presentation &amp; Android Client), Tier 2 (Spring Boot 3.3.4 Application Core &amp; Cryptographic Services), and Tier 3 (MySQL 8.0 Persistence, Encrypted File Store, and Desktop Manager).
        </div>
      </div>
'''
    pages.append(make_page(32, total_pages, "CHAPTER 5 &bull; ARCHITECTURE (2/6)", p32))

    # Page 33: 5.3 Communication Protocol Flow (Figure 5.3)
    p33 = '''
      <h1 class="ch-title">CHAPTER 5: SYSTEM ARCHITECTURE &amp; DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">5.3 Android-to-Backend Communication Protocol Flow</h2>
      <p>
        The protocol exchange between the Android client and the Spring Boot backend utilizes HTTP/1.1 REST over port 8080, illustrated in Figure 5.3:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_05_client_backend_comm.svg" class="figure-img" alt="Communication Protocol Flow"/>
        <div class="fig-caption">FIGURE 5.3 &mdash; ANDROID-TO-BACKEND COMMUNICATION &amp; PROTOCOL FLOW</div>
        <div class="fig-desc">
          Network exchange pipeline: JSON request/response for authentication and metadata; multipart/form-data for chunked file upload; application/octet-stream for decrypted file downloads; Bearer token header injection across all endpoints.
        </div>
      </div>
'''
    pages.append(make_page(33, total_pages, "CHAPTER 5 &bull; ARCHITECTURE (3/6)", p33))

    # Page 34: 5.4 Data Flow Diagrams (Figure 5.4)
    p34 = '''
      <h1 class="ch-title">CHAPTER 5: SYSTEM ARCHITECTURE &amp; DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">5.4 Data Flow Diagrams (DFD Level 0 &amp; Level 1)</h2>
      <p>
        Data transformations across functional processes and storage boundaries are modeled using Level 0 and Level 1 Data Flow Diagrams in Figure 5.4:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_16_dfd_levels.svg" class="figure-img" alt="Data Flow Diagrams"/>
        <div class="fig-caption">FIGURE 5.4 &mdash; DATA FLOW DIAGRAMS (LEVEL 0 CONTEXT &amp; LEVEL 1 DECOMPOSITION)</div>
        <div class="fig-desc">
          Process decomposition: DFD Level 0 Context model; DFD Level 1 functional processes: 1.0 Authentication, 2.0 Upload Staging, 3.0 Encryption &amp; Ingestion, 4.0 Forensic Search, and 5.0 Decryption &amp; Streaming.
        </div>
      </div>
'''
    pages.append(make_page(34, total_pages, "CHAPTER 5 &bull; ARCHITECTURE (4/6)", p34))

    # Page 35: 5.5 Physical Deployment Topology (Figure 5.5)
    p35 = '''
      <h1 class="ch-title">CHAPTER 5: SYSTEM ARCHITECTURE &amp; DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">5.5 Physical Deployment and Network Topology</h2>
      <p>
        The concrete hardware execution nodes, transport channels, and software process bindings are formally documented in Figure 5.5:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_20_deployment.svg" class="figure-img" alt="Deployment Diagram"/>
        <div class="fig-caption">FIGURE 5.5 &mdash; PHYSICAL DEPLOYMENT &amp; NETWORK TOPOLOGY</div>
        <div class="fig-desc">
          Hardware deployment nodes: Android Device node executing ART VM and StrongBox TEE; Host PC node running Spring Boot JVM (Tomcat on port 8080), MySQL 8.0 Daemon on port 3306, and PySide6 Server Manager; interconnected via Wi-Fi LAN / ADB Reverse.
        </div>
      </div>
'''
    pages.append(make_page(35, total_pages, "CHAPTER 5 &bull; ARCHITECTURE (5/6)", p35))

    # Page 36: 5.6 Design Decisions & Trade-offs
    p36 = '''
      <h1 class="ch-title">CHAPTER 5: SYSTEM ARCHITECTURE &amp; DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">5.6 Architectural Trade-offs and Engineering Design Decisions</h2>
      <p>
        During the engineering of CipherVault, several pivotal design trade-offs were evaluated to reconcile performance, security, and usability:
      </p>

      <div class="callout-box">
        <div class="box-title">Trade-off 1: AES-256-GCM vs. ChaCha20-Poly1305</div>
        While ChaCha20-Poly1305 offers superior performance on devices lacking hardware AES acceleration, modern ARM64 mobile processors (ARMv8-A Cryptography Extensions) and x86_64 server CPUs (Intel AES-NI) provide dedicated silicon instructions for AES-GCM, achieving throughput exceeding 2.5 GB/s. AES-GCM was selected for broad hardware-accelerated compatibility and NIST SP 800-38D certification.
      </div>

      <div class="callout-box">
        <div class="box-title">Trade-off 2: Client-Derived UDEK vs. Server KEK Envelope Encryption</div>
        Deriving encryption keys purely on the mobile client prevents the server from ever assisting in thumbnail generation, deduplication, or metadata indexing. CipherVault adopts a balanced <strong>Envelope Encryption Hierarchy</strong>: the server wraps per-user UDEKs with a PBKDF2-derived Server KEK. This allows the backend to perform streaming encryption and thumbnail generation while cryptographically isolating individual user vaults.
      </div>

      <div class="callout-box">
        <div class="box-title">Trade-off 3: Streaming I/O vs. In-Memory Byte Buffering</div>
        In-memory encryption (<code class="inline">byte[]</code> arrays) is simpler to implement but causes fatal <code class="inline">OutOfMemoryError</code> crashes on files exceeding available Java heap space. CipherVault strictly enforces streaming I/O via <code class="inline">CipherOutputStream</code> and 16 KB chunks, achieving constant low memory consumption regardless of file scale.
      </div>

      <div class="callout-box">
        <div class="box-title">Trade-off 4: Dynamic Monet Theming vs. Static Brand Identity</div>
        Strict adherence to Material You Monet can wash out brand identity depending on user wallpaper. CipherVault addresses this by providing dynamic Monet wallpaper palettes on Android 12+ while supporting an instantaneous toggle back to the warm heritage beige/peach brand palette.
      </div>
'''
    pages.append(make_page(36, total_pages, "CHAPTER 5 &bull; ARCHITECTURE (6/6)", p36))

    return pages
