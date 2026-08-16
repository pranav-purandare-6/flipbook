# Flip Book — Complete Android PDF Reader

Build a production-quality Android PDF reader with realistic page-curl animations, offline-first architecture, and premium UI.

## Existing Project State

The workspace at `c:\my_projects\Flip_book` contains a **fresh Android Studio template** with:
- AGP 8.11.2, Kotlin 2.0.21, Compose BOM 2024.09.00, Gradle 8.13
- Package: `com.pranav.flipbook`, compileSdk/targetSdk 36, minSdk 26
- Only a default `MainActivity.kt` with "Hello Android" and the default theme files

All application code will be built from scratch on top of this working project skeleton.

---

## Key Technical Decisions

### PDF Rendering
- **Primary**: Android's built-in `PdfRenderer` (API 21+) — no extra dependencies for rendering pages as bitmaps
- **Text extraction/search**: `PdfDocument` is write-only; for text search, TOC, and metadata we'll use Android's `PdfRenderer` for page bitmaps and attempt text extraction via a lightweight approach. Since `PdfRenderer` doesn't support text extraction, we'll integrate **Apache PDFBox Android Lite** (`com.tom-roush:pdfbox-android:2.0.27.0`) for text search, TOC extraction, and metadata. This is an offline, well-maintained library.

### Page Curl Effect
- Custom Compose Canvas implementation using the **cubic Bézier page-curl algorithm** — the classic approach used in iBooks-style readers. This involves:
  - Computing curl geometry from touch/drag position
  - Drawing the front page, back-of-page (mirrored), and shadow layers
  - Using `Path` clipping, gradient shadows, and perspective transforms
  - Animating with `Animatable` for spring/decay physics

### Database
- **Room** with proper entities, DAOs, relationships, and migrations

### Audio
- **Media3 ExoPlayer** for ambient sounds (bundled as raw resources) and page-turn sound effects

### Architecture
- MVVM with Repository pattern, Kotlin Coroutines/Flow, single-Activity Navigation Compose

---

## Proposed Changes

### Phase 1 — Foundation & Gradle Configuration

#### [MODIFY] [libs.versions.toml](file:///c:/my_projects/Flip_book/gradle/libs.versions.toml)
Add all required dependencies: Room, Navigation Compose, Lifecycle ViewModel Compose, DataStore, Media3, PDFBox Android, Kotlin KSP plugin, Kotlin Serialization.

#### [MODIFY] [build.gradle.kts](file:///c:/my_projects/Flip_book/build.gradle.kts)
Add KSP and Kotlin Serialization plugins.

#### [MODIFY] [app/build.gradle.kts](file:///c:/my_projects/Flip_book/app/build.gradle.kts)
Apply KSP, Room schema export, add all library dependencies.

#### [MODIFY] [AndroidManifest.xml](file:///c:/my_projects/Flip_book/app/src/main/AndroidManifest.xml)
Add configChanges for orientation handling, hardware acceleration, document picker intent filters.

---

### Phase 1B — Database Layer (~10 entities)

#### [NEW] Entity files in `data/entity/`
- `BookEntity.kt` — URI, title, author, pages, progress, cover cache path, favorites, dates
- `BookmarkEntity.kt` — bookId FK, page, title, description, date
- `NoteEntity.kt` — bookId FK, page, text, created/modified dates
- `HighlightEntity.kt` — bookId FK, page, text, color, position data
- `CollectionEntity.kt` — name, description, created date
- `BookCollectionCrossRef.kt` — many-to-many join
- `ReadingSessionEntity.kt` — bookId FK, start/end time, pages read
- `ReadingGoalEntity.kt` — type, target, period
- `AchievementEntity.kt` — type, unlocked, date, progress
- `FavoriteQuoteEntity.kt` — bookId FK, page, text, date

#### [NEW] DAO files in `data/dao/`
- `BookDao.kt`, `BookmarkDao.kt`, `NoteDao.kt`, `HighlightDao.kt`, `CollectionDao.kt`, `ReadingSessionDao.kt`, `ReadingGoalDao.kt`, `AchievementDao.kt`, `FavoriteQuoteDao.kt`

#### [NEW] `data/database/FlipBookDatabase.kt`
Room database class with all entities and type converters.

#### [NEW] Repository files in `data/repository/`
- `BookRepository.kt`, `BookmarkRepository.kt`, `NoteRepository.kt`, `HighlightRepository.kt`, `CollectionRepository.kt`, `ReadingSessionRepository.kt`, `ReadingGoalRepository.kt`, `AchievementRepository.kt`

---

### Phase 1C — Navigation & App Shell

#### [NEW] `navigation/FlipBookNavigation.kt`
Navigation graph with all destinations: Library, Reader, Bookmarks, Notes, Statistics, Collections, Settings, PDFInfo, Search, ReadingHistory, Goals, Achievements.

