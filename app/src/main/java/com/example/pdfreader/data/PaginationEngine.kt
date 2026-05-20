package com.example.pdfreader.data

import android.text.StaticLayout
import android.text.TextPaint

class PaginationEngine(
    private val textPaint: TextPaint,
    private val pageHeight: Int,
    private val pageWidth: Int,
    private val lineSpacingMultiplier: Float = 1.2f
) {
    /**
     * Splits [fullText] into a list of strings, where each string represents
     * a page that fits exactly within [pageWidth] and [pageHeight] using the given [textPaint].
     */
    fun paginate(fullText: String): List<String> {
        if (fullText.isEmpty()) return listOf("")
        
        val pages = mutableListOf<String>()
        var startOffset = 0
        val textLength = fullText.length

        while (startOffset < textLength) {
            // Using StaticLayout to measure text heights fitting the canvas
            val staticLayout = StaticLayout.Builder.obtain(
                fullText,
                startOffset,
                textLength,
                textPaint,
                pageWidth
            )
            .setLineSpacing(0f, lineSpacingMultiplier)
            .build()

            // Find how many lines fit within the page height
            var lineCount = 0
            var currentHeight = 0
            
            while (lineCount < staticLayout.lineCount) {
                val nextHeight = staticLayout.getLineBottom(lineCount)
                if (nextHeight > pageHeight) {
                    break
                }
                currentHeight = nextHeight
                lineCount++
            }

            if (lineCount == 0) {
                // Force at least 1 line to prevent infinite loops on giant text strings or small layouts
                lineCount = 1
            }

            // Extract the fitting text segment
            val endOffset = staticLayout.getLineEnd(lineCount - 1)
            
            // Safety check for endOffset value
            val safeEndOffset = if (endOffset <= startOffset) startOffset + 1 else endOffset
            val clampedEndOffset = Math.min(safeEndOffset, textLength)
            
            val pageText = fullText.substring(startOffset, clampedEndOffset).trim()
            if (pageText.isNotEmpty()) {
                pages.add(pageText)
            }
            
            startOffset = clampedEndOffset
        }
        
        return if (pages.isEmpty()) listOf("") else pages
    }
}
