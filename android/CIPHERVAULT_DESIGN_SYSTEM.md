# CIPHERVAULT MASTER UI DESIGN SYSTEM

This is the permanent visual and interaction design system for the CipherVault Android application.

The purpose is to maintain ONE consistent visual identity across the entire application. Every future screen, component, dialog, bottom sheet, animation, list, chart, file view, settings page, and navigation surface must feel like it belongs to the SAME CipherVault application.

Do NOT design each screen independently. Before implementing any new UI, refer back to these rules.

==================================================
## 1. DESIGN IDENTITY
==================================================
CipherVault is inspired by Samsung One UI.

The target feeling is:
- Premium
- Calm
- Spacious
- Minimal
- Modern
- Professional
- Secure
- Comfortable for one-handed use
- Clean and highly readable

The application should feel like a polished system application rather than a generic Android template or SaaS dashboard. Use Samsung One UI as the design LANGUAGE. Do NOT copy Samsung branding, logos, proprietary artwork, or exact screens. CipherVault must retain its own identity.

==================================================
## 2. EXISTING ONBOARDING IS LOCKED
==================================================
The current approved CipherVault onboarding is the visual anchor.

IMPORTANT:
- DO NOT redesign it.
- DO NOT simplify it.
- DO NOT replace it.
- DO NOT reinterpret it.
- DO NOT change its visual language unless explicitly requested by the user.

All future screens must look like they belong to the same application as this approved onboarding. The onboarding establishes CipherVault's identity. Future UI should extend that identity rather than replace it.

==================================================
## 3. ONE UI SCREEN STRUCTURE
==================================================
Use the One UI concept of:

VIEWING AREA + INTERACTION AREA

Important information and titles should have comfortable space toward the upper portion of the screen. Interactive controls should remain comfortable to reach toward the lower portion where appropriate. Do not compress everything toward the top. Do not create dense desktop-style interfaces.

This principle applies especially to:
- Home
- Files
- Transfers
- Settings
- Storage
- Analytics
- Activity
- Profile
- Security
- Health
- Account pages
- Detail screens

==================================================
## 4. TYPOGRAPHY
==================================================
CipherVault uses a distinctive two-font (optionally three-font) typography system. Typography must create hierarchy through font family, size, weight, and spacing without relying on excessive colors, uppercase text, gradients, or decorative effects.

The combination should feel like:
- **Source Serif 4** = CipherVault's personality and identity
- **Nunito Sans** = CipherVault's functional interface

The result should remain premium, calm, spacious, modern, secure, professional, and highly readable. Do not make the application look like an editorial app; the serif is for hierarchy, while Nunito Sans dominates the functional UI.

**1. Headings / Display Typography — Source Serif 4**
- App/page titles
- Major section headings where appropriate
- Large empty-state headings
- Important display text
- Use primarily Regular/Medium/Bold only where the font supports it
- Purpose: Gives CipherVault a distinctive, premium, elegant identity (similar to modern Times New Roman) but polished for mobile.

**2. UI / Body Typography — Nunito Sans**
- Body text, file names, and descriptions
- Secondary metadata
- Buttons and navigation labels
- Settings rows, form labels, input text
- Status text, dialog text, bottom-sheet content
- General application UI
- Purpose: Provides a rounded, friendly, highly readable character for the functional interface.

**3. Technical Typography — JetBrains Mono (Selective Use)**
- For highly technical values such as SHA-256 hashes, cryptographic identifiers, or debug-style values.
- Do NOT use JetBrains Mono for normal UI text.

PAGE TITLES:
- Large, clear, spacious, usually left-aligned, comfortable distance from the top (Source Serif 4).

SECTION HEADINGS:
- Smaller than page titles, used to organize grouped content (Source Serif 4 or Nunito Sans Semibold).

PRIMARY/SECONDARY TEXT:
- Highly readable (Nunito Sans).
- Secondary text is smaller and softer gray/neutral tone.

Do NOT use excessive uppercase labels. Do NOT use typography that feels like a web dashboard.

==================================================
## 5. COLOR SYSTEM
==================================================
PRIMARY CIPHERVAULT IDENTITY:
Use warm neutral / beige / clay / brown tones as the primary brand language. Avoid making CipherVault look blue or purple.

LIGHT THEME:
- Warm off-white / very light neutral background
- Clean white or very lightly tinted grouped surfaces
- Dark readable text
- Warm brown/beige accent

