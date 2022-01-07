package com.falcofemoralis.hdrezkaapp.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.objects.SettingsData


object Highlighter {
    const val ANIM_DURATION = 250L

    fun zoom(context: Context, layout: LinearLayout, iv: ImageView, titleView: TextView, onFocusCallback: (() -> Unit)?, vararg subViews: TextView?) {
        if (SettingsData.deviceType == DeviceType.TV) {
            layout.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    if (onFocusCallback != null) {
                        onFocusCallback?.let { it() }
                    }

                    iv.setColorFilter(context.getColor(R.color.transparent))
                    titleView.setTextColor(context.getColor(R.color.white))
                    for (subView in subViews) {
                        subView?.setTextColor(context.getColor(R.color.gray))
                    }

                    val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.scale_in_tv)
                    anim.fillAfter = true
                    v.startAnimation(anim)
                } else {
                    iv.setColorFilter(context.getColor(R.color.unselected_film))
                    titleView.setTextColor(context.getColor(R.color.unselected_title))
                    for (subView in subViews) {
                        subView?.setTextColor(context.getColor(R.color.unselected_subtitle))
                    }

                    val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.scale_out_tv)
                    anim.fillAfter = true
                    v.startAnimation(anim)
                }
            }
            iv.setColorFilter(context.getColor(R.color.unselected_film))
        }
    }

    fun highlightButton(btn: TextView, context: Context, isHint: Boolean = false) {
        if (SettingsData.deviceType == DeviceType.TV) {
            btn.setOnFocusChangeListener { v, hasFocus ->
                if (v is TextView) {
                    val bgColorFrom: Int
                    val bgColorTo: Int
                    val textColorFrom: Int
                    val textColorTo: Int

                    if (hasFocus) {
                        bgColorFrom = ContextCompat.getColor(context, R.color.unselected_btn_color)
                        bgColorTo = ContextCompat.getColor(context, R.color.primary_red)
                        textColorFrom = ContextCompat.getColor(context, R.color.white)
                        textColorTo = ContextCompat.getColor(context, R.color.white)
                    } else {
                        bgColorFrom = ContextCompat.getColor(context, R.color.primary_red)
                        bgColorTo = ContextCompat.getColor(context, R.color.unselected_btn_color)
                        textColorFrom = ContextCompat.getColor(context, R.color.white)
                        textColorTo = ContextCompat.getColor(context, R.color.white)
                    }

                    val colorAnimationBg = ValueAnimator.ofObject(ArgbEvaluator(), bgColorFrom, bgColorTo)
                    colorAnimationBg.duration = ANIM_DURATION
                    colorAnimationBg.addUpdateListener { animator -> v.setBackgroundColor(animator.animatedValue as Int) }
                    colorAnimationBg.start()

                    val colorAnimationText = ValueAnimator.ofObject(ArgbEvaluator(), textColorFrom, textColorTo)
                    colorAnimationText.duration = ANIM_DURATION
                    colorAnimationText.addUpdateListener { animator ->
                        v.setTextColor(animator.animatedValue as Int)
                        for (drawable in v.compoundDrawables) {
                            if (drawable != null) {
                                drawable.colorFilter = PorterDuffColorFilter(animator.animatedValue as Int, PorterDuff.Mode.SRC_IN)
                            }
                        }

                        if (isHint) {
                            if (v is AutoCompleteTextView) {
                                v.setHintTextColor(animator.animatedValue as Int)
                            }
                        }
                    }
                    colorAnimationText.start()
                }
            }
        }
    }

    fun highlightImage(iv: View, context: Context) {
        if (SettingsData.deviceType == DeviceType.TV) {
            iv.setOnFocusChangeListener { v, hasFocus ->
                val colorFrom: Int
                val colorTo: Int

                if (hasFocus) {
                    colorFrom = ContextCompat.getColor(context, R.color.white)
                    colorTo = ContextCompat.getColor(context, R.color.primary_red)
                } else {
                    colorFrom = ContextCompat.getColor(context, R.color.primary_red)
                    colorTo = ContextCompat.getColor(context, R.color.white)
                }

                val colorAnimationText = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                colorAnimationText.duration = ANIM_DURATION
                if (v is ImageView) {
                    colorAnimationText.addUpdateListener { animator ->
                        v.colorFilter = PorterDuffColorFilter(animator.animatedValue as Int, PorterDuff.Mode.SRC_IN)
                    }
                } else if (v is TextView) {
                    colorAnimationText.addUpdateListener { animator ->
                        v.setBackgroundColor(animator.animatedValue as Int)
                    }
                }
                colorAnimationText.start()
            }
        }
    }

    fun highlightText(btn: View, context: Context, isBackground: Boolean = false) {
        if (SettingsData.deviceType == DeviceType.TV) {
            btn.setOnFocusChangeListener { v, hasFocus ->
                if (v is TextView) {
                    val textColorFrom: Int
                    val textColorTo: Int

                    if (hasFocus) {
                        textColorFrom = ContextCompat.getColor(context, R.color.white)
                        textColorTo = ContextCompat.getColor(context, R.color.primary_red)
                        if (isBackground && (v.getBackground() is TransitionDrawable)) {
                            (v.getBackground() as TransitionDrawable).startTransition(ANIM_DURATION.toInt())
                        }
                    } else {
                        textColorFrom = ContextCompat.getColor(context, R.color.primary_red)
                        textColorTo = ContextCompat.getColor(context, R.color.white)
                        if (isBackground && (v.getBackground() is TransitionDrawable)) {
                            (v.getBackground() as TransitionDrawable).reverseTransition(ANIM_DURATION.toInt())
                        }
                    }

                    val colorAnimationText = ValueAnimator.ofObject(ArgbEvaluator(), textColorFrom, textColorTo)
                    colorAnimationText.duration = ANIM_DURATION
                    colorAnimationText.addUpdateListener { animator ->
                        v.setTextColor(animator.animatedValue as Int)
                        for (drawable in v.compoundDrawables) {
                            if (drawable != null) {
                                drawable.colorFilter = PorterDuffColorFilter(animator.animatedValue as Int, PorterDuff.Mode.SRC_IN)
                            }
                        }


                    }
                    colorAnimationText.start()
                }
            }
        }
    }

    fun highlightLayout(layout: LinearLayout, textView: TextView, iv: ImageView, context: Context) {
        //  if (SettingsData.deviceType == DeviceType.TV) {
        layout.setOnFocusChangeListener { v, hasFocus ->
            if (v is LinearLayout) {
                val bgColorFrom: Int
                val bgColorTo: Int
                val textColorFrom: Int
                val textColorTo: Int

                if (hasFocus) {
                    bgColorFrom = ContextCompat.getColor(context, R.color.light_background)
                    bgColorTo = ContextCompat.getColor(context, R.color.primary_red)
                    textColorFrom = ContextCompat.getColor(context, R.color.primary_red)
                    textColorTo = ContextCompat.getColor(context, R.color.white)
                } else {
                    bgColorFrom = ContextCompat.getColor(context, R.color.primary_red)
                    bgColorTo = ContextCompat.getColor(context, R.color.light_background)
                    textColorFrom = ContextCompat.getColor(context, R.color.white)
                    textColorTo = ContextCompat.getColor(context, R.color.primary_red)
                }

                val colorAnimationBg = ValueAnimator.ofObject(ArgbEvaluator(), bgColorFrom, bgColorTo)
                colorAnimationBg.duration = ANIM_DURATION
                colorAnimationBg.addUpdateListener { animator -> v.setBackgroundColor(animator.animatedValue as Int) }
                colorAnimationBg.start()

                val colorAnimationText = ValueAnimator.ofObject(ArgbEvaluator(), textColorFrom, textColorTo)
                colorAnimationText.duration = ANIM_DURATION
                colorAnimationText.addUpdateListener { animator ->
                    textView.setTextColor(animator.animatedValue as Int)
                }
                colorAnimationText.start()


                ///
                /*     val colorFrom: Int
                     val colorTo: Int

                     if (hasFocus) {
                         colorFrom = ContextCompat.getColor(context, R.color.white)
                         colorTo = ContextCompat.getColor(context, R.color.white)
                     } else {
                         colorFrom = ContextCompat.getColor(context, R.color.white)
                         colorTo = ContextCompat.getColor(context, R.color.white)
                     }

                     val colorAnimationImage = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                     colorAnimationImage.duration = ANIM_DURATION
                     colorAnimationImage.addUpdateListener { animator ->
                         iv.colorFilter = PorterDuffColorFilter(animator.animatedValue as Int, PorterDuff.Mode.SRC_IN)
                     }
                     colorAnimationImage.start()*/
            }
        }
    }
}