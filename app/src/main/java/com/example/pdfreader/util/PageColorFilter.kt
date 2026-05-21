package com.example.pdfreader.util

import android.graphics.ColorMatrix
import com.example.pdfreader.domain.model.AppTheme

object PageColorFilter {
    fun getColorMatrix(theme: AppTheme): ColorMatrix? {
        return when (theme) {
            AppTheme.LIGHT -> null
            AppTheme.SEPIA -> {
                ColorMatrix().apply {
                    setScale(1.1f, 0.95f, 0.8f, 1f)
                }
            }
            AppTheme.DARK -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0.85f)
                
                // Custom YUV-based hue-preserving invert matrix
                val yuvToRgb = floatArrayOf(
                    1f, 0f, 1.13983f, 0f, 0f,
                    1f, -0.39465f, -0.58060f, 0f, 0f,
                    1f, 2.03211f, 0f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                val invertY = floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                val rgbToYuv = floatArrayOf(
                    0.299f, 0.587f, 0.114f, 0f, 0f,
                    -0.14713f, -0.28886f, 0.436f, 0f, 0f,
                    0.615f, -0.51499f, -0.10001f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                val m1 = ColorMatrix(yuvToRgb)
                val m2 = ColorMatrix(invertY)
                val m3 = ColorMatrix(rgbToYuv)
                
                val temp = ColorMatrix()
                temp.setConcat(m1, m2)
                
                val invertYMatrix = ColorMatrix()
                invertYMatrix.setConcat(temp, m3)
                
                matrix.postConcat(invertYMatrix)
                matrix
            }
            AppTheme.AMOLED -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0.8f) // desaturate 20%
                
                val invertMatrix = ColorMatrix(floatArrayOf(
                    -1f,  0f,  0f, 0f, 255f,
                     0f, -1f,  0f, 0f, 255f,
                     0f,  0f, -1f, 0f, 255f,
                     0f,  0f,  0f, 1f, 0f
                ))
                matrix.postConcat(invertMatrix)
                matrix
            }
        }
    }

    fun getOverlayColor(theme: AppTheme): androidx.compose.ui.graphics.Color {
        return when (theme) {
            AppTheme.LIGHT -> androidx.compose.ui.graphics.Color.Transparent
            AppTheme.SEPIA -> androidx.compose.ui.graphics.Color(0xFFF5ECD7).copy(alpha = 0.3f)
            AppTheme.DARK -> androidx.compose.ui.graphics.Color(0xFF1C1C1E).copy(alpha = 0.2f)
            AppTheme.AMOLED -> androidx.compose.ui.graphics.Color(0xFF000000).copy(alpha = 0.25f)
        }
    }
}
