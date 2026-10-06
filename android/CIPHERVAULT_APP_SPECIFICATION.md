# CipherVault — Complete Master Android App Specification

**MASTER IMPLEMENTATION INSTRUCTION**
We are creating a completely new Android application for CipherVault. The old Android project is REFERENCE ONLY. Do not redesign the old application. Do not modify the old Android project unless explicitly requested. Do not blindly copy its architecture. Do not carry dead code, duplicate code, obsolete navigation, obsolete UI, or obsolete assumptions into the new application. 
The new Android application must be built as one coherent, professionally designed CipherVault product. The backend remains the source of truth for server-side functionality and real data.

The governing design principle is: **CONSISTENCY OVER CREATIVITY**
Do not design each screen independently. Design one CipherVault design system and apply it everywhere.

---

## 1. PRODUCT IDENTITY
CipherVault is a private cloud storage application, Android client, connected to the user's own CipherVault server. It features an encrypted server-side storage system, file management, secure preview, transfer management, storage management, duplicate detection, metadata-aware search, and analytics.
The application should feel like a combination of Samsung One UI, My Files, Gallery, Device Care, and Digital Wellbeing with CipherVault's own visual identity.
**Do NOT make it look like:** Generic Material Design, generic SaaS dashboard, generic cloud storage clone, blue/purple enterprise software, glassmorphism UI, or a technical cybersecurity dashboard.

## 2. DESIGN LANGUAGE
The entire application follows Samsung One UI as the structural/design language while retaining a unique CipherVault identity.
The design must prioritize calmness, spaciousness, premium appearance, professionalism, security, readability, one-handed usability, reachability, clear hierarchy, and minimal cognitive load.
**Viewing-area/interaction-area model:** The upper area is for page title, context, and visual hierarchy. The lower area is for reachable interactive buttons and controls.

## 3. VISUAL IDENTITY
**Colors:** Warm neutral base, beige/clay/brown accent. Off-white/light-gray light theme, deep-gray/near-black dark theme. Semantic colors (success/warning/error) should remain restrained.
**Avoid:** Neon, strong gradients, excessive saturated colors, blue/purple-heavy Material styling.

## 4. TYPOGRAPHY
- **Source Serif 4:** Major page titles, large onboarding headings, important display headings.
- **Nunito Sans:** Body text, buttons, file names, metadata, settings, navigation, forms, labels, secondary information.
- **JetBrains Mono (Selectively):** SHA-256 hashes, technical identifiers. Do not use monospace typography everywhere.

## 5. SPACING
16–24dp page margins, 16–24dp group spacing, 16dp+ internal padding. Generous vertical separation and large title breathing room. Whitespace is an important part of the identity.

## 6. CORNERS AND SURFACES
Major surfaces generally use 24–32dp corner radius. Use rounded grouped surfaces, continuous surfaces, soft containers, and subtle contrast.
**Avoid:** Sharp cards, excessive individual cards, random corner radii, random pill components.

## 7. SHADOWS
Use minimal or no shadows. Depth should primarily come from surface contrast, spacing, grouping, borders/dividers, and tonal differences.

## 8. ICONOGRAPHY
Use simple, monochrome, consistent, recognizable, functional icons. Do not mix unrelated icon families. Every icon must have a purpose.

## 9. TOUCH TARGETS
Every important interactive control must have at least 48dp × 48dp touch area.

## 10. ACCESSIBILITY
Support scalable text, large font sizes, screen readers, meaningful content descriptions, sufficient contrast, accessible controls, and proper focus order. No information communicated by color alone.

## 11. THEMING
Support Light, Dark, System default. Light and dark themes must be the same CipherVault design system. Dark theme should be deep gray, comfortable, low glare.

