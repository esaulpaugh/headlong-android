-dontobfuscate
-dontoptimize
-dontshrink

-keepnames class * {
    *;
}

-keepattributes *Annotation*

-keep class androidx.recyclerview.** { *; }
-keep interface androidx.recyclerview.** { *; }

-keep class androidx.recyclerview.widget.** { *; }

-keep class androidx.annotation.** { *; }

-dontwarn com.google.errorprone.annotations.**