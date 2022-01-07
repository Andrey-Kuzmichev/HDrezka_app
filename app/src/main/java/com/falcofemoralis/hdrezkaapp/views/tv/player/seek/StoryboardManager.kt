package com.falcofemoralis.hdrezkaapp.views.tv.player.seek

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.falcofemoralis.hdrezkaapp.objects.Thumbnail

class StoryboardManager(private val context: Context) {
    private var mSeekPositions: LongArray? = null
    private var mThumbnails: ArrayList<Thumbnail>? = null
    private var currentImageIndex: Int = -1
    private var mSeekDirection = DIRECTION_RIGHT
    private val mCachedImageIndexs: ArrayList<Int>? = null

    fun setThumbnails(thumbnails: ArrayList<Thumbnail>) {
        mThumbnails = thumbnails

        initSeekPositions()
    }

    private fun initSeekPositions() {
        if (mThumbnails == null) {
            return
        }

        mSeekPositions = mThumbnails?.size?.let { LongArray(it) }

        if (mSeekPositions != null) {
            for (i in mSeekPositions!!.indices) {
                mSeekPositions!![i] = mThumbnails?.get(i)?.t2?.toLong()!! * 1000 // convert to ms
            }
        }
    }

    fun getSeekPositions(): LongArray? {
        return if (mSeekPositions == null || mSeekPositions!!.size < 10) {
            // Preventing from video being skipped fully
            null
        } else {
            mSeekPositions
        }
    }


    fun getBitmap(index: Int, callback: (bitmap: Bitmap?) -> Unit) {
        if (mThumbnails == null || mSeekPositions == null || index >= mSeekPositions!!.size) {
            return
        }

        loadPreview(index, mThumbnails!![index], callback)
    }

    private fun loadPreview(index: Int, thumb: Thumbnail, callback: (bitmap: Bitmap?) -> Unit) {
        if (mThumbnails == null) {
            return
        }

        val transformation = GlideThumbnailTransformation(thumb.x, thumb.y, thumb.width, thumb.height)

        Glide.with(context)
            .asBitmap()
            .load(thumb.url)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .transform(transformation)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    // NOP
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    callback(resource)
                }
            })

          if (currentImageIndex != index) {
              mSeekDirection = if (currentImageIndex < index) DIRECTION_RIGHT else DIRECTION_LEFT
              mCachedImageIndexs?.add(index)
              currentImageIndex = index
              preloadNextImage()
          }
    }

    private fun preloadNextImage() {
        if (mThumbnails == null) {
            return
        }

        for (i in 1..MAX_PRELOADED_IMAGES) {
            val imgIndex: Int = if (mSeekDirection == DIRECTION_RIGHT) currentImageIndex + i else currentImageIndex - i // get next image
            preloadImage(imgIndex)
        }
    }

    private fun preloadImage(imgIndex: Int) {
        if (mCachedImageIndexs?.contains(imgIndex) == true || imgIndex < 0) {
            return
        }

        if(mThumbnails != null && imgIndex < mThumbnails!!.size){
            mCachedImageIndexs?.add(imgIndex)
            val link = mThumbnails?.get(imgIndex)?.url

            Glide.with(context)
                .load(link)
                .preload()
        }
    }

    companion object {
        private const val MAX_PRELOADED_IMAGES = 25
        private const val DIRECTION_RIGHT = 0
        private const val DIRECTION_LEFT = 1
    }
}
