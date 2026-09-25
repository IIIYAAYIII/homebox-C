# Proguard rules for Homebox Android
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class software.homebox.android.data.api.** { *; }
