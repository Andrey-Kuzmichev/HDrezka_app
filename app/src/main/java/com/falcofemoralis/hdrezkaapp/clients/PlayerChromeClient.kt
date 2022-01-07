package com.falcofemoralis.hdrezkaapp.clients

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment

class PlayerChromeClient(private val activity: Activity) : WebChromeClient() {
    private var mCustomView: View? = null
    private var mCustomViewCallback: CustomViewCallback? = null
    protected var mFullscreenContainer: FrameLayout? = null
    private var mOriginalSystemUiVisibility = 0

    override fun getDefaultVideoPoster(): Bitmap? {
        return if (mCustomView == null) {
            null
        } else {
            BitmapFactory.decodeResource(activity.applicationContext.resources, 2130837573)
        }
    }

    override fun onHideCustomView() {
        (activity.window.decorView as FrameLayout).removeView(mCustomView)
        mCustomView = null
        activity.window.decorView.setSystemUiVisibility(mOriginalSystemUiVisibility)
        mCustomViewCallback!!.onCustomViewHidden()
        mCustomViewCallback = null

        if(SettingsData.isAutorotate == true){
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        } else{
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        FilmFragment.isFullscreen = false
    }

    override fun onShowCustomView(paramView: View?, paramCustomViewCallback: CustomViewCallback?) {
        if (mCustomView != null) {
            onHideCustomView()
            return
        }
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        mCustomView = paramView
        mOriginalSystemUiVisibility = activity.window.decorView.getSystemUiVisibility()
        mCustomViewCallback = paramCustomViewCallback
        (activity.window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        activity.window.decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        FilmFragment.isFullscreen = true
    }
}