#### [NEW] `FlipBookApplication.kt`
Application class for database singleton initialization.

#### [MODIFY] [MainActivity.kt](file:///c:/my_projects/Flip_book/app/src/main/java/com/pranav/flipbook/MainActivity.kt)
Replace template content with navigation host, edge-to-edge setup, immersive mode support.

---

### Phase 2 — PDF Engine

#### [NEW] `pdf/renderer/PdfRendererManager.kt`
Manages `PdfRenderer` instances — open/close file descriptors, render pages to bitmaps at configurable resolution, page count, thread-safe access.

#### [NEW] `pdf/renderer/PageBitmapCache.kt`
LRU memory cache for rendered page bitmaps. Pre-renders adjacent pages. Cancellation support for rapid flipping.

#### [NEW] `pdf/thumbnails/ThumbnailManager.kt`
Renders low-res thumbnails, disk cache for covers, lazy loading support.

#### [NEW] `pdf/metadata/PdfMetadataExtractor.kt`
Uses PDFBox Android to extract title, author, subject, creator, page count, TOC/outline.

#### [NEW] `pdf/search/PdfTextSearchEngine.kt`
Uses PDFBox Android for text extraction and keyword search with match positions.

---

### Phase 3 — The Reader (Core Feature)

#### [NEW] `ui/reader/pagecurl/PageCurlRenderer.kt`
The star of the app — custom Compose Canvas page-curl implementation:
- Bézier-curve based curl geometry computation
- Front page rendering with clip path
- Back-of-page rendering (horizontally mirrored content, slightly darkened)
- Dynamic shadow gradients under the curl
- Configurable curl radius and perspective
- Touch-driven drag with spring-back animation
- Smooth forward/backward page completion animations

#### [NEW] `ui/reader/pagecurl/PageCurlState.kt`
State machine: IDLE → DRAGGING → ANIMATING_FORWARD → IDLE / ANIMATING_BACKWARD → IDLE. Prevents oscillation.

#### [NEW] `ui/reader/pagecurl/CurlGeometry.kt`
Math utilities for computing curl path, shadow positions, clip regions from touch point.

#### [NEW] `ui/reader/ReaderScreen.kt`
Main reader composable with:
- Page display with zoom/pan (transformable modifier)
- Gesture detection (swipe, edge tap, center tap, pinch)
- Auto-hiding overlay controls
- Page slider
- Immersive mode

#### [NEW] `ui/reader/ReaderOverlay.kt`
Top bar (back, title, bookmark, menu) and bottom bar (page number, slider, progress).

#### [NEW] `ui/reader/ReaderSettingsPanel.kt`
Bottom sheet for reader customization: brightness, theme, margins, transition style, speed.

#### [NEW] `ui/reader/ZoomablePageView.kt`
Handles pinch-to-zoom, double-tap zoom, pan while zoomed, with gesture priority logic.

#### [NEW] `viewmodel/ReaderViewModel.kt`
Manages current page, rendering pipeline, bookmark state, progress saving, reading session tracking.

---

### Phase 4 — Library & Home Screen

#### [NEW] `ui/library/LibraryScreen.kt`
Home screen with sections: Continue Reading, Recently Opened, Favorites, Collections, All Books. FAB for PDF import. Search bar.

#### [NEW] `ui/library/BookCard.kt`
Grid card showing cover, title, progress bar, page count.

#### [NEW] `ui/library/BookListItem.kt`
List row with cover thumbnail, title, progress, last opened.

#### [NEW] `ui/library/EmptyLibraryState.kt`
Beautiful empty state with illustration and import CTA.

#### [NEW] `ui/library/BookshelfView.kt`
Optional visual bookshelf mode with wooden shelf background.

#### [NEW] `viewmodel/LibraryViewModel.kt`
Book list management, PDF import, sorting, filtering, search, grid/list toggle.

---

### Phase 5 — Reading Data & Statistics

#### [NEW] `ui/statistics/StatisticsScreen.kt`
Dashboard with cards: books opened, completed, pages read, reading time, streaks, daily/weekly/monthly stats.

#### [NEW] `ui/statistics/ReadingCalendar.kt`
Calendar heat-map style reading tracker.

#### [NEW] `ui/statistics/GoalsScreen.kt`
Reading goals management and progress display.

#### [NEW] `ui/statistics/AchievementsScreen.kt`
Achievement cards with unlock status and progress.

#### [NEW] `viewmodel/StatisticsViewModel.kt`
Computes all reading stats from ReadingSession data.

---

### Phase 6 — Reader Tools

#### [NEW] `ui/bookmarks/BookmarksScreen.kt`
Bookmark management with jump-to-page support.

