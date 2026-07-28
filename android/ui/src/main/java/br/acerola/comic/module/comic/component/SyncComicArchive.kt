package br.acerola.comic.module.comic.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroButton
import br.acerola.comic.common.ux.tokens.SizeTokens
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R

@Composable
fun Comic.Component.SyncMangaArchive(
    onSyncChapters: () -> Unit,
    onRescanCover: () -> Unit,
    onExtractFirstPageAsCover: () -> Unit,
    onExtractVolumeCovers: (() -> Unit)? = null,
    isSyncingChapters: Boolean = false,
    isRescanningCover: Boolean = false,
    isExtractingFirstPage: Boolean = false,
    isExtractingVolumeCovers: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val anyLoading = isSyncingChapters || isRescanningCover || isExtractingFirstPage || isExtractingVolumeCovers

    Column(modifier = modifier.fillMaxWidth()) {
        Acerola.Component.HeroButton(
            title = stringResource(id = R.string.title_sync_chapters),
            description = stringResource(id = R.string.description_sync_chapters_local),
            iconBackground = MaterialTheme.colorScheme.secondaryContainer,
            onClick = if (anyLoading) null else onSyncChapters,
            icon = {
                AnimatedContent(
                    targetState = isSyncingChapters,
                    label = "syncChaptersLoadingAnimation",
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(SizeTokens.IconMedium),
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Acerola.Component.HeroButton(
            title = stringResource(id = R.string.title_sync_cover_banner),
            description = stringResource(id = R.string.description_sync_cover_banner),
            iconBackground = MaterialTheme.colorScheme.secondaryContainer,
            onClick = if (anyLoading) null else onRescanCover,
            icon = {
                AnimatedContent(
                    targetState = isRescanningCover,
                    label = "rescanCoverLoadingAnimation",
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(SizeTokens.IconMedium),
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Acerola.Component.HeroButton(
            title = stringResource(id = R.string.title_extract_first_page_as_cover),
            description = stringResource(id = R.string.description_extract_first_page_as_cover),
            iconBackground = MaterialTheme.colorScheme.secondaryContainer,
            onClick = if (anyLoading) null else onExtractFirstPageAsCover,
            icon = {
                AnimatedContent(
                    targetState = isExtractingFirstPage,
                    label = "extractFirstPageLoadingAnimation",
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ImageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(SizeTokens.IconMedium),
                        )
                    }
                }
            },
        )

        if (onExtractVolumeCovers != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Acerola.Component.HeroButton(
                title = stringResource(id = R.string.title_extract_volume_covers),
                description = stringResource(id = R.string.description_extract_volume_covers),
                iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                onClick = if (anyLoading) null else onExtractVolumeCovers,
                icon = {
                    AnimatedContent(
                        targetState = isExtractingVolumeCovers,
                        label = "extractVolumeCoversLoadingAnimation",
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesomeMotion,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(SizeTokens.IconMedium),
                            )
                        }
                    }
                },
            )
        }
    }
}
