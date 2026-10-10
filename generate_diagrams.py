import os

DIAGRAMS_DIR = os.path.abspath(r"c:\Users\akhil\OneDrive\Desktop\CipherVault\docs\project-report\diagrams")
os.makedirs(DIAGRAMS_DIR, exist_ok=True)

BG_COLOR = "#FFF8F4"
SURFACE_COLOR = "#FFF1E8"
PEACH_COLOR = "#FEDDBD"
PRIMARY_BROWN = "#815621"
ACCENT_GOLD = "#C49A6C"
DARK_TEXT = "#2D241E"
MUTED_TEXT = "#705D53"
BORDER_COLOR = "#E6D7CD"
GREEN_SECURE = "#2E7D32"
RED_ALERT = "#C62828"
BLUE_TECH = "#1565C0"

def save_svg(filename, content):
    path = os.path.join(DIAGRAMS_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip())
    print(f"Generated {filename}")

# Shared CSS styles
COMMON_STYLE = f'''
    .title {{ font-family: 'Segoe UI', Arial, sans-serif; font-size: 15px; font-weight: bold; fill: {PRIMARY_BROWN}; }}
    .sub {{ font-family: 'Segoe UI', Arial, sans-serif; font-size: 11px; fill: {MUTED_TEXT}; }}
    .box-title {{ font-family: 'Segoe UI', Arial, sans-serif; font-size: 12px; font-weight: bold; fill: {DARK_TEXT}; text-anchor: middle; }}
    .box-desc {{ font-family: 'Segoe UI', Arial, sans-serif; font-size: 9.5px; fill: {MUTED_TEXT}; text-anchor: middle; }}
    .box-desc-left {{ font-family: 'Segoe UI', Arial, sans-serif; font-size: 9.5px; fill: {MUTED_TEXT}; }}
    .step-num {{ font-family: 'Segoe UI', Arial, sans-serif; font-size: 10px; font-weight: bold; fill: #FFFFFF; text-anchor: middle; }}
    .edge-label {{ font-family: 'Segoe UI', Arial, sans-serif; font-size: 9.5px; fill: {PRIMARY_BROWN}; font-weight: 600; text-anchor: middle; }}
    .line {{ stroke: {PRIMARY_BROWN}; stroke-width: 1.6; fill: none; marker-end: url(#arrow); }}
    .dash-line {{ stroke: {MUTED_TEXT}; stroke-width: 1.4; stroke-dasharray: 4,4; fill: none; marker-end: url(#arrow-gray); }}
    .card {{ fill: #FFFFFF; stroke: {BORDER_COLOR}; stroke-width: 1.2; rx: 6; }}
    .card-peach {{ fill: {PEACH_COLOR}; stroke: {PRIMARY_BROWN}; stroke-width: 1.5; rx: 6; }}
    .card-surface {{ fill: {SURFACE_COLOR}; stroke: {BORDER_COLOR}; stroke-width: 1.2; rx: 6; }}
    .card-green {{ fill: #E8F5E9; stroke: {GREEN_SECURE}; stroke-width: 1.5; rx: 6; }}
    .card-blue {{ fill: #E3F2FD; stroke: {BLUE_TECH}; stroke-width: 1.5; rx: 6; }}
    .card-red {{ fill: #FFEBEE; stroke: {RED_ALERT}; stroke-width: 1.5; rx: 6; }}
'''

COMMON_DEFS = f'''
  <defs>
    <style>{COMMON_STYLE}</style>
    <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 1 L 9 5 L 0 9 z" fill="{PRIMARY_BROWN}"/>
    </marker>
    <marker id="arrow-gray" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 1 L 9 5 L 0 9 z" fill="{MUTED_TEXT}"/>
    </marker>
    <marker id="arrow-green" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 1 L 9 5 L 0 9 z" fill="{GREEN_SECURE}"/>
    </marker>
  </defs>
'''

# 1. System Context Diagram
svg_01 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 5.1 &mdash; CIPHERVAULT SYSTEM CONTEXT DIAGRAM (LEVEL 0)</text>
  <text x="30" y="48" class="sub">Interaction boundaries between Mobile User, Host Administrator, Android Client, Backend Server, and Persistence Tier</text>

  <rect x="50" y="140" width="130" height="90" class="card-peach"/>
  <circle cx="115" cy="170" r="16" fill="{PRIMARY_BROWN}"/>
  <text x="115" y="205" class="box-title">Mobile User</text>
  <text x="115" y="218" class="box-desc">Biometric / PIN Authenticated</text>

  <rect x="50" y="310" width="130" height="90" class="card-peach"/>
  <rect x="100" y="330" width="30" height="25" rx="3" fill="{PRIMARY_BROWN}"/>
  <text x="115" y="375" class="box-title">Host Administrator</text>
  <text x="115" y="388" class="box-desc">Desktop Server Operator</text>

  <rect x="250" y="80" width="410" height="360" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="2" rx="10"/>
  <text x="455" y="105" class="box-title" style="fill: {PRIMARY_BROWN}; font-size: 14px;">CIPHERVAULT SYSTEM BOUNDARY</text>

  <rect x="280" y="130" width="160" height="85" class="card-surface"/>
  <text x="360" y="160" class="box-title">Android Client (M3)</text>
  <text x="360" y="178" class="box-desc">Java 21 / SDK 37</text>
  <text x="360" y="193" class="box-desc">Retrofit 2 / Keystore</text>

  <rect x="475" y="130" width="160" height="85" class="card-surface"/>
  <text x="555" y="160" class="box-title">Spring Boot Core</text>
  <text x="555" y="178" class="box-desc">Version 3.3.4 (Tomcat 10)</text>
  <text x="555" y="193" class="box-desc">AES-256-GCM / ExifTool</text>

  <rect x="360" y="270" width="190" height="85" class="card-surface"/>
  <text x="455" y="300" class="box-title">Server Control Manager</text>
  <text x="455" y="318" class="box-desc">PySide6 Desktop Utility</text>
  <text x="455" y="333" class="box-desc">Process &amp; Network Health</text>

  <rect x="720" y="130" width="140" height="85" class="card-blue"/>
  <text x="790" y="160" class="box-title">MySQL 8.0 DB</text>
  <text x="790" y="178" class="box-desc">Port 3306 (InnoDB)</text>
  <text x="790" y="193" class="box-desc">Hashes &amp; Metadata</text>

  <rect x="720" y="270" width="140" height="85" class="card-green"/>
  <text x="790" y="300" class="box-title">Host Filesystem</text>
  <text x="790" y="318" class="box-desc">AES-256-GCM Blobs</text>
  <text x="790" y="333" class="box-desc">Isolated /storage/ path</text>

  <path d="M 180 185 L 280 172" class="line"/>
  <text x="230" y="170" class="edge-label">Touch / Pin</text>

  <path d="M 180 355 L 360 320" class="line"/>
  <text x="260" y="350" class="edge-label">Desktop Control</text>

  <path d="M 440 172 L 475 172" class="line"/>
  <text x="457" y="162" class="edge-label">REST</text>

  <path d="M 455 270 L 530 215" class="dash-line"/>
  <text x="510" y="250" class="edge-label">Port Ping</text>

  <path d="M 635 172 L 720 172" class="line"/>
  <text x="677" y="162" class="edge-label">JPA/JDBC</text>

  <path d="M 635 190 L 720 285" class="line"/>
  <text x="680" y="235" class="edge-label">File I/O</text>
</svg>'''
save_svg("diag_01_system_context.svg", svg_01)

# 2. Overall System Architecture
svg_02 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 5.2 &mdash; MULTI-TIER SYSTEM ARCHITECTURE</text>
  <text x="30" y="48" class="sub">Layered abstraction from Client Edge (Material 3 Native) down to Encrypted Storage &amp; Relational Indexing</text>

  <rect x="30" y="65" width="260" height="395" class="card-surface"/>
  <text x="160" y="90" class="box-title" style="fill:{PRIMARY_BROWN};">TIER 1: PRESENTATION &amp; CLIENT</text>
  
  <rect x="45" y="105" width="230" height="75" class="card"/>
  <text x="160" y="125" class="box-title">UI Activities &amp; Fragments</text>
  <text x="55" y="145" class="box-desc-left">&bull; MainActivity (Home, Vault, Transfers, Settings)</text>
  <text x="55" y="160" class="box-desc-left">&bull; UploadStagingActivity &amp; FileDetailsBottomSheet</text>

  <rect x="45" y="190" width="230" height="75" class="card"/>
  <text x="160" y="210" class="box-title">Theme &amp; Hardware Security</text>
  <text x="55" y="230" class="box-desc-left">&bull; DynamicColors (Monet Engine) + Warm Heritage</text>
  <text x="55" y="245" class="box-desc-left">&bull; MasterKey AES256_GCM / EncryptedSharedPrefs</text>

  <rect x="45" y="275" width="230" height="85" class="card"/>
  <text x="160" y="295" class="box-title">Network &amp; Foreground Engine</text>
  <text x="55" y="315" class="box-desc-left">&bull; OkHttp 4.12 Connection Pool (60s Timeout)</text>
  <text x="55" y="330" class="box-desc-left">&bull; TransferManager (Foreground Notification Service)</text>
  <text x="55" y="345" class="box-desc-left">&bull; Chunked Streaming I/O (up to 2GB payload)</text>

  <rect x="320" y="65" width="260" height="395" class="card-surface"/>
  <text x="450" y="90" class="box-title" style="fill:{PRIMARY_BROWN};">TIER 2: SPRING BOOT 3.3.4 CORE</text>

  <rect x="335" y="105" width="230" height="75" class="card"/>
  <text x="450" y="125" class="box-title">REST API &amp; Security Filter</text>
  <text x="345" y="145" class="box-desc-left">&bull; Stateless JwtAuthenticationFilter (HS256)</text>
  <text x="345" y="160" class="box-desc-left">&bull; Auth, File, Metadata, &amp; User Controllers</text>

  <rect x="335" y="190" width="230" height="80" class="card"/>
  <text x="450" y="210" class="box-title">Cryptographic Engine</text>
  <text x="345" y="230" class="box-desc-left">&bull; KeyManagementService (PBKDF2 KEK)</text>
  <text x="345" y="245" class="box-desc-left">&bull; EncryptionService (AES-256-GCM Envelope)</text>
  <text x="345" y="260" class="box-desc-left">&bull; CipherOutputStream 16KB Streaming Pipeline</text>

  <rect x="335" y="280" width="230" height="80" class="card"/>
  <text x="450" y="300" class="box-title">Storage &amp; Metadata Engine</text>
  <text x="345" y="320" class="box-desc-left">&bull; FileStorageService (Atomic 10GB Quotas)</text>
  <text x="345" y="335" class="box-desc-left">&bull; MetadataExtractionService (ExifTool / Java)</text>
  <text x="345" y="350" class="box-desc-left">&bull; MediaPreviewService (Decrypted Thumbnails)</text>

  <rect x="610" y="65" width="260" height="395" class="card-surface"/>
  <text x="740" y="90" class="box-title" style="fill:{PRIMARY_BROWN};">TIER 3: PERSISTENCE &amp; STORAGE</text>

  <rect x="625" y="105" width="230" height="110" class="card"/>
  <text x="740" y="125" class="box-title">MySQL 8.0 Relational DB</text>
  <text x="635" y="145" class="box-desc-left">&bull; users (quotas, UDEK key, token version)</text>
  <text x="635" y="160" class="box-desc-left">&bull; stored_files (sha256_hash, uq_user_sha256)</text>
  <text x="635" y="175" class="box-desc-left">&bull; file_metadata (exif, camera, codecs, tags)</text>
  <text x="635" y="190" class="box-desc-left">&bull; B-Tree indexes on user_id, hashes, dates</text>

  <rect x="625" y="225" width="230" height="100" class="card"/>
  <text x="740" y="245" class="box-title">Encrypted File Store</text>
  <text x="635" y="265" class="box-desc-left">&bull; storage/encrypted/ (AES-256-GCM Blobs)</text>
  <text x="635" y="280" class="box-desc-left">&bull; [12B Nonce] + [Ciphertext] + [16B GHASH Tag]</text>
  <text x="635" y="295" class="box-desc-left">&bull; Isolated /previews/ &amp; /uploads/ staging</text>

  <rect x="625" y="335" width="230" height="110" class="card"/>
  <text x="740" y="355" class="box-title">PySide6 Desktop Manager</text>
  <text x="635" y="375" class="box-desc-left">&bull; Process Supervisor (Spring Boot PID monitor)</text>
  <text x="635" y="390" class="box-desc-left">&bull; Port 8080 &amp; 3306 Health Probes</text>
  <text x="635" y="405" class="box-desc-left">&bull; Live Log Stream Tailing &amp; ADB Reverse</text>

  <path d="M 275 315 L 335 145" class="line"/>
  <path d="M 565 145 L 625 155" class="line"/>
  <path d="M 565 230 L 625 275" class="line"/>
</svg>'''
save_svg("diag_02_overall_architecture.svg", svg_02)

