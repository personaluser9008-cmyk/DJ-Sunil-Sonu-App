package com.djsunil

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide holder so the ViewModel, the service, the audio effects and the
 * sleep timer all talk to the SAME ExoPlayer instance.
 */
@OptIn(UnstableApi::class)
object PlayerHolder {
    private var player: ExoPlayer? = null
    private var effects: AudioEffectsController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepJob: Job? = null
    private val _sleepRemainingSec = MutableStateFlow(0)
    val sleepRemainingSec: StateFlow<Int> = _sleepRemainingSec.asStateFlow()

    @Synchronized
    fun player(context: Context): ExoPlayer =
        player ?: build(context.applicationContext).also { player = it }

    @Synchronized
    fun effects(context: Context): AudioEffectsController =
        effects ?: AudioEffectsController(player(context)).also { effects = it }

    private fun build(ctx: Context): ExoPlayer {
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val p = ExoPlayer.Builder(ctx)
            .setAudioAttributes(attrs, true)          // audio focus handling
            .setHandleAudioBecomingNoisy(true)        // ACTION_AUDIO_BECOMING_NOISY -> pause
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sessionId = am.generateAudioSessionId()
        if (sessionId != AudioManager.ERROR) p.audioSessionId = sessionId
        return p
    }

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = scope.launch {
            var remaining = minutes * 60
            while (remaining > 0) {
                _sleepRemainingSec.value = remaining
                delay(1000)
                remaining--
            }
            _sleepRemainingSec.value = 0
            player?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepRemainingSec.value = 0
    }

    @Synchronized
    fun release() {
        cancelSleepTimer()
        effects?.release()
        effects = null
        player?.release()
        player = null
    }
}

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlayerHolder.player(this)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.release()
        session = null
        PlayerHolder.release()
        super.onDestroy()
    }
}
