# Paperback PDF Reader - ProGuard Rules

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class com.example.pdfreader.data.local.entity.** { *; }
-keep class com.example.pdfreader.data.local.dao.** { *; }

# Keep data classes used for serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# PDFBox
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Timber
-dontwarn org.jetbrains.annotations.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-dontwarn androidx.compose.**

# Keep ViewModel constructors for Hilt
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# General Android
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service

# Coil
-dontwarn coil.**
