# Health Connect
-keep class androidx.health.connect.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.healthdispatch.**$$serializer { *; }
-keepclassmembers class com.healthdispatch.** { *** Companion; }
-keepclasseswithmembers class com.healthdispatch.** { kotlinx.serialization.KSerializer serializer(...); }
