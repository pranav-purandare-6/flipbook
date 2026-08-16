package com.pranav.flipbook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pranav.flipbook.ui.bookmarks.BookmarksScreen
import com.pranav.flipbook.ui.collections.CollectionDetailScreen
import com.pranav.flipbook.ui.collections.CollectionsScreen
import com.pranav.flipbook.ui.info.PdfInfoScreen
import com.pranav.flipbook.ui.library.LibraryScreen
import com.pranav.flipbook.ui.notes.NotesHighlightsScreen
import com.pranav.flipbook.ui.reader.ReaderScreen
import com.pranav.flipbook.ui.settings.SettingsScreen
import com.pranav.flipbook.ui.statistics.AchievementsScreen
import com.pranav.flipbook.ui.statistics.GoalsScreen
import com.pranav.flipbook.ui.statistics.StatisticsScreen

object Routes {
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val BOOKMARKS = "bookmarks"
    const val BOOKMARKS_FOR_BOOK = "bookmarks/{bookId}"
    const val NOTES_HIGHLIGHTS = "notes_highlights/{bookId}"
    const val STATISTICS = "statistics"
    const val COLLECTIONS = "collections"
    const val COLLECTION_DETAIL = "collection/{collectionId}"
    const val SETTINGS = "settings"
    const val PDF_INFO = "pdf_info/{bookId}"
    const val GOALS = "goals"
    const val ACHIEVEMENTS = "achievements"

    fun reader(bookId: Long) = "reader/$bookId"
    fun bookmarksForBook(bookId: Long) = "bookmarks/$bookId"
    fun notesHighlights(bookId: Long) = "notes_highlights/$bookId"
    fun collectionDetail(collectionId: Long) = "collection/$collectionId"
    fun pdfInfo(bookId: Long) = "pdf_info/$bookId"
}

@Composable
fun FlipBookNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(Routes.reader(bookId)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onStatisticsClick = { navController.navigate(Routes.STATISTICS) },
                onCollectionsClick = { navController.navigate(Routes.COLLECTIONS) },
                onBookInfoClick = { bookId -> navController.navigate(Routes.pdfInfo(bookId)) }
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onBookmarksClick = { navController.navigate(Routes.bookmarksForBook(bookId)) },
                onNotesClick = { navController.navigate(Routes.notesHighlights(bookId)) },
                onInfoClick = { navController.navigate(Routes.pdfInfo(bookId)) }
            )
        }

        composable(Routes.BOOKMARKS) {
            BookmarksScreen(
                bookId = null,
                onBack = { navController.popBackStack() },
                onNavigateToPage = { bookId, _ ->
                    navController.navigate(Routes.reader(bookId))
                }
            )
        }

        composable(
            route = Routes.BOOKMARKS_FOR_BOOK,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            BookmarksScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onNavigateToPage = { bId, _ ->
                    navController.popBackStack()
                    navController.navigate(Routes.reader(bId))
                }
            )
        }

        composable(
            route = Routes.NOTES_HIGHLIGHTS,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            NotesHighlightsScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onNavigateToPage = { bId, _ ->
                    navController.popBackStack()
                    navController.navigate(Routes.reader(bId))
                }
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(
                onBack = { navController.popBackStack() },
                onGoalsClick = { navController.navigate(Routes.GOALS) },
                onAchievementsClick = { navController.navigate(Routes.ACHIEVEMENTS) }
            )
        }

        composable(Routes.COLLECTIONS) {
            CollectionsScreen(
                onBack = { navController.popBackStack() },
                onCollectionClick = { id -> navController.navigate(Routes.collectionDetail(id)) }
            )
        }

        composable(
            route = Routes.COLLECTION_DETAIL,
            arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: return@composable
            CollectionDetailScreen(
                collectionId = collectionId,
                onBack = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate(Routes.reader(bookId)) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PDF_INFO,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            PdfInfoScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GOALS) {
            GoalsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
    }
}
