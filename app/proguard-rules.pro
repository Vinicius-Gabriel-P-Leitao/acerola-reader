# Hilt específico para versões 2.50+
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends androidx.lifecycle.ViewModel

# Regras para o Hilt não remover classes geradas
-keep class com.google.dagger.hilt.** { *; }
-keep class dagger.hilt.** { *; }
-keep class *_HiltModules* { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }

# Se continuar dando erro em bibliotecas externas (como Junrar ou Apollo)
-dontwarn com.github.junrar.**
-dontwarn com.apollographql.apollo.**
-dontwarn arrow.**
-dontwarn org.bouncycastle.**

# Manter atributos de reflexão (Crítico para Hilt e Retrofit)
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}