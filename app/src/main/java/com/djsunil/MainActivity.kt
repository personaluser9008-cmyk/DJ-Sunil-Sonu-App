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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

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
// FROSTED GLASS EFFECT (Same as Image)
// ==========================================
fun Modifier.frostedGlass(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(32.dp)) = this
    .shadow(12.dp, shape, spotColor = Color(0x33000000), ambientColor = Color(0x33000000))
    .background(Color.White.copy(alpha = 0.20f), shape)
    .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
        ),
        shape = shape
    )
    .clip(shape)

val BgColor = Color(0xFF8E7CEA) // Purple Background
val PlayGradient = Brush.linearGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF004D))) // Orange/Pink Gradient

// ==========================================
// MAIN APP UI
// ==========================================
@Composable
fun MusicApp(viewModel: MusicViewModel = viewModel()) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    var showPlayerScreen by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Glowing 3D Background Blobs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF6B58D6), Color.Transparent)), radius = 900f, center = Offset(0f, 0f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFC078E6).copy(alpha=0.6f), Color.Transparent)), radius = 1100f, center = Offset(size.width, size.height))
        }

        AnimatedContent(
            targetState = showPlayerScreen, 
            label = "ScreenTransition",
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            }
        ) { isPlayer ->
            if (isPlayer && currentSong != null) {
                PlayerScreen(viewModel, currentSong!!) { showPlayerScreen = false }
            } else {
                LibraryScreen(viewModel, songs, currentSong, isPlaying) { showPlayerScreen = true }
            }
        }
    }
}

// ==========================================
// LIBRARY SCREEN (Right Side of Image)
// ==========================================
@Composable
fun LibraryScreen(viewModel: MusicViewModel, songs: List<Song>, currentSong: Song?, isPlaying: Boolean, onOpenPlayer: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // Top Bar
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { }, modifier = Modifier.size(50.dp).frostedGlass(CircleShape)) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
                }
                IconButton(onClick = { }, modifier = Modifier.size(50.dp).frostedGlass(CircleShape)) {
                    Icon(Icons.Rounded.GridView, contentDescription = "Menu", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Collections", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Categories
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(3) { index ->
                    val title = if(index == 0) "All Songs" else if(index == 1) "Favorites" else "DJ Mixes"
                    Column {
                        Box(modifier = Modifier.size(110.dp).frostedGlass(RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Recommended", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
            Spacer(modifier = Modifier.height(16.dp))

            // Vertical Song List
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(songs) { song ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).frostedGlass(RoundedCornerShape(20.dp)).clickable { viewModel.playSong(song) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                            Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
                        }
                        Box(modifier = Modifier.size(44.dp).frostedGlass(CircleShape), contentAlignment = Alignment.Center) {
                            Icon(if (currentSong?.id == song.id && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // Floating Bottom Player
        if (currentSong != null) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth().height(85.dp).frostedGlass(RoundedCornerShape(40.dp)).clickable { onOpenPlayer() }.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(54.dp).background(PlayGradient, CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentSong.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 16.sp)
                    Text(currentSong.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
                }
                IconButton(onClick = { viewModel.skipNext() }, modifier = Modifier.size(44.dp).frostedGlass(CircleShape)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

// ==========================================
// PLAYER SCREEN (Left Side of Image)
// ==========================================
@Composable
fun PlayerScreen(viewModel: MusicViewModel, song: Song, onClose: () -> Unit) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progressMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        
        // Top Bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(50.dp).frostedGlass(CircleShape)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Listen now", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = { }, modifier = Modifier.size(50.dp).frostedGlass(CircleShape)) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "Menu", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Large Album Art Glass Square
        Box(modifier = Modifier.size(300.dp).frostedGlass(RoundedCornerShape(48.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF9C27B0))))) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.Center).size(100.dp))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Song Info
        Text(song.title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
        Spacer(modifier = Modifier.height(8.dp))
        Text(song.artist, fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)

        Spacer(modifier = Modifier.height(40.dp))

        // Modern Slider
        Slider(
            value = progress.toFloat(),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFFFF5722),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Playback Controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.skipPrevious() }, modifier = Modifier.size(64.dp).frostedGlass(CircleShape)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            
            // Glowing Play Button
            Box(
                modifier = Modifier.size(88.dp).background(PlayGradient, CircleShape).shadow(16.dp, CircleShape, spotColor = Color(0xFFFF004D)).clickable { viewModel.togglePlayPause() }, 
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            
            IconButton(onClick = { viewModel.skipNext() }, modifier = Modifier.size(64.dp).frostedGlass(CircleShape)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}
