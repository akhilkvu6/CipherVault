"""
Preliminary Pages 1 to 10 for CipherVault Project Report.
Includes Cover, Title, Certificate, Declaration, Acknowledgements, Abstract,
Executive Summary, Table of Contents (2 pages), and List of Figures/Tables/Abbreviations.
"""

from .styles import make_page

def generate_preliminaries(total_pages=112):
    pages = []
    
    # ---------------- PAGE 1: COVER PAGE ----------------
    p1 = f'''
  <div class="page" id="page-1" style="background: linear-gradient(145deg, #FFF8F4 0%, #FFF1E8 100%); display: flex; flex-direction: column; justify-content: space-between; padding: 25mm 20mm 20mm 20mm;">
    <div style="border-top: 4px solid #815621; padding-top: 15px;">
      <div style="font-size: 11pt; letter-spacing: 2.5px; color: #815621; font-weight: bold; text-transform: uppercase;">
        MASTER OF COMPUTER APPLICATIONS &bull; FINAL PROJECT REPORT
      </div>
      <div style="font-size: 8.5pt; letter-spacing: 1.5px; color: #705D53; margin-top: 4px; text-transform: uppercase;">
        ACADEMIC YEAR 2025&ndash;2026 &bull; INDUSTRY-STANDARD ARCHITECTURE
      </div>
    </div>

    <div style="margin: 25px 0;">
      <div style="display: inline-block; background-color: #FEDDBD; color: #815621; padding: 4px 14px; border-radius: 20px; font-size: 8.5pt; font-weight: bold; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 12px; border: 1px solid #D6C2B4;">
        SECURE PRIVATE CLOUD STORAGE
      </div>
      <h1 style="font-size: 32pt; font-weight: 900; color: #815621; margin: 0; line-height: 1.1; letter-spacing: -0.5px;">
        CIPHERVAULT
      </h1>
      <div style="font-size: 14pt; font-weight: 600; color: #2D241E; margin-top: 8px;">
        A Secure Private Cloud Storage Android Application with Zero-Knowledge Architecture
      </div>
      <div style="font-size: 10pt; color: #705D53; margin-top: 12px; max-width: 90%; line-height: 1.5;">
        Native Android Client (Material 3 / Java 21 / SDK 37), Spring Boot 3.3.4 Cryptographic Backend, MySQL 8.0 Forensic Indexing, AES-256-GCM Envelope Encryption &amp; Streaming Chunk Pipeline.
      </div>
    </div>

    <div style="background-color: #FFFFFF; border: 1px solid #E6D7CD; border-radius: 8px; padding: 18px 22px; box-shadow: 0 4px 12px rgba(129, 86, 33, 0.06);">
      <div style="display: flex; justify-content: space-between; border-bottom: 1px solid #F0E4DC; padding-bottom: 10px; margin-bottom: 10px;">
        <div>
          <div style="font-size: 7.5pt; text-transform: uppercase; color: #8C7B70; letter-spacing: 0.8px;">Submitted By</div>
          <div style="font-size: 10pt; font-weight: bold; color: #2D241E;">[CANDIDATE NAME PLACEHOLDER]</div>
          <div style="font-size: 8pt; color: #705D53;">Roll No: [ROLL NO] &bull; Reg No: [REGISTRATION NO]</div>
        </div>
        <div style="text-align: right;">
          <div style="font-size: 7.5pt; text-transform: uppercase; color: #8C7B70; letter-spacing: 0.8px;">Under Guidance Of</div>
          <div style="font-size: 10pt; font-weight: bold; color: #2D241E;">[FACULTY GUIDE NAME PLACEHOLDER]</div>
          <div style="font-size: 8pt; color: #705D53;">[DESIGNATION / DEPARTMENT PLACEHOLDER]</div>
        </div>
      </div>
      <div style="text-align: center; font-size: 8.5pt; color: #5A4334; margin-top: 8px;">
        <strong>DEPARTMENT OF COMPUTER APPLICATIONS</strong><br/>
        [INSTITUTION / UNIVERSITY NAME PLACEHOLDER]<br/>
        [CAMPUS ADDRESS / CITY / STATE / PIN]
      </div>
    </div>

    <div style="border-top: 1px solid #E6D7CD; padding-top: 10px; display: flex; justify-content: space-between; font-size: 8pt; color: #8C7B70;">
      <span>Document ID: CV-MCA-REP-2026-FINAL</span>
      <span>Confidential &bull; Academic Evaluation Copy</span>
      <span>October 2026</span>
    </div>
  </div>
'''
    pages.append(p1)

    # ---------------- PAGE 2: TITLE PAGE ----------------
    p2_content = '''
      <div style="text-align: center; margin-top: 25mm;">
        <div style="font-size: 11pt; letter-spacing: 2px; color: #815621; font-weight: bold; text-transform: uppercase;">
          A PROJECT REPORT ON
        </div>
        <h1 style="font-size: 24pt; font-weight: 800; color: #815621; margin: 15px 0 5px 0;">
          CIPHERVAULT
        </h1>
        <div style="font-size: 12pt; font-weight: 600; color: #2D241E;">
          A Secure Private Cloud Storage Android Application with Zero-Knowledge Architecture
        </div>
        
        <div style="margin: 30px auto; width: 60px; height: 3px; background-color: #FEDDBD;"></div>
        
        <div style="font-size: 9pt; color: #5A4334; line-height: 1.6; max-width: 80%; margin: 0 auto;">
          Submitted in partial fulfillment of the requirements for the award of the degree of<br/>
          <strong style="font-size: 11pt; color: #2D241E;">MASTER OF COMPUTER APPLICATIONS (MCA)</strong>
        </div>
        
        <div style="margin: 40px auto; font-size: 9pt; line-height: 1.6;">
          <div style="color: #705D53;">Submitted by:</div>
          <div style="font-size: 12pt; font-weight: bold; color: #2D241E; margin-top: 3px;">[STUDENT NAME PLACEHOLDER]</div>
          <div style="color: #705D53;">Register No: [REGISTER NO] &bull; Roll No: [ROLL NO]</div>
        </div>

        <div style="margin: 30px auto; font-size: 9pt; line-height: 1.6;">
          <div style="color: #705D53;">Under the Esteemed Guidance of:</div>
          <div style="font-size: 12pt; font-weight: bold; color: #2D241E; margin-top: 3px;">[INTERNAL GUIDE NAME PLACEHOLDER]</div>
          <div style="color: #705D53;">[Designation], Department of Computer Applications</div>
        </div>

        <div style="margin-top: 45px; border-top: 1px solid #E6D7CD; padding-top: 20px; font-size: 9.5pt; color: #2D241E;">
          <strong>DEPARTMENT OF COMPUTER APPLICATIONS</strong><br/>
          <strong>[INSTITUTION / COLLEGE NAME PLACEHOLDER]</strong><br/>
          <span style="font-size: 8.5pt; color: #705D53;">[Affiliated to University Name Placeholder &bull; Approved by AICTE]</span><br/>
          <span style="font-size: 8.5pt; color: #705D53;">October 2026</span>
        </div>
      </div>
'''
    pages.append(make_page(2, total_pages, "PRELIMINARIES &bull; TITLE PAGE", p2_content, True))

    # ---------------- PAGE 3: CERTIFICATE ----------------
    p3_content = '''
      <div style="text-align: center; margin-bottom: 15px;">
        <div style="font-size: 12pt; font-weight: bold; color: #2D241E;">[NAME OF THE INSTITUTION / COLLEGE PLACEHOLDER]</div>
        <div style="font-size: 9pt; color: #705D53;">[Accredited by NAAC with 'A+' Grade &bull; Approved by AICTE]</div>
        <div style="font-size: 10pt; font-weight: bold; color: #815621; margin-top: 4px;">DEPARTMENT OF COMPUTER APPLICATIONS</div>
      </div>
      
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 15px;">BONAFIDE CERTIFICATE</h1>

      <p style="text-indent: 30px; line-height: 1.7; font-size: 9.2pt;">
        This is to certify that the project report entitled <strong>&ldquo;CIPHERVAULT: A SECURE PRIVATE CLOUD STORAGE ANDROID APPLICATION WITH ZERO-KNOWLEDGE ARCHITECTURE&rdquo;</strong> is a bonafide record of independent project work carried out by <strong>[STUDENT NAME PLACEHOLDER]</strong> (Register Number: <strong>[REGISTER NO PLACEHOLDER]</strong>), in partial fulfillment of the requirements for the award of the degree of <strong>Master of Computer Applications (MCA)</strong> during the academic year <strong>2025&ndash;2026</strong>.
      </p>

      <p style="text-indent: 30px; line-height: 1.7; font-size: 9.2pt;">
        The project work embodies the original work of the candidate, carried out under my supervision and guidance. The contents of this report have not been submitted to any other University or Institution for the award of any degree or diploma.
      </p>

      <div style="margin-top: 45px; display: flex; justify-content: space-between;">
        <div style="text-align: center; width: 45%;">
          <div style="border-bottom: 1px dotted #8C7B70; width: 180px; margin: 0 auto 5px auto;"></div>
          <div style="font-size: 9.5pt; font-weight: bold; color: #2D241E;">[INTERNAL GUIDE NAME]</div>
          <div style="font-size: 8pt; color: #705D53;">Internal Faculty Guide<br/>Assistant Professor, Dept of MCA</div>
        </div>
        <div style="text-align: center; width: 45%;">
          <div style="border-bottom: 1px dotted #8C7B70; width: 180px; margin: 0 auto 5px auto;"></div>
          <div style="font-size: 9.5pt; font-weight: bold; color: #2D241E;">[HEAD OF DEPARTMENT NAME]</div>
          <div style="font-size: 8pt; color: #705D53;">Head of the Department<br/>Department of Computer Applications</div>
        </div>
      </div>

      <div style="margin-top: 40px; padding: 12px; background-color: #FFF5EE; border: 1px solid #E6D7CD; border-radius: 4px;">
        <div style="font-size: 8.5pt; font-weight: bold; color: #815621; margin-bottom: 8px;">VIVA-VOCE EXAMINATION RECORD:</div>
        <div style="font-size: 8.2pt; color: #2D241E; line-height: 1.6;">
          Submitted for the Project Viva-Voce examination held on: <strong>[EXAMINATION DATE PLACEHOLDER]</strong>
        </div>
        <div style="display: flex; justify-content: space-between; margin-top: 25px;">
          <div style="text-align: center; width: 45%;">
            <div style="border-bottom: 1px dotted #8C7B70; width: 160px; margin: 0 auto 5px auto;"></div>
            <div style="font-size: 8.5pt; font-weight: bold;">INTERNAL EXAMINER</div>
          </div>
          <div style="text-align: center; width: 45%;">
            <div style="border-bottom: 1px dotted #8C7B70; width: 160px; margin: 0 auto 5px auto;"></div>
            <div style="font-size: 8.5pt; font-weight: bold;">EXTERNAL EXAMINER</div>
          </div>
        </div>
      </div>
'''
    pages.append(make_page(3, total_pages, "PRELIMINARIES &bull; BONAFIDE CERTIFICATE", p3_content, True))

    # ---------------- PAGE 4: DECLARATION ----------------
    p4_content = '''
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 25px;">DECLARATION OF ORIGINALITY</h1>

      <p style="text-indent: 30px; line-height: 1.8; font-size: 9.5pt;">
        I, <strong>[STUDENT NAME PLACEHOLDER]</strong>, hereby declare that the project report entitled <strong>&ldquo;CipherVault: A Secure Private Cloud Storage Android Application with Zero-Knowledge Architecture&rdquo;</strong>, submitted to the <strong>Department of Computer Applications, [INSTITUTION NAME PLACEHOLDER]</strong>, in partial fulfillment of the requirements for the award of the degree of <strong>Master of Computer Applications (MCA)</strong>, is an authentic record of independent technical research, design, implementation, and audit conducted by me during the academic year <strong>2025&ndash;2026</strong>.
      </p>

      <p style="text-indent: 30px; line-height: 1.8; font-size: 9.5pt;">
        I solemnly affirm that the source code, technical diagrams, architectural workflows, and documentation presented in this report represent actual software engineered in accordance with rigorous academic and software engineering standards. All external libraries, open-source frameworks (including Spring Boot, OkHttp, Retrofit, ExifTool, and Android Jetpack), research papers, and technical specifications used in this project have been fully acknowledged and duly cited in the References and Bibliography.
      </p>

      <p style="text-indent: 30px; line-height: 1.8; font-size: 9.5pt;">
        I further declare that this report has not formed the basis for the award of any Degree, Diploma, Associateship, Fellowship, or other similar title in this or any other University or Institution.
      </p>

      <div style="margin-top: 60px; display: flex; justify-content: space-between; align-items: flex-end;">
        <div>
          <div style="font-size: 9pt; color: #2D241E;"><strong>Place:</strong> [CITY / LOCATION PLACEHOLDER]</div>
          <div style="font-size: 9pt; color: #2D241E; margin-top: 4px;"><strong>Date:</strong> [DATE PLACEHOLDER], 2026</div>
        </div>
        <div style="text-align: center;">
          <div style="border-bottom: 1px dotted #8C7B70; width: 200px; margin-bottom: 5px;"></div>
          <div style="font-size: 10pt; font-weight: bold; color: #2D241E;">[SIGNATURE OF THE CANDIDATE]</div>
          <div style="font-size: 9pt; color: #5A4334;">[STUDENT NAME PLACEHOLDER]</div>
          <div style="font-size: 8.5pt; color: #705D53;">Register No: [REGISTER NUMBER]</div>
        </div>
      </div>
'''
    pages.append(make_page(4, total_pages, "PRELIMINARIES &bull; DECLARATION", p4_content, True))

    # ---------------- PAGE 5: ACKNOWLEDGEMENTS ----------------
    p5_content = '''
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 20px;">ACKNOWLEDGEMENTS</h1>

      <p style="line-height: 1.7; font-size: 9pt;">
        The completion of this major academic project, <strong>CipherVault</strong>, marks a significant milestone in my professional and technical journey. I express my profound gratitude to all the individuals whose guidance, expertise, and continuous encouragement made this comprehensive software engineering endeavor possible.
      </p>

      <p style="line-height: 1.7; font-size: 9pt;">
        First and foremost, I express my deepest gratitude to our respected Principal, <strong>[PRINCIPAL NAME PLACEHOLDER]</strong>, and Management of <strong>[INSTITUTION NAME PLACEHOLDER]</strong>, for providing an exemplary academic atmosphere, state-of-the-art laboratory computing infrastructure, and access to the technical resources that enabled this project.
      </p>

      <p style="line-height: 1.7; font-size: 9pt;">
        I express my sincere thanks to <strong>[HOD NAME PLACEHOLDER]</strong>, Head of the Department of Computer Applications, for his visionary leadership, academic encouragement, and insightful feedback during project milestones.
      </p>

      <p style="line-height: 1.7; font-size: 9pt;">
        Words cannot adequately express my heartfelt indebtedness to my internal project guide, <strong>[INTERNAL GUIDE NAME PLACEHOLDER]</strong>, [Designation], Department of Computer Applications. His invaluable constructive criticism, deep architectural insights in distributed cryptography, and persistent guidance in refining Android and Spring Boot workflows were instrumental in shaping CipherVault into an industry-standard product.
      </p>

      <p style="line-height: 1.7; font-size: 9pt;">
        I extend my warm gratitude to all faculty members and technical staff of the Department of Computer Applications for their unceasing cooperation and moral support.
      </p>

      <p style="line-height: 1.7; font-size: 9pt;">
        Finally, I owe my deepest gratitude to my parents and family members for their unconditional sacrifices, moral strength, and patience throughout my education. I also thank my peers and fellow MCA colleagues for their vibrant technical discussions and continuous encouragement.
      </p>

      <div style="margin-top: 35px; text-align: right;">
        <div style="font-size: 10pt; font-weight: bold; color: #815621;">[STUDENT NAME PLACEHOLDER]</div>
        <div style="font-size: 8.5pt; color: #705D53;">Department of Computer Applications</div>
      </div>
'''
    pages.append(make_page(5, total_pages, "PRELIMINARIES &bull; ACKNOWLEDGEMENTS", p5_content, True))

    # ---------------- PAGE 6: ABSTRACT ----------------
    p6_content = '''
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 20px;">ABSTRACT</h1>

      <p style="text-indent: 25px; line-height: 1.7; font-size: 9.1pt;">
        Modern mobile users and organizations face a severe privacy paradox when utilizing commercial cloud storage platforms. Proprietary cloud providers possess unilateral custody of encryption keys, subjecting private files to warrantless surveillance, unauthorized machine-learning dataset scraping, accidental data leaks, and server-side compromise. While client-side cryptographic utilities exist, they typically exhibit cumbersome user interfaces, catastrophic memory overhead on large media payloads, and lack mobile-first forensic search capabilities.
      </p>

      <p style="text-indent: 25px; line-height: 1.7; font-size: 9.1pt;">
        This project presents <strong>CipherVault</strong>, an enterprise-grade, private, self-hosted cloud storage ecosystem engineered with a zero-knowledge architectural paradigm. CipherVault bridges the divide between uncompromising authenticated cryptography and native mobile user experience. The system integrates a native Android client adhering strictly to Material Design 3 guidelines (Java 21 / Android SDK 37) with a high-throughput Spring Boot 3.3.4 micro-backend and MySQL 8.0 relational persistence.
      </p>

      <p style="text-indent: 25px; line-height: 1.7; font-size: 9.1pt;">
        CipherVault protects data using a dual-layer envelope encryption hierarchy: a Server Key Encryption Key (KEK) derived via PBKDF2-HMAC-SHA256 (65,536 iterations) wraps individual 256-bit User Data Encryption Keys (UDEKs). File payloads are encrypted using NIST-standard AES-256 in Galois/Counter Mode (GCM) with unique 12-byte initialization vectors and 128-bit GHASH authentication tags. Large file transfers (up to 2 GB) are processed through an isolated 16 KB chunked streaming pipeline (<code class="inline">CipherOutputStream</code> / <code class="inline">CipherInputStream</code>), ensuring constant low memory consumption regardless of file size.
      </p>

      <p style="text-indent: 25px; line-height: 1.7; font-size: 9.1pt;">
        Furthermore, CipherVault introduces pre-upload client-side staging with decrypted in-memory previews, SHA-256 duplicate avoidance, biometric hardware authentication (<code class="inline">BiometricPrompt</code> / StrongBox Keystore), dynamic Monet wallpaper theming with beige fallback, and a rich multimedia forensic engine capable of querying EXIF, GPS, camera model, and audio/video codecs. Rigorous testing (108 backend tests, 22 Android unit tasks, and a 155-state UI regression pass on Google Pixel 10 Pro XL) proves CipherVault delivers robust, zero-corruption data sovereignty.
      </p>

      <div style="margin-top: 25px; padding: 10px; background-color: #FFF5EE; border: 1px solid #FEDDBD; border-radius: 4px;">
        <strong style="color: #815621; font-size: 8.5pt;">Keywords:</strong>
        <span style="font-size: 8.2pt; color: #2D241E;">
          Private Cloud Storage, Zero-Knowledge Architecture, AES-256-GCM, Envelope Encryption, Spring Boot 3.3.4, Material Design 3, Android Jetpack, SHA-256 Deduplication, ExifTool Forensic Metadata, PBKDF2 Key Derivation.
        </span>
      </div>
'''
    pages.append(make_page(6, total_pages, "PRELIMINARIES &bull; ABSTRACT", p6_content, True))

    # ---------------- PAGE 7: EXECUTIVE SUMMARY ----------------
    p7_content = '''
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 15px;">EXECUTIVE SUMMARY</h1>

      <div class="callout-box">
        <div class="box-title">Core Value Proposition</div>
        CipherVault delivers sovereign, end-to-end encrypted personal cloud storage that combines military-grade authenticated cryptography with an ultra-responsive native Android client. It solves third-party data custody risks without sacrificing transfer performance, mobile ergonomics, or forensic searchability.
      </div>

      <h2 class="sec-title">Architectural Highlights &amp; Verified Engineering Results</h2>
      <ul style="font-size: 8.6pt; line-height: 1.55;">
        <li><strong>Native Android Client (Java 21, SDK 37, Gradle 9.6):</strong> Built on Material Design 3 with full Edge-to-Edge window insets. Implements dynamic Material You Monet wallpaper theming on Android 12+ alongside a warm heritage beige/peach visual brand identity.</li>
        <li><strong>High-Throughput Spring Boot 3.3.4 Backend:</strong> Embedded Tomcat 10.1 container with Spring Security 6 stateless JWT authorization. Manages atomic storage accounting (10 GB user ceiling, 1 GB batch limit) and rate-limited brute-force protection (5 failed attempts trigger 15-minute lockout).</li>
        <li><strong>NIST SP 800-38D Authenticated Encryption:</strong> Streaming AES-256-GCM encryption with 12-byte cryptographically secure random nonces and 128-bit GHASH authentication tags. Detects single-bit disk corruption or tampering via <code class="inline">AEADBadTagException</code>.</li>
        <li><strong>Zero-Memory-Exhaustion Chunked Streaming:</strong> Piped 16 KB streaming I/O prevents Java heap crashes during massive 2 GB multi-gigabyte uploads and downloads.</li>
        <li><strong>Forensic Metadata Extraction &amp; Dynamic Search:</strong> Pure Java and ExifTool pipelines parse image EXIF, GPS coordinates, camera hardware models, video bitrates, and audio tags into indexed MySQL columns, exposed via dynamic suggestion chips.</li>
        <li><strong>Pre-Upload Staging &amp; Vault Ergonomics:</strong> Features pre-upload item deletion, in-dialog preview surfaces, expandable <code class="inline">FileDetailsBottomSheet</code>, multi-select batch deletion, and one-tap SHA-256 clipboard verification.</li>
      </ul>

      <h2 class="sec-title">Verification &amp; Quality Assurance Metrics</h2>
      <table class="report-table">
        <tr>
          <th>Verification Domain</th>
          <th>Tooling &amp; Environment</th>
          <th>Coverage Metric</th>
          <th>Audit Status</th>
        </tr>
        <tr>
          <td><strong>Backend Automated Tests</strong></td>
          <td>JUnit 5 &bull; Mockito &bull; Maven Wrapper</td>
          <td>108 Unit / Integration Tests</td>
          <td><span class="badge badge-build">100% PASS (0 Errors)</span></td>
        </tr>
        <tr>
          <td><strong>Android Unit Tests</strong></td>
          <td>Robolectric &bull; JUnit 4 &bull; Gradle 9.6</td>
          <td>22 Test Tasks Passed</td>
          <td><span class="badge badge-build">100% PASS (0 Errors)</span></td>
        </tr>
        <tr>
          <td><strong>UI Regression &amp; Visual QA</strong></td>
          <td>Pixel 10 Pro XL &bull; Android 17 (API 37)</td>
          <td>155 Independent Frame Captures</td>
          <td><span class="badge badge-runtime">RUNTIME VERIFIED</span></td>
        </tr>
        <tr>
          <td><strong>Desktop Server Manager</strong></td>
          <td>PySide6 &bull; Python 3.14 &bull; TCP Sockets</td>
          <td>Port 8080/3306 &amp; Process PID</td>
          <td><span class="badge badge-runtime">RUNTIME VERIFIED</span></td>
        </tr>
      </table>
'''
    pages.append(make_page(7, total_pages, "PRELIMINARIES &bull; EXECUTIVE SUMMARY", p7_content, True))

    # ---------------- PAGE 8: TABLE OF CONTENTS (PART 1) ----------------
    p8_content = '''
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 12px;">TABLE OF CONTENTS</h1>

      <table class="report-table" style="font-size: 8pt;">
        <tr style="background-color: #FEDDBD;">
          <th style="width: 15%;">Section</th>
          <th style="width: 73%;">Title &amp; Descriptive Scope</th>
          <th style="width: 12%; text-align: right;">Page</th>
        </tr>
        <tr>
          <td><strong>Preliminaries</strong></td>
          <td>Cover Page &bull; Title &bull; Bonafide Certificate &bull; Declaration &bull; Acknowledgements</td>
          <td style="text-align: right;">1 &ndash; 5</td>
        </tr>
        <tr>
          <td></td>
          <td>Abstract &bull; Executive Summary &bull; Table of Contents &bull; Lists of Figures/Tables</td>
          <td style="text-align: right;">6 &ndash; 10</td>
        </tr>
        <tr>
          <td><strong>Chapter 1</strong></td>
          <td><strong>INTRODUCTION</strong></td>
          <td style="text-align: right;"><strong>11</strong></td>
        </tr>
        <tr>
          <td>1.1 &ndash; 1.3</td>
          <td>Project Background &bull; Industry Motivation &bull; Problem Statement</td>
          <td style="text-align: right;">11 &ndash; 12</td>
        </tr>
        <tr>
          <td>1.4 &ndash; 1.7</td>
          <td>Objectives &bull; Operational Scope &bull; User Personas &bull; Document Roadmap</td>
          <td style="text-align: right;">13 &ndash; 15</td>
        </tr>
        <tr>
          <td><strong>Chapter 2</strong></td>
          <td><strong>PROBLEM ANALYSIS AND EXISTING SYSTEMS</strong></td>
          <td style="text-align: right;"><strong>16</strong></td>
        </tr>
        <tr>
          <td>2.1 &ndash; 2.3</td>
          <td>Traditional Cloud Dilemma &bull; Survey of Existing Cloud Providers &bull; Comparative Matrix</td>
          <td style="text-align: right;">16 &ndash; 18</td>
        </tr>
        <tr>
          <td>2.4 &ndash; 2.6</td>
          <td>Structural Gaps &bull; Proposed Solution Paradigm &bull; Feasibility Analysis</td>
          <td style="text-align: right;">19 &ndash; 20</td>
        </tr>
        <tr>
          <td><strong>Chapter 3</strong></td>
          <td><strong>LITERATURE AND TECHNOLOGY REVIEW</strong></td>
          <td style="text-align: right;"><strong>21</strong></td>
        </tr>
        <tr>
          <td>3.1 &ndash; 3.3</td>
          <td>NIST Authenticated Cryptography &bull; Key Derivation (PBKDF2/BCrypt) &bull; Android M3</td>
          <td style="text-align: right;">21 &ndash; 23</td>
        </tr>
        <tr>
          <td>3.4 &ndash; 3.6</td>
          <td>Spring Boot 3.3.4 &bull; MySQL Relational Indexing &bull; Forensic Metadata Engines</td>
          <td style="text-align: right;">24 &ndash; 25</td>
        </tr>
        <tr>
          <td><strong>Chapter 4</strong></td>
          <td><strong>REQUIREMENTS SPECIFICATION</strong></td>
          <td style="text-align: right;"><strong>26</strong></td>
        </tr>
        <tr>
          <td>4.1 &ndash; 4.2</td>
          <td>Functional Requirements (FR-01 to FR-20) &bull; Non-Functional Requirements (NFR)</td>
          <td style="text-align: right;">26 &ndash; 28</td>
        </tr>
        <tr>
          <td>4.3 &ndash; 4.5</td>
          <td>Use Case Modeling [Figure 4.1] &bull; User Stories &bull; System Constraints</td>
          <td style="text-align: right;">29 &ndash; 30</td>
        </tr>
        <tr>
          <td><strong>Chapter 5</strong></td>
          <td><strong>SYSTEM ARCHITECTURE AND DESIGN</strong></td>
          <td style="text-align: right;"><strong>31</strong></td>
        </tr>
        <tr>
          <td>5.1 &ndash; 5.2</td>
          <td>System Context [Figure 5.1] &bull; Multi-Tier Architecture [Figure 5.2]</td>
          <td style="text-align: right;">31 &ndash; 32</td>
        </tr>
        <tr>
          <td>5.3 &ndash; 5.5</td>
          <td>Communication Flow [Figure 5.3] &bull; DFD Levels [Figure 5.4] &bull; Deployment [Figure 5.5]</td>
          <td style="text-align: right;">33 &ndash; 35</td>
        </tr>
        <tr>
          <td>5.6</td>
          <td>Architectural Trade-offs &amp; Engineering Design Decisions</td>
          <td style="text-align: right;">36</td>
        </tr>
        <tr>
          <td><strong>Chapter 6</strong></td>
          <td><strong>ANDROID APPLICATION DESIGN</strong></td>
          <td style="text-align: right;"><strong>37</strong></td>
        </tr>
        <tr>
          <td>6.1 &ndash; 6.3</td>
          <td>Component Architecture [Figure 6.1] &bull; App Lifecycle &bull; Dynamic Theming</td>
          <td style="text-align: right;">37 &ndash; 39</td>
        </tr>
        <tr>
          <td>6.4 &ndash; 6.5</td>
          <td>Navigation Map [Figure 6.2] &bull; ViewHolder Lifecycle Safety Architecture</td>
          <td style="text-align: right;">40 &ndash; 41</td>
        </tr>
        <tr>
          <td>6.6 &ndash; 6.8</td>
          <td>5-Screen Onboarding Flow &bull; AppLock Biometric Security System</td>
          <td style="text-align: right;">42 &ndash; 44</td>
        </tr>
        <tr>
          <td><strong>Chapter 7</strong></td>
          <td><strong>BACKEND DESIGN AND IMPLEMENTATION</strong></td>
          <td style="text-align: right;"><strong>45</strong></td>
        </tr>
        <tr>
          <td>7.1 &ndash; 7.4</td>
          <td>Layered Architecture [Figure 7.1] &bull; Configuration &bull; Security &bull; REST Routing</td>
          <td style="text-align: right;">45 &ndash; 48</td>
        </tr>
        <tr>
          <td>7.5 &ndash; 7.7</td>
          <td>Service Business Logic &bull; 2GB Streaming Engine &bull; Exception Handling</td>
          <td style="text-align: right;">49 &ndash; 51</td>
        </tr>
        <tr>
          <td><strong>Chapter 8</strong></td>
          <td><strong>DATABASE DESIGN</strong></td>
          <td style="text-align: right;"><strong>52</strong></td>
        </tr>
        <tr>
          <td>8.1 &ndash; 8.4</td>
          <td>Schema Topology &bull; ER Diagram [Figure 8.1] &bull; Detailed Table Specifications</td>
          <td style="text-align: right;">52 &ndash; 55</td>
        </tr>
        <tr>
          <td>8.5 &ndash; 8.6</td>
          <td>B-Tree Hash Indexing &bull; Atomic Quotas &bull; Cascade Deletion Lifecycle</td>
          <td style="text-align: right;">56 &ndash; 57</td>
        </tr>
      </table>
'''
    pages.append(make_page(8, total_pages, "PRELIMINARIES &bull; TABLE OF CONTENTS (1/2)", p8_content, True))

    # ---------------- PAGE 9: TABLE OF CONTENTS (PART 2) ----------------
    p9_content = '''
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 12px;">TABLE OF CONTENTS (CONTINUED)</h1>

      <table class="report-table" style="font-size: 8pt;">
        <tr style="background-color: #FEDDBD;">
          <th style="width: 15%;">Section</th>
          <th style="width: 73%;">Title &amp; Descriptive Scope</th>
          <th style="width: 12%; text-align: right;">Page</th>
        </tr>
        <tr>
          <td><strong>Chapter 9</strong></td>
          <td><strong>SECURITY AND CRYPTOGRAPHY</strong></td>
          <td style="text-align: right;"><strong>58</strong></td>
        </tr>
        <tr>
          <td>9.1 &ndash; 9.3</td>
          <td>AES-256-GCM &bull; JWT Lifecycle [Figure 9.1] &bull; Envelope Encryption [Figure 9.2]</td>
          <td style="text-align: right;">58 &ndash; 60</td>
        </tr>
        <tr>
          <td>9.4 &ndash; 9.8</td>
          <td>Key Management &bull; Threat Model [Figure 9.3] &bull; STRIDE Matrix &bull; Client Hardening</td>
          <td style="text-align: right;">61 &ndash; 65</td>
        </tr>
        <tr>
          <td><strong>Chapter 10</strong></td>
          <td><strong>CORE FEATURE WORKFLOWS</strong></td>
          <td style="text-align: right;"><strong>66</strong></td>
        </tr>
        <tr>
          <td>10.1 &ndash; 10.3</td>
          <td>Login Sequence [Fig 10.1] &bull; Upload Sequence [Fig 10.2] &bull; Download [Fig 10.3]</td>
          <td style="text-align: right;">66 &ndash; 68</td>
        </tr>
        <tr>
          <td>10.4 &ndash; 10.7</td>
          <td>Deduplication [Fig 10.4] &bull; Metadata [Fig 10.5] &bull; Preview [Fig 10.6] &bull; Delete [Fig 10.7]</td>
          <td style="text-align: right;">69 &ndash; 72</td>
        </tr>
        <tr>
          <td>10.8 &ndash; 10.9</td>
          <td>Staging Activity [Fig 10.8] &bull; Fault Tolerance Recovery [Fig 10.9]</td>
          <td style="text-align: right;">73</td>
        </tr>
        <tr>
          <td><strong>Chapter 11</strong></td>
          <td><strong>USER INTERFACE AND SCREEN-BY-SCREEN DOCUMENTATION</strong></td>
          <td style="text-align: right;"><strong>74</strong></td>
        </tr>
        <tr>
          <td>11.1 &ndash; 11.3</td>
          <td>Onboarding (All 5 Pages) &bull; Server Connection &bull; QR Scanner Screen</td>
          <td style="text-align: right;">74 &ndash; 76</td>
        </tr>
        <tr>
          <td>11.4 &ndash; 11.6</td>
          <td>Auth &amp; App Lock &bull; Home &bull; Vault Grid &bull; File Details BottomSheet</td>
          <td style="text-align: right;">77 &ndash; 79</td>
        </tr>
        <tr>
          <td>11.7 &ndash; 11.9</td>
          <td>Upload Staging (Delete/Preview) &bull; Transfers &bull; Settings &bull; UI Showcase</td>
          <td style="text-align: right;">80 &ndash; 82</td>
        </tr>
        <tr>
          <td><strong>Chapter 12</strong></td>
          <td><strong>IMPLEMENTATION DETAILS AND CODE HIGHLIGHTS</strong></td>
          <td style="text-align: right;"><strong>83</strong></td>
        </tr>
        <tr>
          <td>12.1 &ndash; 12.5</td>
          <td>Source Tree &bull; UML Class [Fig 12.1] &bull; Android Code &bull; Backend &bull; PySide6 Manager</td>
          <td style="text-align: right;">83 &ndash; 87</td>
        </tr>
        <tr>
          <td><strong>Chapter 13</strong></td>
          <td><strong>TESTING, VERIFICATION AND QUALITY ASSURANCE</strong></td>
          <td style="text-align: right;"><strong>88</strong></td>
        </tr>
        <tr>
          <td>13.1 &ndash; 13.5</td>
          <td>Methodology &bull; Backend Tests (108) &bull; Android Tests (22) &bull; UI Regression &bull; Matrix</td>
          <td style="text-align: right;">88 &ndash; 92</td>
        </tr>
        <tr>
          <td><strong>Chapter 14</strong></td>
          <td><strong>RESULTS AND DISCUSSION</strong></td>
          <td style="text-align: right;"><strong>93</strong></td>
        </tr>
        <tr>
          <td>14.1 &ndash; 14.4</td>
          <td>Capabilities Summary &bull; Streaming Performance &bull; Objectives vs Outcomes</td>
          <td style="text-align: right;">93 &ndash; 96</td>
        </tr>
        <tr>
          <td><strong>Chapter 15</strong></td>
          <td><strong>LIMITATIONS, RISKS AND FUTURE ROADMAP</strong></td>
          <td style="text-align: right;"><strong>97</strong></td>
        </tr>
        <tr>
          <td>15.1 &ndash; 15.5</td>
          <td>Architectural Limits &bull; Gaps &bull; Recommendations (P0-P3) &bull; Future Arch [Fig 15.1]</td>
          <td style="text-align: right;">97 &ndash; 101</td>
        </tr>
        <tr>
          <td><strong>Chapter 16</strong></td>
          <td><strong>DEPLOYMENT, INSTALLATION AND OPERATIONS MANUAL</strong></td>
          <td style="text-align: right;"><strong>102</strong></td>
        </tr>
        <tr>
          <td>16.1 &ndash; 16.3</td>
          <td>Prerequisites &bull; MySQL Setup &bull; Backend Startup &bull; APK Installation Guide</td>
          <td style="text-align: right;">102 &ndash; 104</td>
        </tr>
        <tr>
          <td><strong>Chapter 17</strong></td>
          <td><strong>CONCLUSION</strong></td>
          <td style="text-align: right;"><strong>105</strong></td>
        </tr>
        <tr>
          <td>17.1 &ndash; 17.3</td>
          <td>Concluding Summary &bull; Academic Contributions &bull; Final Remarks</td>
          <td style="text-align: right;">105</td>
        </tr>
        <tr>
          <td><strong>Appendices</strong></td>
          <td><strong>REFERENCES AND APPENDICES</strong></td>
          <td style="text-align: right;"><strong>106</strong></td>
        </tr>
        <tr>
          <td>Appendix A</td>
          <td>Technical References &amp; Academic Bibliography</td>
          <td style="text-align: right;">106</td>
        </tr>
        <tr>
          <td>Appendix B</td>
          <td>Complete REST API Reference (All Endpoints, Payloads, Responses)</td>
          <td style="text-align: right;">107 &ndash; 108</td>
        </tr>
        <tr>
          <td>Appendix C</td>
          <td>Source-File-to-Feature Traceability Matrix</td>
          <td style="text-align: right;">109</td>
        </tr>
        <tr>
          <td>Appendix D</td>
          <td>Database Data Dictionary (Column Types, Constraints, Default Values)</td>
          <td style="text-align: right;">110</td>
        </tr>
        <tr>
          <td>Appendix E</td>
          <td>Requirements Traceability Matrix (RTM)</td>
          <td style="text-align: right;">111</td>
        </tr>
        <tr>
          <td>Appendix F</td>
          <td>Sample Sanitized Payloads, Forensic JSON &amp; Code Snippets</td>
          <td style="text-align: right;">112</td>
        </tr>
      </table>
'''
    pages.append(make_page(9, total_pages, "PRELIMINARIES &bull; TABLE OF CONTENTS (2/2)", p9_content, True))

    # ---------------- PAGE 10: LIST OF FIGURES & ACRONYMS ----------------
    p10_content = '''
      <h1 class="ch-title" style="text-align: center; border-bottom: none; margin-bottom: 10px;">LIST OF FIGURES, TABLES &amp; ACRONYMS</h1>

      <div style="display: flex; gap: 15px;">
        <div style="flex: 1;">
          <div style="font-size: 8.5pt; font-weight: bold; color: #815621; border-bottom: 1px solid #FEDDBD; padding-bottom: 2px; margin-bottom: 4px;">
            KEY TECHNICAL DIAGRAMS (24 FIGURES)
          </div>
          <table class="report-table" style="font-size: 7.2pt; margin: 0;">
            <tr><td>Fig 4.1</td><td>Use Case Modeling Diagram</td><td style="text-align:right;">p.29</td></tr>
            <tr><td>Fig 5.1</td><td>System Context Diagram (Level 0)</td><td style="text-align:right;">p.31</td></tr>
            <tr><td>Fig 5.2</td><td>Multi-Tier System Architecture</td><td style="text-align:right;">p.32</td></tr>
            <tr><td>Fig 5.3</td><td>Communication Protocol Flow</td><td style="text-align:right;">p.33</td></tr>
            <tr><td>Fig 5.4</td><td>Data Flow Diagrams (Level 0 &amp; 1)</td><td style="text-align:right;">p.34</td></tr>
            <tr><td>Fig 5.5</td><td>Physical Deployment Topology</td><td style="text-align:right;">p.35</td></tr>
            <tr><td>Fig 6.1</td><td>Android Component Architecture</td><td style="text-align:right;">p.37</td></tr>
            <tr><td>Fig 6.2</td><td>Android Navigation State Machine</td><td style="text-align:right;">p.40</td></tr>
            <tr><td>Fig 7.1</td><td>Backend Layered Architecture</td><td style="text-align:right;">p.45</td></tr>
            <tr><td>Fig 8.1</td><td>Database ER Diagram (MySQL 8.0)</td><td style="text-align:right;">p.53</td></tr>
            <tr><td>Fig 9.1</td><td>Authentication &amp; JWT Lifecycle</td><td style="text-align:right;">p.59</td></tr>
            <tr><td>Fig 9.2</td><td>Envelope Encryption Hierarchy</td><td style="text-align:right;">p.60</td></tr>
            <tr><td>Fig 9.3</td><td>Security Boundaries &amp; STRIDE</td><td style="text-align:right;">p.63</td></tr>
            <tr><td>Fig 10.1</td><td>User Login Sequence Diagram</td><td style="text-align:right;">p.66</td></tr>
            <tr><td>Fig 10.2</td><td>End-to-End File Upload Sequence</td><td style="text-align:right;">p.67</td></tr>
            <tr><td>Fig 10.3</td><td>File Download &amp; Decryption Flow</td><td style="text-align:right;">p.68</td></tr>
            <tr><td>Fig 10.4</td><td>Duplicate Detection Sequence</td><td style="text-align:right;">p.69</td></tr>
            <tr><td>Fig 10.5</td><td>Metadata Extraction Pipeline</td><td style="text-align:right;">p.70</td></tr>
            <tr><td>Fig 10.6</td><td>In-Memory Decrypted Preview</td><td style="text-align:right;">p.71</td></tr>
            <tr><td>Fig 10.7</td><td>File Deletion &amp; Quota Reclaim</td><td style="text-align:right;">p.72</td></tr>
            <tr><td>Fig 10.8</td><td>Upload Staging Activity Workflow</td><td style="text-align:right;">p.73</td></tr>
            <tr><td>Fig 10.9</td><td>Fault-Tolerance Recovery Flow</td><td style="text-align:right;">p.73</td></tr>
            <tr><td>Fig 12.1</td><td>Core UML Class Diagram</td><td style="text-align:right;">p.84</td></tr>
            <tr><td>Fig 15.1</td><td>Current vs Future Architecture</td><td style="text-align:right;">p.101</td></tr>
          </table>
        </div>

        <div style="flex: 1;">
          <div style="font-size: 8.5pt; font-weight: bold; color: #815621; border-bottom: 1px solid #FEDDBD; padding-bottom: 2px; margin-bottom: 4px;">
            KEY TABLES &amp; ACRONYMS
          </div>
          <table class="report-table" style="font-size: 7.2pt; margin: 0 0 6px 0;">
            <tr><td>Tab 2.1</td><td>Cloud Storage Comparative Evaluation</td><td style="text-align:right;">p.18</td></tr>
            <tr><td>Tab 4.1</td><td>Functional Requirements (FR-01 to 20)</td><td style="text-align:right;">p.27</td></tr>
            <tr><td>Tab 4.2</td><td>Non-Functional Requirements (NFR)</td><td style="text-align:right;">p.28</td></tr>
            <tr><td>Tab 8.1</td><td>MySQL Users Table Data Dictionary</td><td style="text-align:right;">p.54</td></tr>
            <tr><td>Tab 8.2</td><td>Stored Files Relational Schema</td><td style="text-align:right;">p.54</td></tr>
            <tr><td>Tab 8.3</td><td>File Metadata Forensic Schema</td><td style="text-align:right;">p.55</td></tr>
            <tr><td>Tab 9.1</td><td>STRIDE Security Threat Matrix</td><td style="text-align:right;">p.64</td></tr>
            <tr><td>Tab 13.1</td><td>Master Test Execution Register (TC01-25)</td><td style="text-align:right;">p.92</td></tr>
            <tr><td>Tab 14.1</td><td>Verified Capabilities Matrix</td><td style="text-align:right;">p.93</td></tr>
            <tr><td>Tab 15.1</td><td>Prioritized Improvement Register</td><td style="text-align:right;">p.99</td></tr>
          </table>

          <div style="font-size: 8.5pt; font-weight: bold; color: #815621; border-bottom: 1px solid #FEDDBD; padding-bottom: 2px; margin-bottom: 4px;">
            LIST OF ABBREVIATIONS &amp; ACRONYMS
          </div>
          <table class="report-table" style="font-size: 7.2pt; margin: 0;">
            <tr><td><strong>AES-GCM</strong></td><td>Advanced Encryption Standard Galois/Counter Mode</td></tr>
            <tr><td><strong>API</strong></td><td>Application Programming Interface</td></tr>
            <tr><td><strong>BIP-39</strong></td><td>Bitcoin Improvement Proposal 39 (Mnemonic Seed)</td></tr>
            <tr><td><strong>DEK / KEK</strong></td><td>Data Encryption Key / Key Encryption Key</td></tr>
            <tr><td><strong>EXIF</strong></td><td>Exchangeable Image File Format</td></tr>
            <tr><td><strong>GHASH</strong></td><td>Galois Hash Authentication Tag (128-bit)</td></tr>
            <tr><td><strong>IV</strong></td><td>Initialization Vector (12-byte Nonce)</td></tr>
            <tr><td><strong>JWT</strong></td><td>JSON Web Token (RFC 7519)</td></tr>
            <tr><td><strong>NIST</strong></td><td>National Institute of Standards and Technology</td></tr>
            <tr><td><strong>PBKDF2</strong></td><td>Password-Based Key Derivation Function 2</td></tr>
            <tr><td><strong>RTM</strong></td><td>Requirements Traceability Matrix</td></tr>
            <tr><td><strong>SHA-256</strong></td><td>Secure Hash Algorithm 256-bit (FIPS 180-4)</td></tr>
            <tr><td><strong>STRIDE</strong></td><td>Spoofing, Tampering, Repudiation, Info, DoS, Elevation</td></tr>
            <tr><td><strong>UDEK</strong></td><td>User Data Encryption Key (Per-User 256-bit AES)</td></tr>
          </table>
        </div>
      </div>
'''
    pages.append(make_page(10, total_pages, "PRELIMINARIES &bull; FIGURES, TABLES &amp; ACRONYMS", p10_content, True))

    return pages
