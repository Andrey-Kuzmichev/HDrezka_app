package com.falcofemoralis.hdrezkaapp.views.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.HintType
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SeriesUpdateItem
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.presenters.SeriesUpdatesPresenter
import com.falcofemoralis.hdrezkaapp.utils.DialogManager
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.FragmentOpener
import com.falcofemoralis.hdrezkaapp.utils.Highlighter
import com.falcofemoralis.hdrezkaapp.views.MainActivity
import com.falcofemoralis.hdrezkaapp.views.adapters.FilmsListRecyclerViewAdapter
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.SeriesUpdatesView

class SeriesUpdatesFragment : Fragment(), SeriesUpdatesView {
    private var seriesUpdatesPresenter: SeriesUpdatesPresenter? = null
    private lateinit var currentView: View
    private lateinit var fragmentListener: OnFragmentInteractionListener
    private lateinit var scrollView: NestedScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var allUpdatesBtn: TextView

    private var dialog: AlertDialog? = null
    private var dialogView: LinearLayout? = null
    private var container: LinearLayout? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentView = inflater.inflate(R.layout.fragment_series_updates, container, false)

        scrollView = currentView.findViewById(R.id.fragment_series_updates_scroll)
        scrollView.visibility = View.GONE
        progressBar = currentView.findViewById(R.id.fragment_series_updates_pb_data_loading)
        allUpdatesBtn = currentView.findViewById(R.id.fragment_series_updates_tv_all)
        allUpdatesBtn.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show()
        }

        if (seriesUpdatesPresenter == null) {
            seriesUpdatesPresenter = SeriesUpdatesPresenter(this)
        }

        seriesUpdatesPresenter?.initFilmsList()
        seriesUpdatesPresenter?.saveUserUpdatesList(requireContext())

        createAllUpdatedDialog()

        Highlighter.highlightButton(allUpdatesBtn, requireContext())

        return currentView
    }

    override fun setList(films: LinkedHashMap<String, ArrayList<Film>>) {
        val container: LinearLayout = currentView.findViewById(R.id.fragment_series_updates_ll_films)

        fun listCallback(film: Film) {
            FragmentOpener.openWithData(this, fragmentListener, film, "film")
        }

        for ((date, filmsList) in films) {
            val layout = layoutInflater.inflate(R.layout.inflate_actor_career_layout, null)

            layout.findViewById<TextView>(R.id.career_header).text = date
            val recyclerView: RecyclerView = layout.findViewById(R.id.career_films)
            recyclerView.layoutManager = SettingsData.filmsInRow?.let { GridLayoutManager(requireContext(), it) }
            recyclerView.adapter = FilmsListRecyclerViewAdapter(filmsList, ::listCallback, null)

            container.addView(layout)
        }

        scrollView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    override fun setHint(hintType: HintType) {
        progressBar.visibility = View.GONE
        val text = getString(
            when (hintType) {
                HintType.NOT_AUTH -> R.string.updates_for_register_only
                HintType.EMPTY -> R.string.no_updates
            }
        )
        val hintView = currentView.findViewById<TextView>(R.id.fragment_series_updates_hint)
        hintView.visibility = View.VISIBLE
        hintView.text = text
    }

    override fun showConnectionError(type: IConnection.ErrorType, errorText: String) {
        try {
            if (context != null) {
                ExceptionHelper.showToastError(requireContext(), type, errorText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initUserUpdatesData(_context: Context, updateNotifyBadge: (n: Int) -> Unit, createNotifyBtn: () -> Unit) {
        if (seriesUpdatesPresenter == null) {
            seriesUpdatesPresenter = SeriesUpdatesPresenter(this)
        }

        seriesUpdatesPresenter?.initUserUpdatesData(_context, updateNotifyBadge, createNotifyBtn)
    }

    override fun resetBadge(){
        activity?.let {
            (it as MainActivity).updateNotifyBadge(0)
        }
    }

    fun createAllUpdatedDialog() {
        allUpdatesBtn.setOnClickListener {
            val builder = DialogManager.getDialog(requireContext(), R.string.series_update_hot)
            dialogView = layoutInflater.inflate(R.layout.dialog_series_updates, null) as LinearLayout
            container = dialogView?.findViewById(R.id.series_updates_container)

            builder.setView(dialogView)
            builder.setPositiveButton(getString(R.string.ok_text)) { d, id ->
                d.cancel()
            }

            seriesUpdatesPresenter?.initAllUpdatesList()

            dialog = builder.create()
            dialog?.show()
        }
    }

    override fun updateDialog(seriesUpdates: LinkedHashMap<String, ArrayList<SeriesUpdateItem>>) {
        dialogView?.findViewById<ProgressBar>(R.id.series_updates_progress)?.visibility = View.GONE

        for ((date, updateItems) in seriesUpdates) {
            // костыль т.к .collapsed() не срабатывает
            val layout: LinearLayout = layoutInflater.inflate(R.layout.inflate_series_updates_layout, null) as LinearLayout
            val expandedList: LinearLayout = layout.findViewById(R.id.inflate_series_updates_layout_list)

            if (!date.contains("Сегодня")) {
                expandedList.visibility = View.GONE
            }

            layout.findViewById<TextView>(R.id.inflate_series_updates_layout_header).text = date
            layout.findViewById<LinearLayout>(R.id.inflate_series_updates_button).setOnClickListener {
                if (expandedList.isVisible) {
                    expandedList.visibility = View.GONE
                } else {
                    expandedList.visibility = View.VISIBLE
                }
            }

            var lastColor = R.color.light_background
            for (item in updateItems) {
                if (item.title.isEmpty()) {
                    continue
                }

                val itemView = layoutInflater.inflate(R.layout.inflate_series_updates_item, null) as LinearLayout
                itemView.findViewById<TextView>(R.id.inflate_series_updates_item_title).text = item.title
                itemView.findViewById<TextView>(R.id.inflate_series_updates_item_season).text = item.season
                itemView.findViewById<TextView>(R.id.inflate_series_updates_item_episode).text = item.episode
                if (!item.voice.isNullOrEmpty()) {
                    itemView.findViewById<TextView>(R.id.inflate_series_updates_item_voice).text = item.voice
                }

                val film = Film(SettingsData.provider + item.filmLink)
                itemView.setOnClickListener {
                    if (!film.filmLink.isNullOrEmpty()) {
                        dialog?.dismiss()
                        FragmentOpener.openWithData(this, fragmentListener, film, "film")
                    }
                }

                if (item.isUserWatch) {
                    itemView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_red))
                } else {
                    lastColor = if (lastColor == R.color.dark_background) R.color.light_background else R.color.dark_background
                    itemView.setBackgroundColor(ContextCompat.getColor(requireContext(), lastColor))
                }
                expandedList.addView(itemView)
            }
            container?.addView(layout)
        }
    }
}