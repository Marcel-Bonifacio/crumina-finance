-keepattributes Signature, *Annotation*, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
# Keep Gson model classes (serialized by reflection)
-keep class id.tirtawijata.crumina.data.** { *; }

# Gson TypeToken: keep generic signatures of TypeToken subclasses (recommended by Gson for R8
# full mode). The app builds its types with TypeToken.getParameterized; this guards any
# anonymous TypeToken added later.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
