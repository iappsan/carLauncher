package com.iappsan.carlauncher.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.media.MediaState
import com.iappsan.carlauncher.ui.icons.LauncherIcons
import com.iappsan.carlauncher.ui.theme.LauncherTheme

/** Versión compacta del widget de música, para el modo navegación (una sola fila). */
@Composable
fun MediaBar(state: MediaState, actions: MediaActions, modifier: Modifier = Modifier) {
    val nowPlaying = state.nowPlaying
    Surface(
        modifier = modifier.fillMaxWidth().height(88.dp),
        shape = RoundedCornerShape(LauncherTheme.layout.cornerRadius),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Carátula + textos: tocar abre la app que suena
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = nowPlaying != null) { nowPlaying?.let { actions.onOpenPlayer(it.packageName) } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val art = nowPlaying?.art
                if (art != null) {
                    Image(
                        art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("♪", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            nowPlaying == null -> stringResource(R.string.media_nothing_playing)
                            else -> nowPlaying.title.ifEmpty { stringResource(R.string.media_unknown_title) }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    nowPlaying?.artist?.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (nowPlaying != null) {
                FilledTonalIconButton(onClick = actions::onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(LauncherIcons.Previous, stringResource(R.string.media_previous), Modifier.size(30.dp))
                }
                FilledIconButton(onClick = actions::onPlayPause, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (nowPlaying.isPlaying) LauncherIcons.Pause else LauncherIcons.Play,
                        stringResource(if (nowPlaying.isPlaying) R.string.media_pause else R.string.media_play),
                        Modifier.size(36.dp),
                    )
                }
                FilledTonalIconButton(onClick = actions::onNext, modifier = Modifier.size(56.dp)) {
                    Icon(LauncherIcons.Next, stringResource(R.string.media_next), Modifier.size(30.dp))
                }
            }
        }
    }
}