DARK THEME:
- Deep gray/near-black background
- Slightly lighter surfaces
- Carefully adjusted warm brand accent
- Avoid simply inverting the light theme

SEMANTIC COLORS:
Use semantic colors only when necessary.
Examples:
- Green → healthy/success/completed
- Red → error/destructive
- Amber → warning
- Other restrained colors → only when they improve comprehension

Do not turn the application into a multicolor interface. Warm CipherVault tones should remain dominant.

==================================================
## 6. SPACING SYSTEM
==================================================
The interface must breathe. Typical principles:
- Horizontal page margins: approximately 16–24dp
- Group spacing: approximately 16–24dp
- Internal surface padding: approximately 16dp or more
- Comfortable spacing around large titles
- Comfortable spacing between sections
- Adequate spacing between primary and secondary information

Never make the UI unnecessarily dense. Whitespace is part of the CipherVault visual identity.

==================================================
## 7. SURFACE / GROUPED CARD SYSTEM
==================================================
Use grouped surfaces rather than many independent floating cards. Major surfaces should generally have:
- Approximately 24–32dp corner radius
- Minimal or no shadow
- Subtle background contrast
- Generous internal padding

Depth should primarily come from SURFACE COLOR CONTRAST rather than HEAVY SHADOWS. Related information should be grouped into one continuous surface (e.g. Settings).

==================================================
## 8. DIVIDERS
==================================================
Dividers should be:
- Thin
- Very subtle
- Low contrast
- Usually inset
- Aligned with content rather than unnecessarily spanning the entire surface

Do not use heavy borders. Do not visually divide every small element.

==================================================
## 9. ICON SYSTEM
==================================================
# OFFICIAL ICON SYSTEM: TABLER ICONS
Use https://tabler.io/icons as the single official icon library for the entire CipherVault Android application.

Standards:
- Tabler Icons
- Outline style as the default
- 24x24 icon grid
- 2px stroke as the default
- Monochrome icons
- CipherVault theme-based tinting
- Stored as local Android VectorDrawable resources in `res/drawable/`

Do NOT mix Tabler with Material Icons, Lucide, Font Awesome, Phosphor, or random SVGs. The application must have ONE consistent icon language.

Sizing:
- 24dp: Standard list/settings/action icons
- 22-24dp: Bottom navigation
- 20-24dp: Compact secondary actions
- 28-32dp: Larger category/empty-state icons
- Never use a 48dp visible icon merely to satisfy a touch target (keep the icon small while providing a 48dp padding/touch area).

Light/Dark:
- Adapt automatically to themes. Dark neutral/warm active in Light theme; light neutral/warm active in Dark theme. No separate designs for dark mode.

Consistency:
- Always choose the appropriate Tabler icon and apply it everywhere (e.g., Home -> house, Files -> folder, Upload -> upload, Search -> search, Delete -> trash, Settings -> settings, Security -> shield-check).

Do not manually redraw or modify the geometry. Preserve the original Tabler visual language.

==================================================
## 10. BUTTON SYSTEM
==================================================
Primary buttons should be:
- Large enough for comfortable touch
- Rounded
- Brand-colored where appropriate
- Clear
- Simple
- Strongly identifiable

Secondary actions should be visually quieter. Do not create excessive buttons. Only the important action should visually dominate.

==================================================
## 11. SETTINGS ROW SYSTEM
==================================================
Settings rows are a major One UI pattern. A typical row can contain:
[ICON] Primary title + Secondary description >
or:
[ICON] Setting name [SWITCH]

The entire row should be interactive. Minimum interactive target: 48dp or larger. Rows should have comfortable vertical padding, leading icon, and optional trailing control. Applies to Settings, Appearance, Security, Connection, Profile, etc.

==================================================
## 12. BOTTOM NAVIGATION
==================================================
Main application navigation: HOME, FILES, TRANSFERS, SETTINGS.
Keep it: Simple, Clean, Stable, Uncluttered. Do not add unnecessary permanent navigation destinations.

==================================================
## 13. TOP APP BAR
==================================================
Use minimalist top navigation. Typical structure: `< Back Title ⋮`
Avoid heavy colored app bars. The content itself should establish the visual hierarchy.

==================================================
## 14. BOTTOM SHEETS
==================================================
Bottom sheets should follow the same design system. Use large top corner radius, clean background, generous internal spacing, clear title, simple rows/actions. Use for filters, sorting, file details, contextual actions.

