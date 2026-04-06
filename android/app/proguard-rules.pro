# ==============================================================================
# CONFIGURAÇÃO PROGUARD/R8 DEFINITIVA - ACEROLA
# Foco: Estabilidade total (Parcelable, Reflexão em DTOs próprios, Entry Points)
# ==============================================================================

# 1. Metadados Essenciais para Kotlin
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-keep class kotlin.Metadata { *; }

# 2. Pontos de Entrada Android (Manter tudo)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends androidx.lifecycle.ViewModel
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 3. Parcelable e DTOs (Preservação Específica da Aplicação)
# Mantém as classes Parcelable da aplicação intactas.
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
    <fields>;
    <methods>;
}
# Permite otimizações internas mas preserva a estrutura de serialização dos DTOs.
-keepclassmembers,allowoptimization class br.acerola.comic.dto.** { *; }
-keepclassmembers,allowoptimization class br.acerola.comic.data.remote.dto.** { *; }

# 4. Hilt (Injeção de Dependência)
-keepclassmembers class * {
    @dagger.hilt.android.AndroidEntryPoint *;
    @javax.inject.Inject *;
}
