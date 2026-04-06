package br.acerola.comic.remote.anilist

import br.acerola.comic.data.BuildConfig
import com.apollographql.apollo.ApolloClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class AnilistApollo

@Module
@InstallIn(SingletonComponent::class)
object AnilistModule {

    @AnilistApollo
    @Provides
    @Singleton
    fun provideAnilistApolloClient(): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(BuildConfig.ANILIST_BASE_URL)
            .build()
    }
}
