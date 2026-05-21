package com.example.pdfreader.di

import com.example.pdfreader.data.repository.BookRepositoryImpl
import com.example.pdfreader.data.repository.BookmarkRepositoryImpl
import com.example.pdfreader.data.repository.HighlightRepositoryImpl
import com.example.pdfreader.data.repository.ReadingStatsRepositoryImpl
import com.example.pdfreader.domain.repository.BookRepository
import com.example.pdfreader.domain.repository.BookmarkRepository
import com.example.pdfreader.domain.repository.HighlightRepository
import com.example.pdfreader.domain.repository.ReadingStatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(
        impl: BookRepositoryImpl
    ): BookRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(
        impl: BookmarkRepositoryImpl
    ): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindHighlightRepository(
        impl: HighlightRepositoryImpl
    ): HighlightRepository

    @Binds
    @Singleton
    abstract fun bindReadingStatsRepository(
        impl: ReadingStatsRepositoryImpl
    ): ReadingStatsRepository

    @Binds
    @Singleton
    abstract fun bindGeminiRepository(
        impl: com.example.pdfreader.data.repository.GeminiRepositoryImpl
    ): com.example.pdfreader.domain.repository.GeminiRepository
}
