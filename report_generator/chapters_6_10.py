"""
Chapters 6 to 10 for CipherVault Project Report (Pages 37 to 73).
Covers Android Client Design, Backend Design, Database Design, Security & Cryptography, and Core Workflows.
Includes Figures 6.1, 6.2, 7.1, 8.1, 9.1, 9.2, 9.3, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9
and Tables 8.1, 8.2, 8.3, 9.1.
"""

from .styles import make_page

def generate_chapters_6_to_10(total_pages=112):
    pages = []

    # =========================================================================
    # CHAPTER 6: ANDROID APPLICATION DESIGN (Pages 37 to 44)
    # =========================================================================

    # Page 37: 6.1 Component Architecture (Figure 6.1)
    p37 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN</h1>
      
      <h2 class="sec-title">6.1 Android Client Component Architecture</h2>
      <p>
        The Android application is structured according to Google's official Architecture Components guidelines, enforcing a strict separation of concerns between UI presentation, domain managers, hardware security, and network streaming:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_03_android_components.svg" class="figure-img" alt="Android Component Architecture"/>
        <div class="fig-caption">FIGURE 6.1 &mdash; ANDROID CLIENT COMPONENT ARCHITECTURE</div>
        <div class="fig-desc">
          Modular client subsystems: UI Presentation Layer (Activities &amp; Fragments), Adapters &amp; Domain Managers (VaultFilesAdapter, ThemeManager, TransferManager), and Security &amp; Networking Layer (AppLock, Keystore, ApiClient, OkHttp).
        </div>
      </div>
