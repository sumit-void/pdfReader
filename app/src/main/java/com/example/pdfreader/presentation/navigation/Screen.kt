package com.example.pdfreader.presentation.navigation

/**
 * Defines all navigation routes in the Paperback app.
 */
sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: Long): String = "reader/$bookId"
    }
    data object Bookmarks : Screen("bookmarks/{bookId}") {
        fun createRoute(bookId: Long): String = "bookmarks/$bookId"
    }
    data object Highlights : Screen("highlights/{bookId}") {
        fun createRoute(bookId: Long): String = "highlights/$bookId"
    }
    data object TableOfContents : Screen("toc/{bookId}") {
        fun createRoute(bookId: Long): String = "toc/$bookId"
    }
    data object Settings : Screen("settings")
    data object ReadingStats : Screen("reading_stats")
    data object QuoteCardEditor : Screen("quote_card_editor/{highlightId}") {
        fun createRoute(highlightId: Long): String = "quote_card_editor/$highlightId"
    }
}
