# CIPHERVAULT — VAULT FILE DETAILS BOTTOM SHEET (SOURCE-ONLY AUDIT)

> **STATUS:** **NOT VERIFIED — MANUAL TESTING REQUIRED**  
> **TESTING RESTRICTION COMPLIANCE:** Zero emulator instances, zero ADB commands, zero device installations, and zero runtime tests were executed. All verifications are strictly static source inspection, Java compilation checks, and APK packaging builds.

---

## 1. Files Inspected

1. **[`FragmentVault.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/FragmentVault.java)**
   - RecyclerView adapter `VaultFilesAdapter` bind and click listeners.
   - Multi-select mode management, selection counter, and batch action logic.
   - Single file download dialog and single file delete confirmation flow.
2. **[`FileDetailsBottomSheet.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/FileDetailsBottomSheet.java)**
   - Bottom sheet dialog initialization, view binding, and listener attachments.
   - Forensic metadata fields, SHA-256 copy action, and display of encryption specs.
   - Bottom sheet expansion behavior and preview/download/delete button handling.
3. **[`bottom_sheet_file_details.xml`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/res/layout/bottom_sheet_file_details.xml)**
   - Bottom sheet layout hierarchy, scrolling container, and drag handle.
   - 210dp preview surface card (`cardBottomSheetPreview`).
   - Action buttons: Preview (`btnBottomSheetPreview`), Download (`btnBottomSheetDownload`), Delete (`btnBottomSheetDelete`), Close (`btnBottomSheetClose`).
4. **[`item_file_card.xml`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/res/layout/item_file_card.xml)**
   - Vault file card layout, preview/icon containers, text details, and action buttons.
5. **[`FragmentHome.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/FragmentHome.java)**
   - Recent files RecyclerView adapter and item click routing.
6. **[`FilesAdapter.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/FilesAdapter.java)**
   - Metadata search results adapter and bottom sheet invocation.
