package com.falcofemoralis.hdrezkaapp.views.tv.player

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.leanback.media.PlaybackGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.*
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.ActionConstants
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

class VideoPlayerGlue(
    context: Context?,
    playerAdapter: PlayerAdapter?,
    private val mActionListener: PlaybackActionListener,
    private val isSerial: Boolean
) : PlaybackTransportControlGlue<PlayerAdapter?>(context, playerAdapter) {
    private var mRepeatAction: RepeatAction? = null
    private var mThumbsUpAction: ThumbsUpAction? = null
    private var mThumbsDownAction: ThumbsDownAction? = null
    private var mSkipPreviousAction: SkipPreviousAction? = null
    private var mSkipNextAction: SkipNextAction? = null
    private var mFastForwardAction: FastForwardAction? = null
    private var mRewindAction: RewindAction? = null
    private var mSpeedAction: MyAction? = null
    private var mClosedCaptioningAction: ClosedCaptioningAction? = null
    private var mQualityAction: MyAction? = null

    init {
        mSkipPreviousAction = SkipPreviousAction(context)
        mSkipNextAction = SkipNextAction(context)
        mFastForwardAction = FastForwardAction(context)
        mRewindAction = RewindAction(context)

        mThumbsUpAction = ThumbsUpAction(context)
        mThumbsUpAction?.index = ThumbsUpAction.INDEX_OUTLINE
        mThumbsDownAction = ThumbsDownAction(context)
        mThumbsDownAction?.index = ThumbsDownAction.INDEX_OUTLINE
        mRepeatAction = RepeatAction(context)

        mSpeedAction = MyAction(context!!, ActionConstants.ACTION_SPEEDUP, R.drawable.ic_speed_increase, R.string.button_speedup)
        mQualityAction = MyAction(context!!, ActionConstants.ACTION_SELECT_QUALITY, R.drawable.ic_high_quality, R.string.select_quality)
        mClosedCaptioningAction = ClosedCaptioningAction(context)
        val res = context.resources
        val labels = arrayOfNulls<String>(1)
        labels[0] = res.getString(R.string.button_cc)
        mClosedCaptioningAction?.setLabels(labels)
    }

    override fun onCreatePrimaryActions(adapter: ArrayObjectAdapter) {
        // Order matters, super.onCreatePrimaryActions() will create the play / pause action.
        // Will display as follows:
        // play/pause, previous, rewind, fast forward, next
        //   > /||      |<        <<        >>         >|
        super.onCreatePrimaryActions(adapter)
        adapter.add(mRewindAction)
        adapter.add(mFastForwardAction)

        if (isSerial) {
            adapter.add(mSkipPreviousAction)
            adapter.add(mSkipNextAction)
        }
    }

    override fun onCreateSecondaryActions(adapter: ArrayObjectAdapter) {
        super.onCreateSecondaryActions(adapter)
        adapter.add(mClosedCaptioningAction)
        adapter.add(mQualityAction)
        adapter.add(mSpeedAction)
        // adapter.add(mThumbsDownAction)
        // adapter.add(mThumbsUpAction)
        // adapter.add(mRepeatAction)
    }

    override fun onActionClicked(action: Action?) {
        if (shouldDispatchAction(action)) {
            if (action != null) {
                dispatchAction(action)
            }
            return
        }
        // Super class handles play/pause and delegates to abstract methods next()/previous().
        super.onActionClicked(action)
    }

    // Should dispatch actions that the super class does not supply callbacks for.
    // mRewindAction | mSkipNextAction will return false
    private fun shouldDispatchAction(action: Action?): Boolean {
        return action === mRewindAction ||
                action === mFastForwardAction ||
                action === mThumbsDownAction ||
                action === mThumbsUpAction ||
                action === mRepeatAction ||
                action === mSpeedAction ||
                action === mClosedCaptioningAction ||
                action === mQualityAction
    }

    private fun dispatchAction(action: Action) {
        // Primary actions are handled manually.
        if (action === mRewindAction) {
            rewind()
        } else if (action === mFastForwardAction) {
            fastForward()
        } else if (action === mSpeedAction) {
            mActionListener.onSpeed()
        } else if (action === mClosedCaptioningAction) {
            mActionListener.onCaption()
        } else if (action === mQualityAction) {
            mActionListener.onQualitySelected()
        } else if (action is MultiAction) {
            val multiAction = action
            multiAction.nextIndex()

            // Notify adapter of action changes to handle secondary actions, such as, thumbs up/down
            // and repeat.
            notifyActionChanged(
                multiAction,
                controlsRow.secondaryActionsAdapter as ArrayObjectAdapter
            )
        }
    }

    private fun notifyActionChanged(action: MultiAction, adapter: ArrayObjectAdapter?) {
        if (adapter != null) {
            val index = adapter.indexOf(action)
            if (index >= 0) {
                adapter.notifyArrayItemRangeChanged(index, 1)
            }
        }
    }

    /** Skips backwards 10 seconds.  */
    fun rewind() {
        var newPosition = currentPosition - TEN_SECONDS
        newPosition = if (newPosition < 0) 0 else newPosition
        playerAdapter!!.seekTo(newPosition)
    }

    /** Skips forward 10 seconds.  */
    fun fastForward() {
        if (duration > -1) {
            var newPosition = currentPosition + TEN_SECONDS
            newPosition = Math.min(newPosition, duration)
            playerAdapter!!.seekTo(newPosition)
        }
    }

    override fun previous() {
        mActionListener.onPrevious()
    }

    override fun next() {
        mActionListener.onNext()
    }

    override fun onPlayCompleted() {
        if (SettingsData.autoPlayNextEpisode == true && isSerial) {
            mActionListener.onNext()
        }
        super.onPlayCompleted()
    }

    override fun onAttachedToHost(host: PlaybackGlueHost?) {
        super.onAttachedToHost(host)
    }

    interface OnActionClickedListener {
        /** Skip to the previous item in the queue.  */
        fun onPrevious()

        /** Skip to the next item in the queue.  */
        fun onNext()

        //  fun onPlayCompleted(playlistPlayAction: org.mythtv.leanfront.player.VideoPlayerGlue.MyAction?)
        fun onCaption()
        fun onSpeed()
        fun onQualitySelected()
    }

    /**
     * Our custom actions
     */
    class MyAction(context: Context, id: Int, icons: IntArray, labels: IntArray) : MultiAction(id) {
        constructor(context: Context, id: Int, icon: Int, label: Int) : this(context, id, intArrayOf(icon), intArrayOf(label)) {}

        init {
            val res = context.resources
            val drawables = arrayOfNulls<Drawable>(icons.size)
            val labelStr = arrayOfNulls<String>(icons.size)
            for (i in icons.indices) {
                drawables[i] = ResourcesCompat.getDrawable(res, icons[i], null)
                labelStr[i] = res.getString(labels[i])
            }
            setDrawables(drawables)
            setLabels(labelStr)
        }
    }

    fun getCurrentPos(): Long {
        return currentPosition
    }

    companion object {
        val TEN_SECONDS = TimeUnit.SECONDS.toMillis(10)
    }

}