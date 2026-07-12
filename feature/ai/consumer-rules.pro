# Consumer rules merged into the minified :app build.
# Pin the @Serializable cloud DTOs so JSON (de)serialization survives R8. Field
# names are additionally locked via @SerialName in the provider/mapper sources.
-keepclassmembers @kotlinx.serialization.Serializable class com.lumacam.feature.ai.cloud.** {
    <fields>;
    <init>(...);
}
-keep,includedescriptorclasses class com.lumacam.feature.ai.cloud.**$$serializer { *; }
