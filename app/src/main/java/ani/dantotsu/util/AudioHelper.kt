package ani.dantotsu.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer

class AudioHelper(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null

    fun routeAudioToSpeaker() {
        audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
    }

    private val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private var oldVolume: Int = 0
    fun setVolume(percentage: Int) {
        oldVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volume = (maxVolume * percentage) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    fun playAudio(audio: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, audio)
        mediaPlayer?.setOnCompletionListener {
            setVolume(oldVolume)
            audioManager.abandonAudioFocus(null)
            it.release()
        }
        mediaPlayer?.setOnPreparedListener {
            it.start()
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    companion object {
        fun run(context: Context, audio: Int) {
            val audioHelper = AudioHelper(context)
            audioHelper.routeAudioToSpeaker()
            audioHelper.setVolume(90)
            audioHelper.playAudio(audio)
        }
    }
}