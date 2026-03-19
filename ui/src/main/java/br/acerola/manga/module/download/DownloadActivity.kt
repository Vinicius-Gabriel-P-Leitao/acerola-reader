package br.acerola.manga.module.download

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.download.layout.DownloadScreen
import br.acerola.manga.ui.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadActivity(
    override val startDestinationRes: Int = R.string.navigation_download_activity,
) : BaseActivity() {

    override val applyScaffoldPadding: Boolean = false

    object Extra {
        const val MANGA = "MANGA"
    }

    val manga: MangaRemoteInfoDto? by lazy {
        val safeIntent = intent ?: return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            safeIntent.extras?.getParcelable(Extra.MANGA, MangaRemoteInfoDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            safeIntent.extras?.getParcelable(Extra.MANGA)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(R.string.navigation_download_activity)) {
            val m = manga ?: run { finish(); return@composable }
            Download.Layout.DownloadScreen(
                manga = m,
                onBack = { finish() }
            )
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) = Unit

    companion object {
        fun start(context: Context, manga: MangaRemoteInfoDto) {
            val intent = Intent(context, DownloadActivity::class.java).apply {
                putExtra(Extra.MANGA, manga)
            }
            context.startActivity(intent)
        }
    }
}
