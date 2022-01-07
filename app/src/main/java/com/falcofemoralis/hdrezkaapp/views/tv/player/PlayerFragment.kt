package com.falcofemoralis.hdrezkaapp.views.tv.player

import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackGlue
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.models.FilmModel
import com.falcofemoralis.hdrezkaapp.objects.*
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment
import com.falcofemoralis.hdrezkaapp.views.tv.player.seek.StoryboardSeekDataProvider
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule


class PlayerFragment : VideoSupportFragment() {
    /* Player elements */
    private var mPlayerGlue: VideoPlayerGlue? = null
    private var mPlayerAdapter: LeanbackPlayerAdapter? = null
    var mPlayer: SimpleExoPlayer? = null
    private var mTrackSelector: TrackSelector? = null
    var mPlaybackActionListener: PlaybackActionListener? = null
    var mSpeed: Float = SPEED_START_VALUE
    private var mSubtitles: SubtitleView? = null
    private val mSubtitleSize: Int = 100
    var selectedSubtitle: Int = 0
    var selectedQuality: Int = 0
    private var playbackPositionManager: PlaybackPositionManager? = null

    /* Data elements */
    private var mFilm: Film? = null
    private var mStream: Stream? = null
    var mTranslation: Voice? = null
    var mPlaylist: Playlist = Playlist()
    var lastPosition: Long = 0L