'''
    pages.append(make_page(37, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (1/8)", p37))

    # Page 38: 6.2 Application Entry Point, Lifecycle
    p38 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">6.2 Application Entry Point, Cold-Start &amp; Lifecycle</h2>
      <p>
        The lifecycle initialization starts in <code class="inline">CipherVaultApplication</code>, extending <code class="inline">android.app.Application</code>. During cold launch, the application registers global crash handlers, initializes <code class="inline">ThemeManager</code>, and registers dynamic color overrides:
      </p>

      <pre class="code-block">
public class CipherVaultApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Register DynamicColors with user preference precondition
        DynamicColorsOptions options = new DynamicColorsOptions.Builder()
            .setPrecondition((activity, theme) -> 
                CipherVaultPreferences.isDynamicColorEnabled(activity))
            .build();
        DynamicColors.applyToActivitiesIfAvailable(this, options);
    }
}
      </pre>

      <h3 class="subsec-title">Activity Orchestration and Fragment Caching</h3>
      <p>
        <code class="inline">MainActivity</code> serves as the primary navigation host, housing a bottom navigation bar managing four core fragments: <code class="inline">FragmentHome</code>, <code class="inline">FragmentVault</code>, <code class="inline">FragmentTransfers</code>, and <code class="inline">FragmentSettings</code>. To maximize responsiveness and preserve scroll states during tab switching, <code class="inline">MainActivity</code> implements fragment show/hide caching rather than continuously destroying and rebuilding fragment views:
      </p>
      <ul>
        <li><strong>Memory Retention:</strong> Vault scroll position, search filters, and active transfers persist seamlessly across bottom tab navigation.</li>
        <li><strong>Lifecycle Safety:</strong> Paused fragments unsubscribe from heavy UI observers, eliminating background CPU overhead.</li>
      </ul>
'''
    pages.append(make_page(38, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (2/8)", p38))

    # Page 39: 6.3 Dynamic Theming vs Heritage Palette
    p39 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">6.3 Dynamic Theming (Monet) vs. Warm Heritage Palette Architecture</h2>
      <p>
        A core design accomplishment of CipherVault is its dual-mode visual identity system. CipherVault supports both cutting-edge Android 12+ wallpaper extraction and its iconic warm heritage visual identity:
      </p>

      <div style="display: flex; gap: 10px; margin: 8px 0;">
        <div style="flex: 1; border: 1px solid #E6D7CD; border-radius: 4px; padding: 8px; background-color: #FFF9F5;">
          <div style="font-weight: bold; color: #815621; font-size: 8.5pt;">MODE A: DYNAMIC MATERIAL YOU (MONET)</div>
          <p style="font-size: 7.8pt; margin-top: 4px; line-height: 1.4;">
            Active on Android 12+ (API 31+). Uses the system Monet engine to harmonize buttons, card containers, and bottom navigation pills with the user's personal desktop wallpaper tonal swatches.
          </p>
        </div>
        <div style="flex: 1; border: 1px solid #E6D7CD; border-radius: 4px; padding: 8px; background-color: #FFF9F5;">
          <div style="font-weight: bold; color: #815621; font-size: 8.5pt;">MODE B: WARM HERITAGE IDENTITY (DEFAULT)</div>
          <p style="font-size: 7.8pt; margin-top: 4px; line-height: 1.4;">
            Maintains the signature CipherVault identity: Warm Beige background (<code class="inline">#FFF8F4</code>), Cream surface (<code class="inline">#FFF1E8</code>), Peach cards (<code class="inline">#FEDDBD</code>), and Deep Warm-Brown controls (<code class="inline">#815621</code>).
          </p>
        </div>
      </div>

      <h3 class="subsec-title">Root Cause Resolution of Theme Overwrites</h3>
      <p>
        During development, an issue was discovered where calling <code class="inline">activity.setTheme(R.style.Theme_CipherVault)</code> in <code class="inline">ThemeManager.applyTheme()</code> completely wiped out dynamic Monet theme overlays on activity launches. This was resolved by explicitly re-applying dynamic colors immediately following base theme resolution:
      </p>

      <pre class="code-block">
public static void applyTheme(Activity activity) {
    activity.setTheme(R.style.Theme_CipherVault);
    if (CipherVaultPreferences.isDynamicColorEnabled(activity)) {
        DynamicColors.applyIfAvailable(activity);
    }
}
      </pre>
      <p>
        Users can toggle Dynamic Theming in <code class="inline">FragmentSettings</code> with instantaneous <code class="inline">activity.recreate()</code> UI updates.
      </p>
'''
    pages.append(make_page(39, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (3/8)", p39))

    # Page 40: 6.4 Navigation Architecture (Figure 6.2)
    p40 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">6.4 Navigation Architecture &amp; State Machine</h2>
      <p>
        The navigation state machine governs deterministic screen transitions across Splash, Onboarding, Authentication, AppLock, and Main subsystems, illustrated in Figure 6.2:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_21_navigation_map.svg" class="figure-img" alt="Navigation Map"/>
        <div class="fig-caption">FIGURE 6.2 &mdash; ANDROID CLIENT NAVIGATION STATE MACHINE</div>
        <div class="fig-desc">
          State-driven screen graph: Cold launch routes through Splash to 5-screen Onboarding (first run) or Server Connection / Login; AppLock guards all authenticated entries; Bottom navigation links Home, Vault, Transfers, and Settings tabs.
        </div>
      </div>
'''
    pages.append(make_page(40, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (4/8)", p40))

    # Page 41: 6.5 ViewHolder Lifecycle Safety
    p41 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">6.5 RecyclerView Adapter Architecture &amp; ViewHolder Lifecycle Safety</h2>
      <p>
        Handling large collections of encrypted vault files requires strict memory safety and adapter position integrity. In <code class="inline">FragmentVault</code>, <code class="inline">VaultFilesAdapter</code> enforces crucial architectural patterns to prevent stale closures and crash bugs:
      </p>

      <div class="callout-box">
        <div class="box-title">Pattern 1: Dynamic Binding Adapter Position Guard</div>
        In older implementations, passing the raw <code class="inline">int position</code> argument from <code class="inline">onBindViewHolder</code> into click listener lambdas caused <code class="inline">IndexOutOfBoundsException</code> crashes when files were deleted or filtered. The hardened implementation calls <code class="inline">holder.getBindingAdapterPosition()</code> dynamically:
      </div>

      <pre class="code-block">
holder.itemView.setOnClickListener(v -> {
    int pos = holder.getBindingAdapterPosition();
    if (pos == RecyclerView.NO_POSITION || pos &gt;= fileList.size()) return;
    StoredFile file = fileList.get(pos);
    if (isMultiSelectMode) {
        toggleFileSelection(file.getId(), holder);
    } else {
        showFileDetailsBottomSheet(file);
    }
});
      </pre>

      <div class="callout-box">
        <div class="box-title">Pattern 2: Checkbox Recycling Listener Detachment</div>
        When RecyclerView recycles view holders during fast scrolling, re-binding checkboxes triggers false change events. The adapter explicitly unbinds the listener before updating state:
        <code class="inline">holder.cbSelect.setOnCheckedChangeListener(null); holder.cbSelect.setChecked(isSelected);</code>
      </div>
'''
    pages.append(make_page(41, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (5/8)", p41))

    # Page 42: 6.6 Onboarding Screens 1 & 2
    p42 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">6.6 The 5-Screen Onboarding Journey &amp; Visual Hierarchy</h2>
      <p>
        CipherVault welcomes first-time users through a five-screen onboarding carousel. The flow communicates the security guarantees of zero-knowledge storage while establishing the warm heritage visual identity:
      </p>

      <div class="screenshot-row">
        <div class="screenshot-col">
          <img src="screenshots/fig_02_onboarding_1.png" alt="Onboarding Screen 1"/>
          <div class="fig-caption">FIGURE 6.3 &mdash; ONBOARDING SCREEN 1: WELCOME</div>
          <div class="fig-desc">Introduction to private sovereign cloud storage with warm brand typography and primary progression button.</div>
        </div>
        <div class="screenshot-col">
          <img src="screenshots/fig_03_onboarding_2.png" alt="Onboarding Screen 2"/>
          <div class="fig-caption">FIGURE 6.4 &mdash; ONBOARDING SCREEN 2: ENCRYPTION</div>
          <div class="fig-desc">Explanation of AES-256-GCM envelope encryption and zero-knowledge client isolation principles.</div>
        </div>
      </div>
'''
    pages.append(make_page(42, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (6/8)", p42))

    # Page 43: 6.7 Onboarding Screens 3, 4, 5
    p43 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">Onboarding Journey: Completion &amp; Persistence</h2>
      <p>
        The final three onboarding screens explain cryptographic integrity, unified multimedia organization, and private vault controls, completing the educational onboarding flow:
      </p>

      <div class="screenshot-row">
        <div class="screenshot-col" style="flex:1;">
          <img src="screenshots/fig_04_onboarding_3.png" style="max-height:85mm;" alt="Onboarding 3"/>
          <div class="fig-caption">FIG 6.5 &mdash; INTEGRITY &amp; DEDUP</div>
          <div class="fig-desc">SHA-256 integrity and duplicate avoidance.</div>
        </div>
        <div class="screenshot-col" style="flex:1;">
          <img src="screenshots/fig_05_onboarding_4.png" style="max-height:85mm;" alt="Onboarding 4"/>
          <div class="fig-caption">FIG 6.6 &mdash; ALL IN ONE PLACE</div>
          <div class="fig-desc">Unified photos, docs, videos organization.</div>
        </div>
        <div class="screenshot-col" style="flex:1;">
          <img src="screenshots/fig_06_onboarding_5.png" style="max-height:85mm;" alt="Onboarding 5"/>
          <div class="fig-caption">FIG 6.7 &mdash; PRIVATE VAULT</div>
          <div class="fig-desc">Biometric lock and final Get Started button.</div>
        </div>
      </div>

      <p style="font-size: 8.2pt; margin-top: 8px;">
        Upon completing Screen 5, the client sets <code class="inline">CipherVaultPreferences.setOnboardingCompleted(true)</code>, ensuring subsequent launches route directly to Server Connection or AppLock.
      </p>
'''
    pages.append(make_page(43, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (7/8)", p43))

    # Page 44: 6.8 AppLock Subsystem
    p44 = '''
      <h1 class="ch-title">CHAPTER 6: ANDROID APPLICATION DESIGN (CONT.)</h1>
      
      <h2 class="sec-title">6.8 AppLock &amp; BiometricPrompt Security Subsystem</h2>
      <p>
        To prevent unauthorized local access, CipherVault enforces an unbypassable biometric lock. Cold starts load directly into a locked overlay; the native Android <code class="inline">BiometricPrompt</code> is invoked only upon intentional user interaction:
      </p>

      <div class="screenshot-row">
        <div class="screenshot-col">
          <img src="screenshots/fig_12_app_lock.png" alt="App Lock Screen"/>
          <div class="fig-caption">FIGURE 6.8 &mdash; APPLOCK LOCKED OVERLAY</div>
          <div class="fig-desc">Cold-start lock screen requiring explicit user tap to initiate biometric authentication.</div>
        </div>
        <div class="screenshot-col">
          <img src="screenshots/fig_13_biometric_prompt.png" alt="Biometric Prompt Dialog"/>
          <div class="fig-caption">FIGURE 6.9 &mdash; BIOMETRICPROMPT SYSTEM DIALOG</div>
          <div class="fig-desc">Android hardware-backed fingerprint scanner dialog authenticating against TEE Keymaster.</div>
        </div>
      </div>
'''
    pages.append(make_page(44, total_pages, "CHAPTER 6 &bull; ANDROID DESIGN (8/8)", p44))

    # =========================================================================
    # CHAPTER 7: BACKEND DESIGN AND IMPLEMENTATION (Pages 45 to 51)
    # =========================================================================

    # Page 45: 7.1 Backend Layered Architecture (Figure 7.1)
    p45 = '''
      <h1 class="ch-title">CHAPTER 7: BACKEND DESIGN &amp; IMPLEMENTATION</h1>
      
      <h2 class="sec-title">7.1 Backend Layered Architecture</h2>
      <p>
        The Spring Boot 3.3.4 backend is engineered around a four-tier architecture separating HTTP controller endpoints, security filter chains, business domain services, and JPA repositories, illustrated in Figure 7.1:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_04_backend_layered.svg" class="figure-img" alt="Backend Layered Architecture"/>
        <div class="fig-caption">FIGURE 7.1 &mdash; SPRING BOOT BACKEND LAYERED ARCHITECTURE</div>
        <div class="fig-desc">
          Four-tier server decomposition: 1. Controller &amp; REST API Layer; 2. Spring Security 6 Filter Pipeline; 3. Business Service Layer (Encryption, Storage, Metadata); 4. JPA Data Access Layer &amp; MySQL Persistence.
        </div>
      </div>
'''
    pages.append(make_page(45, total_pages, "CHAPTER 7 &bull; BACKEND DESIGN (1/7)", p45))

    # Page 46: 7.2 Bootstrapping & Configuration
    p46 = '''
      <h1 class="ch-title">CHAPTER 7: BACKEND DESIGN &amp; IMPLEMENTATION (CONT.)</h1>
      
      <h2 class="sec-title">7.2 Application Bootstrapping and Configuration</h2>
      <p>
        The backend boots via <code class="inline">CipherVaultApplication.java</code> under Spring Boot 3.3.4 and Java 21 LTS. Core operational settings are configured in <code class="inline">application.properties</code>:
      </p>

      <pre class="code-block">
# Server Network Binding
server.port=8080
server.address=0.0.0.0

# Multipart Streaming Upload Thresholds (2 GB Max)
spring.servlet.multipart.max-file-size=2048MB
spring.servlet.multipart.max-request-size=2048MB
spring.servlet.multipart.file-size-threshold=2MB

# MySQL Database Connectivity (HikariCP Pool)
spring.datasource.url=jdbc:mysql://localhost:3306/ciphervault?useSSL=false&amp;allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=${CIPHERVAULT_DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
      </pre>

      <h3 class="subsec-title">Production Environment Guard</h3>
      <p>
        When running in production (<code class="inline">CIPHERVAULT_ENV=production</code>), the backend enforces strict cryptographic secret verification. If <code class="inline">CIPHERVAULT_MASTER_KEY</code>, <code class="inline">CIPHERVAULT_PBKDF2_SALT</code>, or <code class="inline">CIPHERVAULT_JWT_SECRET</code> are missing, the server halts with an <code class="inline">IllegalStateException</code>, preventing insecure defaults from ever running in production.
      </p>
'''
    pages.append(make_page(46, total_pages, "CHAPTER 7 &bull; BACKEND DESIGN (2/7)", p46))

    # Page 47: 7.3 Spring Security 6 & JWT Filter
    p47 = '''
      <h1 class="ch-title">CHAPTER 7: BACKEND DESIGN &amp; IMPLEMENTATION (CONT.)</h1>
      
      <h2 class="sec-title">7.3 Spring Security 6 &amp; Stateless JWT Filter Pipeline</h2>
      <p>
        Security is managed by a declarative <code class="inline">SecurityFilterChain</code> bean. The system intercepts every incoming request through <code class="inline">JwtAuthenticationFilter</code>:
      </p>

      <pre class="code-block">
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(CorsConfigurer::disable)
        .sessionManagement(s -&gt; s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -&gt; auth
            .requestMatchers("/api/auth/**", "/api/health").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
      </pre>

      <h3 class="subsec-title">Token Parsing and Security Context Population</h3>
      <ol style="font-size: 8.4pt; line-height: 1.5;">
        <li>Extracts the <code class="inline">Authorization: Bearer &lt;token&gt;</code> HTTP header.</li>
        <li>Validates HMAC-SHA256 signature and checks expiry timestamp.</li>
        <li>Extracts user email and queries <code class="inline">UserRepository</code> to verify token version.</li>
        <li>Instantiates <code class="inline">UsernamePasswordAuthenticationToken</code> and sets it into <code class="inline">SecurityContextHolder</code>.</li>
      </ol>
'''
    pages.append(make_page(47, total_pages, "CHAPTER 7 &bull; BACKEND DESIGN (3/7)", p47))

    # Page 48: 7.4 REST Controllers & Routing
    p48 = '''
      <h1 class="ch-title">CHAPTER 7: BACKEND DESIGN &amp; IMPLEMENTATION (CONT.)</h1>
      
      <h2 class="sec-title">7.4 REST Controllers &amp; Request Routing</h2>
      <p>
        The REST API surface is cleanly divided across specialized controllers handling distinct operational responsibilities:
      </p>

      <table class="report-table">
        <tr>
          <th style="width: 25%;">Controller</th>
          <th style="width: 35%;">Path Mapping &amp; Endpoints</th>
          <th style="width: 40%;">Core Operational Responsibility</th>
        </tr>
        <tr>
          <td><strong>AuthController</strong></td>
          <td><code class="inline">/api/auth/register</code><br/><code class="inline">/api/auth/login</code><br/><code class="inline">/api/auth/change-password</code></td>
          <td>User registration, credential verification, BCrypt hashing, JWT token issuance, and password rotation.</td>
        </tr>
        <tr>
          <td><strong>FileController</strong></td>
          <td><code class="inline">/api/files/upload</code><br/><code class="inline">/api/files/{id}/download</code><br/><code class="inline">/api/files/{id} (DELETE)</code><br/><code class="inline">/api/files (GET list)</code></td>
          <td>Multipart streaming ingestion, AES-256-GCM chunked encryption/decryption, quota verification, and deletion.</td>
        </tr>
        <tr>
          <td><strong>MetadataController</strong></td>
          <td><code class="inline">/api/metadata/search</code><br/><code class="inline">/api/metadata/suggestions</code><br/><code class="inline">/api/metadata/{fileId}</code></td>
          <td>Dynamic search query processing, suggestion chip generation, and forensic EXIF attribute retrieval.</td>
        </tr>
        <tr>
          <td><strong>UserController</strong></td>
          <td><code class="inline">/api/user/profile</code><br/><code class="inline">/api/user/photo (POST/GET)</code></td>
          <td>User storage quota accounting, profile metrics retrieval, and profile avatar image management.</td>
        </tr>
      </table>
'''
    pages.append(make_page(48, total_pages, "CHAPTER 7 &bull; BACKEND DESIGN (4/7)", p48))

    # Page 49: 7.5 Service Layer Logic
    p49 = '''
      <h1 class="ch-title">CHAPTER 7: BACKEND DESIGN &amp; IMPLEMENTATION (CONT.)</h1>
      
      <h2 class="sec-title">7.5 Service Layer Business Logic &amp; Transaction Management</h2>
      <p>
        Business domain operations execute inside transactional Spring services to guarantee zero data loss and transactional rollback:
      </p>

      <h3 class="subsec-title">1. KeyManagementService</h3>
      <p>
        Manages the cryptographic envelope hierarchy. Derives the Server KEK via PBKDF2, generates 256-bit AES User Data Encryption Keys (UDEKs), and performs symmetric key wrapping.
      </p>

      <h3 class="subsec-title">2. FileStorageService</h3>
      <p>
        Coordinates the end-to-end file lifecycle. Orchestrates duplicate detection, streaming AES-GCM encryption, disk storage writing, thumbnail extraction, and atomic MySQL storage quota increments:
      </p>

      <pre class="code-block">
@Transactional
public StoredFile storeFile(MultipartFile file, User user) {
    String sha256 = computeSha256(file.getInputStream());
    if (fileRepository.existsByUserIdAndSha256Hash(user.getId(), sha256)) {
        throw new DuplicateFileException("File already exists in vault");
    }
    enforceStorageQuota(user.getId(), file.getSize());
    SecretKey udek = keyService.getUserDataKey(user);
    Path targetPath = encryptionService.encryptToFile(file.getInputStream(), udek);
    // Persist file record and extract metadata
    return persistFileAndMetadata(file, user, sha256, targetPath);
}
      </pre>
'''
    pages.append(make_page(49, total_pages, "CHAPTER 7 &bull; BACKEND DESIGN (5/7)", p49))

    # Page 50: 7.6 2GB Streaming Engine
    p50 = '''
      <h1 class="ch-title">CHAPTER 7: BACKEND DESIGN &amp; IMPLEMENTATION (CONT.)</h1>
      
      <h2 class="sec-title">7.6 Storage Engine &amp; 2GB Streaming I/O Pipeline</h2>
      <p>
        A defining technical achievement of CipherVault is its constant-memory streaming storage engine. In <code class="inline">EncryptionService</code>, streams are piped using isolated 16 KB buffers, preventing <code class="inline">OutOfMemoryError</code> exceptions on large files:
      </p>

      <pre class="code-block">
public void encryptStream(InputStream in, OutputStream out, SecretKey key) throws Exception {
    byte[] iv = new byte[12];
    secureRandom.nextBytes(iv);
    out.write(iv); // Prepend 12-byte Nonce to disk blob
    
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
    
    try (CipherOutputStream cos = new CipherOutputStream(out, cipher)) {
        byte[] buffer = new byte[16384]; // 16 KB chunk buffer
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            cos.write(buffer, 0, bytesRead);
        }
        cos.flush();
    }
}
      </pre>

      <h3 class="subsec-title">Streaming Download Pipeline</h3>
      <p>
        During file download, <code class="inline">FileController</code> returns a Spring <code class="inline">StreamingResponseBody</code>. The server opens a <code class="inline">CipherInputStream</code> on the encrypted disk file, reads the 12-byte IV, and streams plaintext directly into the HTTP response socket, maintaining heap utilization under 32 MB even during 2 GB transfers.
      </p>
'''
    pages.append(make_page(50, total_pages, "CHAPTER 7 &bull; BACKEND DESIGN (6/7)", p50))

    # Page 51: 7.7 Global Exception Handling
    p51 = '''
      <h1 class="ch-title">CHAPTER 7: BACKEND DESIGN &amp; IMPLEMENTATION (CONT.)</h1>
      
      <h2 class="sec-title">7.7 Global Exception Handling &amp; Uniform API Responses</h2>
      <p>
        The backend intercepts all unchecked exceptions using <code class="inline">@RestControllerAdvice</code>, converting internal runtime failures into structured, predictable JSON error payloads:
      </p>

      <pre class="code-block">
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity&lt;ErrorResponse&gt; handleDuplicate(DuplicateFileException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_FILE", ex.getMessage()));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity&lt;ErrorResponse&gt; handleQuota(QuotaExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ErrorResponse("QUOTA_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(AEADBadTagException.class)
    public ResponseEntity&lt;ErrorResponse&gt; handleTamper(AEADBadTagException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INTEGRITY_VIOLATION", "Ciphertext tampering detected"));
    }
}
      </pre>

      <h3 class="subsec-title">Client-Side Error Propagation</h3>
      <p>
        The Android Retrofit client converts these structured error codes into user-friendly UI dialogs and toast alerts, ensuring users always receive actionable feedback upon error conditions.
      </p>
'''
    pages.append(make_page(51, total_pages, "CHAPTER 7 &bull; BACKEND DESIGN (7/7)", p51))

    # =========================================================================
    # CHAPTER 8: DATABASE DESIGN (Pages 52 to 57)
    # =========================================================================

    # Page 52: 8.1 Schema Overview
    p52 = '''
      <h1 class="ch-title">CHAPTER 8: DATABASE DESIGN &amp; DATA MANAGEMENT</h1>
      
      <h2 class="sec-title">8.1 Database Selection &amp; Schema Topology</h2>
      <p>
        CipherVault selects <strong>MySQL Community Server 8.0</strong> with the <strong>InnoDB</strong> storage engine as its persistence tier. The schema topology is designed around three normalized, relational entities:
      </p>
      <ul>
        <li><code class="inline">users</code>: Master account registry, storage quotas, token versions, and PBKDF2-wrapped User Data Encryption Keys.</li>
        <li><code class="inline">stored_files</code>: Encrypted file catalog, physical storage paths, cryptographic SHA-256 digests, and preview paths.</li>
        <li><code class="inline">file_metadata</code>: Forensic multimedia attributes extracted by ExifTool (camera make, model, ISO, resolution, codecs, tags).</li>
      </ul>

      <h2 class="sec-title">Schema Character Set &amp; Collation</h2>
      <p>
        To guarantee full multilingual compatibility and prevent character encoding corruption on international filenames (such as Japanese kanji, Arabic script, or Unicode emojis), the database enforces:
      </p>
      <pre class="code-block">
CREATE DATABASE IF NOT EXISTS ciphervault 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
      </pre>
      <p>
        This configuration ensures 4-byte UTF-8 characters are safely persisted without truncation.
      </p>
'''
    pages.append(make_page(52, total_pages, "CHAPTER 8 &bull; DATABASE DESIGN (1/6)", p52))

    # Page 53: 8.2 ER Diagram (Figure 8.1)
    p53 = '''
      <h1 class="ch-title">CHAPTER 8: DATABASE DESIGN &amp; DATA MANAGEMENT (CONT.)</h1>
      
      <h2 class="sec-title">8.2 Entity-Relationship (ER) Diagram</h2>
      <p>
        The relational entity-relationship structure, primary keys, foreign key constraints, and unique indices are documented in Figure 8.1:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_15_database_er.svg" class="figure-img" alt="Database ER Diagram"/>
        <div class="fig-caption">FIGURE 8.1 &mdash; DATABASE ENTITY-RELATIONSHIP (ER) DIAGRAM</div>
        <div class="fig-desc">
          Relational schema topology for MySQL 8.0: 1:N relationship between users and stored_files; 1:1 relationship between stored_files and file_metadata; cascading delete constraints and unique SHA-256 indexes.
        </div>
      </div>
'''
    pages.append(make_page(53, total_pages, "CHAPTER 8 &bull; DATABASE DESIGN (2/6)", p53))

    # Page 54: 8.3 Table Specs: users & stored_files (Tables 8.1 & 8.2)
    p54 = '''
      <h1 class="ch-title">CHAPTER 8: DATABASE DESIGN &amp; DATA MANAGEMENT (CONT.)</h1>
      
      <h2 class="sec-title">8.3 Table Specifications: `users` and `stored_files`</h2>

      <table class="report-table">
        <tr>
          <th style="width: 20%;">Column Name</th>
          <th style="width: 20%;">SQL Data Type</th>
          <th style="width: 20%;">Constraints</th>
          <th style="width: 40%;">Description &amp; Operational Purpose</th>
        </tr>
        <tr><td><strong>id</strong></td><td>BIGINT</td><td>PK, AUTO_INC</td><td>Unique internal user surrogate identifier.</td></tr>
        <tr><td><strong>username</strong></td><td>VARCHAR(255)</td><td>NOT NULL, UNIQUE</td><td>Unique login handle.</td></tr>
        <tr><td><strong>email</strong></td><td>VARCHAR(255)</td><td>NOT NULL, UNIQUE</td><td>Unique user email address for auth.</td></tr>
        <tr><td><strong>password</strong></td><td>VARCHAR(255)</td><td>NOT NULL</td><td>BCrypt hashed password (work factor 10).</td></tr>
        <tr><td><strong>storage_limit</strong></td><td>BIGINT</td><td>NOT NULL, DEF 10GB</td><td>Account quota limit in bytes (10737418240).</td></tr>
        <tr><td><strong>used_storage</strong></td><td>BIGINT</td><td>NOT NULL, DEF 0</td><td>Current active storage consumption in bytes.</td></tr>
        <tr><td><strong>user_key</strong></td><td>VARCHAR(512)</td><td>NULL</td><td>Server-wrapped 256-bit UDEK key.</td></tr>
        <tr><td><strong>token_version</strong></td><td>BIGINT</td><td>NOT NULL, DEF 0</td><td>Incremented to invalidate outstanding JWTs.</td></tr>
      </table>
      <div class="fig-caption">Table 8.1 &mdash; MySQL `users` Entity Schema Specification</div>

      <table class="report-table" style="margin-top: 10px;">
        <tr>
          <th style="width: 20%;">Column Name</th>
          <th style="width: 20%;">SQL Data Type</th>
          <th style="width: 20%;">Constraints</th>
          <th style="width: 40%;">Description &amp; Operational Purpose</th>
        </tr>
        <tr><td><strong>id</strong></td><td>BIGINT</td><td>PK, AUTO_INC</td><td>Unique stored file record identifier.</td></tr>
        <tr><td><strong>user_id</strong></td><td>BIGINT</td><td>FK &rarr; users(id)</td><td>Owning user ID (ON DELETE CASCADE).</td></tr>
        <tr><td><strong>original_filename</strong></td><td>VARCHAR(255)</td><td>NOT NULL</td><td>Client-supplied display filename.</td></tr>
        <tr><td><strong>file_size</strong></td><td>BIGINT</td><td>NOT NULL</td><td>Exact plaintext file size in bytes.</td></tr>
        <tr><td><strong>content_type</strong></td><td>VARCHAR(255)</td><td>NOT NULL</td><td>MIME type string (e.g., image/jpeg).</td></tr>
        <tr><td><strong>sha256_hash</strong></td><td>VARCHAR(64)</td><td>NOT NULL</td><td>Hex-encoded SHA-256 digest of payload.</td></tr>
        <tr><td><strong>storage_path</strong></td><td>VARCHAR(512)</td><td>NOT NULL</td><td>Filesystem path to encrypted blob.</td></tr>
        <tr><td><strong>encrypted</strong></td><td>BOOLEAN</td><td>NOT NULL, DEF TRUE</td><td>Flag confirming AES-256-GCM encryption.</td></tr>
      </table>
      <div class="fig-caption">Table 8.2 &mdash; MySQL `stored_files` Entity Schema Specification</div>
'''
    pages.append(make_page(54, total_pages, "CHAPTER 8 &bull; DATABASE DESIGN (3/6)", p54))

    # Page 55: 8.4 Table Spec: file_metadata (Table 8.3)
    p55 = '''
      <h1 class="ch-title">CHAPTER 8: DATABASE DESIGN &amp; DATA MANAGEMENT (CONT.)</h1>
      
      <h2 class="sec-title">8.4 Table Specification: `file_metadata`</h2>
      <p>
        The <code class="inline">file_metadata</code> table stores extracted forensic multimedia properties:
      </p>

      <table class="report-table">
        <tr>
          <th style="width: 22%;">Column Name</th>
          <th style="width: 18%;">SQL Data Type</th>
          <th style="width: 20%;">Constraints</th>
          <th style="width: 40%;">Description &amp; Forensic Attribute</th>
        </tr>
        <tr><td><strong>id</strong></td><td>BIGINT</td><td>PK, AUTO_INC</td><td>Unique forensic metadata record identifier.</td></tr>
        <tr><td><strong>file_id</strong></td><td>BIGINT</td><td>FK, UNIQUE</td><td>Links to stored_files (ON DELETE CASCADE).</td></tr>
        <tr><td><strong>camera_make</strong></td><td>VARCHAR(255)</td><td>INDEX</td><td>Camera hardware manufacturer (e.g. Google, Sony).</td></tr>
        <tr><td><strong>camera_model</strong></td><td>VARCHAR(255)</td><td>INDEX</td><td>Device model (e.g. Pixel 10 Pro XL).</td></tr>
        <tr><td><strong>iso</strong></td><td>VARCHAR(50)</td><td>NULL</td><td>Camera sensor ISO speed rating.</td></tr>
        <tr><td><strong>exposure_time</strong></td><td>VARCHAR(100)</td><td>NULL</td><td>Shutter speed duration (e.g. 1/250s).</td></tr>
        <tr><td><strong>f_number</strong></td><td>VARCHAR(50)</td><td>NULL</td><td>Lens aperture F-stop value.</td></tr>
        <tr><td><strong>width / height</strong></td><td>INT</td><td>NULL</td><td>Image or video frame pixel dimensions.</td></tr>
        <tr><td><strong>resolution</strong></td><td>VARCHAR(100)</td><td>INDEX</td><td>Formatted resolution string (e.g. 1280x960).</td></tr>
        <tr><td><strong>video_codec</strong></td><td>VARCHAR(100)</td><td>INDEX</td><td>Video compression format (e.g. H.264, HEVC).</td></tr>
        <tr><td><strong>audio_codec</strong></td><td>VARCHAR(100)</td><td>NULL</td><td>Audio track format (e.g. AAC, MP3).</td></tr>
        <tr><td><strong>author / title</strong></td><td>VARCHAR(255)</td><td>INDEX</td><td>Document author or audio track title.</td></tr>
        <tr><td><strong>raw_metadata_json</strong></td><td>TEXT</td><td>NULL</td><td>Full unparsed JSON dump from ExifTool.</td></tr>
      </table>
      <div class="fig-caption">Table 8.3 &mdash; MySQL `file_metadata` Entity Schema Specification</div>
'''
    pages.append(make_page(55, total_pages, "CHAPTER 8 &bull; DATABASE DESIGN (4/6)", p55))

    # Page 56: 8.5 Indexing Strategy & Atomic Quotas
    p56 = '''
      <h1 class="ch-title">CHAPTER 8: DATABASE DESIGN &amp; DATA MANAGEMENT (CONT.)</h1>
      
      <h2 class="sec-title">8.5 Indexing Strategy, Unique Constraints &amp; Atomic Quotas</h2>
      <p>
        To prevent high concurrency bottlenecks and guarantee database integrity under heavy loads, CipherVault incorporates sophisticated database-level constraints:
      </p>

      <h3 class="subsec-title">1. Deduplication via Unique Composite Constraint</h3>
      <p>
        The constraint <code class="inline">uq_stored_files_user_sha256 UNIQUE (user_id, sha256_hash)</code> operates as both a deduplication validator and a lightning-fast B-Tree search index. If two concurrent upload threads attempt to ingest identical files for the same user, MySQL enforces atomicity, rejecting the second insert with a duplicate key violation.
      </p>

      <h3 class="subsec-title">2. Atomic Storage Quota Tracking</h3>
      <p>
        Updating storage usage in application memory before writing to the database creates severe race conditions. CipherVault executes storage updates directly inside atomic SQL expressions:
      </p>
      <pre class="code-block">
UPDATE users 
SET used_storage = used_storage + :size 
WHERE id = :userId 
  AND (used_storage + :size) &lt;= storage_limit;
      </pre>
      <p>
        If the resulting total exceeds <code class="inline">storage_limit</code>, the update affects 0 rows, prompting the service layer to immediately throw a <code class="inline">QuotaExceededException</code> before writing bytes to disk.
      </p>
'''
    pages.append(make_page(56, total_pages, "CHAPTER 8 &bull; DATABASE DESIGN (5/6)", p56))

    # Page 57: 8.6 Cascading Deletions & Retention
    p57 = '''
      <h1 class="ch-title">CHAPTER 8: DATABASE DESIGN &amp; DATA MANAGEMENT (CONT.)</h1>
      
      <h2 class="sec-title">8.6 Data Lifecycle, Retention &amp; Cascading Deletion</h2>
      <p>
        CipherVault enforces zero-orphan relational data lifecycle rules:
      </p>

      <div class="security-box">
        <div class="box-title">Foreign Key Cascade Invariants</div>
        Every foreign key in CipherVault is registered with <code class="inline">ON DELETE CASCADE</code>:
        <ul>
          <li>Deleting a file in <code class="inline">stored_files</code> automatically purges its corresponding record in <code class="inline">file_metadata</code>.</li>
          <li>Deleting a user account in <code class="inline">users</code> automatically triggers cascading deletion of all associated files and metadata records.</li>
        </ul>
      </div>

      <h3 class="subsec-title">Physical Disk Unlink Lifecycle</h3>
      <p>
        Relational record deletion is paired with physical filesystem unlinking in <code class="inline">FileStorageService.deleteFile()</code>:
      </p>
      <ol style="font-size: 8.4pt; line-height: 1.5;">
        <li>Fetches file entity and validates ownership against authenticated session user ID.</li>
        <li>Deletes database record inside transaction (decrementing <code class="inline">used_storage</code>).</li>
        <li>Invokes <code class="inline">Files.deleteIfExists(storagePath)</code> to delete the encrypted blob.</li>
        <li>Unlinks cached thumbnail preview file in <code class="inline">storage/previews/</code>.</li>
      </ol>
      <p>
        This transactional sequence guarantees that deleted files leave zero residual traces on host disks.
      </p>
'''
    pages.append(make_page(57, total_pages, "CHAPTER 8 &bull; DATABASE DESIGN (6/6)", p57))

    # =========================================================================
    # CHAPTER 9: SECURITY AND CRYPTOGRAPHY (Pages 58 to 65)
    # =========================================================================

    # Page 58: 9.1 NIST Authenticated Encryption
    p58 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY</h1>
      
      <h2 class="sec-title">9.1 NIST Authenticated Encryption (AES-256-GCM)</h2>
      <p>
        CipherVault strictly rejects unauthenticated legacy ciphers (such as DES, RC4, or unauthenticated CBC mode) in favor of NIST SP 800-38D standard <strong>AES-256 in Galois/Counter Mode (GCM)</strong>:
      </p>

      <div class="security-box">
        <div class="box-title">Cryptographic Parameters in CipherVault</div>
        <ul>
          <li><strong>Cipher Algorithm:</strong> <code class="inline">AES/GCM/NoPadding</code> (256-bit symmetric key).</li>
          <li><strong>Nonce / IV Length:</strong> 96 bits (12 bytes), generated via <code class="inline">SecureRandom</code> per upload.</li>
          <li><strong>Authentication Tag:</strong> 128 bits (16 bytes) computed by GHASH over ciphertext.</li>
          <li><strong>Buffer Chunk Size:</strong> 16,384 bytes (16 KB) streaming pipeline.</li>
        </ul>
      </div>

      <h3 class="subsec-title">Tamper-Evident Security Invariant</h3>
      <p>
        The 128-bit GHASH tag provides cryptographic integrity. If an adversary attempts to modify, inject, or truncate even a single bit of the encrypted blob stored on disk, the decryption cipher will detect the algebraic mismatch and throw <code class="inline">javax.crypto.AEADBadTagException</code>. The decryption pipeline halts instantly, guaranteeing that corrupt or tampered plaintext is never released to the client.
      </p>
'''
    pages.append(make_page(58, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (1/8)", p58))

    # Page 59: 9.2 JWT Lifecycle (Figure 9.1)
    p59 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY (CONT.)</h1>
      
      <h2 class="sec-title">9.2 Authentication &amp; JWT Token Lifecycle</h2>
      <p>
        User authentication and session verification follow a strictly stateless lifecycle mediated by JSON Web Tokens, illustrated in Figure 9.1:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_06_auth_jwt_lifecycle.svg" class="figure-img" alt="JWT Lifecycle"/>
        <div class="fig-caption">FIGURE 9.1 &mdash; AUTHENTICATION &amp; JWT TOKEN LIFECYCLE</div>
        <div class="fig-desc">
          Complete token lifecycle: 1. Registration; 2. Authentication &amp; HS256 Token Issuance; 3. Client Keystore Storage; 4. Authorized Bearer Requests; 5. Server Token Validation; 6. Expiration &amp; Automatic Revocation.
        </div>
      </div>
'''
    pages.append(make_page(59, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (2/8)", p59))

    # Page 60: 9.3 Envelope Encryption (Figure 9.2)
    p60 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY (CONT.)</h1>
      
      <h2 class="sec-title">9.3 Envelope Encryption Hierarchy &amp; Storage Workflow</h2>
      <p>
        CipherVault organizes cryptographic keys into a dual-layer envelope hierarchy, separating server secrets from individual user data keys, illustrated in Figure 9.2:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_09_encryption_storage_workflow.svg" class="figure-img" alt="Envelope Encryption"/>
        <div class="fig-caption">FIGURE 9.2 &mdash; ENVELOPE ENCRYPTION &amp; DISK STORAGE WORKFLOW</div>
        <div class="fig-desc">
          Key hierarchy: Layer 1 derives Server KEK via PBKDF2; Layer 2 wraps per-user UDEKs; Layer 3 streams payload through CipherOutputStream, writing physical blobs formatted as [12B Nonce] + [Ciphertext] + [16B GHASH Tag].
        </div>
      </div>
'''
    pages.append(make_page(60, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (3/8)", p60))

    # Page 61: 9.4 Key Management
    p61 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY (CONT.)</h1>
      
      <h2 class="sec-title">9.4 Cryptographic Key Management &amp; Lifecycle</h2>
      <p>
        Key management in CipherVault enforces strict isolation across cryptographic domains:
      </p>

      <h3 class="subsec-title">1. Server Key Encryption Key (KEK) Derivation</h3>
      <p>
        On startup, <code class="inline">KeyManagementService</code> extracts the master server secret (<code class="inline">CIPHERVAULT_MASTER_KEY</code>) and 16-byte salt (<code class="inline">CIPHERVAULT_PBKDF2_SALT</code>). It derives the 256-bit KEK using <code class="inline">SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")</code> with 65,536 iterations. The KEK resides solely in server memory and is never written to disk or logs.
      </p>

      <h3 class="subsec-title">2. User Data Encryption Key (UDEK) Generation and Wrapping</h3>
      <p>
        Upon user registration, the server invokes <code class="inline">KeyGenerator.getInstance("AES").generateKey()</code> to create a fresh 256-bit symmetric UDEK. The UDEK is immediately wrapped (encrypted) under the Server KEK via AES-256-GCM and stored in <code class="inline">users.user_key</code>:
      </p>
      <ul>
        <li><strong>Database Compromise Defense:</strong> If an attacker dumps the MySQL database, they obtain only wrapped UDEKs; without the server master secret, keys cannot be unwrapped.</li>
        <li><strong>Host Storage Compromise Defense:</strong> If an attacker steals encrypted blobs from disk, they cannot decrypt files without unwrapping the user's UDEK.</li>
      </ul>
'''
    pages.append(make_page(61, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (4/8)", p61))

    # Page 62: 9.5 Password Hashing & Salts
    p62 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY (CONT.)</h1>
      
      <h2 class="sec-title">9.5 Password Hashing &amp; Salt Handling</h2>
      <p>
        Authentication credentials in CipherVault are protected against offline dictionary and rainbow-table attacks using modern hashing standards:
      </p>

      <div class="callout-box">
        <div class="box-title">BCrypt Adaptive Hashing with Cost Factor 10</div>
        User passwords are submitted via TLS/HTTPS, validated against strength rules (&ge; 6 characters), and hashed using BCrypt. The implementation automatically embeds a cryptographically random 128-bit salt into the output string:
        <code class="inline">$2a$10$e8wF5qH...60characterhash</code>
      </div>

      <h3 class="subsec-title">Brute-Force Rate Limiting</h3>
      <p>
        To prevent automated online credential stuffing attacks, <code class="inline">LoginRateLimiterService</code> tracks failed authentication attempts keyed by client IP and username:
      </p>
      <ul>
        <li>Each failed attempt increments an internal failure counter.</li>
        <li>Reaching <strong>5 consecutive failures</strong> triggers an automatic <strong>15-minute lockout</strong> (<code class="inline">HTTP 429 Too Many Requests</code>).</li>
        <li>Successful authentication resets the counter immediately.</li>
      </ul>
'''
    pages.append(make_page(62, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (5/8)", p62))

    # Page 63: 9.6 Threat Model & STRIDE (Figure 9.3)
    p63 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY (CONT.)</h1>
      
      <h2 class="sec-title">9.6 Security Trust Boundaries &amp; STRIDE Threat Model</h2>
      <p>
        The threat surface of CipherVault was systematically audited using Microsoft's STRIDE methodology across Client, Network, and Server trust boundaries, illustrated in Figure 9.3:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_23_threat_model.svg" class="figure-img" alt="Threat Model"/>
        <div class="fig-caption">FIGURE 9.3 &mdash; SECURITY TRUST BOUNDARIES &amp; STRIDE THREAT MODEL</div>
        <div class="fig-desc">
          Trust boundaries and attack vectors: Client Boundary (token theft, shoulder surfing, memory scavenging); Network Boundary (MITM eavesdropping, replay attacks, DoS flooding); Server Boundary (ciphertext tampering, key leaks, brute-force auth).
        </div>
      </div>
'''
    pages.append(make_page(63, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (6/8)", p63))

    # Page 64: 9.7 STRIDE Matrix (Table 9.1)
    p64 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY (CONT.)</h1>
      
      <h2 class="sec-title">9.7 Comprehensive Threat Risk Assessment Matrix</h2>
      <p>
        Table 9.1 details the STRIDE threat classification, risk severity, and verified architectural mitigations:
      </p>

      <table class="report-table">
        <tr>
          <th style="width: 14%;">STRIDE Class</th>
          <th style="width: 26%;">Identified Attack Vector</th>
          <th style="width: 12%;">Severity</th>
          <th style="width: 33%;">Verified Implementation Mitigation</th>
          <th style="width: 15%;">Status</th>
        </tr>
        <tr>
          <td><strong>Spoofing</strong></td>
          <td>Stolen JWT token replay from network capture or backup.</td>
          <td>HIGH</td>
          <td>Short-lived 24h JWT validity; tokens stored in Android Keystore EncryptedSharedPreferences.</td>
          <td><span class="badge badge-source">MITIGATED</span></td>
        </tr>
        <tr>
          <td><strong>Tampering</strong></td>
          <td>Bit-flipping ciphertext on host disk to alter decrypted data.</td>
          <td>CRITICAL</td>
          <td>AES-256-GCM 128-bit GHASH tag; immediate abort via AEADBadTagException upon modification.</td>
          <td><span class="badge badge-source">MITIGATED</span></td>
        </tr>
        <tr>
          <td><strong>Repudiation</strong></td>
          <td>User denies deleting or uploading files to server.</td>
          <td>LOW</td>
          <td>All file transfers and auth operations logged with timestamps and user IDs in audit_logs.</td>
          <td><span class="badge badge-source">MITIGATED</span></td>
        </tr>
        <tr>
          <td><strong>Info Disclosure</strong></td>
          <td>Plaintext extraction from host disk or Android heap dumps.</td>
          <td>CRITICAL</td>
          <td>Blobs encrypted at rest with AES-GCM; in-memory streaming; FLAG_SECURE on preview activities.</td>
          <td><span class="badge badge-source">MITIGATED</span></td>
        </tr>
        <tr>
          <td><strong>Denial of Service</strong></td>
          <td>Massive multi-gigabyte upload flooding to crash JVM heap.</td>
          <td>HIGH</td>
          <td>16 KB chunked streaming I/O; 2 GB upload ceiling; atomic 10 GB quota ceiling per account.</td>
          <td><span class="badge badge-source">MITIGATED</span></td>
        </tr>
        <tr>
          <td><strong>Elevation of Privilege</strong></td>
          <td>User requesting another user's files by altering file ID.</td>
          <td>CRITICAL</td>
          <td>FileStorageService queries explicitly enforce user_id match against authenticated JWT principal.</td>
          <td><span class="badge badge-source">MITIGATED</span></td>
        </tr>
      </table>
      <div class="fig-caption">Table 9.1 &mdash; STRIDE Security Risk Assessment &amp; Mitigation Register</div>
'''
    pages.append(make_page(64, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (7/8)", p64))

    # Page 65: 9.8 Android Client Hardening
    p65 = '''
      <h1 class="ch-title">CHAPTER 9: SECURITY &amp; CRYPTOGRAPHY (CONT.)</h1>
      
      <h2 class="sec-title">9.8 Android Client Security Hardening</h2>
      <p>
        The Android client implements comprehensive OS-level hardening to prevent forensic extraction from compromised devices:
      </p>

      <div class="security-box">
        <div class="box-title">1. Android Keystore MasterKey Binding</div>
        Session tokens and configuration keys are encrypted via <code class="inline">EncryptedSharedPreferences</code> backed by a Keystore <code class="inline">MasterKey</code> using <code class="inline">AES256_GCM</code>. Cryptographic keys reside inside the hardware TEE / StrongBox chip and cannot be extracted via ADB or filesystem root tools.
      </div>

      <div class="security-box">
        <div class="box-title">2. Screen Scraping Defense (FLAG_SECURE)</div>
        <code class="inline">FileViewerActivity</code> and <code class="inline">FileDetailsBottomSheet</code> enforce <code class="inline">WindowManager.LayoutParams.FLAG_SECURE</code>. The Android OS prevents screenshots, screen recording, and suppresses preview thumbnails in the recent apps switcher.
      </div>

      <div class="security-box">
        <div class="box-title">3. ADB Backup Suppression</div>
        <code class="inline">android:allowBackup="false"</code> is declared in <code class="inline">AndroidManifest.xml</code>. This prevents attackers from extracting application private databases or cache directories via <code class="inline">adb backup</code>.
      </div>
'''
    pages.append(make_page(65, total_pages, "CHAPTER 9 &bull; SECURITY &amp; CRYPTO (8/8)", p65))

    # =========================================================================
    # CHAPTER 10: CORE FEATURE WORKFLOWS (Pages 66 to 73)
    # =========================================================================

    # Page 66: 10.1 Login Sequence (Figure 10.1)
    p66 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS</h1>
      
      <h2 class="sec-title">10.1 User Registration &amp; Login Workflow</h2>
      <p>
        The end-to-end user authentication sequence involves credential validation, rate limiting, and JWT token issuance, illustrated in Figure 10.1:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_07_login_sequence.svg" class="figure-img" alt="Login Sequence"/>
        <div class="fig-caption">FIGURE 10.1 &mdash; USER LOGIN SEQUENCE DIAGRAM</div>
        <div class="fig-desc">
          Step-by-step authentication: 1. User submits credentials; 2. Retrofit posts to /api/auth/login; 3. AuthController validates via AuthManager; 4. Database returns BCrypt hash; 5. Rate limiter verifies &lt;5 fails; 6. HS256 JWT issued and stored in EncryptedSharedPreferences.
        </div>
      </div>
'''
    pages.append(make_page(66, total_pages, "CHAPTER 10 &bull; WORKFLOWS (1/8)", p66))

    # Page 67: 10.2 File Upload Sequence (Figure 10.2)
    p67 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS (CONT.)</h1>
      
      <h2 class="sec-title">10.2 End-to-End File Upload Sequence</h2>
      <p>
        File ingestion spans pre-upload validation, duplicate verification, streaming encryption, and metadata persistence, illustrated in Figure 10.2:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_08_file_upload_sequence.svg" class="figure-img" alt="Upload Sequence"/>
        <div class="fig-caption">FIGURE 10.2 &mdash; END-TO-END FILE UPLOAD &amp; ENCRYPTION SEQUENCE</div>
        <div class="fig-desc">
          Streaming upload sequence: Staged file passed to TransferManager; multipart streamed to FileController; duplicate hash verified; AES-256-GCM chunked encryption executed; EXIF extracted; MySQL records committed atomically.
        </div>
      </div>
'''
    pages.append(make_page(67, total_pages, "CHAPTER 10 &bull; WORKFLOWS (2/8)", p67))

    # Page 68: 10.3 File Download Sequence (Figure 10.3)
    p68 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS (CONT.)</h1>
      
      <h2 class="sec-title">10.3 File Download &amp; Streaming Decryption</h2>
      <p>
        The download sequence retrieves encrypted blobs, parses nonces, verifies GHASH tags, and streams plaintext to the client, illustrated in Figure 10.3:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_10_download_decryption_sequence.svg" class="figure-img" alt="Download Sequence"/>
        <div class="fig-caption">FIGURE 10.3 &mdash; FILE DOWNLOAD &amp; STREAMING DECRYPTION SEQUENCE</div>
        <div class="fig-desc">
          Download pipeline: GET /api/files/{id}/download; server reads 12B IV from disk blob; initializes CipherInputStream with user UDEK; GHASH tag verifies integrity; chunks streamed to client DownloadManager.
        </div>
      </div>
'''
    pages.append(make_page(68, total_pages, "CHAPTER 10 &bull; WORKFLOWS (3/8)", p68))

    # Page 69: 10.4 Duplicate Detection (Figure 10.4)
    p69 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS (CONT.)</h1>
      
      <h2 class="sec-title">10.4 SHA-256 Cryptographic Integrity &amp; Duplicate Detection</h2>
      <p>
        Deduplication prevents redundant network transmission and storage consumption, illustrated in Figure 10.4:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_11_duplicate_detection.svg" class="figure-img" alt="Duplicate Detection"/>
        <div class="fig-caption">FIGURE 10.4 &mdash; SHA-256 CRYPTOGRAPHIC INTEGRITY &amp; DUPLICATE DETECTION</div>
        <div class="fig-desc">
          Two-stage deduplication: Client computes SHA-256 via MessageDigest block stream; Server executes unique constraint query against MySQL; duplicates trigger HTTP 409 Conflict, conserving host storage.
        </div>
      </div>
'''
    pages.append(make_page(69, total_pages, "CHAPTER 10 &bull; WORKFLOWS (4/8)", p69))

    # Page 70: 10.5 Metadata Search (Figure 10.5)
    p70 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS (CONT.)</h1>
      
      <h2 class="sec-title">10.5 Forensic Metadata Extraction &amp; Search Pipeline</h2>
      <p>
        The metadata pipeline extracts multimedia attributes during upload and exposes them via dynamic suggestion chips, illustrated in Figure 10.5:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_12_metadata_search_workflow.svg" class="figure-img" alt="Metadata Search"/>
        <div class="fig-caption">FIGURE 10.5 &mdash; FORENSIC METADATA EXTRACTION &amp; SEARCH PIPELINE</div>
        <div class="fig-desc">
          Metadata pipeline: Ingestion inspects EXIF/codecs; attributes indexed in file_metadata; client fetches dynamic suggestion chips; user tap executes multi-attribute JPA Criteria search.
        </div>
      </div>
'''
    pages.append(make_page(70, total_pages, "CHAPTER 10 &bull; WORKFLOWS (5/8)", p70))

    # Page 71: 10.6 File Preview (Figure 10.6)
    p71 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS (CONT.)</h1>
      
      <h2 class="sec-title">10.6 File Preview &amp; In-App Decryption Pipeline</h2>
      <p>
        In-memory previewing enables immediate media inspection without leaking plaintext files to unencrypted storage, illustrated in Figure 10.6:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_13_file_preview_workflow.svg" class="figure-img" alt="File Preview"/>
        <div class="fig-caption">FIGURE 10.6 &mdash; FILE PREVIEW &amp; IN-APP DECRYPTION PIPELINE</div>
        <div class="fig-desc">
          Decrypted previewing: Path A previews staged files via local modal dialog; Path B decrypts vault files on-the-fly into RAM Bitmap buffers within FileViewerActivity with pinch-to-zoom and FLAG_SECURE.
        </div>
      </div>
'''
    pages.append(make_page(71, total_pages, "CHAPTER 10 &bull; WORKFLOWS (6/8)", p71))

    # Page 72: 10.7 File Deletion (Figure 10.7)
    p72 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS (CONT.)</h1>
      
      <h2 class="sec-title">10.7 File Deletion &amp; Storage Quota Reclaim Workflow</h2>
      <p>
        The deletion sequence coordinates database foreign key cascading, atomic quota decrementing, and disk unlinking, illustrated in Figure 10.7:
      </p>

      <div class="figure-container">
        <img src="diagrams/diag_14_file_deletion_workflow.svg" class="figure-img" alt="File Deletion"/>
        <div class="fig-caption">FIGURE 10.7 &mdash; FILE DELETION &amp; STORAGE QUOTA RECLAIM WORKFLOW</div>
        <div class="fig-desc">
          Deletion sequence: User confirms delete; DELETE /api/files/{id} issued; ownership validated; stored_files record removed; file_metadata cascades; quota decremented; disk blob securely unlinked.
        </div>
      </div>
'''
    pages.append(make_page(72, total_pages, "CHAPTER 10 &bull; WORKFLOWS (7/8)", p72))

    # Page 73: 10.8 Staging & 10.9 Error Recovery (Figures 10.8 & 10.9)
    p73 = '''
      <h1 class="ch-title">CHAPTER 10: CORE FEATURE WORKFLOWS (CONT.)</h1>
      
      <h2 class="sec-title">10.8 Activity Workflow for Staging &amp; 10.9 Fault-Tolerance Recovery</h2>
      <p>
        Branching decision workflows for Upload Staging and Fault-Tolerance Error Recovery are documented in Figures 10.8 and 10.9:
      </p>

      <div class="figure-container" style="margin-bottom: 4px;">
        <img src="diagrams/diag_18_activity_workflows.svg" style="max-height:55mm;" class="figure-img" alt="Staging Workflow"/>
        <div class="fig-caption">FIGURE 10.8 &mdash; ACTIVITY WORKFLOW FOR UPLOAD STAGING &amp; PREVIEW</div>
      </div>

      <div class="figure-container">
        <img src="diagrams/diag_22_error_handling.svg" style="max-height:55mm;" class="figure-img" alt="Error Recovery"/>
        <div class="fig-caption">FIGURE 10.9 &mdash; FAULT-TOLERANCE &amp; ERROR-HANDLING RECOVERY FLOW</div>
      </div>
'''
    pages.append(make_page(73, total_pages, "CHAPTER 10 &bull; WORKFLOWS (8/8)", p73))

    return pages
