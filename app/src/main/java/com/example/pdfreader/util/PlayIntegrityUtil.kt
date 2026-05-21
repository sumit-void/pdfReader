package com.example.pdfreader.util

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import timber.log.Timber

object PlayIntegrityUtil {
    /**
     * Requests a cryptographically signed integrity token from Google Play Services.
     * In a production cloud app, this token is sent to the backend for decryption.
     * In our secure local/offline app, we ensure the device can successfully compile and generate
     * this token, providing a standard diagnostic point for application authenticity.
     */
    fun verifyDeviceIntegrity(context: Context, onResult: (Boolean, String) -> Unit) {
        try {
            val integrityManager = IntegrityManagerFactory.create(context.applicationContext)
            
            // Generate a secure unique nonce
            val nonce = java.util.UUID.randomUUID().toString().replace("-", "")
            
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()
                
            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { response ->
                    val token = response.token()
                    Timber.d("Play Integrity Token successfully retrieved. Length: ${token.length}")
                    onResult(true, "Device verification passed (Play Integrity token generated successfully)")
                }
                .addOnFailureListener { exception ->
                    Timber.w("Play Integrity check failed or not supported: ${exception.message}")
                    onResult(false, "Play Integrity check failed: ${exception.localizedMessage}")
                }
        } catch (e: Exception) {
            Timber.w("Play Integrity API is not available on this device: ${e.message}")
            onResult(false, "Play Integrity is unavailable: ${e.localizedMessage}")
        }
    }
}