==================================================
## 15. DIALOGS
==================================================
Dialogs should be Simple, Focused, Calm, Spacious, Clearly hierarchical. Use for confirmation, destructive actions, important warnings.

==================================================
## 16. FILE UI
==================================================
CipherVault's file interface should combine Samsung My Files and Gallery-inspired principles.
LIST MODE: [FILE ICON/THUMBNAIL] Filename | Size · Date · Type
GRID MODE: Large thumbnail, Rounded corners, Minimal metadata.
Use real thumbnails for media. Use functional icons for categories.

==================================================
## 17. MEDIA / GALLERY UI
==================================================
Media collections should feel Gallery-inspired. Use large rounded thumbnails, clean grids, minimal metadata, clear hierarchy. Avoid excessive labels over images.

==================================================
## 18. FILE DETAILS
==================================================
File details should use a clean grouped-information layout. Never expose keys, JWTs, or secrets. Should feel like Samsung My Files / Gallery information screens.

==================================================
## 19. SEARCH
==================================================
Search should be Simple, Spacious, Clearly identifiable. Search results should use the same file list/grid system as My Files.

==================================================
## 20. FILTERS AND SORTING
==================================================
Filters should use a clean bottom-sheet or grouped-selection design. Do not make filtering visually complicated.

==================================================
## 21. STORAGE UI
==================================================
Storage screens should feel inspired by Samsung Device Care. Use large storage summary, used/available values, clean progress visualization. Prefer large numbers + simple progress over dense statistics.

==================================================
## 22. CHARTS
==================================================
Charts should feel restrained and Samsung Health-like. Use simple bars, rounded bar tops, clean ring charts, rounded progress bars. Minimal labels and grid lines.

==================================================
## 23. HOME / DASHBOARD
==================================================
Home should NOT look like a generic SaaS dashboard. It should feel like a Samsung system application. Use grouped sections.

==================================================
## 24. TRANSFERS
==================================================
Transfers should prioritize live status (Progress, ETA, Speed). Keep it operational but calm.

==================================================
## 25. ACTIVITY / HISTORY
==================================================
Activity is historical. Use grouped chronological sections. Do NOT show meaningless UI interactions.

==================================================
## 26. SETTINGS
==================================================
Settings should strongly follow One UI settings patterns with leading icons, titles, and trailing controls.

==================================================
## 27. PROFILE / ACCOUNT
==================================================
Profile and Account Management should feel like system settings. Destructive actions must be clearly separated.

==================================================
## 28. SECURITY
==================================================
Security screens should feel trustworthy and restrained. Never expose secrets. Do not use exaggerated military-grade graphics.

==================================================
## 29. VAULT HEALTH
==================================================
Vault Health should feel similar to a restrained Device Care status page.

==================================================
## 30. ANIMATION SYSTEM
==================================================
Animations must be Smooth, Native, Subtle, Purposeful, Short, Consistent. Avoid bouncy or flashing transitions. Use simple fades, slides, scales.

==================================================
## 31. LIGHT / DARK / SYSTEM
==================================================
Every major screen must support LIGHT, DARK, SYSTEM. Dark mode needs its own carefully chosen surface hierarchy (not just inverted).

==================================================
## 32. ACCESSIBILITY
==================================================
Every interactive element must have an adequate touch target (Minimum: 48dp).

==================================================
## 33. RESPONSIVE DESIGN
==================================================
The UI must work on small, normal, and large phones. Use dp, sp, ConstraintLayout.

==================================================
## 34. WHAT CIPHERVAULT MUST NOT BECOME
==================================================
NEVER turn CipherVault into a generic Material demo, blue/purple template, SaaS dashboard, or over-animated application. Avoid excessive pills, shadows, gradients, clutter.

==================================================
## 35-39. RULES AND MAPPINGS
==================================================
CONSISTENCY OVER CREATIVITY.
If a new screen looks individually attractive but does not look like CipherVault, it is WRONG. Every new screen must answer: "Does this look like it belongs to the same CipherVault application?" The answer must be YES.

Before implementing any future screen:
1. Refer to this design system.
2. Refer to the visual references (One UI).
3. Refer to the existing approved CipherVault onboarding.
4. Reuse established components.
5. Maintain the same spacing, typography, colors, radii and interaction behavior.

TREAT THIS ENTIRE DOCUMENT AS A PERMANENT PROJECT-LEVEL DESIGN REFERENCE FOR CIPHERVAULT.
