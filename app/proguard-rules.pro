# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.robbstacy.bedtimecast.**$$serializer { *; }
-keepclassmembers class com.robbstacy.bedtimecast.** {
    *** Companion;
}
-keepclasseswithmembers class com.robbstacy.bedtimecast.** {
    kotlinx.serialization.KSerializer serializer(...);
}
