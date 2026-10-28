# kotlinx.serialization: @Serializable 클래스 유지
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit 인터페이스
-keep,allowobfuscation,allowshrinking interface io.github.jennaslee.silverapp.data.remote.SilverApi
-keepattributes Signature, Exceptions
