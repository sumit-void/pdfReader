package com.example.pdfreader.util

import android.os.Build
import java.io.File

object RootDetectionUtil {
    fun isDeviceRooted(): Boolean {
        // 1. Check su binary and superuser paths
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }

        // 2. Check test-keys
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 3. Execute which su command
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            process.inputStream.use { it.readBytes() }.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
