# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.mireru.app.model.** { *; }
-keepclassmembers class com.mireru.app.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * { @dagger.hilt.* <methods>; }

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Keep enums
-keepclassmembers enum * { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }
