# Mantém as Activities para o AndroidManifest não se perder
-keep class br.acerola.manga.module.** { *; }

# Mantém a BaseActivity para garantir que métodos sobrescritos (como setupNavGraph) funcionem
-keep class br.acerola.manga.common.activity.BaseActivity { *; }

# Mantém os nomes dos ViewModels (ajuda o Hilt/Lifecycle)
-keep class * extends androidx.lifecycle.ViewModel { *; }