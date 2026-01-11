# Mantém todos os DTOs para que o Android os encontre via Intent/Bundle e Moshi
-keep class br.acerola.manga.dto.** { *; }

# Essencial para @Parcelize do Kotlin funcionar no Release
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Mantém as entidades do Room para não quebrar o banco de dados
-keep class br.acerola.manga.local.database.entity.** { *; }

# Mantém as interfaces do Retrofit para que os métodos não sejam renomeados
-keep interface br.acerola.manga.remote.mangadex.api.** { *; }
-keep class br.acerola.manga.remote.mangadex.api.** { *; }