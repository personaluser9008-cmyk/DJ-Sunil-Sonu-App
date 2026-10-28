package com.djsunil

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFF5F5F5), // Pure Colorless Light Gray
                    surface = Color.Transparent,
                    onSurface = Color.Black,
                    primary = Color.Black
                )
            ) {
                MusicApp()
            }
        }
    }
}

// ==========================================
// PURE WATER GLASS MODIFIER
// ==========================================
fun Modifier.waterGlass() = this
    .shadow(12.dp, spotColor = Color.Black.copy(alpha = 0.15f), shape = RoundedCornerShape(24.dp))
    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
    .clip(RoundedCornerShape(24.dp))

// ==========================================
// MAIN UI COMPONENTS
// ==========================================
@Composable
fun MusicApp(viewModel: MusicViewModel = viewModel()) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    var showFullPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Request Permissions
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {
        // Subtle Background Animation (Colorless)
        WaterBackground()

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                text = "DJ SUNIL SONU",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier.padding(24.dp)
            )

            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Fetching Songs...", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(songs) { song ->
                        SongItem(song, currentSong?.id == song.id) {
                            viewModel.playSong(song)
                        }
                    }
                }
            }
        }

        // Mini Player
        if (currentSong != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                MiniPlayer(currentSong!!, isPlaying, onPlayPause = { viewModel.togglePlayPause() }) {
                    showFullPlayer = true
                }
            }
        }

        // Full Player Screen
        AnimatedVisibility(
            visible = showFullPlayer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullPlayerScreen(viewModel, onClose = { showFullPlayer = false }, onOpenSettings = { showSettings = true })
        }

        // DJ Tools / Settings Dialog
        if (showSettings) {
            SettingsDialog(viewModel) { showSettings = false }
        }
    }
}

@Composable
fun SongItem(song: Song, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .waterGlass()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.GraphicEq else Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
            Text(song.artist, fontSize = 12.sp, color = Color.DarkGray, maxLines = 1)
        }
    }
}

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onPlayPause: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .waterGlass()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Album, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun FullPlayerScreen(viewModel: MusicViewModel, onClose: () -> Unit, onOpenSettings: () -> Unit) {
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).statusBarsPadding()) {
        WaterBackground()
        
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose, modifier = Modifier.waterGlass()) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.Black)
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.waterGlass()) {
                    Icon(Icons.Default.Tune, contentDescription = "EQ", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Pure Water Vinyl Record (Drawn with Canvas)
            Box(
                modifier = Modifier.size(280.dp).waterGlass().padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    drawCircle(color = Color.Black.copy(alpha = 0.8f), radius = size.minDimension / 2)
                    for (i in 1..4) {
                        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = (size.minDimension / 2) - (i * 20.dp.toPx()), style = Stroke(width = 2f))
                    }
                    drawCircle(color = Color(0xFFF5F5F5), radius = 30.dp.toPx()) // Center hole
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(song?.title ?: "No Song", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, maxLines = 1)
            Text(song?.artist ?: "Unknown", fontSize = 16.sp, color = Color.DarkGray, maxLines = 1)

            Spacer(modifier = Modifier.height(40.dp))

            Slider(
                value = progress.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Black, activeTrackColor = Color.Black, inactiveTrackColor = Color.LightGray)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.skipPrevious() }, modifier = Modifier.waterGlass().size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.waterGlass().size(80.dp)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { viewModel.skipNext() }, modifier = Modifier.waterGlass().size(56.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val bassStrength by viewModel.effects.bassStrength.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.waterGlass().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DJ TOOLS & EQ", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Bass Boost", fontWeight = FontWeight.Bold, color = Color.Black)
            Slider(
                value = bassStrength.toFloat(),
                onValueChange = { viewModel.effects.setBassStrength(it.toInt()) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(thumbColor = Color.Black, activeTrackColor = Color.Black)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss, 
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.border(1.dp, Color.Black, RoundedCornerShape(50))
            ) {
                Text("Close", color = Color.Black)
            }
        }
    }
}

@Composable
fun WaterBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = ""
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = Color.White, radius = 200.dp.toPx(), center = Offset(size.width * 0.2f, size.height * 0.1f + yOffset))
        drawCircle(color = Color.White, radius = 250.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.8f - yOffset))
    }
}
