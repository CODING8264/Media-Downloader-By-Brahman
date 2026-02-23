# Keep all Activities
-keep class com.brahman.downloader.*Activity { *; }

# Keep your Application class
-keep class com.brahman.downloader.App { *; }

# Gson library classes
-keep class com.google.gson.** { *; }
-keepattributes Signature, *Annotation*

# Glide library classes
-keep class com.bumptech.glide.** { *; }
-keep interface com.bumptech.glide.** { *; }

# Ignore warnings
-dontwarn androidx.**