7. **[`MetadataSearchActivity.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/MetadataSearchActivity.java)**
   - Search result item actions, download dialogs, and deletion API callbacks.

---

## 2. Root Cause Analysis

In `FragmentVault.java` lines 656–670, `VaultFilesAdapter`'s `holder.itemView.setOnClickListener` directly created an explicit `Intent` for `FileViewerActivity`:

```java
// PREVIOUS FLAWED IMPLEMENTATION:
holder.itemView.setOnClickListener(v -> {
    if (isMultiSelectMode) {
        ...
    } else {
        Intent intent = new Intent(ctx, FileViewerActivity.class);
        intent.putExtra(FileViewerActivity.EXTRA_FILE_ID, file.getId());
        intent.putExtra(FileViewerActivity.EXTRA_FILE_NAME, file.getOriginalFilename());
        intent.putExtra(FileViewerActivity.EXTRA_CONTENT_TYPE, file.getContentType());
        intent.putExtra(FileViewerActivity.EXTRA_FILE_SIZE, file.getFileSize());
        startActivity(intent);
    }
});
```

### Consequences:
1. **Bypassed Metadata Inspection**: Tapping a file in Vault bypassed `FileDetailsBottomSheet` entirely, jumping directly into the full-screen decrypted viewer.
2. **Missing Forensic Information**: Users could not inspect the SHA-256 integrity hash, exact byte size, encryption badge, or extended forensic metadata (camera model, resolution, codec, duration, author, title) from the Vault list.
3. **Stale Adapter Position Risks**: In the previous adapter code, `position` was captured from `onBindViewHolder` into the lambda closure instead of dynamically calling `holder.getBindingAdapterPosition()`. If items were deleted or filtered, this could cause stale file references or out-of-bounds adapter positions.
4. **Missing Scrollability on Smaller Devices**: `bottom_sheet_file_details.xml` had a root `LinearLayout` without `NestedScrollView`, which risked cutting off lower action buttons on smaller screens or in landscape orientation.

---

## 3. Changes Made

### A. Vault Adapter Click Routing & Stale Reference Prevention (`FragmentVault.java`)
- **Direct Bottom Sheet Invocation**: Bound `FileDetailsBottomSheet.show(...)` to the card and all individual file detail subviews (filename, file size, encryption badge, thumbnail, file icon).
- **Dynamic Adapter Position Resolution**: All click listeners now resolve `holder.getBindingAdapterPosition()`, verifying that the index is neither `RecyclerView.NO_POSITION` nor out of bounds before querying `displayedFiles`:
  ```java
  View.OnClickListener openDetailsOrSelect = v -> {
      int currentPos = holder.getBindingAdapterPosition();
      if (currentPos == RecyclerView.NO_POSITION || currentPos >= displayedFiles.size()) return;
      StoredFile currentFile = displayedFiles.get(currentPos);
      if (isMultiSelectMode) {
          if (selectedFileIds.contains(currentFile.getId())) selectedFileIds.remove(currentFile.getId());
          else selectedFileIds.add(currentFile.getId());
          tvSelectedCount.setText(selectedFileIds.size() + " selected");
          notifyItemChanged(currentPos);
      } else {
          FileDetailsBottomSheet.show(ctx, currentFile,
                  FragmentVault.this::downloadSingleFile,
                  FragmentVault.this::confirmAndDeleteFile);
      }
  };

  holder.itemView.setOnClickListener(openDetailsOrSelect);
  holder.tvFileName.setOnClickListener(openDetailsOrSelect);
  holder.tvFileSize.setOnClickListener(openDetailsOrSelect);
  holder.tvEncryptionBadge.setOnClickListener(openDetailsOrSelect);
  holder.ivThumbnail.setOnClickListener(openDetailsOrSelect);
  holder.ivFileIcon.setOnClickListener(openDetailsOrSelect);
  ```
- **Recycled Checkbox Listener Cleanup**: In multi-select mode, `holder.cbFileSelect.setOnCheckedChangeListener(null)` is called before `setChecked(...)` to avoid stale triggers during view recycling.
- **Unified Deletion Flow**: Extracted deletion logic into `confirmAndDeleteFile(StoredFile file)`:
  - Displays `MaterialAlertDialogBuilder` confirmation dialog.
  - Calls `apiService.deleteFile(file.getId())`.
  - Shows feedback Toast and triggers `performMetadataSearchOrLoad()` to refresh the Vault list.

### B. Full-Screen Preview Action & Scrollable Layout (`FileDetailsBottomSheet.java` & `bottom_sheet_file_details.xml`)
- **Added `OnPreviewRequestedListener`**: Allows custom preview interception or defaults directly to `FileViewerActivity` with file ID, filename, content type, and file size extras.
- **Clickable Preview Surface**: Added `android:id="@+id/cardBottomSheetPreview"`, ripple feedback, and click listener on the 210dp preview card.
- **Dedicated "Preview / Decrypt File" Button**: Added `btnBottomSheetPreview` (M3 Tonal Button with `ic_lucide_eye` icon) above the Download button.
- **Expanded State by Default**: Configured `dialog.setOnShowListener` to set `behavior.setState(BottomSheetBehavior.STATE_EXPANDED)` and `behavior.setSkipCollapsed(true)`.
- **Nested Scrolling**: Wrapped root in `<androidx.core.widget.NestedScrollView android:fillViewport="true"...>` so metadata and actions remain scrollable on smaller screens or landscape mode.

### C. Consistency Across Home and Search (`FragmentHome.java` & `FilesAdapter.java`)
- **Home Recent Uploads**: Updated `handleFileClick` in `FragmentHome.java` to route through `FileDetailsBottomSheet.show(...)` with `downloadSingleFile` and `handleDeleteClick`.
- **Adapter Safety**: Updated `RecentFilesAdapter` (`FragmentHome.java`) and `FilesAdapter` (`FilesAdapter.java`) to use `holder.getBindingAdapterPosition()` checks, eliminating stale position risks across the entire app.

---

## 4. Compilation & Packaging Results

Executed via Gradle without touching ADB, emulators, or connected devices:

### 1. Java Compilation (`compileDebugJavaWithJavac`)
```text
> Task :app:compileDebugJavaWithJavac
BUILD SUCCESSFUL in 11s
7 actionable tasks: 1 executed, 6 up-to-date
Configuration cache entry reused.
```
- **Result:** **0 compile errors, 0 warnings converted to errors**.

### 2. Full Debug Packaging (`assembleDebug`)
```text
> Task :app:packageDebug
> Task :app:assembleDebug
BUILD SUCCESSFUL in 15s
35 actionable tasks: 3 executed, 32 up-to-date
Configuration cache entry reused.
```
- **Result:** **0 resource errors, 0 manifest merging conflicts, Debug APK successfully generated**.

---

## 5. Manual Testing Checklist (For Physical Device Testing)

> **NOTICE:** **NOT VERIFIED — MANUAL TESTING REQUIRED**  
> Please run the following verification steps on your physical Android device:

- [ ] **1. Vault Item Tap**:
  - Open CipherVault and navigate to the **Vault** tab.
  - Tap on the **filename** of any file -> Verify `FileDetailsBottomSheet` opens.
  - Tap on the **file size / encryption badge** -> Verify `FileDetailsBottomSheet` opens.
  - Tap on the **thumbnail / file icon** -> Verify `FileDetailsBottomSheet` opens.
  - Tap on the **card background** (outside action buttons) -> Verify `FileDetailsBottomSheet` opens.

- [ ] **2. Bottom Sheet Presentation & Scrolling**:
  - Verify the bottom sheet slides up in its **fully expanded state** immediately.
  - On a smaller screen or in landscape, verify the sheet smoothly **scrolls** to reveal all forensic metadata rows and bottom action buttons.

- [ ] **3. Preview / Decrypt File**:
  - In the bottom sheet, tap either the **210dp preview image** or the **"Preview / Decrypt File"** button.
  - Verify that `FileViewerActivity` opens and decrypts the file on-the-fly.
  - Tap back -> Verify return to the Vault screen.

- [ ] **4. SHA-256 Checksum Copy**:
  - In the bottom sheet, tap the **SHA-256 hash card**.
  - Verify Toast: `"SHA-256 copied to clipboard"`.
  - Paste into any text field to confirm valid 64-character hex string.

- [ ] **5. Download Action**:
  - In the bottom sheet, tap **"Download"**.
  - Verify download dialog appears with decrypt option.
  - Tap "Download" -> Verify download starts via `TransferManager` foreground notification.

- [ ] **6. Delete Action**:
  - In the bottom sheet, tap **"Delete"**.
  - Verify confirmation alert: *"Are you sure you want to delete ... from your encrypted vault?"*.
  - Tap "Delete" -> Verify file is deleted, bottom sheet closes, Toast displays `"File deleted"`, and the Vault list automatically reloads.

- [ ] **7. Multi-Select Mode Preservation**:
  - Long-press any file item in the Vault list to enter multi-select mode.
  - Tap another file item -> Verify the checkbox toggles and selected count increments/decrements.
  - Verify `FileDetailsBottomSheet` **does NOT open** while in multi-select mode.
  - Exit multi-select mode -> Verify tapping files opens `FileDetailsBottomSheet` again normally.

- [ ] **8. Home & Metadata Search Consistency**:
  - Navigate to **Home** and tap any file under "Recent Uploads" -> Verify `FileDetailsBottomSheet` opens.
  - Navigate to **Search**, search for a file, and tap the search result -> Verify `FileDetailsBottomSheet` opens.
