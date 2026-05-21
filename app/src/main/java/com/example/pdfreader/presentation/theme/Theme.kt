package com.example.pdfreader.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import com.example.pdfreader.domain.model.AppTheme

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor
)

private val SepiaColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = Color(0xFFE0D4B8),
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundSepia,
    onBackground = OnBackgroundSepia,
    surface = SurfaceSepia,
    onSurface = OnSurfaceSepia,
    surfaceVariant = Color(0xFFE8DCC4),
    onSurfaceVariant = Color(0xFF4A3C2D),
    outline = Color(0xFF8A7A68),
    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = Color(0xFF2A2420),
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundAmoled,
    onBackground = OnBackgroundAmoled,
    surface = SurfaceAmoled,
    onSurface = OnSurfaceAmoled,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFBDB3A4),
    outline = Color(0xFF6E655A),
    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor
)

private object ScaleIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val isPressed by interactionSource.collectIsPressedAsState()
        val scaleState = animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "PressedScale"
        )
        val rippleIndication = ripple()
        val rippleInstance = rippleIndication.rememberUpdatedInstance(interactionSource)
        return remember(interactionSource) {
            ScaleIndicationInstance(scaleState, rippleInstance)
        }
    }

    private class ScaleIndicationInstance(
        private val scaleState: State<Float>,
        private val rippleInstance: IndicationInstance
    ) : IndicationInstance {
        override fun ContentDrawScope.draw() {
            scale(scaleState.value, scaleState.value) {
                this@draw.drawContent()
                with(rippleInstance) {
                    draw()
                }
            }
        }
    }
}

/**
 * Main theme composable for the Paperback app.
 * Supports Light, Dark, Sepia, and AMOLED themes.
 */
@Composable
fun PaperbackTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.SEPIA -> SepiaColorScheme
        AppTheme.AMOLED -> AmoledColorScheme
    }

    CompositionLocalProvider(
        LocalIndication provides ScaleIndication
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PaperbackTypography,
            content = content
        )
    }
}
