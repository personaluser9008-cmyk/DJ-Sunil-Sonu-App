package com.djsunil

import android.app.Application
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class LibraryTab { ALL, FAVORITES }

@OptIn(UnstableApi::class)
class AudioEffectsController(private val player: ExoPlayer) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _eqAvailable = MutableStateFlow(false)
    private val _eqEnabled = MutableStateFlow(false)
    private val _bandFreqs = MutableStateFlow<List<Int>>(emptyList())
    private val _bandLevels = MutableStateFlow<List<Int>>(emptyList())
    private val _bandRange = MutableStateFlow(-1500 to 1500)
    private val _presets = MutableStateFlow<List<String>>(emptyList())
    private val _selectedPreset = MutableStateFlow(-1)
    private val _bassSupported = MutableStateFlow(false)
    private val _bassStrength = MutableStateFlow(0)

    val eqAvailable: StateFlow<Boolean> = _eqAvailable.asStateFlow()
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()
    val bandFreqs: StateFlow<List<Int>> = _bandFreqs.asStateFlow()
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()
    val bandRange: StateFlow<Pair<Int, Int>> = _bandRange.asStateFlow()
    val presets: StateFlow<List<String>> = _presets.asStateFlow()
    val selectedPreset: StateFlow<Int> = _selectedPreset.asStateFlow()
    val bassSupported: StateFlow<Boolean> = _bassSupported.asStateFlow()
    val bassStrength: StateFlow<Int> = _bassStrength.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attach(audioSessionId)
        }
    }

    init {
        player.addListener(listener)
        attach(player.audioSessionId)
    }

    private fun attach(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return
        releaseEffects()
        try {
            val eq = Equalizer(0, sessionId)
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minL = range[0].toInt()
            val maxL = range[1].toInt()
            val saved = _bandLevels.value
            val levels = if (saved.size == bands) {
                saved.map { it.coerceIn(minL, maxL) }
            } else {
                List(bands) { eq.getBandLevel(it.toShort()).toInt() }
            }
            levels.forEachIndexed { i, l -> eq.setBandLevel(i.toShort(), l.toShort()) }
            eq.setEnabled(_eqEnabled.value)
            _bandFreqs.value = List(bands) { eq.getCenterFreq(it.toShort()) / 1000 }
            _bandLevels.value = levels
            _bandRange.value = minL to maxL
            _presets.value = List(eq.numberOfPresets.toInt()) { eq.getPresetName(it.toShort()) }
            equalizer = eq
            _eqAvailable.value = true
        } catch (t: Throwable) {
            equalizer = null
            _eqAvailable.value = false
        }
        try {
            val bb = BassBoost(0, sessionId)
            _bassSupported.value = bb.strengthSupported
            if (bb.strengthSupported) {
                bb.setStrength(_bassStrength.value.toShort())
                bb.setEnabled(_bassStrength.value > 0)
            }
            bassBoost = bb
        } catch (t: Throwable) {
            bassBoost = null
            _bassSupported.value = false
        }
    }

    fun setEqEnabled(on: Boolean) {
        _eqEnabled.value = on
        try { equalizer?.setEnabled(on) } catch (_: Throwable) {}
    }

    fun setBandLevel(index: Int, level: Int) {
        val (mn, mx) = _bandRange.value
        val l = level.coerceIn(mn, mx)
        _bandLevels.value = _bandLevels.value.toMutableList().also { if (index in it.indices) it[index] = l }
        _selectedPreset.value = -1
        try { equalizer?.setBandLevel(index.toShort(), l.toShort()) } catch (_: Throwable) {}
    }

    fun usePreset(index: Int) {
        val eq = equalizer ?: return
        try {
            eq.usePreset(index.toShort())
            _bandLevels.value = List(eq.numberOfBands.toInt()) { eq.getBandLevel(it.toShort()).toInt() }
            _selectedPreset.value = index
            if (!_eqEnabled.value) setEqEnabled(true)
        } catch (_: Throwable) {}
    }

    fun setBassStrength(strength: Int) {
        val s = strength.coerceIn(0, 1000)
        _bassStrength.value = s
        val bb = bassBoost ?: return
        try {
            if (bb.strengthSupported) {
                bb.setStrength(s.toShort())
                bb.setEnabled(s > 0)
            }
        } catch (_: Throwable) {} // Yeh line cut ho gayi thi, isko maine fix kar diya hai
    }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (_: Throwable) {}
        equalizer = null
        try { bassBoost?.release() } catch (_: Throwable) {}
        bassBoost = null
        _eqAvailable.value = false
        _bassSupported.value = false
    }

    fun release() {
        player.removeListener(listener)
        releaseEffects()
    }
}

// Yahan se aage ka ViewModel ka hissa maine pura likh diya hai
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    
    private val fetcher = MusicFetcher(application)
    private val favoriteDao = AppDatabase.get(application).favoriteDao()
    val player = PlayerHolder.player(application)
    val effects = PlayerHolder.effects(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _currentTab = MutableStateFlow(LibraryTab.ALL)
    val currentTab: StateFlow<LibraryTab> = _currentTab.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            updateCurrentSong(mediaItem)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _durationMs.value = player.duration.coerceAtLeast(0L)
            }
        }
    }

    init {
        player.addListener(playerListener)
        loadSongs()
        observeFavorites()
        startProgressTracker()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _songs.value = fetcher.fetchSongs()
            updateCurrentSong(player.currentMediaItem)
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoriteDao.observeIds().collect { ids ->
                _favorites.value = ids.toSet()
            }
        }
    }

    private fun startProgressTracker() {
        viewModelScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    _progressMs.value = player.currentPosition.coerceAtLeast(0L)
                }
                delay(1000)
            }
        }
    }

    private fun updateCurrentSong(mediaItem: androidx.media3.common.MediaItem?) {
        val id = mediaItem?.mediaId?.toLongOrNull()
        if (id != null) {
            _currentSong.value = _songs.value.find { it.id == id }
        } else {
            _currentSong.value = null
        }
        _durationMs.value = player.duration.coerceAtLeast(0L)
        _progressMs.value = player.currentPosition.coerceAtLeast(0L)
    }

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
    }

    fun playSong(song: Song) {
        val playlist = if (_currentTab.value == LibraryTab.FAVORITES) {
            _songs.value.filter { _favorites.value.contains(it.id) }
        } else {
            _songs.value
        }
        
        if (playlist.isEmpty()) return
        
        val startIndex = playlist.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        
        player.setMediaItems(playlist.map { it.toMediaItem() }, startIndex, C.TIME_UNSET)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun skipNext() = player.seekToNextMediaItem()
    
    fun skipPrevious() = player.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _progressMs.value = positionMs
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_favorites.value.contains(songId)) {
                favoriteDao.remove(songId)
            } else {
                favoriteDao.add(FavoriteEntity(songId))
            }
        }
    }

    override fun onCleared() {
        player.removeListener(playerListener)
        super.onCleared()
    }
}
