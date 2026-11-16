# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.aiutil.videoplayer.** {
    *** Companion;
}
-keepclasseswithmembers class com.aiutil.videoplayer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit & Media3
-dontwarn com.google.mlkit.**
-dontwarn androidx.media3.**