# 3. Android Application Component Architecture
svg_03 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 6.1 &mdash; ANDROID CLIENT COMPONENT ARCHITECTURE</text>
  <text x="30" y="48" class="sub">Structural relationship between UI Controllers, ViewModels/Adapters, Core Security Managers, and OkHttp Engine</text>

  <rect x="40" y="70" width="250" height="380" class="card-surface"/>
  <text x="165" y="95" class="box-title" style="fill: {PRIMARY_BROWN};">UI PRESENTATION LAYER</text>
  
  <rect x="55" y="110" width="220" height="45" class="card-peach"/>
  <text x="165" y="132" class="box-title">MainActivity</text>
  <text x="165" y="146" class="box-desc">BottomNavigationView + FragmentManager</text>

  <rect x="55" y="165" width="105" height="45" class="card"/>
  <text x="107" y="187" class="box-title">FragmentHome</text>
  <text x="107" y="200" class="box-desc">Metrics &amp; Recents</text>

  <rect x="170" y="165" width="105" height="45" class="card"/>
  <text x="222" y="187" class="box-title">FragmentVault</text>
  <text x="222" y="200" class="box-desc">Grid / Multi-select</text>

  <rect x="55" y="220" width="105" height="45" class="card"/>
  <text x="107" y="242" class="box-title">FragmentTrans</text>
  <text x="107" y="255" class="box-desc">Audit Logs / Progress</text>

  <rect x="170" y="220" width="105" height="45" class="card"/>
  <text x="222" y="242" class="box-title">FragmentSettings</text>
  <text x="222" y="255" class="box-desc">Dynamic Theme Toggle</text>

  <rect x="55" y="275" width="220" height="40" class="card"/>
  <text x="165" y="295" class="box-title">UploadStagingActivity</text>
  <text x="165" y="308" class="box-desc">Pre-upload Delete &amp; Preview Dialog</text>

  <rect x="55" y="325" width="220" height="40" class="card"/>
  <text x="165" y="345" class="box-title">FileDetailsBottomSheet</text>
  <text x="165" y="358" class="box-desc">NestedScrollView, EXIF, Hash Copy, Actions</text>

  <rect x="55" y="375" width="220" height="40" class="card"/>
  <text x="165" y="395" class="box-title">FileViewerActivity</text>
  <text x="165" y="408" class="box-desc">Full-Screen Decrypted Media &amp; Zoom</text>

  <!-- Middle Column: Adapters & Logic -->
  <rect x="325" y="70" width="250" height="380" class="card-surface"/>
  <text x="450" y="95" class="box-title" style="fill: {PRIMARY_BROWN};">ADAPTERS &amp; MANAGERS</text>

  <rect x="340" y="110" width="220" height="65" class="card"/>
  <text x="450" y="132" class="box-title">VaultFilesAdapter</text>
  <text x="450" y="148" class="box-desc">getBindingAdapterPosition() Guard</text>
  <text x="450" y="162" class="box-desc">Single Tap &rarr; BottomSheet / Multi-Select Mode</text>

  <rect x="340" y="185" width="220" height="60" class="card"/>
  <text x="450" y="207" class="box-title">ThemeManager</text>
  <text x="450" y="222" class="box-desc">DynamicColors.applyIfAvailable()</text>
  <text x="450" y="235" class="box-desc">Monet Wallpaper Palette + Beige Fallback</text>

  <rect x="340" y="255" width="220" height="65" class="card"/>
  <text x="450" y="277" class="box-title">TransferManager</text>
  <text x="450" y="292" class="box-desc">Foreground Notification Service</text>
  <text x="450" y="306" class="box-desc">Concurrent Upload/Download Queue</text>

  <rect x="340" y="330" width="220" height="60" class="card"/>
  <text x="450" y="352" class="box-title">ProfilePhotoHelper</text>
  <text x="450" y="367" class="box-desc">Local Bitmap Cache &amp; Circular Masking</text>
  <text x="450" y="380" class="box-desc">Synchronized with Backend /api/user</text>

  <!-- Right Column: Services & Security -->
  <rect x="610" y="70" width="250" height="380" class="card-surface"/>
  <text x="735" y="95" class="box-title" style="fill: {PRIMARY_BROWN};">SECURITY &amp; NETWORKING</text>

  <rect x="625" y="110" width="220" height="65" class="card-green"/>
  <text x="735" y="132" class="box-title">AppLockManager &amp; Biometrics</text>
  <text x="735" y="148" class="box-desc">Cold-Start Lock Screen Overlay</text>
  <text x="735" y="162" class="box-desc">Explicit User-Initiated BiometricPrompt</text>

  <rect x="625" y="185" width="220" height="65" class="card-green"/>
  <text x="735" y="207" class="box-title">CipherVaultPreferences</text>
  <text x="735" y="222" class="box-desc">EncryptedSharedPreferences</text>
  <text x="735" y="236" class="box-desc">MasterKey AES256_GCM Keystore Bound</text>

  <rect x="625" y="260" width="220" height="65" class="card-blue"/>
  <text x="735" y="282" class="box-title">ApiClient &amp; Retrofit 2</text>
  <text x="735" y="297" class="box-desc">Bearer Token AuthInterceptor</text>
  <text x="735" y="311" class="box-desc">Gson Converter &amp; Custom Exception Adapter</text>

  <rect x="625" y="335" width="220" height="65" class="card-blue"/>
  <text x="735" y="357" class="box-title">OkHttp 4.12 Streaming Engine</text>
  <text x="735" y="372" class="box-desc">60s Connection &amp; Read Timeouts</text>
  <text x="735" y="386" class="box-desc">Chunked Streaming (2GB Large File Buffer)</text>

  <path d="M 275 187 L 340 142" class="line"/>
  <path d="M 275 295 L 340 287" class="line"/>
  <path d="M 560 287 L 625 367" class="line"/>
  <path d="M 560 142 L 625 292" class="line"/>
</svg>'''
save_svg("diag_03_android_components.svg", svg_03)

# 4. Backend Layered Architecture
svg_04 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 7.1 &mdash; SPRING BOOT BACKEND LAYERED ARCHITECTURE</text>
  <text x="30" y="48" class="sub">Decomposition of Controllers, Security Filter Chain, Business Services, and JPA Repositories</text>

  <!-- Layer 1: Controllers -->
  <rect x="50" y="70" width="800" height="75" class="card-surface"/>
  <text x="70" y="92" class="box-title" style="fill:{PRIMARY_BROWN}; text-anchor:start;">1. CONTROLLER &amp; REST API LAYER (Tomcat 10 / HTTP 8080)</text>
  
  <rect x="70" y="102" width="170" height="32" class="card"/>
  <text x="155" y="122" class="box-title">AuthController</text>
  <rect x="260" y="102" width="170" height="32" class="card"/>
  <text x="345" y="122" class="box-title">FileController</text>
  <rect x="450" y="102" width="170" height="32" class="card"/>
  <text x="535" y="122" class="box-title">MetadataController</text>
  <rect x="640" y="102" width="170" height="32" class="card"/>
  <text x="725" y="122" class="box-title">UserController</text>

  <!-- Layer 2: Security Filter -->
  <rect x="50" y="160" width="800" height="65" class="card-green"/>
  <text x="70" y="182" class="box-title" style="fill:{GREEN_SECURE}; text-anchor:start;">2. SECURITY &amp; FILTER PIPELINE (Spring Security 6)</text>
  
  <rect x="70" y="190" width="220" height="26" class="card"/>
  <text x="180" y="207" class="box-desc" style="font-weight:600;">JwtAuthenticationFilter</text>
  <rect x="310" y="190" width="220" height="26" class="card"/>
  <text x="420" y="207" class="box-desc" style="font-weight:600;">LoginRateLimiterService (5 fails/15m)</text>
  <rect x="550" y="190" width="260" height="26" class="card"/>
  <text x="680" y="207" class="box-desc" style="font-weight:600;">SecurityFilterChain (Stateless Session)</text>

  <!-- Layer 3: Services -->
  <rect x="50" y="240" width="800" height="95" class="card-surface"/>
  <text x="70" y="262" class="box-title" style="fill:{PRIMARY_BROWN}; text-anchor:start;">3. BUSINESS SERVICE LAYER</text>

  <rect x="70" y="272" width="180" height="50" class="card"/>
  <text x="160" y="292" class="box-title">EncryptionService</text>
  <text x="160" y="308" class="box-desc">AES-256-GCM / 16KB Stream</text>

  <rect x="265" y="272" width="180" height="50" class="card"/>
  <text x="355" y="292" class="box-title">KeyManagementService</text>
  <text x="355" y="308" class="box-desc">PBKDF2 KEK / Per-User UDEK</text>

  <rect x="460" y="272" width="180" height="50" class="card"/>
  <text x="550" y="292" class="box-title">FileStorageService</text>
  <text x="550" y="308" class="box-desc">Atomic Quota (10GB Limit)</text>

  <rect x="655" y="272" width="175" height="50" class="card"/>
  <text x="742" y="292" class="box-title">MetadataExtractor</text>
  <text x="742" y="308" class="box-desc">ExifTool &amp; Java Media Parser</text>

  <!-- Layer 4: Data Access & DB -->
  <rect x="50" y="350" width="800" height="95" class="card-surface"/>
  <text x="70" y="372" class="box-title" style="fill:{PRIMARY_BROWN}; text-anchor:start;">4. DATA ACCESS LAYER &amp; PERSISTENCE</text>

  <rect x="70" y="382" width="230" height="50" class="card-blue"/>
  <text x="185" y="402" class="box-title">UserRepository</text>
  <text x="185" y="418" class="box-desc">User Auth, Storage Counter, Token Ver</text>

  <rect x="320" y="382" width="250" height="50" class="card-blue"/>
  <text x="445" y="402" class="box-title">StoredFileRepository</text>
  <text x="445" y="418" class="box-desc">SHA-256 Unique Hash, Cascade Delete</text>

  <rect x="590" y="382" width="240" height="50" class="card-blue"/>
  <text x="710" y="402" class="box-title">FileMetadataRepository</text>
  <text x="710" y="418" class="box-desc">JPA Criteria Search across 15+ Fields</text>

  <path d="M 450 145 L 450 160" class="line"/>
  <path d="M 450 225 L 450 240" class="line"/>
  <path d="M 450 335 L 450 350" class="line"/>
</svg>'''
save_svg("diag_04_backend_layered.svg", svg_04)

# 5. Android-to-Backend Communication Flow
svg_05 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 5.3 &mdash; ANDROID-TO-BACKEND COMMUNICATION &amp; PROTOCOL FLOW</text>
  <text x="30" y="48" class="sub">Request/Response pipeline, JSON REST serialization, Multipart file upload, and Streaming Octet-Stream downloads</text>

  <rect x="50" y="90" width="200" height="340" class="card-peach"/>
  <text x="150" y="115" class="box-title">ANDROID CLIENT</text>
  <rect x="65" y="130" width="170" height="45" class="card"/>
  <text x="150" y="152" class="box-title">Retrofit Interface</text>
  <text x="150" y="165" class="box-desc">Call&lt;ApiResponse&gt;</text>
  <rect x="65" y="190" width="170" height="55" class="card"/>
  <text x="150" y="210" class="box-title">AuthInterceptor</text>
  <text x="150" y="225" class="box-desc">Authorization: Bearer &lt;jwt&gt;</text>
  <text x="150" y="238" class="box-desc">Dynamic Header Injection</text>
  <rect x="65" y="260" width="170" height="50" class="card"/>
  <text x="150" y="280" class="box-title">OkHttpClient</text>
  <text x="150" y="295" class="box-desc">ConnectionPool / Timeout 60s</text>
  <rect x="65" y="325" width="170" height="85" class="card"/>
  <text x="150" y="347" class="box-title">Request Payloads</text>
  <text x="75" y="365" class="box-desc-left">&bull; JSON: Auth, Profile, Search</text>
  <text x="75" y="380" class="box-desc-left">&bull; Multipart: Binary Stream</text>
  <text x="75" y="395" class="box-desc-left">&bull; Chunk buffer: 16KB</text>

  <!-- Wire Protocol -->
  <rect x="330" y="90" width="240" height="340" class="card-surface"/>
  <text x="450" y="115" class="box-title">HTTP/1.1 REST PROTOCOL (PORT 8080)</text>

  <rect x="345" y="135" width="210" height="60" class="card-blue"/>
  <text x="450" y="155" class="box-title">POST /api/auth/login</text>
  <text x="450" y="170" class="box-desc">Content-Type: application/json</text>
  <text x="450" y="183" class="box-desc">Returns: JWT Token + User Metadata</text>

  <rect x="345" y="210" width="210" height="65" class="card-green"/>
  <text x="450" y="230" class="box-title">POST /api/files/upload</text>
  <text x="450" y="245" class="box-desc">Content-Type: multipart/form-data</text>
  <text x="450" y="258" class="box-desc">Payload: file binary stream</text>
  <text x="450" y="270" class="box-desc">Headers: Authorization: Bearer</text>

  <rect x="345" y="290" width="210" height="60" class="card-blue"/>
  <text x="450" y="310" class="box-title">GET /api/files/{id}/download</text>
  <text x="450" y="325" class="box-desc">Accept: application/octet-stream</text>
  <text x="450" y="338" class="box-desc">Returns: Decrypted Stream + Tag</text>

  <rect x="345" y="365" width="210" height="50" class="card"/>
  <text x="450" y="385" class="box-title">GET /api/metadata/search</text>
  <text x="450" y="398" class="box-desc">Query params: ?query=&amp;type=</text>

  <!-- Backend -->
  <rect x="650" y="90" width="200" height="340" class="card-surface"/>
  <text x="750" y="115" class="box-title">SPRING BOOT SERVER</text>
  <rect x="665" y="130" width="170" height="55" class="card"/>
  <text x="750" y="152" class="box-title">Tomcat 10 Connector</text>
  <text x="750" y="165" class="box-desc">Max Upload: 2048 MB</text>
  <text x="750" y="177" class="box-desc">Max Request: 2048 MB</text>
  <rect x="665" y="195" width="170" height="50" class="card"/>
  <text x="750" y="217" class="box-title">Security Filter</text>
  <text x="750" y="232" class="box-desc">JWT HS256 Verification</text>
  <rect x="665" y="255" width="170" height="55" class="card"/>
  <text x="750" y="275" class="box-title">Controllers</text>
  <text x="750" y="290" class="box-desc">@RestController mappings</text>
  <text x="750" y="302" class="box-desc">ResponseEntity&lt;T&gt;</text>
  <rect x="665" y="320" width="170" height="90" class="card"/>
  <text x="750" y="342" class="box-title">Streaming Pipeline</text>
  <text x="675" y="360" class="box-desc-left">&bull; InputStream piping</text>
  <text x="675" y="375" class="box-desc-left">&bull; Zero memory accumulation</text>
  <text x="675" y="390" class="box-desc-left">&bull; Streaming response entity</text>

  <path d="M 250 160 L 330 160" class="line"/>
  <path d="M 570 160 L 650 160" class="line"/>
  <path d="M 250 240 L 330 240" class="line"/>
  <path d="M 570 240 L 650 240" class="line"/>
  <path d="M 650 320 L 570 320" class="line"/>
  <path d="M 330 320 L 250 320" class="line"/>
</svg>'''
save_svg("diag_05_client_backend_comm.svg", svg_05)

# 6. Authentication and JWT Lifecycle
svg_06 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 9.1 &mdash; AUTHENTICATION &amp; JWT TOKEN LIFECYCLE</text>
  <text x="30" y="48" class="sub">Complete token lifecycle from registration and credential verification to bearer authorization and expiration handling</text>

  <!-- Step 1: Registration -->
  <rect x="50" y="80" width="230" height="110" class="card"/>
  <circle cx="75" cy="105" r="12" fill="{PRIMARY_BROWN}"/>
  <text x="75" y="109" class="step-num">1</text>
  <text x="165" y="105" class="box-title">User Registration</text>
  <text x="65" y="130" class="box-desc-left">&bull; User enters email, username, pwd</text>
  <text x="65" y="145" class="box-desc-left">&bull; Backend hashes password (BCrypt)</text>
  <text x="65" y="160" class="box-desc-left">&bull; Generates 256-bit UDEK key</text>
  <text x="65" y="175" class="box-desc-left">&bull; Encrypts UDEK with Server KEK</text>

  <!-- Step 2: Login & Issue -->
  <rect x="335" y="80" width="230" height="110" class="card"/>
  <circle cx="360" cy="105" r="12" fill="{PRIMARY_BROWN}"/>
  <text x="360" y="109" class="step-num">2</text>
  <text x="450" y="105" class="box-title">Authentication &amp; JWT Issue</text>
  <text x="350" y="130" class="box-desc-left">&bull; POST /api/auth/login</text>
  <text x="350" y="145" class="box-desc-left">&bull; BCrypt matches hash in DB</text>
  <text x="350" y="160" class="box-desc-left">&bull; Rate limiter checks (&lt;5 fails)</text>
  <text x="350" y="175" class="box-desc-left">&bull; Issues HS256 JWT (24-hour expiry)</text>

  <!-- Step 3: Secure Storage -->
  <rect x="620" y="80" width="230" height="110" class="card-peach"/>
  <circle cx="645" cy="105" r="12" fill="{PRIMARY_BROWN}"/>
  <text x="645" y="109" class="step-num">3</text>
  <text x="735" y="105" class="box-title">Client-Side Storage</text>
  <text x="635" y="130" class="box-desc-left">&bull; Client receives JWT &amp; username</text>
  <text x="635" y="145" class="box-desc-left">&bull; Writes to EncryptedSharedPrefs</text>
  <text x="635" y="160" class="box-desc-left">&bull; Keystore MasterKey encryption</text>
  <text x="635" y="175" class="box-desc-left">&bull; Plaintext token never on disk</text>

  <!-- Step 4: Authorized Request -->
  <rect x="620" y="270" width="230" height="110" class="card"/>
  <circle cx="645" cy="295" r="12" fill="{PRIMARY_BROWN}"/>
  <text x="645" y="299" class="step-num">4</text>
  <text x="735" y="295" class="box-title">Authorized Requests</text>
  <text x="635" y="320" class="box-desc-left">&bull; Interceptor extracts token</text>
  <text x="635" y="335" class="box-desc-left">&bull; Adds 'Authorization: Bearer &lt;jwt&gt;'</text>
  <text x="635" y="350" class="box-desc-left">&bull; Injected across all Retrofit APIs</text>
  <text x="635" y="365" class="box-desc-left">&bull; Valid on Vault, Search, Transfers</text>

  <!-- Step 5: Backend Validation -->
  <rect x="335" y="270" width="230" height="110" class="card-green"/>
  <circle cx="360" cy="295" r="12" fill="{GREEN_SECURE}"/>
  <text x="360" y="299" class="step-num">5</text>
  <text x="450" y="295" class="box-title">Server Token Validation</text>
  <text x="350" y="320" class="box-desc-left">&bull; JwtAuthenticationFilter intercepts</text>
  <text x="350" y="335" class="box-desc-left">&bull; Verifies HS256 HMAC signature</text>
  <text x="350" y="350" class="box-desc-left">&bull; Checks expiry timestamp</text>
  <text x="350" y="365" class="box-desc-left">&bull; Populates SecurityContextHolder</text>

  <!-- Step 6: Expiry & Invalidation -->
  <rect x="50" y="270" width="230" height="110" class="card-red"/>
  <circle cx="75" cy="295" r="12" fill="{RED_ALERT}"/>
  <text x="75" y="299" class="step-num">6</text>
  <text x="165" y="295" class="box-title">Token Expiry &amp; Revocation</text>
  <text x="65" y="320" class="box-desc-left">&bull; Expired token returns 401 Unauthorized</text>
  <text x="65" y="335" class="box-desc-left">&bull; Token version mismatch invalidates</text>
  <text x="65" y="350" class="box-desc-left">&bull; Client clears EncryptedSharedPrefs</text>
  <text x="65" y="365" class="box-desc-left">&bull; Automatic redirect to LoginActivity</text>

  <path d="M 280 135 L 335 135" class="line"/>
  <path d="M 565 135 L 620 135" class="line"/>
  <path d="M 735 190 L 735 270" class="line"/>
  <path d="M 620 325 L 565 325" class="line"/>
  <path d="M 335 325 L 280 325" class="line"/>
</svg>'''
save_svg("diag_06_auth_jwt_lifecycle.svg", svg_06)

# 7. User Login Sequence Diagram
svg_07 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.1 &mdash; USER LOGIN SEQUENCE DIAGRAM</text>
  <text x="30" y="48" class="sub">Interaction between Android UI, Network Client, Spring Security Filter, Authentication Manager, and Database</text>

  <!-- Lifeline Headers -->
  <rect x="50" y="70" width="120" height="35" class="card-peach"/>
  <text x="110" y="92" class="box-title">Android User/UI</text>
  <line x1="110" y1="105" x2="110" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="220" y="70" width="120" height="35" class="card"/>
  <text x="280" y="92" class="box-title">ApiClient (Retrofit)</text>
  <line x1="280" y1="105" x2="280" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="390" y="70" width="120" height="35" class="card"/>
  <text x="450" y="92" class="box-title">AuthController</text>
  <line x1="450" y1="105" x2="450" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="560" y="70" width="130" height="35" class="card"/>
  <text x="625" y="92" class="box-title">AuthManager/BCrypt</text>
  <line x1="625" y1="105" x2="625" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="740" y="70" width="110" height="35" class="card-blue"/>
  <text x="795" y="92" class="box-title">MySQL Database</text>
  <line x1="795" y1="105" x2="795" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <!-- Messages -->
  <path d="M 110 130 L 280 130" class="line"/>
  <text x="195" y="122" class="edge-label">1. Submit email &amp; password</text>

  <path d="M 280 160 L 450 160" class="line"/>
  <text x="365" y="152" class="edge-label">2. POST /api/auth/login</text>

  <path d="M 450 190 L 625 190" class="line"/>
  <text x="535" y="182" class="edge-label">3. authenticate(email, pwd)</text>

  <path d="M 625 220 L 795 220" class="line"/>
  <text x="710" y="212" class="edge-label">4. findByEmail(email)</text>

  <path d="M 795 250 L 625 250" class="dash-line"/>
  <text x="710" y="242" class="edge-label">5. User record + BCrypt hash</text>

  <path d="M 625 280 L 450 280" class="dash-line"/>
  <text x="535" y="272" class="edge-label">6. BCrypt.checkpw() SUCCESS</text>

  <path d="M 450 310 L 450 335" stroke="{PRIMARY_BROWN}" stroke-width="1.5" fill="none"/>
  <path d="M 450 335 L 480 335" stroke="{PRIMARY_BROWN}" stroke-width="1.5" fill="none"/>
  <path d="M 480 335 L 480 320" stroke="{PRIMARY_BROWN}" stroke-width="1.5" fill="none"/>
  <path d="M 480 320 L 455 320" class="line"/>
  <text x="525" y="330" class="edge-label" style="text-anchor:start;">Generate HS256 JWT</text>

  <path d="M 450 365 L 280 365" class="dash-line"/>
  <text x="365" y="357" class="edge-label">7. HTTP 200 OK + JWT Token</text>

  <path d="M 280 395 L 110 395" class="dash-line"/>
  <text x="195" y="387" class="edge-label">8. Save EncryptedSharedPrefs</text>

  <rect x="60" y="410" width="100" height="25" class="card-green"/>
  <text x="110" y="427" class="box-desc" style="font-weight:bold; fill:{GREEN_SECURE};">Route to Main</text>
</svg>'''
save_svg("diag_07_login_sequence.svg", svg_07)

# 8. File Upload Sequence Diagram
svg_08 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.2 &mdash; END-TO-END FILE UPLOAD &amp; ENCRYPTION SEQUENCE</text>
  <text x="30" y="48" class="sub">Streaming upload pipeline: SHA-256 validation, duplicate avoidance, AES-256-GCM chunked encryption, and metadata extraction</text>

  <!-- Lifelines -->
  <rect x="30" y="70" width="110" height="35" class="card-peach"/>
  <text x="85" y="92" class="box-title">UploadStaging</text>
  <line x1="85" y1="105" x2="85" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="170" y="70" width="110" height="35" class="card"/>
  <text x="225" y="92" class="box-title">TransferManager</text>
  <line x1="225" y1="105" x2="225" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="310" y="70" width="110" height="35" class="card"/>
  <text x="365" y="92" class="box-title">FileController</text>
  <line x1="365" y1="105" x2="365" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="450" y="70" width="120" height="35" class="card"/>
  <text x="510" y="92" class="box-title">EncryptionService</text>
  <line x1="510" y1="105" x2="510" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="600" y="70" width="120" height="35" class="card"/>
  <text x="660" y="92" class="box-title">MetadataExtractor</text>
  <line x1="660" y1="105" x2="660" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="750" y="70" width="110" height="35" class="card-blue"/>
  <text x="805" y="92" class="box-title">MySQL &amp; Disk</text>
  <line x1="805" y1="105" x2="805" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <!-- Messages -->
  <path d="M 85 130 L 225 130" class="line"/>
  <text x="155" y="122" class="edge-label">1. Start upload (staged)</text>

  <path d="M 225 160 L 365 160" class="line"/>
  <text x="295" y="152" class="edge-label">2. POST /api/files/upload</text>

  <path d="M 365 190 L 805 190" class="line"/>
  <text x="585" y="182" class="edge-label">3. Check duplicate (sha256_hash + user_id)</text>

  <path d="M 805 220 L 365 220" class="dash-line"/>
  <text x="585" y="212" class="edge-label">4. Unique verified &amp; Quota OK</text>

  <path d="M 365 250 L 510 250" class="line"/>
  <text x="435" y="242" class="edge-label">5. Stream to CipherOutputStream</text>

  <path d="M 510 280 L 805 280" class="line"/>
  <text x="660" y="272" class="edge-label">6. Write AES-GCM blob: [IV] + [Cipher] + [Tag]</text>

  <path d="M 365 310 L 660 310" class="line"/>
  <text x="510" y="302" class="edge-label">7. Extract EXIF / codecs / tags</text>

  <path d="M 660 340 L 805 340" class="line"/>
  <text x="730" y="332" class="edge-label">8. Save stored_files &amp; file_metadata</text>

  <path d="M 805 370 L 365 370" class="dash-line"/>
  <text x="585" y="362" class="edge-label">9. DB transaction committed</text>

  <path d="M 365 400 L 225 400" class="dash-line"/>
  <text x="295" y="392" class="edge-label">10. HTTP 200 OK + StoredFile DTO</text>

  <path d="M 225 425 L 85 425" class="dash-line"/>
  <text x="155" y="417" class="edge-label">11. Update Transfer &amp; Refresh Vault</text>
</svg>'''
save_svg("diag_08_file_upload_sequence.svg", svg_08)

# 9. File Encryption and Storage Workflow
svg_09 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 9.2 &mdash; ENVELOPE ENCRYPTION &amp; DISK STORAGE WORKFLOW</text>
  <text x="30" y="48" class="sub">Key hierarchy: Master Secret &rarr; Server KEK &rarr; User Data Key (UDEK) &rarr; AES-256-GCM Authenticated Ciphertext Blob</text>

  <!-- KEK Derivation -->
  <rect x="50" y="80" width="370" height="150" class="card-surface"/>
  <text x="235" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">LAYER 1: KEY ENCRYPTION KEY (KEK) DERIVATION</text>
  
  <rect x="70" y="120" width="130" height="40" class="card"/>
  <text x="135" y="145" class="box-desc" style="font-weight:600;">Server Master Secret</text>

  <rect x="70" y="170" width="130" height="40" class="card"/>
  <text x="135" y="195" class="box-desc" style="font-weight:600;">Static Salt (16 Bytes)</text>

  <rect x="230" y="140" width="170" height="55" class="card-peach"/>
  <text x="315" y="162" class="box-title">PBKDF2-HMAC-SHA256</text>
  <text x="315" y="178" class="box-desc">65,536 Iterations &rarr; 256-bit KEK</text>

  <path d="M 200 140 L 230 160" class="line"/>
  <path d="M 200 190 L 230 175" class="line"/>

  <!-- UDEK Wrapping -->
  <rect x="480" y="80" width="370" height="150" class="card-surface"/>
  <text x="665" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">LAYER 2: USER DATA ENCRYPTION KEY (UDEK)</text>

  <rect x="500" y="125" width="150" height="45" class="card-green"/>
  <text x="575" y="147" class="box-title">Generate UDEK</text>
  <text x="575" y="160" class="box-desc">SecureRandom 256-bit AES</text>

  <rect x="680" y="125" width="150" height="45" class="card-peach"/>
  <text x="755" y="147" class="box-title">Wrapped UDEK</text>
  <text x="755" y="160" class="box-desc">Encrypted with Server KEK</text>

  <rect x="590" y="185" width="160" height="35" class="card-blue"/>
  <text x="670" y="205" class="box-desc" style="font-weight:600;">Persisted in users.user_key</text>

  <path d="M 400 168 L 500 148" class="line"/>
  <path d="M 650 148 L 680 148" class="line"/>
  <path d="M 755 170 L 670 185" class="line"/>

  <!-- Payload Encryption -->
  <rect x="50" y="260" width="800" height="190" class="card-surface"/>
  <text x="450" y="285" class="box-title" style="fill:{PRIMARY_BROWN};">LAYER 3: FILE PAYLOAD ENCRYPTION (AES-256-GCM STREAMING)</text>

  <rect x="70" y="305" width="150" height="45" class="card"/>
  <text x="145" y="325" class="box-title">Plaintext Stream</text>
  <text x="145" y="340" class="box-desc">User File (up to 2GB)</text>

  <rect x="70" y="365" width="150" height="45" class="card"/>
  <text x="145" y="385" class="box-title">Secure Random IV</text>
  <text x="145" y="400" class="box-desc">12 Bytes Unique Nonce</text>

  <rect x="250" y="330" width="180" height="60" class="card-peach"/>
  <text x="340" y="355" class="box-title">CipherOutputStream</text>
  <text x="340" y="372" class="box-desc">AES/GCM/NoPadding (16KB)</text>

  <!-- Output Blob Format -->
  <rect x="460" y="320" width="370" height="80" fill="#FFFFFF" stroke="{GREEN_SECURE}" stroke-width="2" rx="8"/>
  <text x="645" y="345" class="box-title" style="fill:{GREEN_SECURE};">PHYSICAL ENCRYPTED BLOB ON DISK</text>
  
  <rect x="475" y="360" width="90" height="30" class="card-peach"/>
  <text x="520" y="380" class="box-desc" style="font-weight:600;">12B Nonce</text>

  <rect x="575" y="360" width="140" height="30" class="card"/>
  <text x="645" y="380" class="box-desc" style="font-weight:600;">N Bytes Ciphertext</text>

  <rect x="725" y="360" width="90" height="30" class="card-green"/>
  <text x="770" y="380" class="box-desc" style="font-weight:600;">16B GHASH Tag</text>

  <path d="M 220 327 L 250 350" class="line"/>
  <path d="M 220 387 L 250 370" class="line"/>
  <path d="M 430 360 L 460 360" class="line"/>
