package com.falcofemoralis.hdrezkaapp.views.tv.player

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.objects.Subtitle
import com.google.android.exoplayer2.PlaybackParameters
import java.util.*
import kotlin.collections.ArrayList

class PlaybackActionListener(private val playerFragment: PlayerFragment) : VideoPlayerGlue.OnActionClickedListener {
    var mDialog: AlertDialog? = null

    override fun onPrevious() {
        playerFragment.skipToPrevious()
    }

    override fun onNext() {
        playerFragment.skipToNext()
    }

    override fun onCaption() {
        // This code could be used for rotating among captions instead of showing a list
        // playbackFragment.mTextSelection = playbackFragment.trackSelector(C.TRACK_TYPE_TEXT, playbackFragment.mTextSelection,
        //         R.string.msg_subtitle_on, R.string.msg_subtitle_off, true, true);
        showCaptionSelector()
    }

    private fun showCaptionSelector() {
        playerFragment.hideControlsOverlay(false)

        val subtitles: ArrayList<Subtitle>? = playerFragment.mTranslation?.subtitles
        val prompts = ArrayList<String>()
        val actions = ArrayList<Int>()

        if (subtitles != null) {
            for (ix in 0 until subtitles.size) {
                prompts.add(subtitles[ix].lang)
                actions.add(ix)
            }
        }

        prompts.add(playerFragment.getString(R.string.msg_subtitle_off))
        actions.add(-1)

        // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
        val builder = playerFragment.context?.let { AlertDialog.Builder(it, R.style.Theme_AppCompat_Dialog_Alert) }
        val adapter = playerFragment.context?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, prompts) }

        builder
            ?.setTitle(R.string.title_select_caption)
/*            ?.setItems(prompts.toTypedArray(),
                object : DialogInterface.OnClickListener {
                    var mActions = actions // needed because used in inner class

                    override fun onClick(dialog: DialogInterface, which: Int) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (which < mActions.size) {
                            playerFragment.subtitleSelector(mActions[which])
                        }
                    }
                })*/
            ?.setSingleChoiceItems(adapter, playerFragment.selectedSubtitle, object : DialogInterface.OnClickListener {
                var mActions = actions // needed because used in inner class

                override fun onClick(dialog: DialogInterface?, which: Int) {
                    if (which < mActions.size) {
                        playerFragment.subtitleSelector(mActions[which])
                        mDialog?.hide()
                    }
                }

            })
        mDialog = builder?.create()
        mDialog?.show()

        val lp = mDialog!!.window!!.attributes
        lp.dimAmount = 0.0f // Dim level. 0.0 - no dim, 1.0 - completely opaque
        mDialog!!.window!!.attributes = lp
        mDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.argb(100, 0, 0, 0)))
    }

    override fun onSpeed() {
        showSpeedSelector()
    }

    private fun showSpeedSelector(): Boolean {
        playerFragment.hideControlsOverlay(true)
        val builder = playerFragment.getContext()?.let { AlertDialog.Builder(it, R.style.Theme_AppCompat_Dialog_Alert) }
        builder?.setTitle(R.string.title_select_speed)?.setView(R.layout.leanback_preference_widget_seekbar)
        mDialog = builder?.create()
        mDialog?.show()
        val lp: WindowManager.LayoutParams? = mDialog?.window?.getAttributes()
        lp?.dimAmount = 0.0f // Dim level. 0.0 - no dim, 1.0 - completely opaque
        mDialog?.window?.attributes = lp
        mDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.argb(100, 0, 0, 0)))
        val seekBar: SeekBar? = mDialog?.findViewById<SeekBar>(R.id.seekbar)
        seekBar?.max = 800
        seekBar?.progress = Math.round(playerFragment.mSpeed * 100.0f).toInt()
        val seekValue: TextView? = mDialog?.findViewById<TextView>(R.id.seekbar_value)
        val value = (playerFragment.mSpeed * 100.0f)
        seekValue?.text = "${value.toInt()}"

        mDialog?.setOnKeyListener { dlg: DialogInterface, keyCode: Int, event: KeyEvent ->
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    dlg.dismiss()
                    return@setOnKeyListener true
                }
            }
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            var value = seekBar?.progress
            if (value != null) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN -> value -= if (value > 10) 10 else return@setOnKeyListener true
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP -> value += if (value <= 790) 10 else return@setOnKeyListener true
                    KeyEvent.KEYCODE_BACK -> return@setOnKeyListener false
                    else -> {
                        dlg.dismiss()
                        playerFragment.activity?.onKeyDown(event.keyCode, event)
                        return@setOnKeyListener true
                    }
                }
            }
            if (value != null) {
                seekBar?.progress = value
            }
            true
        }
        seekBar?.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    var value = value
                    value = value / 10 * 10
                    if (value < 10) value = 10
                    seekValue?.text = "${value.toInt()}"
                    playerFragment.mSpeed = value.toFloat() * 0.01f
                    val parms = PlaybackParameters(playerFragment.mSpeed)
                    playerFragment.mPlayer?.playbackParameters = parms
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )
        mDialog?.setOnDismissListener(
            DialogInterface.OnDismissListener { dialog: DialogInterface? ->
                mDialog = null
                playerFragment.hideNavigation()
            }
        )

        return true
    }

    override fun onQualitySelected() {
        playerFragment.hideControlsOverlay(false)

        val streams = playerFragment.mTranslation?.streams
        val prompts = ArrayList<String>()
        val actions = ArrayList<Int>()

        if (streams != null) {
            for (ix in 0 until streams.size) {
                prompts.add(streams[ix].quality)
                actions.add(ix)
            }
        }

        // Theme_AppCompat_Light_Dialog_Alert or Theme_AppCompat_Dialog_Alert
        val builder = playerFragment.context?.let { AlertDialog.Builder(it, R.style.Theme_AppCompat_Dialog_Alert) }
        val adapter = playerFragment.context?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, prompts) }

        builder
            ?.setTitle(R.string.select_quality)
            /*           ?.setItems(prompts.toTypedArray(),
                           object : DialogInterface.OnClickListener {
                               var mActions = actions // needed because used in inner class

                               override fun onClick(dialog: DialogInterface, which: Int) {
                                   // The 'which' argument contains the index position
                                   // of the selected item
                                   if (which < mActions.size) {
                                       playerFragment.qualitySelector(mActions[which])
                                   }
                               }
                           })*/
            ?.setSingleChoiceItems(adapter, playerFragment.selectedQuality, object : DialogInterface.OnClickListener {
                var mActions = actions // needed because used in inner class

                override fun onClick(dialog: DialogInterface?, which: Int) {
                    if (which < mActions.size) {
                        playerFragment.qualitySelector(mActions[which])
                        mDialog?.hide()
                    }
                }

            })
        mDialog = builder?.create()
        mDialog?.show()

        val lp = mDialog!!.window!!.attributes
        lp.dimAmount = 0.0f // Dim level. 0.0 - no dim, 1.0 - completely opaque
        mDialog!!.window!!.attributes = lp
        mDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.argb(100, 0, 0, 0)))
    }
}