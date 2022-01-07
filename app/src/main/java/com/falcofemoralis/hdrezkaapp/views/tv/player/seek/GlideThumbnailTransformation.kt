package com.falcofemoralis.hdrezkaapp.views.tv.player.seek

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.ByteBuffer
import java.security.MessageDigest

class GlideThumbnailTransformation(private val x: Int, private val y: Int, private val width: Int, private val height: Int) : BitmapTransformation() {
    private fun getX(): Int {
        return x
    }

    private fun getY(): Int {
        return y
    }

   override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return Bitmap.createBitmap(toTransform, x, y, width, height)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        val data = ByteBuffer.allocate(8).putInt(x).putInt(y).array()
        messageDigest.update(data)
    }

    override fun hashCode(): Int {
        return (x.toString() + y.toString()).hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is GlideThumbnailTransformation) {
            return false
        }
        return obj.getX() == x && obj.getY() == y
    }
}