package com.tinashe.hymnal.ui.hymns.sing.player

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.tinashe.hymnal.data.model.constants.Hymnals
import com.tinashe.hymnal.extensions.arch.SingleLiveEvent
import com.tinashe.hymnal.extensions.arch.asLiveData
import com.tinashe.hymnal.extensions.prefs.HymnalPrefs
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class SimpleTunePlayer @Inject constructor(
    private val context: Context,
    private val prefs: HymnalPrefs
) : DefaultLifecycleObserver {

    private val mutablePlaybackState = SingleLiveEvent<PlaybackState>()
    val playbackLiveData: LiveData<PlaybackState> = mutablePlaybackState.asLiveData()

    @VisibleForTesting
    var mediaPlayer: MediaPlayer? = null

    private val isPlaying: Boolean get() = mediaPlayer?.isPlaying == true

    fun canPlayTune(number: Int): Boolean = try {
        if (isPlaying) {
            stopMedia()
        }
        context.assets.openFd("$FOLDER/$number$EXTENSION")
        !unavailableCodes.contains(prefs.getSelectedHymnal())
    } catch (ex: IOException) {
        false
    }

    fun togglePlayTune(number: Int) {
        if (isPlaying) {
            stopMedia()
            return
        }

        try {
            mediaPlayer = MediaPlayer()

            val afd = context.assets.openFd("$FOLDER/$number$EXTENSION")
            mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mediaPlayer?.setOnCompletionListener {
                mutablePlaybackState.postValue(PlaybackState.ON_COMPLETE)
            }
            mediaPlayer?.setOnPreparedListener {
                mediaPlayer?.start()
                mutablePlaybackState.postValue(PlaybackState.ON_PLAY)
            }
            mediaPlayer?.prepare()
        } catch (ex: Exception) {
            Timber.e(ex)
            mutablePlaybackState.postValue(PlaybackState.ON_STOP)
        }
    }

    fun stopMedia() {
        if (isPlaying) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
        mutablePlaybackState.postValue(PlaybackState.ON_STOP)
    }

    override fun onPause(owner: LifecycleOwner) {
        stopMedia()
        super.onPause(owner)
    }

    companion object {
        private const val FOLDER = "midis"
        private const val EXTENSION = ".mid"

        /**
         * Add here [Hymnals] that do not have audio matching the original [Hymnals.ENGLISH].
         */
        private val unavailableCodes = listOf(
            Hymnals.SWAHILI,
            Hymnals.ABAGUSII,
            Hymnals.GIKUYU,
            Hymnals.DHOLUO,
            Hymnals.SDAH
        ).map { it.key }
    }
}
