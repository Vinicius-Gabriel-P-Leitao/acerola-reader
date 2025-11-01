package br.acerola.manga.ui.feature.home.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.acerola.manga.R
import br.acerola.manga.domain.permission.FolderAccessManager
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.layout.AcerolaScaffold
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.home.screen.FolderAccessScreen

// @formatter:off
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val manager = FolderAccessManager(applicationContext)
        val viewModel = ViewModelProvider(
            this,  FolderAccessViewModelFactory(application, manager)
        )[FolderAccessViewModel::class.java]

        setContent {
            AcerolaTheme(dynamicColor = false) {
                AcerolaScaffold() {
                    Scaffold(
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            Text(
                                text = "Testando",
                                modifier = Modifier.statusBarsPadding()
                            )
                        },
                    ) { innerPadding ->
                        SmartButton(
                            onClick = { println("Clicou no ícone!") },
                            type = ButtonType.ICON_TEXT,
                            text = "Botão",
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favorito"
                            )
                        }
                        CardDynamicImagePreview()
                        CardDynamicContentPreview()
                        FolderAccessScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Card de imagem")
fun CardDynamicImagePreview() {
    Column {
        SmartCard(
            type = CardType.IMAGE,
            title = "Paisagem Montanhosa",
            imagePainter = painterResource(id = R.drawable.ic_launcher_background),
            footerText = "Foto tirada nos Alpes Suíços",
            modifier = Modifier.width(width = 200.dp).height(height = 350.dp).padding(all = 16.dp)
        ) {
            println("Card com imagem")
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Card de Conteúdo")
fun CardDynamicContentPreview() {
    Column {
        SmartCard(
            type = CardType.CONTENT,
            title = "Jetpack Compose",
            contentText = "Jetpack Compose é um kit de ferramentas de UI moderno para criar apps Android nativos. Ele simplifica e acelera o desenvolvimento da UI no Android.",
            footerText = "Publicado em: 30 de Outubro de 2025",
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            println("Card sem imagem")
        }
    }
}
