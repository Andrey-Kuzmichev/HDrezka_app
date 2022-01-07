package com.falcofemoralis.hdrezkaapp.views.tv.grid

import android.content.Context
import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.falcofemoralis.hdrezkaapp.objects.Film

/**
 * Presenter for rendering video cards on the Vertical Grid fragment.
 */
class VideoCardViewPresenter(context: Context?, cardThemeResId: Int) : ImageCardViewPresenter(context, cardThemeResId) {
    override fun onBindViewHolder(film: Film?, cardView: ImageCardView?) {
        super.onBindViewHolder(film, cardView)

        if (film != null) {
            if (cardView != null) {
                Glide.with(context)
                    .asBitmap()
                    .load(film.posterPath)
                    .into(cardView.mainImageView)
            }
        }
    }
}