</svg>'''
save_svg("diag_09_encryption_storage_workflow.svg", svg_09)

# 10. File Download and Decryption Sequence Diagram
svg_10 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.3 &mdash; FILE DOWNLOAD &amp; STREAMING DECRYPTION SEQUENCE</text>
  <text x="30" y="48" class="sub">Decryption pipeline: Authentication verification, Nonce parsing, AES-GCM streaming decryption, and GHASH tamper detection</text>

  <!-- Lifelines -->
  <rect x="40" y="70" width="120" height="35" class="card-peach"/>
  <text x="100" y="92" class="box-title">Android Client</text>
  <line x1="100" y1="105" x2="100" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="230" y="70" width="120" height="35" class="card"/>
  <text x="290" y="92" class="box-title">FileController</text>
  <line x1="290" y1="105" x2="290" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="420" y="70" width="130" height="35" class="card"/>
  <text x="485" y="92" class="box-title">FileStorageService</text>
  <line x1="485" y1="105" x2="485" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="610" y="70" width="130" height="35" class="card"/>
  <text x="675" y="92" class="box-title">EncryptionService</text>
  <line x1="675" y1="105" x2="675" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="760" y="70" width="100" height="35" class="card-blue"/>
  <text x="810" y="92" class="box-title">Disk Storage</text>
  <line x1="810" y1="105" x2="810" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <!-- Messages -->
  <path d="M 100 130 L 290 130" class="line"/>
  <text x="195" y="122" class="edge-label">1. GET /api/files/{{id}}/download (Bearer)</text>

  <path d="M 290 160 L 485 160" class="line"/>
  <text x="387" y="152" class="edge-label">2. loadResource(fileId, userId)</text>

  <path d="M 485 190 L 810 190" class="line"/>
  <text x="647" y="182" class="edge-label">3. Open FileInputStream on encrypted blob</text>

  <path d="M 810 220 L 675 220" class="line"/>
  <text x="742" y="212" class="edge-label">4. Read first 12 bytes &rarr; Nonce/IV</text>

  <path d="M 675 250 L 675 275" stroke="{PRIMARY_BROWN}" stroke-width="1.5" fill="none"/>
  <path d="M 675 275 L 710 275" stroke="{PRIMARY_BROWN}" stroke-width="1.5" fill="none"/>
  <path d="M 710 275 L 710 260" stroke="{PRIMARY_BROWN}" stroke-width="1.5" fill="none"/>
  <path d="M 710 260 L 680 260" class="line"/>
  <text x="750" y="270" class="edge-label" style="text-anchor:start;">Init CipherInputStream(UDEK, IV)</text>

  <path d="M 675 300 L 485 300" class="dash-line"/>
  <text x="580" y="292" class="edge-label">5. Decrypted streaming pipe ready</text>

  <path d="M 485 330 L 290 330" class="dash-line"/>
  <text x="387" y="322" class="edge-label">6. Return ResponseEntity&lt;StreamingResponseBody&gt;</text>

  <path d="M 290 360 L 100 360" class="dash-line"/>
  <text x="195" y="352" class="edge-label">7. HTTP 200 (Stream chunks via OkHttp)</text>

  <rect x="50" y="380" width="280" height="50" class="card-green"/>
  <text x="190" y="400" class="box-title" style="fill:{GREEN_SECURE};">GHASH Integrity Verification</text>
  <text x="190" y="415" class="box-desc">If tag matches &rarr; Complete download to device</text>
  <text x="190" y="426" class="box-desc">If tampered &rarr; AEADBadTagException &rarr; Abort</text>
</svg>'''
save_svg("diag_10_download_decryption_sequence.svg", svg_10)

# 11. SHA-256 Duplicate Detection Workflow
svg_11 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.4 &mdash; SHA-256 CRYPTOGRAPHIC INTEGRITY &amp; DUPLICATE DETECTION</text>
  <text x="30" y="48" class="sub">Two-stage verification: Client-side cryptographic hash computation and server-side atomic unique constraint enforcement</text>

  <rect x="50" y="80" width="240" height="360" class="card-surface"/>
  <text x="170" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">STAGE 1: CLIENT COMPUTATION</text>

  <rect x="70" y="125" width="200" height="60" class="card"/>
  <text x="170" y="147" class="box-title">File Selection</text>
  <text x="170" y="162" class="box-desc">File picked from Android Storage</text>
  <text x="170" y="174" class="box-desc">ContentResolver Uri stream opened</text>

  <rect x="70" y="200" width="200" height="70" class="card-peach"/>
  <text x="170" y="222" class="box-title">MessageDigest SHA-256</text>
  <text x="170" y="238" class="box-desc">Streaming block hash (8KB buffer)</text>
  <text x="170" y="252" class="box-desc">Prevents reading full file to RAM</text>
  <text x="170" y="264" class="box-desc">Produces 64-char Hex Digest</text>

  <rect x="70" y="290" width="200" height="60" class="card"/>
  <text x="170" y="312" class="box-title">Multipart Header</text>
  <text x="170" y="328" class="box-desc">Hash attached to upload request</text>
  <text x="170" y="340" class="box-desc">Header: X-File-SHA256: &lt;hash&gt;</text>

  <!-- Stage 2: Server -->
  <rect x="330" y="80" width="520" height="360" class="card-surface"/>
  <text x="590" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">STAGE 2: SERVER VERIFICATION &amp; DEDUPLICATION</text>

  <rect x="350" y="125" width="220" height="65" class="card"/>
  <text x="460" y="147" class="box-title">Receive &amp; Stream Ingestion</text>
  <text x="460" y="162" class="box-desc">DigestInputStream calculates hash</text>
  <text x="460" y="176" class="box-desc">Validates stream hash matches header</text>

  <rect x="350" y="210" width="220" height="75" class="card-blue"/>
  <text x="460" y="232" class="box-title">Database Deduplication Query</text>
  <text x="460" y="248" class="box-desc">SELECT id FROM stored_files</text>
  <text x="460" y="262" class="box-desc">WHERE user_id = :uid</text>
  <text x="460" y="274" class="box-desc">AND sha256_hash = :hash</text>

  <!-- Decisions -->
  <rect x="610" y="140" width="220" height="90" class="card-red"/>
  <text x="720" y="162" class="box-title" style="fill:{RED_ALERT};">DUPLICATE DETECTED</text>
  <text x="720" y="178" class="box-desc">Record exists under user account</text>
  <text x="720" y="192" class="box-desc">&bull; Return HTTP 409 Conflict</text>
  <text x="720" y="206" class="box-desc">&bull; Disk storage quota conserved</text>
  <text x="720" y="218" class="box-desc">&bull; Client displays duplicate prompt</text>

  <rect x="610" y="250" width="220" height="90" class="card-green"/>
  <text x="720" y="272" class="box-title" style="fill:{GREEN_SECURE};">UNIQUE FILE VERIFIED</text>
  <text x="720" y="288" class="box-desc">Hash is novel for this user</text>
  <text x="720" y="302" class="box-desc">&bull; Proceed to AES-256-GCM write</text>
  <text x="720" y="316" class="box-desc">&bull; Enforce uq_user_sha256 constraint</text>
  <text x="720" y="328" class="box-desc">&bull; Return HTTP 200 OK</text>

  <path d="M 270 320 L 350 160" class="line"/>
  <path d="M 460 190 L 460 210" class="line"/>
  <path d="M 570 230 L 610 185" class="line"/>
  <text x="590" y="200" class="edge-label">Exists</text>
  <path d="M 570 260 L 610 290" class="line"/>
  <text x="590" y="280" class="edge-label">Novel</text>
</svg>'''
save_svg("diag_11_duplicate_detection.svg", svg_11)

# 12. Metadata Extraction and Search Workflow
svg_12 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.5 &mdash; FORENSIC METADATA EXTRACTION &amp; SEARCH PIPELINE</text>
  <text x="30" y="48" class="sub">Pipeline: Upload ingestion &rarr; EXIF/codec forensic extraction &rarr; MySQL indexing &rarr; Dynamic suggestion chips &rarr; JPA search</text>

  <rect x="50" y="80" width="370" height="360" class="card-surface"/>
  <text x="235" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">PHASE 1: INGESTION &amp; FORENSIC EXTRACTION</text>

  <rect x="70" y="125" width="330" height="60" class="card"/>
  <text x="235" y="147" class="box-title">Upload Completed &amp; Decrypted Head Cached</text>
  <text x="235" y="162" class="box-desc">First 64KB inspected for Magic Bytes &amp; TIFF/EXIF headers</text>
  <text x="235" y="174" class="box-desc">ExifTool / Pure Java Media Framework invoked</text>

  <rect x="70" y="200" width="330" height="110" class="card-peach"/>
  <text x="235" y="222" class="box-title">Extracted Forensic Fields</text>
  <text x="85" y="242" class="box-desc-left">&bull; Photos: Camera Make, Model, ISO, Exposure, Focal Length, GPS</text>
  <text x="85" y="258" class="box-desc-left">&bull; Videos: Resolution, Frame Rate, Video Codec, Audio Codec, Bitrate</text>
  <text x="85" y="274" class="box-desc-left">&bull; Audio: Artist, Album, Title, Genre, Release Year</text>
  <text x="85" y="290" class="box-desc-left">&bull; Documents: Author, Creator, Subject, Keywords, Modified Date</text>

  <rect x="70" y="325" width="330" height="60" class="card-blue"/>
  <text x="235" y="347" class="box-title">Persist in file_metadata Table</text>
  <text x="235" y="362" class="box-desc">One-to-One FK constraint to stored_files (ON DELETE CASCADE)</text>
  <text x="235" y="374" class="box-desc">B-Tree indexes created on make, model, codec, author, title</text>

  <!-- Search Phase -->
  <rect x="480" y="80" width="370" height="360" class="card-surface"/>
  <text x="665" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">PHASE 2: DYNAMIC SEARCH &amp; RETRIEVAL</text>

  <rect x="500" y="125" width="330" height="65" class="card"/>
  <text x="665" y="147" class="box-title">Client Dynamic Suggestion Chips</text>
  <text x="665" y="162" class="box-desc">GET /api/metadata/suggestions fetches distinct tags</text>
  <text x="665" y="174" class="box-desc">Pills: [PDF] [1280x960] [Google] [JPEG] [H.264]</text>

  <rect x="500" y="205" width="330" height="65" class="card-peach"/>
  <text x="665" y="227" class="box-title">Client Search Input &amp; Chip Tap</text>
  <text x="665" y="242" class="box-desc">User enters query or selects suggestion pill</text>
  <text x="665" y="254" class="box-desc">GET /api/metadata/search?query=...&amp;category=...</text>

  <rect x="500" y="285" width="330" height="70" class="card-blue"/>
  <text x="665" y="307" class="box-title">JPA Criteria Multi-Attribute Query</text>
  <text x="665" y="322" class="box-desc">Searches filename OR camera_model OR author OR codecs</text>
  <text x="665" y="335" class="box-desc">Scoped strictly to authenticated user_id</text>

  <rect x="500" y="370" width="330" height="55" class="card-green"/>
  <text x="665" y="392" class="box-title">Results Rendered in FilesAdapter</text>
  <text x="665" y="407" class="box-desc">Click opens FileDetailsBottomSheet with full forensic metadata</text>

  <path d="M 400 355 L 500 320" class="line"/>
</svg>'''
save_svg("diag_12_metadata_search_workflow.svg", svg_12)

