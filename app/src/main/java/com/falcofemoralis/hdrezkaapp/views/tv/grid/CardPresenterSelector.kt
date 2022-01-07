package com.falcofemoralis.hdrezkaapp.views.tv.grid

import android.content.Context
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.Presenter
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.objects.Film
import java.lang.RuntimeException
import java.util.ArrayList

class CardPresenterSelector(private val mContext: Context) : PresenterSelector() {
    override fun getPresenter(item: Any): Presenter {
        if (item !is Film) throw RuntimeException(String.format("The PresenterSelector only supports data items of type '%s'", Film::class.java.getName()))
        return VideoCardViewPresenter(mContext, R.style.VideoGridCardTheme)
    }
}