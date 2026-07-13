# LumaCam R8 / ProGuard keep rules (release minify + resource shrink).
# Most AndroidX/Google libraries ship their own consumer rules; the entries below
# are defensive keeps for the reflection-driven paths this app relies on.

# ---- Kotlin metadata & coroutines ------------------------------------------
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature,EnclosingMethod
-dontwarn kotlinx.coroutines.**

# ---- Hilt / Dagger ---------------------------------------------------------
# Hilt generates components/modules; keep generated + annotated types.
-keep,allowobfuscation @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-dontwarn dagger.hilt.**

# ---- Room ------------------------------------------------------------------
# Room generates <Dao>_Impl / <Database>_Impl accessed reflectively at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep class **_Impl { *; }
-dontwarn androidx.room.**

# ---- kotlinx.serialization -------------------------------------------------
# Keep generated serializers and the runtime; @Serializable field pinning is also
# reinforced with @SerialName in the cloud DTOs. Module-local DTO keeps live in
# feature/ai/consumer-rules.pro.
-keepattributes *Annotation*
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-dontwarn kotlinx.serialization.**

# ---- ML Kit / Google Play Services -----------------------------------------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ---- CameraX ---------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---- OkHttp / Okio ---------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# ---- LiteRT-LM / former MediaPipe protobuf annotation stubs ----------------------
# LiteRT-LM (and the MediaPipe LLM Inference API it replaces) reference 5 protobuf
# annotation classes (Internal$ProtoMethodMayReturnNull, ProtoField, ProtoPresenceBits,
# ProtoPresenceCheckedField, ProtoNonnullApi) that ship in NO published protobuf
# release and are not needed at runtime. Tell R8 they may be absent so release
# minification does not fail on "Missing class".
-dontwarn com.google.protobuf.Internal**
-dontwarn com.google.protobuf.ProtoField
-dontwarn com.google.protobuf.ProtoPresenceBits
-dontwarn com.google.protobuf.ProtoPresenceCheckedField
-dontwarn com.google.protobuf.ProtoNonnullApi