# 13. File Preview Workflow
svg_13 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.6 &mdash; FILE PREVIEW &amp; IN-APP DECRYPTION PIPELINE</text>
  <text x="30" y="48" class="sub">Decrypted in-memory preview pipeline for Staged Files and Stored Vault Files without unencrypted disk leakage</text>

  <rect x="50" y="80" width="370" height="360" class="card-surface"/>
  <text x="235" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">PATH A: PRE-UPLOAD STAGED PREVIEW</text>

  <rect x="70" y="125" width="330" height="60" class="card"/>
  <text x="235" y="147" class="box-title">User Taps Preview in Staging List</text>
  <text x="235" y="162" class="box-desc">UploadStagingActivity item click event</text>
  <text x="235" y="174" class="box-desc">Local ContentResolver Uri inspected</text>

  <rect x="70" y="200" width="330" height="70" class="card-peach"/>
  <text x="235" y="222" class="box-title">dialog_staged_file_preview.xml</text>
  <text x="235" y="238" class="box-desc">Loads 240dp thumbnail surface</text>
  <text x="235" y="252" class="box-desc">Displays original filename, MIME type, size</text>
  <text x="235" y="264" class="box-desc">Provides 'View Fullscreen' &amp; 'Remove' buttons</text>

  <rect x="70" y="285" width="330" height="65" class="card-green"/>
  <text x="235" y="307" class="box-title">Full-Screen In-Memory Inspection</text>
  <text x="235" y="322" class="box-desc">Decoded directly into Bitmap without disk cache</text>
  <text x="235" y="334" class="box-desc">Pinch-to-zoom &amp; Pan support</text>

  <!-- Path B -->
  <rect x="480" y="80" width="370" height="360" class="card-surface"/>
  <text x="665" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">PATH B: VAULT DECRYPTED PREVIEW</text>

  <rect x="500" y="125" width="330" height="60" class="card"/>
  <text x="665" y="147" class="box-title">User Taps File in Vault or Search</text>
  <text x="665" y="162" class="box-desc">Opens FileDetailsBottomSheet (expanded state)</text>
  <text x="665" y="174" class="box-desc">Displays metadata &amp; 'Preview / Decrypt' button</text>

  <rect x="500" y="200" width="330" height="70" class="card-peach"/>
  <text x="665" y="222" class="box-title">Launch FileViewerActivity</text>
  <text x="665" y="238" class="box-desc">Passes fileId, fileName, contentType extras</text>
  <text x="665" y="252" class="box-desc">Requests decrypted byte stream from backend</text>
  <text x="665" y="264" class="box-desc">Backend streams through CipherInputStream</text>

  <rect x="500" y="285" width="330" height="65" class="card-green"/>
  <text x="665" y="307" class="box-title">Zero-Plaintext Leakage Guarantee</text>
  <text x="665" y="322" class="box-desc">Images rendered in RAM Bitmap buffer</text>
  <text x="665" y="334" class="box-desc">Window protected with FLAG_SECURE</text>

  <rect x="500" y="365" width="330" height="55" class="card-blue"/>
  <text x="665" y="387" class="box-title">Activity Exit / Memory Eviction</text>
  <text x="665" y="402" class="box-desc">Bitmap recycled on onDestroy(), GC reclaimed</text>
</svg>'''
save_svg("diag_13_file_preview_workflow.svg", svg_13)

# 14. File Deletion Workflow
svg_14 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.7 &mdash; FILE DELETION &amp; STORAGE QUOTA RECLAIM WORKFLOW</text>
  <text x="30" y="48" class="sub">Atomic sequence: User confirmation &rarr; DELETE API &rarr; Database cascade &rarr; Quota decrement &rarr; Secure disk unlink</text>

  <rect x="40" y="70" width="120" height="35" class="card-peach"/>
  <text x="100" y="92" class="box-title">Vault / BottomSheet</text>
  <line x1="100" y1="105" x2="100" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="230" y="70" width="120" height="35" class="card"/>
  <text x="290" y="92" class="box-title">FileController</text>
  <line x1="290" y1="105" x2="290" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="420" y="70" width="130" height="35" class="card"/>
  <text x="485" y="92" class="box-title">FileStorageService</text>
  <line x1="485" y1="105" x2="485" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="610" y="70" width="120" height="35" class="card-blue"/>
  <text x="670" y="92" class="box-title">MySQL Database</text>
  <line x1="670" y1="105" x2="670" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <rect x="760" y="70" width="110" height="35" class="card-green"/>
  <text x="815" y="92" class="box-title">Encrypted Disk</text>
  <line x1="815" y1="105" x2="815" y2="440" stroke="{BORDER_COLOR}" stroke-width="1.5" stroke-dasharray="4,4"/>

  <path d="M 100 130 L 290 130" class="line"/>
  <text x="195" y="122" class="edge-label">1. DELETE /api/files/{{id}} (Bearer)</text>

  <path d="M 290 160 L 485 160" class="line"/>
  <text x="387" y="152" class="edge-label">2. deleteFile(fileId, userId)</text>

  <path d="M 485 190 L 670 190" class="line"/>
  <text x="577" y="182" class="edge-label">3. Verify ownership: user_id == auth.id</text>

  <path d="M 670 220 L 485 220" class="dash-line"/>
  <text x="577" y="212" class="edge-label">4. StoredFile entity retrieved</text>

  <path d="M 485 250 L 670 250" class="line"/>
  <text x="577" y="242" class="edge-label">5. DELETE FROM stored_files WHERE id = :id</text>

  <path d="M 670 280 L 670 305" stroke="{BLUE_TECH}" stroke-width="1.5" fill="none"/>
  <path d="M 670 305 L 705 305" stroke="{BLUE_TECH}" stroke-width="1.5" fill="none"/>
  <path d="M 705 305 L 705 290" stroke="{BLUE_TECH}" stroke-width="1.5" fill="none"/>
  <path d="M 705 290 L 675 290" class="line"/>
  <text x="715" y="300" class="edge-label" style="text-anchor:start;">FK Cascade: file_metadata deleted</text>

  <path d="M 485 325 L 670 325" class="line"/>
  <text x="577" y="317" class="edge-label">6. UPDATE users SET used_storage = used_storage - :size</text>

  <path d="M 485 355 L 815 355" class="line"/>
  <text x="650" y="347" class="edge-label">7. Files.deleteIfExists(storage_path)</text>

  <path d="M 485 385 L 290 385" class="dash-line"/>
  <text x="387" y="377" class="edge-label">8. Return HTTP 200 OK</text>

  <path d="M 290 415 L 100 415" class="dash-line"/>
  <text x="195" y="407" class="edge-label">9. Dismiss Sheet &amp; Refresh Vault List</text>
</svg>'''
save_svg("diag_14_file_deletion_workflow.svg", svg_14)

# 15. Database ER Diagram
svg_15 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 8.1 &mdash; DATABASE ENTITY-RELATIONSHIP (ER) DIAGRAM</text>
  <text x="30" y="48" class="sub">Relational schema topology for MySQL 8.0: Primary keys, Foreign key cascades, and unique cryptographic constraints</text>

  <!-- Table 1: users -->
  <rect x="40" y="70" width="240" height="380" class="card"/>
  <rect x="40" y="70" width="240" height="35" class="card-peach"/>
  <text x="160" y="92" class="box-title">users</text>
  
  <text x="50" y="125" class="box-desc-left">&bull; <tspan font-weight="bold">id</tspan>: BIGINT (PK, AUTO_INC)</text>
  <text x="50" y="145" class="box-desc-left">&bull; <tspan font-weight="bold">name</tspan>: VARCHAR(255) NOT NULL</text>
  <text x="50" y="165" class="box-desc-left">&bull; <tspan font-weight="bold">username</tspan>: VARCHAR(255) UNIQUE</text>
  <text x="50" y="185" class="box-desc-left">&bull; <tspan font-weight="bold">email</tspan>: VARCHAR(255) UNIQUE</text>
  <text x="50" y="205" class="box-desc-left">&bull; <tspan font-weight="bold">password</tspan>: VARCHAR(255) (BCrypt)</text>
  <text x="50" y="225" class="box-desc-left">&bull; <tspan font-weight="bold">storage_limit</tspan>: BIGINT (10GB def)</text>
  <text x="50" y="245" class="box-desc-left">&bull; <tspan font-weight="bold">used_storage</tspan>: BIGINT DEFAULT 0</text>
  <text x="50" y="265" class="box-desc-left">&bull; <tspan font-weight="bold">user_key</tspan>: VARCHAR(512) (Wrapped UDEK)</text>
  <text x="50" y="285" class="box-desc-left">&bull; <tspan font-weight="bold">token_version</tspan>: BIGINT DEFAULT 0</text>
  <text x="50" y="305" class="box-desc-left">&bull; <tspan font-weight="bold">profile_photo_path</tspan>: VARCHAR(512)</text>
  <text x="50" y="325" class="box-desc-left">&bull; <tspan font-weight="bold">created_at</tspan>: DATETIME NOT NULL</text>

  <!-- Table 2: stored_files -->
  <rect x="330" y="70" width="250" height="380" class="card"/>
  <rect x="330" y="70" width="250" height="35" class="card-peach"/>
  <text x="455" y="92" class="box-title">stored_files</text>

  <text x="340" y="125" class="box-desc-left">&bull; <tspan font-weight="bold">id</tspan>: BIGINT (PK, AUTO_INC)</text>
  <text x="340" y="145" class="box-desc-left">&bull; <tspan font-weight="bold">user_id</tspan>: BIGINT (FK &rarr; users.id)</text>
  <text x="340" y="165" class="box-desc-left">&bull; <tspan font-weight="bold">original_filename</tspan>: VARCHAR(255)</text>
  <text x="340" y="185" class="box-desc-left">&bull; <tspan font-weight="bold">stored_filename</tspan>: VARCHAR(255)</text>
  <text x="340" y="205" class="box-desc-left">&bull; <tspan font-weight="bold">file_size</tspan>: BIGINT NOT NULL</text>
  <text x="340" y="225" class="box-desc-left">&bull; <tspan font-weight="bold">content_type</tspan>: VARCHAR(255)</text>
  <text x="340" y="245" class="box-desc-left">&bull; <tspan font-weight="bold">sha256_hash</tspan>: VARCHAR(64) NOT NULL</text>
  <text x="340" y="265" class="box-desc-left">&bull; <tspan font-weight="bold">storage_path</tspan>: VARCHAR(512)</text>
  <text x="340" y="285" class="box-desc-left">&bull; <tspan font-weight="bold">encrypted</tspan>: BOOLEAN DEFAULT TRUE</text>
  <text x="340" y="305" class="box-desc-left">&bull; <tspan font-weight="bold">has_preview</tspan>: BOOLEAN DEFAULT FALSE</text>
  <text x="340" y="325" class="box-desc-left">&bull; <tspan font-weight="bold">preview_path</tspan>: VARCHAR(512)</text>
  <text x="340" y="345" class="box-desc-left">&bull; <tspan font-weight="bold">created_at</tspan>: DATETIME NOT NULL</text>
  <text x="340" y="375" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">CONSTRAINT: uq_user_sha256 (user_id, hash)</text>
  <text x="340" y="395" class="box-desc-left" style="fill:{BLUE_TECH};">INDEX: idx_files_user (user_id)</text>

  <!-- Table 3: file_metadata -->
  <rect x="630" y="70" width="240" height="380" class="card"/>
  <rect x="630" y="70" width="240" height="35" class="card-peach"/>
  <text x="750" y="92" class="box-title">file_metadata</text>

  <text x="640" y="125" class="box-desc-left">&bull; <tspan font-weight="bold">id</tspan>: BIGINT (PK, AUTO_INC)</text>
  <text x="640" y="145" class="box-desc-left">&bull; <tspan font-weight="bold">file_id</tspan>: BIGINT UNIQUE (FK &rarr; stored_files)</text>
  <text x="640" y="165" class="box-desc-left">&bull; <tspan font-weight="bold">camera_make</tspan>: VARCHAR(255)</text>
  <text x="640" y="185" class="box-desc-left">&bull; <tspan font-weight="bold">camera_model</tspan>: VARCHAR(255)</text>
  <text x="640" y="205" class="box-desc-left">&bull; <tspan font-weight="bold">iso / exposure_time / f_number</tspan></text>
  <text x="640" y="225" class="box-desc-left">&bull; <tspan font-weight="bold">width, height, resolution</tspan></text>
  <text x="640" y="245" class="box-desc-left">&bull; <tspan font-weight="bold">duration, video_codec, audio_codec</tspan></text>
  <text x="640" y="265" class="box-desc-left">&bull; <tspan font-weight="bold">title, artist, album, genre</tspan></text>
  <text x="640" y="285" class="box-desc-left">&bull; <tspan font-weight="bold">author, creator, subject</tspan></text>
  <text x="640" y="305" class="box-desc-left">&bull; <tspan font-weight="bold">keywords, doc_created_date</tspan></text>
  <text x="640" y="325" class="box-desc-left">&bull; <tspan font-weight="bold">raw_metadata_json</tspan>: TEXT</text>
  <text x="640" y="355" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">FK: ON DELETE CASCADE</text>

  <!-- Connectors -->
  <path d="M 280 145 L 330 145" class="line"/>
  <text x="305" y="137" class="edge-label">1 : N</text>

  <path d="M 580 145 L 630 145" class="line"/>
  <text x="605" y="137" class="edge-label">1 : 1</text>
