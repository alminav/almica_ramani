# --- GraphHopper ---
-keep class com.graphhopper.** { *; }
-keep interface com.graphhopper.** { *; }
-dontwarn com.graphhopper.**

# --- MapLibre ---
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# --- Kotlin Serialization / Reflection (if used) ---
-keepattributes Signature, Annotation, InnerClasses

# --- Preserve line numbers for debugging ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile