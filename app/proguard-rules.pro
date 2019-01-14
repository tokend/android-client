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

# SDK
-dontwarn com.squareup.**
-dontwarn okio.**
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-dontwarn java.lang.**
-dontwarn javax.lang.**
-dontwarn javax.annotation.**
-keepclassmembers class * {
    @com.github.jasminb.jsonapi.annotations.Id <fields>;
}
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# JSON
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers enum * { *; }
-keep class org.codehaus.** { *; }
-keep class com.fasterxml.jackson.annotation.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Debug
-keepattributes *Annotation*
-keepattributes Exceptions, SourceFile, LineNumberTable
-keep public class * extends java.lang.Exception

# Optimize
-repackageclasses
-optimizations !method/removal/parameter

# Markdown
-dontwarn com.caverock.androidsvg.SVGImageView

# Kotlin issue
# https://youtrack.jetbrains.com/issue/KT-24986
-keepclassmembers class  *  {
   void $$clinit();
}

# MultiDex
-keep class android.support.multidex.**

# Support library
-keep class android.support.v7.widget.SearchView { *; }