</svg>'''
save_svg("diag_15_database_er.svg", svg_15)

# 16. Data-Flow Diagrams — Level 0 and Level 1
svg_16 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 5.4 &mdash; DATA FLOW DIAGRAMS (LEVEL 0 CONTEXT &amp; LEVEL 1 DECOMPOSITION)</text>
  <text x="30" y="48" class="sub">Structured flow of data items across functional processes, external entities, and data stores</text>

  <!-- DFD Level 0 -->
  <rect x="40" y="70" width="820" height="150" class="card-surface"/>
  <text x="60" y="92" class="box-title" style="fill:{PRIMARY_BROWN}; text-anchor:start;">DFD LEVEL 0: CONTEXT LEVEL</text>

  <rect x="60" y="115" width="120" height="70" class="card-peach"/>
  <text x="120" y="147" class="box-title">Mobile User</text>
  <text x="120" y="162" class="box-desc">Entity</text>

  <circle cx="450" cy="150" r="45" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <text x="450" y="147" class="box-title">0.0</text>
  <text x="450" y="160" class="box-title" style="font-size:11px;">CipherVault</text>

  <rect x="720" y="115" width="120" height="70" class="card-blue"/>
  <text x="780" y="147" class="box-title">Persistence Store</text>
  <text x="780" y="162" class="box-desc">DB + Disk</text>

  <path d="M 180 140 L 405 140" class="line"/>
  <text x="290" y="132" class="edge-label">Uploads, Auth, Search</text>

  <path d="M 405 160 L 180 160" class="line"/>
  <text x="290" y="175" class="edge-label">Files, Tokens, Metadata</text>

  <path d="M 495 140 L 720 140" class="line"/>
  <text x="605" y="132" class="edge-label">Encrypted Blobs &amp; Index</text>

  <path d="M 720 160 L 495 160" class="line"/>
  <text x="605" y="175" class="edge-label">Stored File Records</text>

  <!-- DFD Level 1 -->
  <rect x="40" y="240" width="820" height="220" class="card-surface"/>
  <text x="60" y="262" class="box-title" style="fill:{PRIMARY_BROWN}; text-anchor:start;">DFD LEVEL 1: PROCESS DECOMPOSITION</text>

  <circle cx="110" cy="330" r="30" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="1.8"/>
  <text x="110" y="327" class="box-title" style="font-size:10px;">1.0</text>
  <text x="110" y="339" class="box-desc" style="font-weight:600;">Auth</text>

  <circle cx="280" cy="330" r="30" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="1.8"/>
  <text x="280" y="327" class="box-title" style="font-size:10px;">2.0</text>
  <text x="280" y="339" class="box-desc" style="font-weight:600;">Staging</text>

  <circle cx="450" cy="330" r="30" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="1.8"/>
  <text x="450" y="327" class="box-title" style="font-size:10px;">3.0</text>
  <text x="450" y="339" class="box-desc" style="font-weight:600;">Encrypt</text>

  <circle cx="620" cy="330" r="30" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="1.8"/>
  <text x="620" y="327" class="box-title" style="font-size:10px;">4.0</text>
  <text x="620" y="339" class="box-desc" style="font-weight:600;">Search</text>

  <circle cx="780" cy="330" r="30" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="1.8"/>
  <text x="780" y="327" class="box-title" style="font-size:10px;">5.0</text>
  <text x="780" y="339" class="box-desc" style="font-weight:600;">Decrypt</text>

  <!-- Data stores -->
  <line x1="240" y1="410" x2="320" y2="410" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="240" y1="430" x2="320" y2="430" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <text x="280" y="424" class="box-desc" style="font-weight:bold;">D1: users</text>

  <line x1="410" y1="410" x2="490" y2="410" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="410" y1="430" x2="490" y2="430" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <text x="450" y="424" class="box-desc" style="font-weight:bold;">D2: files</text>

  <line x1="580" y1="410" x2="660" y2="410" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="580" y1="430" x2="660" y2="430" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <text x="620" y="424" class="box-desc" style="font-weight:bold;">D3: metadata</text>

  <path d="M 140 330 L 250 330" class="line"/>
  <path d="M 310 330 L 420 330" class="line"/>
  <path d="M 480 330 L 590 330" class="line"/>
  <path d="M 650 330 L 750 330" class="line"/>
  <path d="M 110 360 L 240 420" class="dash-line"/>
  <path d="M 450 360 L 450 410" class="dash-line"/>
  <path d="M 620 360 L 620 410" class="dash-line"/>
</svg>'''
save_svg("diag_16_dfd_levels.svg", svg_16)

# 17. Use-Case Diagram
svg_17 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 4.1 &mdash; USE CASE MODELING DIAGRAM</text>
  <text x="30" y="48" class="sub">Core interactions for Mobile Vault User and System Host Administrator</text>

  <!-- Actor 1: Mobile User -->
  <circle cx="80" cy="200" r="16" fill="{PRIMARY_BROWN}"/>
  <line x1="80" y1="216" x2="80" y2="260" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="60" y1="230" x2="100" y2="230" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="80" y1="260" x2="65" y2="295" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="80" y1="260" x2="95" y2="295" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <text x="80" y="320" class="box-title">Mobile User</text>

  <!-- System Boundary -->
  <rect x="180" y="70" width="540" height="380" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="2" rx="10"/>
  <text x="450" y="95" class="box-title" style="fill:{PRIMARY_BROWN}; font-size:13px;">CIPHERVAULT APPLICATION SYSTEM</text>

  <!-- Use cases (Ovals) -->
  <ellipse cx="320" cy="130" rx="95" ry="22" class="card-peach"/>
  <text x="320" y="135" class="box-desc" style="font-weight:600;">UC-01: Authenticate (PIN/Bio)</text>

  <ellipse cx="320" cy="185" rx="95" ry="22" class="card"/>
  <text x="320" y="190" class="box-desc" style="font-weight:600;">UC-02: Stage &amp; Preview File</text>

  <ellipse cx="320" cy="240" rx="95" ry="22" class="card"/>
  <text x="320" y="245" class="box-desc" style="font-weight:600;">UC-03: Upload &amp; Encrypt File</text>

  <ellipse cx="320" cy="295" rx="95" ry="22" class="card"/>
  <text x="320" y="300" class="box-desc" style="font-weight:600;">UC-04: Browse &amp; Sort Vault</text>

  <ellipse cx="320" cy="350" rx="95" ry="22" class="card"/>
  <text x="320" y="355" class="box-desc" style="font-weight:600;">UC-05: Inspect Forensic Details</text>

  <ellipse cx="320" cy="405" rx="95" ry="22" class="card"/>
  <text x="320" y="410" class="box-desc" style="font-weight:600;">UC-06: Decrypt &amp; Download</text>

  <ellipse cx="580" cy="150" rx="95" ry="22" class="card"/>
  <text x="580" y="155" class="box-desc" style="font-weight:600;">UC-07: Search by Metadata</text>

  <ellipse cx="580" cy="210" rx="95" ry="22" class="card"/>
  <text x="580" y="215" class="box-desc" style="font-weight:600;">UC-08: Batch Multi-Select Delete</text>

  <ellipse cx="580" cy="270" rx="95" ry="22" class="card"/>
  <text x="580" y="275" class="box-desc" style="font-weight:600;">UC-09: Toggle Dynamic Theme</text>

  <ellipse cx="580" cy="350" rx="95" ry="22" class="card-blue"/>
  <text x="580" y="355" class="box-desc" style="font-weight:600;">UC-10: Server Lifecycle Control</text>

  <ellipse cx="580" cy="405" rx="95" ry="22" class="card-blue"/>
  <text x="580" y="410" class="box-desc" style="font-weight:600;">UC-11: Monitor Ports &amp; Logs</text>

  <!-- Actor 2: Host Admin -->
  <circle cx="820" cy="350" r="16" fill="{PRIMARY_BROWN}"/>
  <line x1="820" y1="366" x2="820" y2="410" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="800" y1="380" x2="840" y2="380" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="820" y1="410" x2="805" y2="445" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <line x1="820" y1="410" x2="835" y2="445" stroke="{PRIMARY_BROWN}" stroke-width="2"/>
  <text x="820" y="465" class="box-title">Host Admin</text>

  <!-- Links -->
  <line x1="100" y1="220" x2="225" y2="135" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="230" x2="225" y2="185" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="240" x2="225" y2="240" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="250" x2="225" y2="295" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="260" x2="225" y2="350" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="270" x2="225" y2="405" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="235" x2="485" y2="150" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="245" x2="485" y2="210" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="100" y1="255" x2="485" y2="270" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>

  <line x1="800" y1="375" x2="675" y2="355" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
  <line x1="800" y1="385" x2="675" y2="405" stroke="{PRIMARY_BROWN}" stroke-width="1.3"/>
</svg>'''
save_svg("diag_17_use_case.svg", svg_17)

# 18. Activity Diagrams for Major Workflows
svg_18 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.8 &mdash; ACTIVITY DIAGRAM FOR UPLOAD STAGING &amp; PREVIEW</text>
  <text x="30" y="48" class="sub">Branching decision workflow in UploadStagingActivity: File staging, pre-upload preview dialog, deletion, and upload initiation</text>

  <circle cx="100" cy="110" r="14" fill="{PRIMARY_BROWN}"/>
  <text x="100" y="140" class="box-desc" style="font-weight:bold;">Start: Pick Files</text>

  <rect x="180" y="85" width="150" height="50" class="card"/>
  <text x="255" y="107" class="box-title">Populate Staging</text>
  <text x="255" y="122" class="box-desc">List items with delete &amp; preview</text>

  <!-- Decision diamond -->
  <polygon points="410,110 440,85 470,110 440,135" fill="{PEACH_COLOR}" stroke="{PRIMARY_BROWN}" stroke-width="1.5"/>
  <text x="440" y="148" class="edge-label">Action?</text>

  <!-- Branch 1: Preview -->
  <rect x="530" y="70" width="150" height="45" class="card"/>
  <text x="605" y="92" class="box-title">Show Preview Dialog</text>
  <text x="605" y="105" class="box-desc">240dp modal with metadata</text>

  <!-- Branch 2: Delete -->
  <rect x="530" y="130" width="150" height="45" class="card-red"/>
  <text x="605" y="152" class="box-title" style="fill:{RED_ALERT};">Remove Item</text>
  <text x="605" y="165" class="box-desc">Evict from staging queue</text>

  <!-- Branch 3: Upload -->
  <rect x="365" y="210" width="150" height="50" class="card-green"/>
  <text x="440" y="232" class="box-title" style="fill:{GREEN_SECURE};">Tap Upload All</text>
  <text x="440" y="247" class="box-desc">Validate batch size (&lt;1GB)</text>

  <polygon points="410,310 440,285 470,310 440,335" fill="{PEACH_COLOR}" stroke="{PRIMARY_BROWN}" stroke-width="1.5"/>
  <text x="440" y="348" class="edge-label">Valid?</text>

  <rect x="230" y="290" width="140" height="45" class="card-red"/>
  <text x="300" y="312" class="box-title" style="fill:{RED_ALERT};">Batch &gt; 1GB</text>
  <text x="300" y="325" class="box-desc">Show limit warning</text>

  <rect x="530" y="290" width="160" height="55" class="card"/>
  <text x="610" y="312" class="box-title">TransferManager</text>
  <text x="610" y="327" class="box-desc">Foreground notification service</text>
  <text x="610" y="339" class="box-desc">Post multipart stream</text>

  <rect x="530" y="380" width="160" height="45" class="card-green"/>
  <text x="610" y="402" class="box-title" style="fill:{GREEN_SECURE};">Upload Complete</text>
  <text x="610" y="415" class="box-desc">Clear staging &amp; open Vault</text>

  <circle cx="760" cy="402" r="14" fill="{PRIMARY_BROWN}"/>
  <circle cx="760" cy="402" r="10" fill="#FFFFFF"/>
  <circle cx="760" cy="402" r="6" fill="{PRIMARY_BROWN}"/>

  <!-- Connecting Lines -->
  <path d="M 114 110 L 180 110" class="line"/>
  <path d="M 330 110 L 410 110" class="line"/>
  <path d="M 470 100 L 530 92" class="line"/>
  <text x="500" y="88" class="edge-label">Preview</text>
  <path d="M 470 120 L 530 145" class="line"/>
  <text x="500" y="140" class="edge-label">Delete</text>
  <path d="M 440 135 L 440 210" class="line"/>
  <text x="445" y="180" class="edge-label">Upload</text>
  <path d="M 440 260 L 440 285" class="line"/>
  <path d="M 410 310 L 370 310" class="line"/>
  <text x="390" y="302" class="edge-label">No</text>
  <path d="M 470 310 L 530 310" class="line"/>
  <text x="500" y="302" class="edge-label">Yes</text>
  <path d="M 610 345 L 610 380" class="line"/>
  <path d="M 690 402 L 746 402" class="line"/>
</svg>'''
save_svg("diag_18_activity_workflows.svg", svg_18)

# 19. Relevant UML Class Diagram
svg_19 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 12.1 &mdash; CORE UML CLASS DIAGRAM</text>
  <text x="30" y="48" class="sub">Structural class relationships between Android UI Controllers, Client Models, and Spring Boot Backend Services</text>

  <!-- Android Classes -->
  <rect x="50" y="70" width="370" height="175" class="card"/>
  <rect x="50" y="70" width="370" height="28" class="card-peach"/>
  <text x="235" y="89" class="box-title">com.ciphervault.app.FragmentVault</text>
  <text x="60" y="112" class="box-desc-left">- recyclerView: RecyclerView</text>
  <text x="60" y="126" class="box-desc-left">- adapter: VaultFilesAdapter</text>
  <text x="60" y="140" class="box-desc-left">- isMultiSelectMode: boolean</text>
  <text x="60" y="154" class="box-desc-left">- selectedFileIds: Set&lt;Long&gt;</text>
  <line x1="50" y1="162" x2="420" y2="162" stroke="{BORDER_COLOR}"/>
  <text x="60" y="177" class="box-desc-left">+ showFileDetailsBottomSheet(StoredFile file): void</text>
  <text x="60" y="191" class="box-desc-left">+ confirmAndDeleteFile(StoredFile file): void</text>
  <text x="60" y="205" class="box-desc-left">+ toggleSelectionMode(boolean enable): void</text>
  <text x="60" y="219" class="box-desc-left">+ loadVaultFiles(): void</text>

  <rect x="50" y="265" width="370" height="185" class="card"/>
  <rect x="50" y="265" width="370" height="28" class="card-peach"/>
  <text x="235" y="284" class="box-title">com.ciphervault.app.FileDetailsBottomSheet</text>
  <text x="60" y="307" class="box-desc-left">- tvOriginalFilename, tvFileSize, tvSha256: TextView</text>
  <text x="60" y="321" class="box-desc-left">- btnBottomSheetPreview, btnBottomSheetDownload: Button</text>
  <text x="60" y="335" class="box-desc-left">- currentFile: StoredFile</text>
  <line x1="50" y1="343" x2="420" y2="343" stroke="{BORDER_COLOR}"/>
  <text x="60" y="358" class="box-desc-left">+ show(FragmentManager fm, StoredFile file, Callback cb): void</text>
  <text x="60" y="372" class="box-desc-left">+ bindForensicMetadata(FileMetadata meta): void</text>
  <text x="60" y="386" class="box-desc-left">+ copySha256ToClipboard(): void</text>
  <text x="60" y="400" class="box-desc-left">+ launchDecryptedViewer(): void</text>
  <text x="60" y="414" class="box-desc-left">+ onDismiss(): void</text>

  <!-- Backend Classes -->
  <rect x="480" y="70" width="370" height="175" class="card"/>
  <rect x="480" y="70" width="370" height="28" class="card-blue"/>
  <text x="665" y="89" class="box-title">com.ciphervault.service.FileStorageService</text>
  <text x="490" y="112" class="box-desc-left">- fileRepository: StoredFileRepository</text>
  <text x="490" y="126" class="box-desc-left">- encryptionService: EncryptionService</text>
  <text x="490" y="140" class="box-desc-left">- metadataService: MetadataExtractionService</text>
  <line x1="480" y1="148" x2="850" y2="148" stroke="{BORDER_COLOR}"/>
  <text x="490" y="163" class="box-desc-left">+ storeFile(MultipartFile file, User user): StoredFile</text>
  <text x="490" y="177" class="box-desc-left">+ loadFileResource(Long fileId, Long userId): Resource</text>
  <text x="490" y="191" class="box-desc-left">+ deleteFile(Long fileId, Long userId): void</text>
  <text x="490" y="205" class="box-desc-left">+ checkDuplicate(String sha256, Long userId): boolean</text>
  <text x="490" y="219" class="box-desc-left">+ enforceStorageQuota(Long userId, long size): void</text>

  <rect x="480" y="265" width="370" height="185" class="card"/>
  <rect x="480" y="265" width="370" height="28" class="card-green"/>
  <text x="665" y="284" class="box-title">com.ciphervault.service.EncryptionService</text>
  <text x="490" y="307" class="box-desc-left">- keyManagementService: KeyManagementService</text>
  <text x="490" y="321" class="box-desc-left">- BUFFER_SIZE: int = 16384 (16KB)</text>
  <line x1="480" y1="329" x2="850" y2="329" stroke="{BORDER_COLOR}"/>
  <text x="490" y="344" class="box-desc-left">+ encryptStream(InputStream in, OutputStream out, SecretKey udek): void</text>
  <text x="490" y="358" class="box-desc-left">+ decryptStream(InputStream in, OutputStream out, SecretKey udek): void</text>
  <text x="490" y="372" class="box-desc-left">+ generateIv(): byte[] (12 Bytes)</text>
  <text x="490" y="386" class="box-desc-left">+ verifyGhashTag(): boolean</text>
  <text x="490" y="400" class="box-desc-left">+ wrapUserKey(SecretKey udek, SecretKey kek): String</text>
  <text x="490" y="414" class="box-desc-left">+ unwrapUserKey(String wrapped, SecretKey kek): SecretKey</text>

  <path d="M 235 245 L 235 265" class="line"/>
  <path d="M 665 245 L 665 265" class="line"/>
