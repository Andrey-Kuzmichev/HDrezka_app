package com.falcofemoralis.hdrezkaapp.views.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.AppliedFilter
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.presenters.NewestFilmsPresenter
import com.falcofemoralis.hdrezkaapp.utils.DialogManager
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.Highlighter
import com.falcofemoralis.hdrezkaapp.views.elements.RadioGridGroup
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmListCallView
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.NewestFilmsView

class NewestFilmsFragment : Fragment(), NewestFilmsView, FilmListCallView {
    private lateinit var currentView: View
    private lateinit var newestFilmsPresenter: NewestFilmsPresenter
    private lateinit var filmsListFragment: FilmsListFragment
    private lateinit var fragmentListener: OnFragmentInteractionListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        currentView = inflater.inflate(R.layout.fragment_newest_films, container, false) as LinearLayout

        initFilmsList()

        return currentView
    }

    private fun initFilmsList() {
        filmsListFragment = FilmsListFragment()
        filmsListFragment.setCallView(this)
        childFragmentManager.beginTransaction().replace(R.id.fragment_newest_films_fcv_container, filmsListFragment).commit()
    }

    override fun onFilmsListCreated() {
        newestFilmsPresenter = NewestFilmsPresenter(this, filmsListFragment)
        newestFilmsPresenter.initFilms()

        initFiltersBtn()
    }

    override fun onFilmsListDataInit() {
    }

    override fun triggerEnd() {
        newestFilmsPresenter.filmsListPresenter.getNextFilms()
    }

    private fun initFiltersBtn() {
        val builder = DialogManager.getDialog(requireContext(), null)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filters_newest, null)

        val sortGroupView: RadioGridGroup = dialogView.findViewById(R.id.film_sort)
        sortGroupView.check(
            when (SettingsData.defaultSort) {
                0 -> R.id.sort_new
                1 -> R.id.sort_last
                2 -> R.id.sort_popular
                3 -> R.id.sort_announce
                4 -> R.id.sort_now
                else -> R.id.sort_last
            }
        )
        sortGroupView.setOnCheckedChangeListener { group, checkedId ->
            run {
                if (checkedId != -1) {
                    val pos: Int = when (checkedId) {
                        R.id.sort_new -> 0
                        R.id.sort_last -> 1
                        R.id.sort_popular -> 2
                        R.id.sort_announce -> 3
                        R.id.sort_now -> 4
                        else -> 1
                    }
                    newestFilmsPresenter.setFilter(AppliedFilter.SORT, pos)
                } else {
                    group.findViewById<RadioButton>(R.id.sort_last).isChecked = true
                }
            }
        }

        val typeGroupView: RadioGridGroup = dialogView.findViewById(R.id.film_types)
        typeGroupView.setOnCheckedChangeListener { group, checkedId ->
            run {
                if (checkedId != -1) {
                    val pos: Int = when (checkedId) {
                        R.id.type_all -> 0
                        R.id.type_films -> 1
                        R.id.type_serials -> 2
                        R.id.type_multfilms -> 3
                        R.id.type_anime -> 4
                        else -> 0
                    }
                    newestFilmsPresenter.setFilter(AppliedFilter.TYPE, pos)
                } else {
                    group.findViewById<RadioButton>(R.id.type_all).isChecked = true
                }
            }
        }

        builder.setView(dialogView)
        builder.setPositiveButton(R.string.ok_text) { d, i ->
            newestFilmsPresenter.applyFilters()
            d.dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { d, i ->
            d.dismiss()
        }

        val d = builder.create()
        val filtersBtn = currentView.findViewById<TextView>(R.id.fragment_newest_films_tv_filters)
        filtersBtn.setOnClickListener {
            d.show()
        }

        Highlighter.highlightButton(filtersBtn, requireContext())
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
}