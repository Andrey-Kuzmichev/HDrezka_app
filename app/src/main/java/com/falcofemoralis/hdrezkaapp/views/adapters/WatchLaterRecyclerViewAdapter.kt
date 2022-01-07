package com.falcofemoralis.hdrezkaapp.views.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.models.FilmModel
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.WatchLater
import com.falcofemoralis.hdrezkaapp.utils.Highlighter.zoom
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchLaterRecyclerViewAdapter(private val context: Context, private val watchLaterList: ArrayList<WatchLater>, private val openFilm: (film: Film) -> Unit, private val deleteHandler: (id: String) -> Unit) :
    RecyclerView.Adapter<WatchLaterRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.inflate_watch_later, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        zoom(context, holder.layout, holder.posterView, holder.nameView, null, holder.infoView, holder.dateView)

        val watchLaterItem = watchLaterList[position]

        if(position == 0){
            holder.layout.requestFocus()
        }

        var glide = Glide.with(context).load(watchLaterItem.posterPath).fitCenter()
        glide = glide.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                return false
            }

            override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                GlobalScope.launch {
                    withContext(Dispatchers.Main){
                        holder.posterProgressBar.visibility = View.GONE
                        holder.posterView.visibility = View.VISIBLE
                    }
                }
                return false
            }
        })
        glide = glide.error (R.drawable.nopersonphoto) // TODO
        glide = glide.override(Target.SIZE_ORIGINAL)
        glide.into(holder.posterView)
        glide.submit()

        holder.dateView.text = watchLaterItem.date
        holder.nameView.text = watchLaterItem.name
        holder.infoView.text = watchLaterItem.additionalInfo

        holder.layout.setOnClickListener {
            openFilm(Film(watchLaterItem.filmLInk))
        }

        holder.deleteView.setOnClickListener {
            deleteHandler(watchLaterItem.id)
            watchLaterList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }
    }

    override fun getItemCount(): Int = watchLaterList.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.watchLater_layout)
        val posterView: ImageView = view.findViewById(R.id.inflate_watch_poster)
        val posterProgressBar: ProgressBar = view.findViewById(R.id.inflate_watch_poster_loading)
        val dateView: TextView = view.findViewById(R.id.watchLater_date)
        val nameView: TextView = view.findViewById(R.id.watchLater_name)
        val infoView: TextView = view.findViewById(R.id.watchLater_info)
        val deleteView: TextView = view.findViewById(R.id.watchLater_delete)
    }
}