</svg>'''
save_svg("diag_19_uml_class.svg", svg_19)

# 20. Deployment Diagram
svg_20 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 5.5 &mdash; PHYSICAL DEPLOYMENT &amp; NETWORK TOPOLOGY</text>
  <text x="30" y="48" class="sub">Hardware execution nodes, network transport channels, port bindings, and storage mounts</text>

  <!-- Node 1: Mobile Device -->
  <rect x="50" y="80" width="280" height="360" class="card-surface"/>
  <text x="190" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">&laquo;device&raquo; Android Hardware Node</text>
  <text x="190" y="120" class="box-desc">Pixel 10 Pro XL / Physical Device (API 37)</text>

  <rect x="70" y="140" width="240" height="110" class="card-peach"/>
  <text x="190" y="162" class="box-title">&laquo;execution environment&raquo;</text>
  <text x="190" y="177" class="box-title">ART / Android 17</text>
  <text x="80" y="200" class="box-desc-left">&bull; CipherVault App (v2.4.0)</text>
  <text x="80" y="215" class="box-desc-left">&bull; Android Keystore (MasterKey)</text>
  <text x="80" y="230" class="box-desc-left">&bull; EncryptedSharedPreferences</text>

  <rect x="70" y="270" width="240" height="70" class="card"/>
  <text x="190" y="292" class="box-title">&laquo;hardware&raquo; StrongBox / TEE</text>
  <text x="80" y="312" class="box-desc-left">&bull; Hardware AES-256 Key Vault</text>
  <text x="80" y="325" class="box-desc-left">&bull; Biometric Sensor Subsystem</text>

  <!-- Network Transport -->
  <rect x="360" y="180" width="180" height="160" class="card"/>
  <text x="450" y="205" class="box-title">NETWORK LINK</text>
  <text x="450" y="225" class="box-desc">Wi-Fi LAN / ADB Reverse</text>
  <text x="450" y="245" class="box-desc" style="font-weight:600;">TCP Port 8080</text>
  <text x="450" y="265" class="box-desc">HTTP/1.1 REST Protocol</text>
  <text x="450" y="285" class="box-desc">Chunked Streaming (2GB)</text>
  <text x="450" y="305" class="box-desc">TLS in Production</text>

  <!-- Node 2: Host PC -->
  <rect x="570" y="80" width="280" height="360" class="card-surface"/>
  <text x="710" y="105" class="box-title" style="fill:{PRIMARY_BROWN};">&laquo;device&raquo; Host Server PC</text>
  <text x="710" y="120" class="box-desc">Windows 11 Enterprise (x86_64)</text>

  <rect x="590" y="135" width="240" height="100" class="card"/>
  <text x="710" y="157" class="box-title">&laquo;execution environment&raquo;</text>
  <text x="710" y="172" class="box-title">OpenJDK 21 LTS JVM</text>
  <text x="600" y="195" class="box-desc-left">&bull; Spring Boot 3.3.4 (PID 18212)</text>
  <text x="600" y="210" class="box-desc-left">&bull; Embedded Tomcat 10.1 (0.0.0.0:8080)</text>
  <text x="600" y="225" class="box-desc-left">&bull; AES-256-GCM Streaming Engine</text>

  <rect x="590" y="245" width="240" height="85" class="card-blue"/>
  <text x="710" y="267" class="box-title">&laquo;database&raquo; MySQL 8.0</text>
  <text x="600" y="287" class="box-desc-left">&bull; Port 3306 (InnoDB Engine)</text>
  <text x="600" y="302" class="box-desc-left">&bull; ciphervault DB (Users, Files, Meta)</text>
  <text x="600" y="317" class="box-desc-left">&bull; B-Tree Index on Hashes</text>

  <rect x="590" y="340" width="240" height="85" class="card-green"/>
  <text x="710" y="362" class="box-title">&laquo;storage&raquo; Host Filesystem</text>
  <text x="600" y="382" class="box-desc-left">&bull; storage/encrypted/ (AES Blobs)</text>
  <text x="600" y="397" class="box-desc-left">&bull; storage/previews/ (Thumbs)</text>
  <text x="600" y="412" class="box-desc-left">&bull; storage/uploads/ (Staging)</text>

  <!-- Connectors -->
  <path d="M 310 230 L 360 230" class="line"/>
  <path d="M 540 230 L 590 185" class="line"/>
</svg>'''
save_svg("diag_20_deployment.svg", svg_20)

# 21. Application Navigation Map
svg_21 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 6.2 &mdash; ANDROID CLIENT NAVIGATION STATE MACHINE</text>
  <text x="30" y="48" class="sub">Complete screen graph: Splash, Onboarding (1-5), Server Setup, AppLock, Bottom Navigation Tabs, Staging, and Viewers</text>

  <rect x="40" y="80" width="90" height="35" class="card"/>
  <text x="85" y="102" class="box-title">Splash</text>

  <rect x="160" y="80" width="120" height="35" class="card-peach"/>
  <text x="220" y="102" class="box-title">Onboarding (1-5)</text>

  <rect x="310" y="80" width="120" height="35" class="card"/>
  <text x="370" y="102" class="box-title">Server Connection</text>

  <rect x="460" y="80" width="100" height="35" class="card"/>
  <text x="510" y="102" class="box-title">QR Scanner</text>

  <rect x="590" y="80" width="110" height="35" class="card"/>
  <text x="645" y="102" class="box-title">Login / Signup</text>

  <rect x="730" y="80" width="120" height="35" class="card-green"/>
  <text x="790" y="102" class="box-title">AppLock (Bio/PIN)</text>

  <!-- Bottom Nav Tabs Container -->
  <rect x="40" y="170" width="810" height="150" class="card-surface"/>
  <text x="445" y="195" class="box-title" style="fill:{PRIMARY_BROWN}; font-size:13px;">MAIN ACTIVITY &mdash; BOTTOM NAVIGATION BAR</text>

  <rect x="60" y="215" width="170" height="85" class="card"/>
  <text x="145" y="240" class="box-title">FragmentHome</text>
  <text x="145" y="258" class="box-desc">Account Quota Progress</text>
  <text x="145" y="272" class="box-desc">Recent Uploads Carousel</text>
  <text x="145" y="285" class="box-desc">Quick Staging FAB</text>

  <rect x="255" y="215" width="170" height="85" class="card-peach"/>
  <text x="340" y="240" class="box-title">FragmentVault</text>
  <text x="340" y="258" class="box-desc">Grid / List Layout Toggle</text>
  <text x="340" y="272" class="box-desc">Sort (Date, Size, Name)</text>
  <text x="340" y="285" class="box-desc">Multi-Select Batch Delete</text>

  <rect x="450" y="215" width="170" height="85" class="card"/>
  <text x="535" y="240" class="box-title">FragmentTransfers</text>
  <text x="535" y="258" class="box-desc">Live Upload / Download</text>
  <text x="535" y="272" class="box-desc">Active Progress Bars</text>
  <text x="535" y="285" class="box-desc">Audit Log History</text>

  <rect x="645" y="215" width="185" height="85" class="card"/>
  <text x="737" y="240" class="box-title">FragmentSettings</text>
  <text x="737" y="258" class="box-desc">Dynamic Wallpaper Theme</text>
  <text x="737" y="272" class="box-desc">Profile Editor Mode</text>
  <text x="737" y="285" class="box-desc">UI Showcase Easter Egg</text>

  <!-- Sub-screens -->
  <rect x="60" y="360" width="170" height="85" class="card"/>
  <text x="145" y="385" class="box-title">UploadStagingActivity</text>
  <text x="145" y="403" class="box-desc">File selection &amp; camera</text>
  <text x="145" y="417" class="box-desc">Pre-upload preview dialog</text>
  <text x="145" y="430" class="box-desc">Staged item deletion</text>

  <rect x="255" y="360" width="170" height="85" class="card-peach"/>
  <text x="340" y="385" class="box-title">FileDetailsSheet</text>
  <text x="340" y="403" class="box-desc">NestedScrollView expandable</text>
  <text x="340" y="417" class="box-desc">EXIF &amp; SHA-256 Hash copy</text>
  <text x="340" y="430" class="box-desc">Preview, Download, Delete</text>

  <rect x="450" y="360" width="170" height="85" class="card-green"/>
  <text x="535" y="385" class="box-title">FileViewerActivity</text>
  <text x="535" y="403" class="box-desc">Full-Screen Decrypted View</text>
  <text x="535" y="417" class="box-desc">Pinch-to-zoom images</text>
  <text x="535" y="430" class="box-desc">FLAG_SECURE protection</text>

  <rect x="645" y="360" width="185" height="85" class="card-blue"/>
  <text x="737" y="385" class="box-title">MetadataSearch</text>
  <text x="737" y="403" class="box-desc">Dynamic Suggestion Chips</text>
  <text x="737" y="417" class="box-desc">Multi-field Criteria search</text>
  <text x="737" y="430" class="box-desc">Click &rarr; FileDetailsSheet</text>

  <!-- Connectors -->
  <path d="M 130 97 L 160 97" class="line"/>
  <path d="M 280 97 L 310 97" class="line"/>
  <path d="M 430 97 L 460 97" class="line"/>
  <path d="M 560 97 L 590 97" class="line"/>
  <path d="M 700 97 L 730 97" class="line"/>
  <path d="M 790 115 L 790 170" class="line"/>

  <path d="M 145 300 L 145 360" class="line"/>
  <path d="M 340 300 L 340 360" class="line"/>
  <path d="M 340 400 L 450 400" class="line"/>
  <path d="M 645 400 L 425 400" class="line"/>