## 12. ANIMATION PHILOSOPHY
Animations must be smooth, subtle, native-feeling, purposeful, short.
- Page transitions: 200–250ms
- Theme transition: 250–350ms
- Chart animation: 500–700ms
- Initial storage animation: 600–800ms
- Thumbnail fade: 150–200ms
- Bottom sheet movement: 250–300ms
- Dialog transitions: 180–220ms
**Avoid:** Bouncing, excessive scaling, decorative spinning, continuous animations, parallax everywhere, flashing, excessive Lottie, fake progress animation.

## 13. ABSOLUTELY AVOID
Gradients, Neon colors, Blue/purple-heavy Material appearance, Excessive cards, Random pills everywhere, Excessive shadows, Glassmorphism, Cluttered dashboards, Tiny text, Dense tables, Fake statistics, Decorative UI without function, Generic SaaS/cybersecurity dashboards, Unnecessary FABs.

## 14. MAIN NAVIGATION
Permanent bottom navigation: small, centered floating pill.
**Primary destinations:** Home, Files, Transfers, Settings.
Upload is NOT a permanent navigation destination (starts from Home/Files).

## 15. PRIMARY VS SECONDARY SCREENS
- **Primary:** Large title, main content, floating bottom navigation.
- **Secondary:** `< Page Title`, optional subtitle, main content, actions. No duplicate navigation controls.

## 16. COMPLETE NAVIGATION
Splash → Welcome / Onboarding → Connect to CipherVault → Sign In (Create Account / Home).
- **Home:** Dashboard, Storage, Analytics, Recent Files, Upload, Activity, Vault Health.
- **Files:** Search, Filters, Sort, Collections, File Preview, File Details, Upload, Storage, Duplicate Files, Large Files, Recycle Bin (if supported).
- **Transfers:** Transfer Details.
- **Settings:** Profile, Storage, Appearance, Connection, Security, Vault Health, Activity & History, About.

## 17. GLOBAL SCREEN STATES
Every data-driven screen must support: Loading, Content/Empty, Refresh, Error. Where applicable: Processing, Success, Failure, Retry. No fake content while loading.

## 18-22. ONBOARDING & AUTHENTICATION
- **Splash:** Auto-routing based on configuration/session.
- **Onboarding:** Locked 5-page flow.
- **Connect:** Real setup screen. Validates URL format and connectivity. Re-uses saved address.
- **Sign In / Create:** Meaningful errors. No "Something went wrong" generic messages. Validates inputs before sending to server.

## 23. HOME / DASHBOARD
Samsung Device Care + Digital Wellbeing inspired. Greeting, authoritative storage usage, Vault Health, actual analytics (Charts), Recent files, Quick actions.

## 24-27. MY FILES & SORT/SELECT
Grid/List views. Categories. Actual collections (not fake AI). Sorting via bottom sheet (only if backend supports). Long-press multi-selection with contextual actions.

## 28-30. SEARCH & FILTERS
Search supports actual backend metadata. Clean filter bottom sheet (Time, Type, Size, Encryption). Only expose filters/sorts the backend satisfies.

## 31-32. FILE PREVIEW & DETAILS
- **Preview Workflow:** Encrypted server file → Temp local decryption → Secure preview → Cleanup temp data.
- **Details:** Bottom sheet (Filename, bytes, MIME, timestamp, SHA-256, Encryption, Category).

## 33. UPLOAD WORKFLOW
Select → SHA-256 Dup Check → Duplicate Review → Metadata → Quota Check → Encryption → Upload.
Requires actual cancellation, speed, ETA, and proper failure/retry per file. Updates storage counts on success.

## 34-39. TRANSFERS & LIFECYCLE
Permanent transfer manager (Active/Completed/Failed).
**Background Transfers:** Must survive app lifecycle constraints using modern Android architecture. Support persistence, recovery, cancellation, retry, cleanup of partials.
**Notifications:** Real state progress.
**Download:** Verification post-decryption. No silently saving corrupted files.

## 40-44. STORAGE MANAGEMENT & DUPLICATES
Device Care style categories. Manage Duplicate Files (grouped by actual SHA-256 match, not names). Manage Large Files.

