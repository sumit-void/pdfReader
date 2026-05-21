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

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = keyStore.getKey(DB_KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
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

    private fun getEncryptedSharedPreferences(context: Context): android.content.SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Synchronized
    fun getDatabaseKey(context: Context): ByteArray {
        val prefs = getEncryptedSharedPreferences(context)
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
            } catch (e: Exception) {
                // If decryption fails, generate a new key
            }
        }

        // Generate a new 32-byte key
        val secureRandom = SecureRandom()
        val rawKey = ByteArray(32)
        secureRandom.nextBytes(rawKey)

        // Encrypt and store key
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
}