</svg>'''
save_svg("diag_21_navigation_map.svg", svg_21)

# 22. Error-Handling and Recovery Flow
svg_22 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 10.9 &mdash; FAULT-TOLERANCE &amp; ERROR-HANDLING RECOVERY FLOW</text>
  <text x="30" y="48" class="sub">Structured recovery paths: Network timeout, Authentication expiry, Quota exhaustion, Tamper detection, and Duplicate conflict</text>

  <!-- Fault 1 -->
  <rect x="50" y="80" width="240" height="110" class="card-red"/>
  <text x="170" y="105" class="box-title" style="fill:{RED_ALERT};">FAULT: NETWORK TIMEOUT</text>
  <text x="60" y="125" class="box-desc-left">&bull; SocketTimeoutException (&gt;60s)</text>
  <text x="60" y="140" class="box-desc-left">&bull; Host offline / Wi-Fi disconnected</text>
  <line x1="50" y1="147" x2="290" y2="147" stroke="{BORDER_COLOR}"/>
  <text x="60" y="162" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">RECOVERY:</text>
  <text x="60" y="177" class="box-desc-left">&bull; Retry with exponential backoff</text>
  <text x="60" y="187" class="box-desc-left">&bull; Show Connection Dialog with IP edit</text>

  <!-- Fault 2 -->
  <rect x="330" y="80" width="240" height="110" class="card-red"/>
  <text x="450" y="105" class="box-title" style="fill:{RED_ALERT};">FAULT: 401 UNAUTHORIZED</text>
  <text x="340" y="125" class="box-desc-left">&bull; JWT Token expired (&gt;24 hours)</text>
  <text x="340" y="140" class="box-desc-left">&bull; Token version mismatch (revoked)</text>
  <line x1="330" y1="147" x2="570" y2="147" stroke="{BORDER_COLOR}"/>
  <text x="340" y="162" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">RECOVERY:</text>
  <text x="340" y="177" class="box-desc-left">&bull; Clear EncryptedSharedPreferences</text>
  <text x="340" y="187" class="box-desc-left">&bull; Route to LoginActivity with toast</text>

  <!-- Fault 3 -->
  <rect x="610" y="80" width="240" height="110" class="card-red"/>
  <text x="730" y="105" class="box-title" style="fill:{RED_ALERT};">FAULT: STORAGE QUOTA EXCEEDED</text>
  <text x="620" y="125" class="box-desc-left">&bull; Upload exceeds 10 GB user quota</text>
  <text x="620" y="140" class="box-desc-left">&bull; HTTP 413 Payload Too Large</text>
  <line x1="610" y1="147" x2="850" y2="147" stroke="{BORDER_COLOR}"/>
  <text x="620" y="162" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">RECOVERY:</text>
  <text x="620" y="177" class="box-desc-left">&bull; Abort stream before disk write</text>
  <text x="620" y="187" class="box-desc-left">&bull; Display Storage Full BottomSheet</text>

  <!-- Fault 4 -->
  <rect x="50" y="230" width="240" height="110" class="card-red"/>
  <text x="170" y="255" class="box-title" style="fill:{RED_ALERT};">FAULT: GHASH TAMPER FAILURE</text>
  <text x="60" y="275" class="box-desc-left">&bull; AEADBadTagException on download</text>
  <text x="60" y="290" class="box-desc-left">&bull; Ciphertext altered or truncated</text>
  <line x1="50" y1="297" x2="290" y2="297" stroke="{BORDER_COLOR}"/>
  <text x="60" y="312" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">RECOVERY:</text>
  <text x="60" y="327" class="box-desc-left">&bull; Instantly abort decryption pipeline</text>
  <text x="60" y="337" class="box-desc-left">&bull; Show 'File Tampering Alert' dialog</text>

  <!-- Fault 5 -->
  <rect x="330" y="230" width="240" height="110" class="card-red"/>
  <text x="450" y="255" class="box-title" style="fill:{RED_ALERT};">FAULT: 409 CONFLICT (DUPLICATE)</text>
  <text x="340" y="275" class="box-desc-left">&bull; SHA-256 hash exists in vault</text>
  <text x="340" y="290" class="box-desc-left">&bull; uq_user_sha256 constraint hits</text>
  <line x1="330" y1="297" x2="570" y2="297" stroke="{BORDER_COLOR}"/>
  <text x="340" y="312" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">RECOVERY:</text>
  <text x="340" y="327" class="box-desc-left">&bull; Skip payload disk write</text>
  <text x="340" y="337" class="box-desc-left">&bull; Highlight existing file in Vault</text>

  <!-- Fault 6 -->
  <rect x="610" y="230" width="240" height="110" class="card-red"/>
  <text x="730" y="255" class="box-title" style="fill:{RED_ALERT};">FAULT: 429 TOO MANY REQUESTS</text>
  <text x="620" y="275" class="box-desc-left">&bull; &gt;5 consecutive failed logins</text>
  <text x="620" y="290" class="box-desc-left">&bull; Brute-force rate limiter tripped</text>
  <line x1="610" y1="297" x2="850" y2="297" stroke="{BORDER_COLOR}"/>
  <text x="620" y="312" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:bold;">RECOVERY:</text>
  <text x="620" y="327" class="box-desc-left">&bull; 15-minute temporary IP lockout</text>
  <text x="620" y="337" class="box-desc-left">&bull; Display cooldown countdown timer</text>

  <!-- Summary Banner -->
  <rect x="50" y="380" width="800" height="60" class="card-green"/>
  <text x="450" y="405" class="box-title" style="fill:{GREEN_SECURE}; font-size:13px;">ZERO-DATA-CORRUPTION &amp; SAFE-FAIL PRINCIPLE</text>
  <text x="450" y="422" class="box-desc">All exceptions trigger clean state rollback, clear sensitive buffers, and display actionable user-friendly recovery alerts</text>
</svg>'''
save_svg("diag_22_error_handling.svg", svg_22)

# 23. Security Trust Boundaries and Threat Model
svg_23 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 9.3 &mdash; SECURITY TRUST BOUNDARIES &amp; STRIDE THREAT MODEL</text>
  <text x="30" y="48" class="sub">Trust boundaries, attack vectors, and verified cryptographic mitigations across Client, Network, and Server domains</text>

  <!-- Zone 1: Client -->
  <rect x="50" y="75" width="240" height="375" fill="#FFFFFF" stroke="{GREEN_SECURE}" stroke-width="2" rx="8"/>
  <text x="170" y="100" class="box-title" style="fill:{GREEN_SECURE};">TRUST BOUNDARY 1: CLIENT</text>

  <rect x="65" y="115" width="210" height="70" class="card"/>
  <text x="170" y="135" class="box-title">Threat: Token Theft</text>
  <text x="75" y="153" class="box-desc-left">&bull; ADB backup / Root extraction</text>
  <text x="75" y="168" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: MasterKey Keystore</text>

  <rect x="65" y="195" width="210" height="70" class="card"/>
  <text x="170" y="215" class="box-title">Threat: Unauthorized Access</text>
  <text x="75" y="233" class="box-desc-left">&bull; Device shoulder surfing / theft</text>
  <text x="75" y="248" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: Cold AppLock Overlay</text>

  <rect x="65" y="275" width="210" height="70" class="card"/>
  <text x="170" y="295" class="box-title">Threat: Screenshot Scraping</text>
  <text x="75" y="313" class="box-desc-left">&bull; Background screen recorder</text>
  <text x="75" y="328" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: FLAG_SECURE window</text>

  <rect x="65" y="355" width="210" height="80" class="card"/>
  <text x="170" y="375" class="box-title">Threat: Memory Scavenging</text>
  <text x="75" y="393" class="box-desc-left">&bull; Heap inspection / core dump</text>
  <text x="75" y="408" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: In-memory streaming,</text>
  <text x="75" y="420" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Bitmap recycle on exit</text>

  <!-- Zone 2: Network -->
  <rect x="330" y="75" width="240" height="375" fill="#FFFFFF" stroke="{BLUE_TECH}" stroke-width="2" rx="8"/>
  <text x="450" y="100" class="box-title" style="fill:{BLUE_TECH};">TRUST BOUNDARY 2: NETWORK</text>

  <rect x="345" y="115" width="210" height="95" class="card"/>
  <text x="450" y="135" class="box-title">Threat: MITM Eavesdropping</text>
  <text x="355" y="153" class="box-desc-left">&bull; Wi-Fi packet sniffing</text>
  <text x="355" y="168" class="box-desc-left">&bull; Cleartext traffic interception</text>
  <text x="355" y="185" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: TLS / HTTPS required in</text>
  <text x="355" y="198" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">prod network_security_config</text>

  <rect x="345" y="225" width="210" height="95" class="card"/>
  <text x="450" y="245" class="box-title">Threat: Replay Attacks</text>
  <text x="355" y="263" class="box-desc-left">&bull; Replaying captured request</text>
  <text x="355" y="278" class="box-desc-left">&bull; Expired token reuse</text>
  <text x="355" y="295" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: Short-lived JWT (24h)</text>
  <text x="355" y="308" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">+ unique token_version</text>

  <rect x="345" y="335" width="210" height="100" class="card"/>
  <text x="450" y="355" class="box-title">Threat: DoS Stream Flooding</text>
  <text x="355" y="373" class="box-desc-left">&bull; Infinite payload upload</text>
  <text x="355" y="388" class="box-desc-left">&bull; Network buffer exhaustion</text>
  <text x="355" y="405" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: Max upload limit 2GB,</text>
  <text x="355" y="418" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">OkHttp 60s read/write timeout</text>

  <!-- Zone 3: Server -->
  <rect x="610" y="75" width="240" height="375" fill="#FFFFFF" stroke="{PRIMARY_BROWN}" stroke-width="2" rx="8"/>
  <text x="730" y="100" class="box-title" style="fill:{PRIMARY_BROWN};">TRUST BOUNDARY 3: SERVER</text>

  <rect x="625" y="115" width="210" height="70" class="card"/>
  <text x="730" y="135" class="box-title">Threat: Ciphertext Tampering</text>
  <text x="635" y="153" class="box-desc-left">&bull; Bit-flipping attacks on disk</text>
  <text x="635" y="168" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: AES-GCM GHASH Tag</text>

  <rect x="625" y="195" width="210" height="70" class="card"/>
  <text x="730" y="215" class="box-title">Threat: Master Key Leak</text>
  <text x="635" y="233" class="box-desc-left">&bull; Hardcoded key in repo</text>
  <text x="635" y="248" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: Env var enforcement</text>

  <rect x="625" y="275" width="210" height="70" class="card"/>
  <text x="730" y="295" class="box-title">Threat: Brute-Force Auth</text>
  <text x="635" y="313" class="box-desc-left">&bull; Automated credential stuffing</text>
  <text x="635" y="328" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: LoginRateLimiter (5/15m)</text>

  <rect x="625" y="355" width="210" height="80" class="card"/>
  <text x="730" y="375" class="box-title">Threat: Quota Exhaustion</text>
  <text x="635" y="393" class="box-desc-left">&bull; Filling host disk to 100%</text>
  <text x="635" y="408" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">Mitigation: Atomic DB checks,</text>
  <text x="635" y="420" class="box-desc-left" style="fill:{GREEN_SECURE}; font-weight:600;">10GB ceiling per user</text>
</svg>'''
save_svg("diag_23_threat_model.svg", svg_23)

# 24. Current Implementation versus Proposed Future Architecture
svg_24 = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 480" width="900" height="480">
  {COMMON_DEFS}
  <rect width="900" height="480" fill="{BG_COLOR}"/>
  <text x="30" y="32" class="title">FIGURE 15.1 &mdash; CURRENT IMPLEMENTATION VS. PROPOSED FUTURE ARCHITECTURE</text>
  <text x="30" y="48" class="sub">Evolutionary architectural roadmap from single-node self-hosted prototype to high-availability enterprise cloud vault</text>

  <!-- Current Architecture -->
  <rect x="40" y="70" width="380" height="385" class="card-surface"/>
  <text x="230" y="95" class="box-title" style="fill:{PRIMARY_BROWN};">CURRENT ARCHITECTURE (VERIFIED v2.4.0)</text>

  <rect x="60" y="115" width="340" height="60" class="card"/>
  <text x="230" y="137" class="box-title">Client Tier</text>
  <text x="70" y="155" class="box-desc-left">&bull; Android Client (Material 3, SDK 37)</text>
  <text x="70" y="167" class="box-desc-left">&bull; Manual IP/Port connection, AppLock, Transfers</text>

  <rect x="60" y="185" width="340" height="65" class="card"/>
  <text x="230" y="207" class="box-title">Application Core</text>
  <text x="70" y="225" class="box-desc-left">&bull; Single-instance Spring Boot 3.3.4 (Tomcat 10)</text>
  <text x="70" y="237" class="box-desc-left">&bull; Envelope encryption (PBKDF2 Server KEK + UDEK)</text>

  <rect x="60" y="260" width="340" height="60" class="card"/>
  <text x="230" y="282" class="box-title">Persistence &amp; Storage</text>
  <text x="70" y="300" class="box-desc-left">&bull; Standalone MySQL 8.0 on localhost:3306</text>
  <text x="70" y="312" class="box-desc-left">&bull; Local host disk encrypted blob directory</text>

  <rect x="60" y="330" width="340" height="110" class="card-peach"/>
  <text x="230" y="352" class="box-title">Key Architectural Constraints</text>
  <text x="70" y="372" class="box-desc-left">&bull; Single Point of Failure (Host PC offline &rarr; Vault down)</text>
  <text x="70" y="387" class="box-desc-left">&bull; No seed phrase recovery (lost password &rarr; data lost)</text>
  <text x="70" y="402" class="box-desc-left">&bull; Non-resumable streaming (drop midway &rarr; restart)</text>
  <text x="70" y="417" class="box-desc-left">&bull; Flat vault list (no nested hierarchical folders)</text>

  <!-- Proposed Future Architecture -->
  <rect x="480" y="70" width="380" height="385" class="card-surface"/>
  <text x="670" y="95" class="box-title" style="fill:{GREEN_SECURE};">PROPOSED FUTURE ARCHITECTURE (ROADMAP)</text>

  <rect x="500" y="115" width="340" height="60" class="card-green"/>
  <text x="670" y="137" class="box-title">Hardened Client Tier</text>
  <text x="510" y="155" class="box-desc-left">&bull; BIP-39 12/24-word Mnemonic Seed Phrase Backup</text>
  <text x="510" y="167" class="box-desc-left">&bull; Duress / Decoy Vault PIN mode &amp; Auto-destruct</text>

  <rect x="500" y="185" width="340" height="65" class="card-green"/>
  <text x="670" y="207" class="box-title">High-Availability Backend Cluster</text>
  <text x="510" y="225" class="box-desc-left">&bull; Distributed Spring Boot microservices behind Nginx</text>
  <text x="510" y="237" class="box-desc-left">&bull; Resumable chunked transfers (TUS / Byte-range protocol)</text>

  <rect x="500" y="260" width="340" height="60" class="card-green"/>
  <text x="670" y="282" class="box-title">Distributed Object Storage</text>
  <text x="510" y="300" class="box-desc-left">&bull; MinIO / S3-compatible multi-node encrypted chunks</text>
  <text x="510" y="312" class="box-desc-left">&bull; Master-Replica MySQL cluster with automated failover</text>

  <rect x="500" y="330" width="340" height="110" class="card-blue"/>
  <text x="670" y="352" class="box-title">Advanced Security &amp; Automation</text>
  <text x="510" y="372" class="box-desc-left">&bull; Background Camera Auto-Sync (WorkManager)</text>
  <text x="510" y="387" class="box-desc-left">&bull; In-app ExoPlayer encrypted video stream pipeline</text>
  <text x="510" y="402" class="box-desc-left">&bull; Hierarchical virtual encrypted folder tree</text>
  <text x="510" y="417" class="box-desc-left">&bull; End-to-end encrypted ephemeral share links</text>

  <path d="M 420 262 L 480 262" class="line"/>
</svg>'''
save_svg("diag_24_current_vs_future_arch.svg", svg_24)

print("All 24 diagrams generated successfully!")