#### [NEW] `ui/notes/NotesScreen.kt`
Notes list with search, edit, delete, jump-to-page.

#### [NEW] `ui/notes/NotesHighlightsScreen.kt`
Unified tabbed screen (Notes / Highlights / Bookmarks) with book filter.

#### [NEW] `ui/reader/TableOfContentsSheet.kt`
Bottom sheet showing PDF outline/chapters with navigation.

#### [NEW] `ui/reader/ThumbnailNavigator.kt`
Horizontal/grid thumbnail strip for page navigation.

#### [NEW] `ui/reader/PageJumpDialog.kt`
"Go to page" dialog.

#### [NEW] `ui/reader/PdfSearchBar.kt`
In-reader search with result count, next/prev navigation, highlight matches.

---

### Phase 7 — Organization

#### [NEW] `ui/collections/CollectionsScreen.kt`
Collection list, create/edit/delete collection.

#### [NEW] `ui/collections/CollectionDetailScreen.kt`
Books within a collection.

#### [NEW] `viewmodel/CollectionViewModel.kt`

---

### Phase 8 — Settings & Customization

#### [NEW] `ui/settings/SettingsScreen.kt`
Full settings with sections: Reader, Appearance, Library, Audio, Storage, Data (backup/restore).

#### [MODIFY] Theme files
Custom book-inspired dark/warm color scheme. Replace default purple theme.

#### [NEW] `ui/reader/ReaderTheme.kt`
Reader-specific themes: Light, Dark, Sepia, Warm, B&W, Custom.

---

### Phase 9 — Premium Features

#### [NEW] `audio/AmbientSoundPlayer.kt`
Media3-based ambient sound player with bundled sounds (generated programmatically as white noise / simple tones since we can't bundle real audio files without assets — we'll generate procedural audio or use very small bundled WAV samples).

#### [NEW] `audio/PageTurnSoundPlayer.kt`
Short page-rustle sound effect on page turn.

#### [NEW] `ui/reader/BookOpeningAnimation.kt`
Elegant cover → open-book transition animation.

#### [NEW] `ui/library/BookshelfView.kt`
Visual bookshelf with shelf texture.

#### [NEW] `ui/statistics/FavoriteQuotesScreen.kt`
Quote collection screen.

---

### Phase 10 — Error Handling, Backup & Polish

#### [NEW] `utils/ErrorHandler.kt`
Centralized error handling for corrupt PDFs, permission loss, OOM, etc.

#### [NEW] `utils/BackupManager.kt`
Export/import app data as JSON file using DocumentProvider.

#### [NEW] `ui/components/` — Shared components
Loading states, error dialogs, empty states, confirmation dialogs.

---

## Open Questions

> [!IMPORTANT]
> **Ambient audio files**: Since the app must be fully offline with no internet permission, ambient sounds need to be bundled. I plan to generate simple procedural white noise and use very small (<50KB) synthesized WAV files for rain/fireplace effects. Real high-quality ambient audio would significantly increase APK size. Should I:
> - **(A)** Generate simple procedural audio (tiny APK, basic quality)
> - **(B)** Bundle small lo-fi ambient samples (~200KB total, decent quality)
> - **(C)** Skip ambient audio entirely and focus on the page-turn sound only

> [!IMPORTANT]  
> **PDFBox Android dependency size**: PDFBox Android adds ~4-5MB to the APK but enables text search, TOC extraction, and metadata reading. Without it, we lose in-PDF search and text highlighting entirely (PdfRenderer only renders bitmaps). Is this acceptable, or should we skip text-based features and keep the APK smaller?

> [!NOTE]
> **Compose BOM version**: The current project uses BOM `2024.09.00`. I'll update to a more recent stable BOM (e.g., `2025.01.01` or latest compatible) for better Material 3 component support, unless you prefer keeping the existing version.

---

## File Count Estimate

- **~60-70 Kotlin source files** across entity, DAO, repository, viewmodel, UI, pdf, audio, utils packages
- **~5 resource files** (strings, themes, raw audio, drawable)
- **3 Gradle config files** modified

---

## Verification Plan

### Automated Tests
- Room DAO unit tests (insert, query, update, delete for all entities)
- ViewModel unit tests for sorting, filtering, progress calculation
- `./gradlew test` for unit tests
- `./gradlew connectedAndroidTest` for instrumentation tests (if emulator available)

### Build Validation
```bash
./gradlew assembleDebug
```

### Manual Verification
Following the 37-step user test flow from the requirements:
1. Launch → Library → Import PDF → Cover generated → Book in shelf
2. Open → Render → Swipe/tap page turns → Zoom → Bookmark
3. Close → Reopen → Resume position → Progress correct
4. Search library → Favorite → Collection → Statistics
5. Theme change → Transition change → Rotation → Remove book → Stable