## 45-47. ANALYTICS, ACTIVITY & TRASH
- **Analytics:** Actual server data charts. No fake metrics.
- **Activity/History:** Display real operations (Upload, Download, Delete, Login) if backend supports it.
- **Recycle Bin:** Only implement if backend provides recoverable deletion.

## 48-51. SETTINGS, PROFILE, APPEARANCE & SECURITY
One UI inspired settings rows. Profile handles destructive actions carefully. Appearance applies instantly. Security Status only shows what is actually active. No exposing JWT/keys.

## 52-60. SESSION, HEALTH & ERROR MODELS
Handle session expiry elegantly. Real Vault Health checks. Explicit destruct-data vs delete-account. Real operational states (Idle → Processing → Success/Failure). Distinguish network errors (No internet, unreachable, timeout, auth failure).

## 61-63. REFRESH & DATA CONSISTENCY
Refresh requests current data. The app feels like one system: deleting a file updates files, storage, analytics, and activity everywhere. The backend is the single source of truth.

## 64-66. ARCHITECTURE & STATE MANAGEMENT
Modern architecture: UI → ViewModel/State → Repository → Remote. Separate UI from business logic. State must survive rotation/recreation. Android back button logic must correctly exit local states before closing app.

## 67-69. STORAGE & SECURITY LIFECYCLE
Respect Android Scoped Storage. Temp decrypted files must be deleted after preview/failure. Never log passwords, tokens, or plaintext.

## 70-71. UPLOAD/DOWNLOAD SECURITY PIPELINE
Client pipeline must mirror the authoritative backend contract for hashing, encrypting, decrypting, and verifying.

## 72-77. QUOTA, PERFORMANCE, STATES
Check quota before expensive processing. Handle large files via streaming without blocking UI. Avoid loading thousands of thumbnails at once. Every screen needs meaningful empty and loading (skeleton) states.

## 78-83. UX REFERENCES & REACHABILITY
- **Samsung One UI:** Structure, Settings, Navigation.
- **Samsung My Files:** Browsing, Trash.
- **Samsung Gallery:** Media grids.
- **Samsung Device Care:** Storage, Health.
- **Samsung Digital Wellbeing:** Analytics.
- **Google Drive (Behavior only):** Uploads, transfers, offline behavior.
Use Bottom Sheets for reachability. Adapt to large screens/tablets without just stretching phone UI. Use targeted notifications.

## 84-89. CAPABILITY GATING & BACKEND TRUTH
Connection experience remembers settings. Never expose secrets. Only show UI for features the backend actually supports (e.g. no Recycle Bin if backend lacks it, no rename if backend lacks it). Do not fabricate values.

## 90-95. RESPONSIBILITIES & CLEANUP
Android handles UI, selection, local crypto processing, and presentation. Backend handles auth, storage, global deduplication, and truth. Clean up temp files always.

## 96-100. COMPONENT SYSTEM
Reuse XML components: `FileGridItem`, `FileListItem`, `SettingItem`, `TransferItem`, `StorageSummary`, `FileDetailsSheet`, `DeleteFileDialog`, etc.

## 101. XML NAMING
Use logical prefixes: `screen_`, `item_`, `sheet_`, `dialog_`.

## 102-106. FINAL ARCHITECTURE & INVENTORY
29 visual concepts mapping to Main App sections (Home, Files, Transfers, Settings). Every screen must answer: What am I looking at? What can I do here? What happens if it fails?

## 107-111. IMPLEMENTATION ORDER
A. Foundation → B. Onboarding/Auth → C. Main Shell → D. Files → E. Upload/Transfer → F. Storage/Analytics → G. Settings/Security → H. Hardening.

## 112-116. GAP AUDIT & GOLDEN RULE
Before coding, audit the backend API to identify gaps/mismatches. Test every workflow E2E.
**CONSISTENCY OVER CREATIVITY.** The application must have one identity, one design language, one interaction philosophy, one data model, one source of truth.
