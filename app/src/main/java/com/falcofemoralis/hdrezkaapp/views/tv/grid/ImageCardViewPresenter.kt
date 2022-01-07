package com.falcofemoralis.hdrezkaapp.views.tv.grid

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.leanback.widget.ImageCardView
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.objects.Film

/**
 * A very basic [ImageCardView] [androidx.leanback.widget.Presenter].You can
 * pass a custom style for the ImageCardView in the constructor. Use the default constructor to
 * create a Presenter with a default ImageCardView style.
 */
open class ImageCardViewPresenter @JvmOverloads constructor(context: Context?, cardThemeResId: Int = R.style.DefaultCardTheme) :
    AbstractCardPresenter<ImageCardView?>(ContextThemeWrapper(context, cardThemeResId)) {

    override fun onCreateView(): ImageCardView {
        //        imageCardView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getContext(), "Clicked on ImageCardView", Toast.LENGTH_SHORT).show();
//            }
//        });
        return ImageCardView(context)
    }

    override fun onBindViewHolder(film: Film?, cardView: ImageCardView?) {
        //cardView.setTag(Film)
        if (film != null) {
            cardView?.titleText = film.title
        }
        if (film != null) {
            cardView?.contentText = film.description
        }

        cardView?.setMainImageDimensions(400, 500)
        /* if (card.getLocalImageResourceName() != null) {
             val resourceId: Int = getContext().getResources()
                 .getIdentifier(
                     card.getLocalImageResourceName(),
                     "drawable", getContext().getPackageName()
                 )
             Glide.with(getContext())
                 .asBitmap()
                 .load(resourceId)
                 .into(cardView.getMainImageView())
         }*/
    }
}