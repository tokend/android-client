-dontobfuscate
-android

# Keep everything in kept classes and enums, because I'm tired of this.
#-keepclassmembers class **  { *; }
-keepclassmembers class io.tokend.template.**  { *; }
-keepclassmembers class org.tokend.sdk.**  { *; }
-keepclassmembers enum ** { *; }
-keepattributes **

# class [META-INF/versions/9/module-info.class] unexpectedly contains class [module-info]
-dontwarn module-info

# ProGuard issues
# https://sourceforge.net/p/proguard/bugs/573/
-optimizations !class/unboxing/enum
-optimizations !method/removal/parameter

# Somthing general
-dontwarn javax.annotation.**

# SVG library
-dontwarn com.caverock.androidsvg.**

# --- TokenD SDK ---

# Crypto
-dontwarn net.i2p.crypto.**
-keep class net.i2p.crypto.** { *; }

# JSONAPI and Jackson
-dontwarn com.fasterxml.**
-keep class com.fasterxml.**  { *; }
-keep class com.github.jasminb.jsonapi.**  { *; }
-keep @interface kotlin.Metadata { *; } # Very important
-keep class kotlin.reflect.**  { *; }

# Wallet
-keep class org.tokend.wallet.xdr.*  { *; }
-dontnote org.tokend.wallet.xdr.*

# ------------------