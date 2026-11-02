package com.iappsan.carlauncher.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.media.MediaState
import com.iappsan.carlauncher.data.media.NowPlaying
import com.iappsan.carlauncher.ui.icons.LauncherIcons
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme
import com.iappsan.carlauncher.ui.theme.LauncherTheme

/** Callbacks del widget de música. */
interface MediaActions {
    fun onPlayPause()
    fun onNext()
    fun onPrevious()
    fun onOpenPlayer(packageName: String)
    fun onRequestPermission()
}

@Composable
fun MediaWidget(
    state: MediaState,
    actions: MediaActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(LauncherTheme.layout.cornerRadius),
        color = MaterialTheme.colorScheme.surface,
    ) {
        val nowPlaying = state.nowPlaying
        when {
            !state.hasPermission -> PermissionPanel(onRequest = actions::onRequestPermission)
            nowPlaying == null -> NothingPlaying()
            else -> NowPlayingPanel(nowPlaying, actions)
        }
    }
}

@Composable
private fun PermissionPanel(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("♪", fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.media_permission_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.media_permission_text),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest, modifier = Modifier.heightIn(min = 56.dp)) {
            Text(stringResource(R.string.media_permission_button), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NothingPlaying() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("♪", fontSize = 64.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.media_nothing_playing), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.media_nothing_playing_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NowPlayingPanel(nowPlaying: NowPlaying, actions: MediaActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Carátula + textos ocupan el espacio sobrante; tocarlos abre la app que suena.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { actions.onOpenPlayer(nowPlaying.packageName) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // La carátula es cuadrada y tan grande como permita el espacio, dejando sitio al texto.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side: Dp = min(maxWidth, maxHeight)
                Artwork(nowPlaying, Modifier.size(side))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = nowPlaying.title.ifEmpty { stringResource(R.string.media_unknown_title) },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(nowPlaying.artist, nowPlaying.appLabel).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Controls(isPlaying = nowPlaying.isPlaying, actions = actions)
    }
}

@Composable
private fun Artwork(nowPlaying: NowPlaying, modifier: Modifier) {
    val art = nowPlaying.art
    if (art != null) {
        Image(
            bitmap = art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("♪", fontSize = 56.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/** Botones grandes (64–76dp) para poder pulsarlos sin mirar. */
@Composable
private fun Controls(isPlaying: Boolean, actions: MediaActions) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryControl(LauncherIcons.Previous, stringResource(R.string.media_previous), actions::onPrevious)
        FilledIconButton(onClick = actions::onPlayPause, modifier = Modifier.size(76.dp)) {
            Icon(
                imageVector = if (isPlaying) LauncherIcons.Pause else LauncherIcons.Play,
                contentDescription = stringResource(if (isPlaying) R.string.media_pause else R.string.media_play),
                modifier = Modifier.size(44.dp),
            )
        }
        SecondaryControl(LauncherIcons.Next, stringResource(R.string.media_next), actions::onNext)
    }
}

@Composable
private fun SecondaryControl(icon: ImageVector, description: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(64.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(36.dp))
    }
}

private val PreviewActions = object : MediaActions {
    override fun onPlayPause() {}
    override fun onNext() {}
    override fun onPrevious() {}
    override fun onOpenPlayer(packageName: String) {}
    override fun onRequestPermission() {}
}

@Preview(widthDp = 380, heightDp = 560)
@Composable
private fun MediaWidgetPlayingPreview() {
    CarLauncherTheme {
        MediaWidget(
            state = MediaState(
                hasPermission = true,
                nowPlaying = NowPlaying("pkg", "Spotify", "Canción de prueba", "Artista", null, isPlaying = true),
            ),
            actions = PreviewActions,
        )
    }
}

@Preview(widthDp = 380, heightDp = 560)
@Composable
private fun MediaWidgetPermissionPreview() {
    CarLauncherTheme { MediaWidget(state = MediaState(hasPermission = false), actions = PreviewActions) }
}
