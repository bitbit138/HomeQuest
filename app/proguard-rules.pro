# ── HomeQuest ProGuard / R8 rules ─────────────────────────────────────────
# Release build: isMinifyEnabled = true, isShrinkResources = true
# These rules prevent obfuscation of classes that are accessed by reflection
# or by external libraries at runtime.

# ── Glide ─────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# ── Firebase Firestore ─────────────────────────────────────────────────────
# Firestore uses reflection to deserialize documents into model classes.
# All fields on model classes must be kept.
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class dev.tombit.homequest.model.** {
    *;
}
# Firebase internal
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Gson ───────────────────────────────────────────────────────────────────
# Gson uses reflection on data classes serialized via SharedPreferencesManager.
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
# Keep all model classes used with Gson (same as Firestore keep above)
-keepclassmembers class dev.tombit.homequest.model.** {
    <fields>;
    <init>();
}

# ── Lottie ─────────────────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── Kotlin coroutines ──────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Kotlin metadata (required for reflection in some Firebase SDK paths) ───
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── OkHttp (transitive dep of Firebase) ────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── General Android ────────────────────────────────────────────────────────
# Keep custom Application class
-keep class dev.tombit.homequest.App { *; }
# Keep all Activity classes (referenced by manifest)
-keep class dev.tombit.homequest.*Activity { *; }
# Keep MessagingService
-keep class dev.tombit.homequest.utilities.HomeQuestMessagingService { *; }
