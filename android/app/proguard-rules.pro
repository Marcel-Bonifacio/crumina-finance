-keepattributes Signature, *Annotation*, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
# Keep Gson model classes (serialized by reflection)
-keep class id.tirtawijata.crumina.data.** { *; }
