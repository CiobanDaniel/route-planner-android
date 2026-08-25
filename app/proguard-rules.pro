# WebView JS bridge — method names are called from map.html as AndroidBridge.*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# Room entities / DAOs (additive to the Room consumer rules)
-keep class com.danielcioban.routeplanner.data.local.** { *; }

# Play Code Scanner (optional camera scan from the HUD)
-keep class com.google.mlkit.vision.codescanner.** { *; }