    /* Variable elements */
    private var title: String? = null
    private var isSerial: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeData()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || mPlayer == null) {
            initializePlayer()
        }

        hideNavigation()
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()

        if (mPlayerGlue != null && mPlayerGlue?.isPlaying == true) {
            mPlayerGlue?.pause()
        }

        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()

        saveCurrentTime()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializeData() {
        mFilm = activity?.intent?.getSerializableExtra(PlayerActivity.FILM) as Film
        mStream = activity?.intent?.getSerializableExtra(PlayerActivity.STREAM) as Stream
        mTranslation = activity?.intent?.getSerializableExtra(PlayerActivity.TRANSLATION) as Voice
        val mSelectedSubtitle: Subtitle? = activity?.intent?.getSerializableExtra(PlayerActivity.SUBTITLE) as Subtitle?

        if (mTranslation?.seasons != null && mTranslation?.seasons!!.size > 0) {
            isSerial = true
            val seasons: LinkedHashMap<String, ArrayList<String>> = mTranslation?.seasons!!
            for ((season, episodes) in seasons) {
                for (episode in episodes) {
                    val item = Playlist.PlaylistItem(season, episode)
                    mPlaylist.add(item)

                    if (season == mTranslation?.selectedEpisode?.first && episode == mTranslation?.selectedEpisode?.second) {
                        mPlaylist.setCurrentPosition(mPlaylist.size() - 1)
                    }
                }
            }
            title = "${mFilm?.title} Сезон ${mTranslation?.selectedEpisode?.first} - Эпизод ${mTranslation?.selectedEpisode?.second}"
        } else {
            isSerial = false
            mFilm?.title?.let { title = it }
        }

        val mAvaliableStreams = mTranslation?.streams
        if (mAvaliableStreams != null && mStream != null) {
            for (ix in 0 until mAvaliableStreams.size) {
                if (mAvaliableStreams[ix].quality == mStream!!.quality) {
                    selectedQuality = ix
                }
            }
        }

        selectedSubtitle = if (mSelectedSubtitle != null && mTranslation?.subtitles != null && mTranslation!!.subtitles!!.size > 0) {
            var selSub = -1
            for ((i, sub) in mTranslation?.subtitles!!.withIndex()) {
                if (sub.lang == mSelectedSubtitle!!.lang) {
                    selSub = i
                }
            }
            selSub
        } else if (mTranslation?.subtitles != null && mTranslation!!.subtitles!!.size > 0) {
            0
        } else {
            -1
        }

        if (playbackPositionManager == null) {
            playbackPositionManager = PlaybackPositionManager(requireContext(), isSerial)
        }

        lastPosition = if (isSerial && playbackPositionManager != null) {
            playbackPositionManager!!.getLastTime(mFilm?.filmId, mTranslation?.id, mTranslation?.selectedEpisode?.first, mTranslation?.selectedEpisode?.second)
        } else {
            playbackPositionManager!!.getLastTime(mFilm?.filmId, mTranslation?.id)
        }
    }

    private fun initializePlayer() {
        val renderFactory = DefaultRenderersFactory(requireContext())
        renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        renderFactory.setEnableDecoderFallback(true)
        mTrackSelector = DefaultTrackSelector(requireContext())
        mPlayer = SimpleExoPlayer.Builder(requireContext(), renderFactory).setTrackSelector(mTrackSelector as DefaultTrackSelector).build()

        mSubtitles = requireActivity().findViewById(R.id.leanback_subtitles)
        val textComponent = mPlayer?.textComponent
        if (textComponent != null && mSubtitles != null) {
            mSubtitles?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * mSubtitleSize / 100.0f)
/*            mSubtitles?.setApplyEmbeddedFontSizes(false)
            mSubtitles?.setApplyEmbeddedStyles(false)
            mSubtitles?.setFixedTextSize(TEXT_SIZE_TYPE_ABSOLUTE, 30F)
            mSubtitles?.setBottomPaddingFraction(.1F)

            val style = CaptionStyleCompat(
                Color.WHITE, Color.GRAY, Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_NONE, Color.TRANSPARENT, null)
            mSubtitles?.setStyle(style)*/
            textComponent.addTextOutput(mSubtitles!!)
        }

        if (mPlaybackActionListener == null) {
            mPlaybackActionListener = PlaybackActionListener(this)
        }
        mPlayerAdapter = LeanbackPlayerAdapter(requireActivity(), mPlayer!!, UPDATE_DELAY)
        mPlayerGlue = VideoPlayerGlue(activity, mPlayerAdapter, mPlaybackActionListener!!, isSerial)
        mPlayerGlue?.host = VideoSupportFragmentGlueHost(this)
        mPlayerGlue?.isSeekEnabled = true
        mPlayerGlue?.isControlsOverlayAutoHideEnabled = false
        //  StoryboardSeekDataProvider.setSeekProvider(mTranslation!!, mPlayerGlue!!, requireContext())

        if (mPlayerGlue?.isPrepared == true) {
            mPlayerGlue?.seekProvider = StoryboardSeekDataProvider(mTranslation!!, requireContext())
        } else {
            mPlayerGlue?.addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
                override fun onPreparedStateChanged(glue: PlaybackGlue) {
                    if (mPlayerGlue?.isPrepared == true) {
                        mPlayerGlue?.removePlayerCallback(this)
                        mPlayerGlue?.seekProvider = StoryboardSeekDataProvider(mTranslation!!, requireContext())

                        if (lastPosition > 0) {
                            mPlayerGlue?.seekTo(lastPosition)
                        }
                    }
                }
            })
        }
        mPlayerGlue?.playWhenPrepared()

        hide()

        initAutoSave()

        mStream?.let { play(it) }
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            mPlayer!!.release()
            mPlayer = null
            mTrackSelector = null
            mPlayerGlue = null
            mPlayerAdapter = null
            mPlaybackActionListener = null
        }
    }

    private fun initAutoSave() {
        if (mPlayerGlue?.getCurrentPos() != 0L) {
            Timer("ProgressSave", false).schedule(SAVE_EVERY_5_MIN) {
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        saveCurrentTime()
                        initAutoSave()
                    }
                }
            }
        }
    }

    private fun play(stream: Stream) {
        mPlayerGlue?.title = title
        mPlayerGlue?.subtitle = mFilm?.description
        prepareMediaForPlaying(stream.url, mTranslation?.subtitles?.get(selectedSubtitle)?.url, true)
        mPlayerGlue?.play()
    }

    private fun play(playlistItem: Playlist.PlaylistItem) {
        mPlayerGlue?.title = "${mFilm?.title} Сезон ${playlistItem.season} - Эпизод ${playlistItem.episode}"
        mPlayerGlue?.subtitle = mFilm?.description

        GlobalScope.launch {
            FilmModel.getStreamsByEpisodeId(mTranslation!!, mFilm?.filmId!!, playlistItem.season, playlistItem.episode)

            withContext(Dispatchers.Main) {
                for (stream in mTranslation?.streams!!) {
                    if (stream.quality == mStream?.quality) {
                        mStream = stream
                        mTranslation?.selectedEpisode = Pair(playlistItem.season, playlistItem.episode)
                        mPlayerGlue?.seekProvider = StoryboardSeekDataProvider(mTranslation!!, requireContext())
                        prepareMediaForPlaying(stream.url, mTranslation?.subtitles?.get(selectedSubtitle)?.url, true)
                        mPlayerGlue?.play()
                        break
                    }
                }
            }
        }
    }

    private fun prepareMediaForPlaying(mediaSourceUri: String, subtitleUri: String?, resetPosition: Boolean) {
      //  val userAgent = Util.getUserAgent(requireActivity(), "VideoPlayerGlue")
        val factory = DefaultDataSourceFactory(requireActivity(), SettingsData.useragent)
        val mediaSource: MediaSource = ProgressiveMediaSource
            .Factory(factory)
            .setExtractorsFactory(DefaultExtractorsFactory())
            .createMediaSource(Uri.parse(mediaSourceUri))

        if (subtitleUri != null && subtitleUri.isNotEmpty()) {
            // create subtitle text format
            val textFormat = Format.Builder().setSampleMimeType(MimeTypes.TEXT_VTT).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()

            // create the subtitle source
            val subtitleSource = SingleSampleMediaSource
                .Factory(factory)
                .createMediaSource(Uri.parse(subtitleUri), textFormat, C.TIME_UNSET)

            val mergedSource = MergingMediaSource(mediaSource, subtitleSource)

            mPlayer?.prepare(mergedSource, resetPosition, true)
        } else {
            mPlayer?.prepare(mediaSource, resetPosition, true)
        }
    }

    fun skipToNext() {
        mPlaylist.next()?.let { play(it) }
        updateWatchLater()
    }

    fun skipToPrevious() {
        mPlaylist.previous()?.let { play(it) }
        updateWatchLater()
    }

    fun updateWatchLater() {
        val item: Playlist.PlaylistItem = mPlaylist.getCurrentItem()
        mTranslation?.selectedEpisode = Pair(item.season, item.episode)

        if (UserData.isLoggedIn == true) {
            mTranslation?.let { FilmFragment.presenter?.updateWatchLater(it) }
        }
    }

    fun rewind() {
        mPlayerGlue?.rewind()
    }

    fun fastForward() {
        mPlayerGlue?.fastForward()
    }

    fun hide() {
        hideControlsOverlay(false)
    }

    fun hideNavigation() {
        if (requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            val view = view
            view!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }


    // trackSelection = current selection. -1 = disabled, -2 = leave as is
    // disable = true : Include disabled in the rotation
    // doChange = true : select a new track, false = leave same track
    // Return = new track selection.

    fun subtitleSelector(selected: Int) {
        if (selected == selectedSubtitle) {
            return
        }
        selectedSubtitle = selected

        if (mTranslation != null && mTranslation?.streams != null) {
            if (selectedSubtitle >= 0) {
                if (mTranslation?.subtitles != null) {
                    if (selectedSubtitle < mTranslation?.subtitles!!.size) {
                        val subtitleUrl = mTranslation?.subtitles!!.get(selectedSubtitle).url
                        mStream?.let { prepareMediaForPlaying(mTranslation?.streams!![selectedQuality].url, subtitleUrl, false) }
                    }
                }
            } else if (selectedSubtitle == -1) {
                mStream?.let { prepareMediaForPlaying(mTranslation?.streams!![selectedQuality].url, null, false) }
            }
        }

/*        val curPos = mPlayerGlue?.currentPosition
        mPlayerGlue?.play()
        if (curPos != null) {
            mPlayerGlue?.seekTo(curPos)
        }*/
    }

    fun qualitySelector(selected: Int) {
        if (selected == selectedQuality) {
            return
        }
        selectedQuality = selected
        val stream = mTranslation?.streams?.get(selected)

        if (stream != null) { // && mTranslation?.subtitles != null
            val subtitleUrl = if (selectedSubtitle >= 0) {
                mTranslation?.subtitles!![selectedSubtitle].url
            } else {
                null
            }
            prepareMediaForPlaying(stream.url, subtitleUrl, false)
        }
    }

    fun onDispatchKeyEvent(event: KeyEvent?) {
        // NOP
    }

    fun onDispatchTouchEvent(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            tickle()
        }
    }

    private fun saveCurrentTime() {
        val lastPos = mPlayerGlue?.getCurrentPos()
        if (lastPos != null) {
            // save
            if (isSerial) {
                playbackPositionManager?.updateTime(mFilm?.filmId, mTranslation?.id, mTranslation?.selectedEpisode?.first, mTranslation?.selectedEpisode?.second, lastPos)
            } else {
                playbackPositionManager?.updateTime(mFilm?.filmId, mTranslation?.id, lastPos)
            }
        }
    }


    companion object {
        const val UPDATE_DELAY = 16
        const val SPEED_START_VALUE = 1.0f
        val SAVE_EVERY_5_MIN = TimeUnit.MINUTES.toMillis(5)
    }
}