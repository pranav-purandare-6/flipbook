package com.pranav.flipbook.data.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.database.FlipBookDatabase
import com.pranav.flipbook.data.entity.*
import com.pranav.flipbook.viewmodel.SettingsKeys
import com.pranav.flipbook.viewmodel.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(private val context: Context) {

    private val db: FlipBookDatabase by lazy {
        (context.applicationContext as FlipBookApplication).database
    }

    suspend fun createBackup(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", 2)
            root.put("exportDate", System.currentTimeMillis())
            root.put("app", "Flip Book")

            root.put("books", booksToJson(db.bookDao().getAllBooksSnapshot()))
            root.put("bookmarks", bookmarksToJson(db.bookmarkDao().getAllBookmarksSnapshot()))
            root.put("notes", notesToJson(db.noteDao().getAllNotesSnapshot()))
            root.put("highlights", highlightsToJson(db.highlightDao().getAllHighlightsSnapshot()))
            root.put("collections", collectionsToJson(db.collectionDao().getAllCollectionsSnapshot()))
            root.put("book_collections", crossRefsToJson(db.collectionDao().getAllBookCollectionCrossRefs()))
            root.put("reading_lists", readingListsToJson(db.readingListDao().getAllListsSnapshot()))
            root.put("reading_list_books", readingListRefsToJson(db.readingListDao().getAllListBookCrossRefs()))
            root.put("reading_sessions", sessionsToJson(db.readingSessionDao().getAllSessionsSnapshot()))
            root.put("reading_goals", goalsToJson(db.readingGoalDao().getAllGoalsSnapshot()))
            root.put("achievements", achievementsToJson(db.achievementDao().getAllAchievementsSnapshot()))
            root.put("favorite_quotes", quotesToJson(db.favoriteQuoteDao().getAllQuotesSnapshot()))
            root.put("settings", settingsToJson())

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return@withContext false

            val root = JSONObject(content)
            if (root.optInt("version", 0) < 1) return@withContext false

            // Restore in dependency order
            restoreBooks(root.optJSONArray("books"))
            restoreBookmarks(root.optJSONArray("bookmarks"))
            restoreNotes(root.optJSONArray("notes"))
            restoreHighlights(root.optJSONArray("highlights"))
            restoreCollections(root.optJSONArray("collections"))
            restoreBookCollections(root.optJSONArray("book_collections"))
            restoreReadingLists(root.optJSONArray("reading_lists"))
            restoreReadingListBooks(root.optJSONArray("reading_list_books"))
            restoreSessions(root.optJSONArray("reading_sessions"))
            restoreGoals(root.optJSONArray("reading_goals"))
            restoreAchievements(root.optJSONArray("achievements"))
            restoreQuotes(root.optJSONArray("favorite_quotes"))
            restoreSettings(root.optJSONObject("settings"))

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun booksToJson(books: List<BookEntity>) = JSONArray().apply {
        books.forEach { b ->
            put(JSONObject().apply {
                put("id", b.id)
                put("uri", b.uri)
                put("title", b.title)
                put("customTitle", b.customTitle)
                put("author", b.author)
                put("fileName", b.fileName)
                put("fileSize", b.fileSize)
                put("pageCount", b.pageCount)
                put("currentPage", b.currentPage)
                put("readingProgress", b.readingProgress.toDouble())
                put("coverPath", b.coverPath)
                put("isFavorite", b.isFavorite)
                put("dateAdded", b.dateAdded)
                put("lastOpened", b.lastOpened)
                put("totalReadingTime", b.totalReadingTime)
                put("totalPagesRead", b.totalPagesRead)
                put("isCompleted", b.isCompleted)
            })
        }
    }

    private suspend fun restoreBooks(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.bookDao().insert(
                BookEntity(
                    id = o.optLong("id", 0),
                    uri = o.getString("uri"),
                    title = o.getString("title"),
                    customTitle = o.optString("customTitle").ifBlank { null },
                    author = o.optString("author").ifBlank { null },
                    fileName = o.getString("fileName"),
                    fileSize = o.optLong("fileSize"),
                    pageCount = o.optInt("pageCount"),
                    currentPage = o.optInt("currentPage"),
                    readingProgress = o.optDouble("readingProgress").toFloat(),
                    coverPath = o.optString("coverPath").ifBlank { null },
                    isFavorite = o.optBoolean("isFavorite"),
                    dateAdded = o.optLong("dateAdded"),
                    lastOpened = if (o.has("lastOpened") && !o.isNull("lastOpened")) o.getLong("lastOpened") else null,
                    totalReadingTime = o.optLong("totalReadingTime"),
                    totalPagesRead = o.optInt("totalPagesRead"),
                    isCompleted = o.optBoolean("isCompleted")
                )
            )
        }
    }

    private fun bookmarksToJson(items: List<BookmarkEntity>) = JSONArray().apply {
        items.forEach { b ->
            put(JSONObject().apply {
                put("id", b.id)
                put("bookId", b.bookId)
                put("page", b.page)
                put("title", b.title)
                put("description", b.description)
                put("createdDate", b.createdDate)
            })
        }
    }

    private suspend fun restoreBookmarks(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.bookmarkDao().insert(
                BookmarkEntity(
                    id = o.optLong("id", 0),
                    bookId = o.getLong("bookId"),
                    page = o.getInt("page"),
                    title = o.optString("title").ifBlank { null },
                    description = o.optString("description").ifBlank { null },
                    createdDate = o.optLong("createdDate")
                )
            )
        }
    }

    private fun notesToJson(items: List<NoteEntity>) = JSONArray().apply {
        items.forEach { n ->
            put(JSONObject().apply {
                put("id", n.id)
                put("bookId", n.bookId)
                put("page", n.page)
                put("text", n.text)
                put("createdDate", n.createdDate)
                put("modifiedDate", n.modifiedDate)
            })
        }
    }

    private suspend fun restoreNotes(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.noteDao().insert(
                NoteEntity(
                    id = o.optLong("id", 0),
                    bookId = o.getLong("bookId"),
                    page = o.getInt("page"),
                    text = o.getString("text"),
                    createdDate = o.optLong("createdDate"),
                    modifiedDate = o.optLong("modifiedDate")
                )
            )
        }
    }

    private fun highlightsToJson(items: List<HighlightEntity>) = JSONArray().apply {
        items.forEach { h ->
            put(JSONObject().apply {
                put("id", h.id)
                put("bookId", h.bookId)
                put("page", h.page)
                put("text", h.text)
                put("color", h.color)
                put("startOffset", h.startOffset)
                put("endOffset", h.endOffset)
                put("createdDate", h.createdDate)
            })
        }
    }

    private suspend fun restoreHighlights(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.highlightDao().insert(
                HighlightEntity(
                    id = o.optLong("id", 0),
                    bookId = o.getLong("bookId"),
                    page = o.getInt("page"),
                    text = o.getString("text"),
                    color = o.optInt("color"),
                    startOffset = o.optInt("startOffset"),
                    endOffset = o.optInt("endOffset"),
                    createdDate = o.optLong("createdDate")
                )
            )
        }
    }

    private fun collectionsToJson(items: List<CollectionEntity>) = JSONArray().apply {
        items.forEach { c ->
            put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("description", c.description)
                put("createdDate", c.createdDate)
            })
        }
    }

    private suspend fun restoreCollections(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.collectionDao().insertCollection(
                CollectionEntity(
                    id = o.optLong("id", 0),
                    name = o.getString("name"),
                    description = o.optString("description").ifBlank { null },
                    createdDate = o.optLong("createdDate")
                )
            )
        }
    }

    private fun crossRefsToJson(refs: List<BookCollectionCrossRef>) = JSONArray().apply {
        refs.forEach { r ->
            put(JSONObject().apply {
                put("bookId", r.bookId)
                put("collectionId", r.collectionId)
            })
        }
    }

    private suspend fun restoreBookCollections(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.collectionDao().addBookToCollection(
                BookCollectionCrossRef(
                    bookId = o.getLong("bookId"),
                    collectionId = o.getLong("collectionId")
                )
            )
        }
    }

    private fun readingListsToJson(items: List<ReadingListEntity>) = JSONArray().apply {
        items.forEach { l ->
            put(JSONObject().apply {
                put("id", l.id)
                put("name", l.name)
                put("description", l.description)
                put("iconName", l.iconName)
                put("isSystemList", l.isSystemList)
                put("createdDate", l.createdDate)
                put("modifiedDate", l.modifiedDate)
            })
        }
    }

    private suspend fun restoreReadingLists(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.readingListDao().insertList(
                ReadingListEntity(
                    id = o.optLong("id", 0),
                    name = o.getString("name"),
                    description = o.optString("description").ifBlank { null },
                    iconName = o.optString("iconName", "list"),
                    isSystemList = o.optBoolean("isSystemList"),
                    createdDate = o.optLong("createdDate"),
                    modifiedDate = o.optLong("modifiedDate")
                )
            )
        }
    }

    private fun readingListRefsToJson(refs: List<ReadingListBookCrossRef>) = JSONArray().apply {
        refs.forEach { r ->
            put(JSONObject().apply {
                put("listId", r.listId)
                put("bookId", r.bookId)
                put("displayOrder", r.displayOrder)
                put("addedDate", r.addedDate)
            })
        }
    }

    private suspend fun restoreReadingListBooks(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.readingListDao().addBookToList(
                ReadingListBookCrossRef(
                    listId = o.getLong("listId"),
                    bookId = o.getLong("bookId"),
                    displayOrder = o.optInt("displayOrder"),
                    addedDate = o.optLong("addedDate")
                )
            )
        }
    }

    private fun sessionsToJson(items: List<ReadingSessionEntity>) = JSONArray().apply {
        items.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                put("bookId", s.bookId)
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("startPage", s.startPage)
                put("endPage", s.endPage)
                put("pagesRead", s.pagesRead)
                put("duration", s.duration)
            })
        }
    }

    private suspend fun restoreSessions(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.readingSessionDao().insert(
                ReadingSessionEntity(
                    id = o.optLong("id", 0),
                    bookId = o.getLong("bookId"),
                    startTime = o.getLong("startTime"),
                    endTime = o.getLong("endTime"),
                    startPage = o.getInt("startPage"),
                    endPage = o.getInt("endPage"),
                    pagesRead = o.getInt("pagesRead"),
                    duration = o.getLong("duration")
                )
            )
        }
    }

    private fun goalsToJson(items: List<ReadingGoalEntity>) = JSONArray().apply {
        items.forEach { g ->
            put(JSONObject().apply {
                put("id", g.id)
                put("type", g.type)
                put("target", g.target)
                put("createdDate", g.createdDate)
                put("isActive", g.isActive)
            })
        }
    }

    private suspend fun restoreGoals(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.readingGoalDao().insert(
                ReadingGoalEntity(
                    id = o.optLong("id", 0),
                    type = o.getString("type"),
                    target = o.getInt("target"),
                    createdDate = o.optLong("createdDate"),
                    isActive = o.optBoolean("isActive", true)
                )
            )
        }
    }

    private fun achievementsToJson(items: List<AchievementEntity>) = JSONArray().apply {
        items.forEach { a ->
            put(JSONObject().apply {
                put("id", a.id)
                put("type", a.type)
                put("title", a.title)
                put("description", a.description)
                put("isUnlocked", a.isUnlocked)
                put("unlockedDate", a.unlockedDate)
                put("progress", a.progress)
                put("target", a.target)
            })
        }
    }

    private suspend fun restoreAchievements(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.achievementDao().insert(
                AchievementEntity(
                    id = o.optLong("id", 0),
                    type = o.getString("type"),
                    title = o.getString("title"),
                    description = o.getString("description"),
                    isUnlocked = o.optBoolean("isUnlocked"),
                    unlockedDate = if (o.has("unlockedDate") && !o.isNull("unlockedDate")) o.getLong("unlockedDate") else null,
                    progress = o.optInt("progress"),
                    target = o.optInt("target")
                )
            )
        }
    }

    private fun quotesToJson(items: List<FavoriteQuoteEntity>) = JSONArray().apply {
        items.forEach { q ->
            put(JSONObject().apply {
                put("id", q.id)
                put("bookId", q.bookId)
                put("page", q.page)
                put("text", q.text)
                put("createdDate", q.createdDate)
            })
        }
    }

    private suspend fun settingsToJson(): JSONObject {
        val prefs = context.settingsDataStore.data.first()
        return JSONObject().apply {
            put("darkMode", prefs[SettingsKeys.DARK_MODE])
            put("transitionStyle", prefs[SettingsKeys.TRANSITION_STYLE])
            put("animationSpeed", prefs[SettingsKeys.ANIMATION_SPEED])
            put("pageSound", prefs[SettingsKeys.PAGE_SOUND])
            put("pageSoundVolume", prefs[SettingsKeys.PAGE_SOUND_VOLUME])
            put("ambientSound", prefs[SettingsKeys.AMBIENT_SOUND])
            put("ambientVolume", prefs[SettingsKeys.AMBIENT_VOLUME])
            put("readerBrightness", prefs[SettingsKeys.READER_BRIGHTNESS])
            put("readerTheme", prefs[SettingsKeys.READER_THEME])
            put("marginSize", prefs[SettingsKeys.MARGIN_SIZE])
            put("autoHideControls", prefs[SettingsKeys.AUTO_HIDE_CONTROLS])
            put("libraryLayout", prefs[SettingsKeys.LIBRARY_LAYOUT])
            put("sortOrder", prefs[SettingsKeys.SORT_ORDER])
            put("pageMode", prefs[SettingsKeys.PAGE_MODE])
            put("showBookOpening", prefs[SettingsKeys.SHOW_BOOK_OPENING])
        }
    }

    private suspend fun restoreSettings(settings: JSONObject?) {
        settings ?: return
        context.settingsDataStore.edit { prefs ->
            if (settings.has("darkMode") && !settings.isNull("darkMode")) prefs[SettingsKeys.DARK_MODE] = settings.getBoolean("darkMode")
            if (settings.has("transitionStyle") && !settings.isNull("transitionStyle")) prefs[SettingsKeys.TRANSITION_STYLE] = settings.getString("transitionStyle")
            if (settings.has("animationSpeed") && !settings.isNull("animationSpeed")) prefs[SettingsKeys.ANIMATION_SPEED] = settings.getInt("animationSpeed")
            if (settings.has("pageSound") && !settings.isNull("pageSound")) prefs[SettingsKeys.PAGE_SOUND] = settings.getBoolean("pageSound")
            if (settings.has("pageSoundVolume") && !settings.isNull("pageSoundVolume")) prefs[SettingsKeys.PAGE_SOUND_VOLUME] = settings.getDouble("pageSoundVolume").toFloat()
            if (settings.has("ambientSound") && !settings.isNull("ambientSound")) prefs[SettingsKeys.AMBIENT_SOUND] = settings.getString("ambientSound")
            if (settings.has("ambientVolume") && !settings.isNull("ambientVolume")) prefs[SettingsKeys.AMBIENT_VOLUME] = settings.getDouble("ambientVolume").toFloat()
            if (settings.has("readerBrightness") && !settings.isNull("readerBrightness")) prefs[SettingsKeys.READER_BRIGHTNESS] = settings.getDouble("readerBrightness").toFloat()
            if (settings.has("readerTheme") && !settings.isNull("readerTheme")) prefs[SettingsKeys.READER_THEME] = settings.getString("readerTheme")
            if (settings.has("marginSize") && !settings.isNull("marginSize")) prefs[SettingsKeys.MARGIN_SIZE] = settings.getString("marginSize")
            if (settings.has("autoHideControls") && !settings.isNull("autoHideControls")) prefs[SettingsKeys.AUTO_HIDE_CONTROLS] = settings.getBoolean("autoHideControls")
            if (settings.has("libraryLayout") && !settings.isNull("libraryLayout")) prefs[SettingsKeys.LIBRARY_LAYOUT] = settings.getString("libraryLayout")
            if (settings.has("sortOrder") && !settings.isNull("sortOrder")) prefs[SettingsKeys.SORT_ORDER] = settings.getString("sortOrder")
            if (settings.has("pageMode") && !settings.isNull("pageMode")) prefs[SettingsKeys.PAGE_MODE] = settings.getString("pageMode")
            if (settings.has("showBookOpening") && !settings.isNull("showBookOpening")) prefs[SettingsKeys.SHOW_BOOK_OPENING] = settings.getBoolean("showBookOpening")
        }
    }

    private suspend fun restoreQuotes(arr: JSONArray?) {
        arr ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            db.favoriteQuoteDao().insert(
                FavoriteQuoteEntity(
                    id = o.optLong("id", 0),
                    bookId = o.getLong("bookId"),
                    page = o.getInt("page"),
                    text = o.getString("text"),
                    createdDate = o.optLong("createdDate")
                )
            )
        }
    }
}
