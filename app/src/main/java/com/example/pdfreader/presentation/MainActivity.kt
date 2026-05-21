package com.example.pdfreader.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.pdfreader.BuildConfig
import com.example.pdfreader.R
import com.example.pdfreader.data.preferences.UserPreferences
import com.example.pdfreader.domain.model.AppTheme
import com.example.pdfreader.presentation.navigation.PaperbackNavGraph
import com.example.pdfreader.presentation.theme.PaperbackTheme
import com.example.pdfreader.util.RootDetectionUtil
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private var backgroundTimestamp: Long = 0L
    private val lockTimeoutMs = 3 * 60 * 1000L // 3 minutes
    private val isAppLocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe Lifecycle events for lock timeout
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                backgroundTimestamp = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_START) {
                lifecycleScope.launch {
                    val enabled = userPreferences.appLockEnabled.first()
                    if (enabled) {
                        val elapsed = System.currentTimeMillis() - backgroundTimestamp
                        if (backgroundTimestamp == 0L || elapsed > lockTimeoutMs) {
                            isAppLocked.value = true
                        }
                    }
                }
            }
        })

        // Screenshot blocker
        lifecycleScope.launch {
            userPreferences.blockScreenshots.collect { block ->
                if (block) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            val themeString by userPreferences.theme.collectAsState(initial = "LIGHT")
            val appTheme = try {
                AppTheme.valueOf(themeString)
            } catch (_: Exception) {
                AppTheme.LIGHT
            }

            var showSplash by remember { mutableStateOf(true) }
            var showRootWarning by remember { mutableStateOf(false) }

            // Dismiss the starting window splash when compose is ready
            LaunchedEffect(Unit) {
                keepSplashOnScreen = false
            }

            // Root Warning logic
            LaunchedEffect(Unit) {
                if (!BuildConfig.DEBUG && RootDetectionUtil.isDeviceRooted()) {
                    showRootWarning = true
                }
            }

            PaperbackTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            showSplash -> {
                                SplashScreenComposable(
                                    onSplashFinished = {
                                        showSplash = false
                                        // Once splash screen completes, trigger biometric prompt if app is locked
                                        if (isAppLocked.value) {
                                            triggerBiometricPrompt()
                                        }
                                    }
                                )
                            }
                            isAppLocked.value -> {
                                LockScreenComposable(
                                    onUnlockClick = { triggerBiometricPrompt() }
                                )
                            }
                            else -> {
                                val navController = rememberNavController()
                                PaperbackNavGraph(navController = navController)
                            }
                        }

                        if (showRootWarning) {
                            RootWarningDialog(onDismiss = { showRootWarning = false })
                        }
                    }
                }
            }
        }
    }

    private fun triggerBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAppLocked.value = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Keep app locked on authentication error/dismiss
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Paperback")
            .setSubtitle("Use your biometric credential to unlock the app")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun SplashScreenComposable(onSplashFinished: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1500) // Display splash screen for 1.5 seconds
        onSplashFinished()
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 80f
        ),
        label = "LogoScale"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 300
        ),
        label = "TextAlpha"
    )

    val provider = GoogleFont.Provider(
        "com.google.android.gms.fonts",
        "com.google.android.gms",
        R.array.com_google_android_gms_fonts_certs
    )

    val playfairFont = GoogleFont("Playfair Display")
    val playfairFontFamily = FontFamily(
        Font(googleFont = playfairFont, fontProvider = provider)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5ECD7)), // Warm cream matching sepia theme
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_paperback_logo),
                contentDescription = "Paperback Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = startAnim,
                enter = fadeIn(tween(600, delayMillis = 300)),
                exit = fadeOut()
            ) {
                Text(
                    text = "Paperback",
                    fontFamily = playfairFontFamily,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B4E3D)
                )
            }
        }
    }
}

@Composable
fun LockScreenComposable(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5ECD7)), // Warm cream
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_paperback_logo),
                contentDescription = "Paperback Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Unlock Paperback",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B4E3D)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B4E3D),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Unlock", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun RootWarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Security Warning", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Your device appears to be rooted. Running Paperback on a rooted device might expose your data to security vulnerabilities.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("I Understand")
            }
        }
    )
}
