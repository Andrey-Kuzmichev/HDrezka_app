package com.falcofemoralis.hdrezkaapp.views.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.FilmType
import com.falcofemoralis.hdrezkaapp.models.FilmModel
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.utils.Highlighter.zoom
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FilmsListRecyclerViewAdapter(private val films: ArrayList<Film>, private val openFilm: (film: Film) -> Unit, private val onListEndTrigger: (() -> Unit)?) :
    RecyclerView.Adapter<FilmsListRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.inflate_film, parent, false)
        val context: Context = parent.getContext()

        return ViewHolder(view, context)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // holder.layout.clearAnimation()
        //holder.posterLayoutView.clearAnimation()
        holder.itemView.clearAnimation()
    }


    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        fun onFocusCallback() {
            if (position > films.size - 1 - (SettingsData.filmsInRow?.times(2) ?: 14)) {
                //Log.d("TESTREST", "$position > ${films.size - 1 - (SettingsData.filmsInRow?.times(2) ?: 14)}")
                if (onListEndTrigger != null) {
                    onListEndTrigger?.let { it() }
                }
            }
        }

        zoom(holder.context, holder.layout, holder.filmPoster, holder.titleView, ::onFocusCallback, holder.infoView)

        val film = films[position]

        var glide = Glide.with(holder.context).load(film.posterPath).fitCenter()
        if (film.subInfo?.contains(FilmModel.AWAITING_TEXT) == true) {
            glide = glide.transform(ColorFilterTransformation(R.color.black))
        }

        glide = glide.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                return false
            }

            override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        holder.progressView.visibility = View.GONE
                        holder.posterLayoutView.visibility = View.VISIBLE
                    }
                }
                return false
            }
        })
        glide = glide.error(R.drawable.nopersonphoto) // TODO
        glide = glide.override(Target.SIZE_ORIGINAL)
        glide.into(holder.filmPoster)
        glide.submit()

        holder.titleView.text = film.title

        var info = ""
        film.year?.let {
            info += film.year
        }
        film.countries?.let {
            if (film.countries!!.size > 0) {
                info = addComma(info)
                info += film.countries!![0]
            }
        }
        film.genres?.let {
            if (film.genres!!.size > 0) {
                info = addComma(info)
                info += film.genres!![0]
            }
        }

        holder.infoView.text = info

        var typeText = film.type
        if (film.ratingKP?.isNotEmpty() == true) {
            typeText = typeText?.let { addComma(it) }
            typeText += film.ratingKP
        }
        holder.typeView.text = typeText

        val color: Int = when (film.constFilmType) {
            FilmType.FILM -> ContextCompat.getColor(holder.context, R.color.film)
            FilmType.MULTFILMS -> ContextCompat.getColor(holder.context, R.color.multfilm)
            FilmType.SERIES -> ContextCompat.getColor(holder.context, R.color.serial)
            FilmType.ANIME -> ContextCompat.getColor(holder.context, R.color.anime)
            FilmType.TVSHOWS -> ContextCompat.getColor(holder.context, R.color.tv_show)
            else -> ContextCompat.getColor(holder.context, R.color.background)
        }
        holder.typeView.setBackgroundColor(color)

        if (film.subInfo?.isNotEmpty() == true) {
            holder.subInfoView.visibility = View.VISIBLE
            holder.subInfoView.text = film.subInfo
            holder.subInfoView.setBackgroundColor(color)
        } else {
            holder.subInfoView.visibility = View.GONE
        }

        holder.layout.setOnClickListener {
            openFilm(film)
        }
    }

    private fun addComma(text: String): String {
        var info = text
        if (text.isNotEmpty()) {
            info += ", "
        }
        return info
    }

    override fun getItemCount(): Int = films.size


    inner class ViewHolder(view: View, val context: Context) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.film_layout)
        val filmPoster: ImageView = view.findViewById(R.id.film_poster)
        val titleView: TextView = view.findViewById(R.id.film_title)
        val infoView: TextView = view.findViewById(R.id.film_info)
        val typeView: TextView = view.findViewById(R.id.film_type)
        val progressView: ProgressBar = view.findViewById(R.id.film_loading)
        val posterLayoutView: RelativeLayout = view.findViewById(R.id.film_posterLayout)
        val subInfoView: TextView = view.findViewById(R.id.film_sub_info)
    }
}