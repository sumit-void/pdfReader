package com.example.pdfreader.ui.view

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class BookPageTransformer : ViewPager2.PageTransformer {
    
    override fun transformPage(page: View, position: Float) {
        val pageShadowOverlay = page.findViewWithTag<View>("shadow_overlay")
        
        page.cameraDistance = 25000f

        when {
            position < -1 -> { // [-Infinity,-1)
                // Page is way off-screen to the left.
                page.alpha = 0f
            }
            position <= 0 -> { // [-1,0]
                // Page is turning away to the left.
                page.alpha = 1f
                // Rotate around left edge
                page.pivotX = 0f
                page.rotationY = 90f * position
                
                // Offset the default horizontal slide translation
                page.translationX = -position * page.width
                
                // Darken shadow overlay as page flips
                pageShadowOverlay?.alpha = -position * 0.7f
            }
            position <= 1 -> { // (0,1]
                // Page is the next page (being revealed behind or coming from right).
                page.alpha = 1f
                page.pivotX = 0f
                page.rotationY = 0f
                
                // Keep next page static in place behind the turning page
                page.translationX = -position * page.width
                
                // Shadow fades out as page is fully revealed
                pageShadowOverlay?.alpha = (1f - position) * 0.3f
            }
            else -> { // (1,+Infinity]
                // Page is way off-screen to the right.
                page.alpha = 0f
            }
        }
    }
}
