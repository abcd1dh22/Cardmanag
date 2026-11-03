# Encrypted storage (Tink) - keep everything, it uses reflection internally
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.crypto.tink.**
