package com.example.pdfreader.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityKeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val DB_KEY_ALIAS = "PaperbackDbMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_FILE = "paperback_secure_prefs"
    private const val ENCRYPTED_KEY_PREF = "encrypted_db_key"
    private const val IV_PREF = "encrypted_db_key_iv"

    private const val FALLBACK_PREFS_FILE = "paperback_fallback_prefs"
    private const val FALLBACK_KEY_PREF = "fallback_db_key"

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = keyStore.getKey(DB_KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    DB_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setIsStrongBoxBacked(true)
                    .build()
                keyGenerator.init(spec)
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                // StrongBox not supported or failed, fallback to standard keystore
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            DB_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getEncryptedSharedPreferences(context: Context): android.content.SharedPreferences? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getFallbackSharedPreferences(context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
    }

    private fun getAndroidIdHash(context: Context): ByteArray {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "paperback_resilient_salt_constant"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(androidId.toByteArray(Charsets.UTF_8))
    }

    private fun obfuscateKey(rawKey: ByteArray, context: Context): String {
        val hash = getAndroidIdHash(context)
        val obfuscated = ByteArray(rawKey.size)
        for (i in rawKey.indices) {
            obfuscated[i] = (rawKey[i].toInt() xor hash[i % hash.size].toInt()).toByte()
        }
        return Base64.encodeToString(obfuscated, Base64.DEFAULT)
    }

    private fun deobfuscateKey(obfuscatedBase64: String, context: Context): ByteArray {
        val obfuscated = Base64.decode(obfuscatedBase64, Base64.DEFAULT)
        val hash = getAndroidIdHash(context)
        val rawKey = ByteArray(obfuscated.size)
        for (i in obfuscated.indices) {
            rawKey[i] = (obfuscated[i].toInt() xor hash[i % hash.size].toInt()).toByte()
        }
        return rawKey
    }

    private fun generateRandomKey(): ByteArray {
        val secureRandom = SecureRandom()
        val rawKey = ByteArray(32)
        secureRandom.nextBytes(rawKey)
        return rawKey
    }

    @Synchronized
    fun getDatabaseKey(context: Context): ByteArray {
        // 1. Try EncryptedSharedPreferences and KeyStore
        try {
            val prefs = getEncryptedSharedPreferences(context)
            if (prefs != null) {
                val encryptedKeyBase64 = prefs.getString(ENCRYPTED_KEY_PREF, null)
                val ivBase64 = prefs.getString(IV_PREF, null)

                if (encryptedKeyBase64 != null && ivBase64 != null) {
                    try {
                        val secretKey = getOrCreateMasterKey()
                        val cipher = Cipher.getInstance(TRANSFORMATION)
                        val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                        val encryptedBytes = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
                        return cipher.doFinal(encryptedBytes)
                    } catch (decException: Exception) {
                        // Decryption failed. Let's delete the stored encrypted prefs key to regenerate
                        prefs.edit().remove(ENCRYPTED_KEY_PREF).remove(IV_PREF).apply()
                    }
                }

                // If not found or decryption failed, generate a new key
                val rawKey = generateRandomKey()
                val secretKey = getOrCreateMasterKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val encryptedBytes = cipher.doFinal(rawKey)
                val iv = cipher.iv

                prefs.edit().apply {
                    putString(ENCRYPTED_KEY_PREF, Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
                    putString(IV_PREF, Base64.encodeToString(iv, Base64.DEFAULT))
                    apply()
                }
                return rawKey
            }
        } catch (e: Exception) {
            // Failed to use KeyStore/EncryptedSharedPreferences, fall through to custom backup
        }

        // 2. Fallback to obfuscated SharedPreferences
        try {
            val fallbackPrefs = getFallbackSharedPreferences(context)
            val obfuscatedKey = fallbackPrefs.getString(FALLBACK_KEY_PREF, null)
            if (obfuscatedKey != null) {
                return deobfuscateKey(obfuscatedKey, context)
            }

            // Generate new fallback key
            val rawKey = generateRandomKey()
            val obfuscated = obfuscateKey(rawKey, context)
            fallbackPrefs.edit().putString(FALLBACK_KEY_PREF, obfuscated).apply()
            return rawKey
        } catch (e: Exception) {
            // Absolute baseline fallback: constant generated key derived from Android ID
            return getAndroidIdHash(context)
        }
    }

    @Synchronized
    fun resetDatabaseKey(context: Context) {
        // Clear EncryptedSharedPreferences if possible
        try {
            val prefs = getEncryptedSharedPreferences(context)
            prefs?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            // Ignore
        }

        // Clear Fallback SharedPreferences
        try {
            val fallbackPrefs = getFallbackSharedPreferences(context)
            fallbackPrefs.edit().clear().apply()
        } catch (e: Exception) {
            // Ignore
        }

        // Delete master key from keyStore
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            keyStore.deleteEntry(DB_KEY_ALIAS)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
