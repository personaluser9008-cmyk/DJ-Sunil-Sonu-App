package com.djsunil

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MusicApp()
            }
        }
    }
}

// ==========================================
// ULTRA LIQUID GLASS MODIFIER (Transparent & Smooth)
// ==========================================
fun Modifier.liquidGlass() = this
    .shadow(16.dp, spotColor = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
    .clip(RoundedCornerShape(24.dp))
    .background(Color.White.copy(alpha = 0.15f)) // Translucent Pani Jaisa
    .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))

fun Modifier.liquidGlassCircle() = this
    .shadow(16.dp, spotColor = Color.Black.copy(alpha = 0.5f), shape = CircleShape)
    .clip(CircleShape)
    .background(Color.White.copy(alpha = 0.15f))
    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)

// ==========================================
// MAIN APP UI
// ==========================================
@Composable
fun MusicApp(viewModel: MusicViewModel = viewModel()) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    
    var showFullPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.loadSongs()
    }

    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Deep Animated Gradient Background to make Glass visible
        AnimatedGradientBackground()

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            
            // Top Bar
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DJ SUNIL SONU", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                IconButton(onClick = { showSettings = true }, modifier = Modifier.liquidGlassCircle()) {
                    Icon(Icons.Default.Tune, contentDescription = "Settings", tint = Color.White)
                }
            }

            // Categories / Library Tabs (Liquid Glass)
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { CategoryTab("All Songs", currentTab == LibraryTab.ALL) { viewModel.setTab(LibraryTab.ALL) } }
                item { CategoryTab("Favorites ❤️", currentTab == LibraryTab.FAVORITES) { viewModel.setTab(LibraryTab.FAVORITES) } }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Song List
            val displaySongs = if (currentTab == LibraryTab.FAVORITES) {
                songs.filter { viewModel.favorites.value.contains(it.id) }
            } else songs

            if (displaySongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Songs Found...", color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 120.dp)) {
                    items(displaySongs) { song ->
                        val isFav = viewModel.favorites.collectAsState().value.contains(song.id)
                        SongItem(song, currentSong?.id == song.id, isFav, onPlay = { viewModel.playSong(song) }, onFav = { viewModel.toggleFavorite(song.id) })
                    }
                }
            }
        }

        // Floating Mini Player (Glass)
        if (currentSong != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                MiniPlayer(currentSong!!, isPlaying, onPlayPause = { viewModel.togglePlayPause() }) {
                    showFullPlayer = true
                }
            }
        }

        // Full Player Screen (Animated)
        AnimatedVisibility(visible = showFullPlayer, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
            FullPlayerScreen(viewModel, onClose = { showFullPlayer = false })
        }

        // Settings Dialog (DJ Tools)
        if (showSettings) {
            SettingsDialog(viewModel) { showSettings = false }
        }
    }
}

@Composable
fun CategoryTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .then(if (isSelected) Modifier.liquidGlass() else Modifier.background(Color.Transparent))
            .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun SongItem(song: Song, isPlaying: Boolean, isFav: Boolean, onPlay: () -> Unit, onFav: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .liquidGlass()
            .clickable { onPlay() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(song.artist, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
        }
        IconButton(onClick = onFav) {
            Icon(imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Fav", tint = if (isFav) Color.Red else Color.White)
        }
    }
}

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onPlayPause: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f).liquidGlass().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Album, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
        IconButton(onClick = onPlayPause, modifier = Modifier.liquidGlassCircle()) {
            Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun FullPlayerScreen(viewModel: MusicViewModel, onClose: () -> Unit) {
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progressMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()

    var rotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            rotation += 1f
            if (rotation >= 360f) rotation = 0f
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AnimatedGradientBackground()
        
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose, modifier = Modifier.liquidGlassCircle()) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Spinning Glass Record
            Box(modifier = Modifier.size(300.dp).liquidGlassCircle().padding(12.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = size.minDimension / 2)
                    drawCircle(color = Color.White.copy(alpha = 0.3f), radius = size.minDimension / 2.5f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 30.dp.toPx()) // Center hole
                }
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(50.dp))

            Text(song?.title ?: "No Song", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
            Text(song?.artist ?: "Unknown", fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)

            Spacer(modifier = Modifier.height(40.dp))

            Slider(
                value = progress.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.skipPrevious() }, modifier = Modifier.liquidGlassCircle().size(64.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.liquidGlassCircle().size(84.dp)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { viewModel.skipNext() }, modifier = Modifier.liquidGlassCircle().size(64.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val bassStrength by viewModel.effects.bassStrength.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass()
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DJ CONTROL PANEL", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
                Spacer(modifier = Modifier.height(30.dp))
                
                Text("Super Bass Boost", fontWeight = FontWeight.Bold, color = Color.White)
                Slider(
                    value = bassStrength.toFloat(),
                    onValueChange = { viewModel.effects.setBassStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                )
                
                Spacer(modifier = Modifier.height(30.dp))
                Button(
                    onClick = onDismiss, 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White, RoundedCornerShape(50))
                ) {
                    Text("Apply & Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF0F2027), targetValue = Color(0xFF203A43),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = ""
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF203A43), targetValue = Color(0xFF2C5364),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = ""
    )
    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(color1, color2))))
}
