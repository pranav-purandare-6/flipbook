# Flip Book — Development Tasks

## Phase 1 — Foundation
- [/] Update Gradle configuration (libs.versions.toml, build.gradle.kts files)
- [ ] Create Room entities (10 entities)
- [ ] Create DAOs (9 DAOs)
- [ ] Create FlipBookDatabase
- [ ] Create Repositories (9 repositories)
- [ ] Create Navigation graph
- [ ] Update Theme (book-inspired colors)
- [ ] Create FlipBookApplication class
- [ ] Update AndroidManifest.xml
- [ ] Update MainActivity

## Phase 2 — PDF Engine
- [ ] PdfRendererManager
- [ ] PageBitmapCache (LRU)
- [ ] ThumbnailManager
- [ ] PdfMetadataExtractor (PDFBox)
- [ ] PdfTextSearchEngine (PDFBox)

## Phase 3 — Reader (Core Feature)
- [ ] PageCurlState (state machine)
- [ ] CurlGeometry (math)
- [ ] PageCurlRenderer (Canvas drawing)
- [ ] ZoomablePageView
- [ ] ReaderScreen
- [ ] ReaderOverlay (controls)
- [ ] ReaderSettingsPanel
- [ ] ReaderViewModel

## Phase 4 — Library & Home
- [ ] LibraryScreen
- [ ] BookCard / BookListItem
- [ ] EmptyLibraryState
- [ ] BookshelfView
- [ ] LibraryViewModel

## Phase 5 — Reading Data & Statistics
- [ ] StatisticsScreen
- [ ] ReadingCalendar
- [ ] GoalsScreen
- [ ] AchievementsScreen
- [ ] StatisticsViewModel

## Phase 6 — Reader Tools
- [ ] BookmarksScreen
- [ ] NotesScreen / NotesHighlightsScreen
- [ ] TableOfContentsSheet
- [ ] ThumbnailNavigator
- [ ] PageJumpDialog
- [ ] PdfSearchBar
- [ ] NotesViewModel

## Phase 7 — Organization
- [ ] CollectionsScreen / CollectionDetailScreen
- [ ] CollectionViewModel
- [ ] PdfInfoScreen

## Phase 8 — Settings & Customization
- [ ] SettingsScreen (full)
- [ ] SettingsViewModel
- [ ] Reader themes (Sepia, Warm, Dark, B&W)
- [ ] BackupManager

## Phase 9 — Premium Features
- [ ] AmbientSoundPlayer
- [ ] PageTurnSoundPlayer
- [ ] BookOpeningAnimation
- [ ] Shared UI components (LoadingState, EmptyState, Dialogs)
- [ ] ErrorHandler / Extensions

## Build & QA
- [ ] Gradle sync
- [ ] Debug build
- [ ] Fix compilation errors
- [ ] Verify 37-step test flow
