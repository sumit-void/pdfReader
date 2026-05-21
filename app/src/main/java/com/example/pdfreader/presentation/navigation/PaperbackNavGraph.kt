package com.example.pdfreader.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.pdfreader.presentation.screens.bookmarks.BookmarksScreen
import com.example.pdfreader.presentation.screens.highlights.HighlightsScreen
import com.example.pdfreader.presentation.screens.highlights.QuoteCardEditorScreen
import com.example.pdfreader.presentation.screens.library.LibraryScreen
import com.example.pdfreader.presentation.screens.reader.ReaderScreen
import com.example.pdfreader.presentation.screens.readingstats.ReadingStatsScreen
import com.example.pdfreader.presentation.screens.settings.SettingsScreen
import com.example.pdfreader.presentation.screens.toc.TableOfContentsScreen

@Composable
fun PaperbackNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Library.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(280)) + slideInHorizontally { it / 6 }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220)) + slideOutHorizontally { it / 6 }
        }
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onBookClick = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onStatsClick = {
                    navController.navigate(Screen.ReadingStats.route)
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
            deepLinks = listOf(navDeepLink { uriPattern = "paperback://reader/{bookId}" })
        ) {
            ReaderScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookmarks = { bookId ->
                    navController.navigate(Screen.Bookmarks.createRoute(bookId))
                },
                onNavigateToHighlights = { bookId ->
                    navController.navigate(Screen.Highlights.createRoute(bookId))
                },
                onNavigateToToc = { bookId ->
                    navController.navigate(Screen.TableOfContents.createRoute(bookId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Bookmarks.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) {
            BookmarksScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPage = { bookId, page ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("jumpToPage", page)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Highlights.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) {
            HighlightsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPage = { bookId, page ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("jumpToPage", page)
                    navController.popBackStack()
                },
                onNavigateToQuoteCardEditor = { highlightId ->
                    navController.navigate(Screen.QuoteCardEditor.createRoute(highlightId))
                }
            )
        }

        composable(
            route = Screen.TableOfContents.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) {
            TableOfContentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPage = { page ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("jumpToPage", page)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ReadingStats.route) {
            ReadingStatsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QuoteCardEditor.route,
            arguments = listOf(navArgument("highlightId") { type = NavType.LongType })
        ) {
            QuoteCardEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
