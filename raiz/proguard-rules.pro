-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.raiz.toc.**$$serializer { *; }
-keepclassmembers class com.raiz.toc.** { *** Companion; }
-keepclasseswithmembers class com.raiz.toc.** { kotlinx.serialization.KSerializer serializer(...); }

# SDK de Anthropic (Jackson usa reflexión) y dependencias opcionales que no existen en Android.
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes Signature, EnclosingMethod
-dontwarn com.fasterxml.jackson.**
-dontwarn com.github.victools.**
-dontwarn io.swagger.**
-dontwarn org.apache.hc.**
-dontwarn org.slf4j.**
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
