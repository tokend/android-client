# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# General reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn javax.annotation.**
-keepclassmembers enum * { *; }

# Retrofit
-dontwarn org.codehaus.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Jackson
-keep class com.fasterxml.** { *; }
-keep @com.fasterxml.jackson.annotation.** class * { *; }
-dontwarn com.fasterxml.jackson.databind.**

# JSONAPI
-keepclassmembers class * { @com.github.jasminb.jsonapi.annotations.Id <fields>; }
-keep class * implements com.github.jasminb.jsonapi.ResourceIdHandler

# Wallet
# Uncomment this if you would like to decode XDRs
-keep class org.tokend.wallet.xdr.* { *; }

# General
-keepattributes SourceFile,LineNumberTable,*Annotation*,EnclosingMethod,Signature,Exceptions,InnerClasses
-keep public class * extends java.lang.Exception

# Optimize
-repackageclasses
-optimizations !method/removal/parameter

# Markdown
-dontwarn com.caverock.androidsvg.**

# Kotlin issue
# https://youtrack.jetbrains.com/issue/KT-24986
-keepclassmembers class  *  {
   void $$clinit();
}

# MultiDex
-keep class android.support.multidex.**

# Support library
-keep class android.support.v7.widget.SearchView { *; }

# KYC state storage
-keepnames class org.tokend.template.features.kyc.** { *; }

# BottomNavigationView shifting hack
-keepclassmembers class android.support.design.internal.BottomNavigationMenuView {
    boolean mShiftingMode;
}

# Keep JsonCreator
-keepclassmembers class * {
     @com.fasterxml.jackson.annotation.JsonCreator *;
}

# Legacy Picasso downloader
-dontwarn com.squareup.picasso.OkHttpDownloader