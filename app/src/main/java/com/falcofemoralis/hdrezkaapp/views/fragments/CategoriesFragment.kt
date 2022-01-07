package com.falcofemoralis.hdrezkaapp.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.AppliedFilter
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.presenters.CategoriesPresenter
import com.falcofemoralis.hdrezkaapp.utils.DialogManager
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.Highlighter
import com.falcofemoralis.hdrezkaapp.views.elements.RadioGridGroup
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.CategoriesView
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmListCallView

class CategoriesFragment : Fragment(), CategoriesView, AdapterView.OnItemSelectedListener, FilmListCallView {
    private lateinit var currentView: View
    private lateinit var categoriesPresenter: CategoriesPresenter
    private lateinit var filmsListFragment: FilmsListFragment
    private var dialogView: View? = null
    private var isYearsSet: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        currentView = inflater.inflate(R.layout.fragment_categories, container, false)

        initFilmsList()

        return currentView
    }

    private fun initFilmsList() {
        filmsListFragment = FilmsListFragment()
        filmsListFragment.setCallView(this)
        childFragmentManager.beginTransaction().replace(R.id.fragment_categories_fcv_container, filmsListFragment).commit()
    }

    override fun onFilmsListCreated() {
        categoriesPresenter = CategoriesPresenter(this, filmsListFragment)

        initFiltersBtn()
    }

    override fun onFilmsListDataInit() {}

    private fun initFiltersBtn() {
        val builder = DialogManager.getDialog(requireContext(), null)
        dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filters_categories, null)

        val typeGroupView: RadioGridGroup? = dialogView?.findViewById(R.id.film_types)
        typeGroupView?.setOnCheckedChangeListener { group, checkedId ->
            run {
                if (checkedId != -1) {
                    val pos: Int = when (checkedId) {
                        R.id.type_films -> 1
                        R.id.type_serials -> 3
                        R.id.type_multfilms -> 2
                        R.id.type_anime -> 0
                        else -> 1
                    }
                    categoriesPresenter.setFilter(AppliedFilter.TYPE, pos)
                } else {
                    group.findViewById<RadioButton>(R.id.type_all).isChecked = true
                }
            }
        }
        dialogView?.findViewById<LinearLayout>(R.id.filters_categories_container)?.visibility = View.GONE
        dialogView?.findViewById<LinearLayout>(R.id.filters_categories_sp_container)?.visibility = View.GONE
        dialogView?.findViewById<SmartMaterialSpinner<String>>(R.id.sp_years)?.onItemSelectedListener = this
        dialogView?.findViewById<SmartMaterialSpinner<String>>(R.id.sp_genres)?.onItemSelectedListener = this

        builder.setView(dialogView)
        builder.setPositiveButton(R.string.ok_text) { d, i ->
            categoriesPresenter.applyFilters()
            d.dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { d, i ->
            d.dismiss()
        }

        val d = builder.create()
        val filtersBtn = currentView.findViewById<TextView>(R.id.fragment_categories_films_tv_filters)
        filtersBtn.setOnClickListener {
            if (categoriesPresenter.category == null) {
                categoriesPresenter.initCategories()
            }

            d.show()
        }

        Highlighter.highlightButton(filtersBtn, requireContext())

    }

    override fun showFilters() {
        dialogView?.findViewById<LinearLayout>(R.id.filters_categories_container)?.visibility = View.VISIBLE
        dialogView?.findViewById<ProgressBar>(R.id.filters_categories_pb)?.visibility = View.GONE
    }

    override fun setFilters(years: ArrayList<String>, genres: ArrayList<String>) {
        dialogView?.findViewById<LinearLayout>(R.id.filters_categories_sp_container)?.visibility = View.VISIBLE

        if (!isYearsSet) {
            val yearsSpinner = dialogView?.findViewById<SmartMaterialSpinner<String>>(R.id.sp_years)
            yearsSpinner?.item = years
            isYearsSet = true
        }

        val genresSpinner = dialogView?.findViewById<SmartMaterialSpinner<String>>(R.id.sp_genres)
        genresSpinner?.item = genres
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        when (parent.id) {
            R.id.sp_genres -> {
                categoriesPresenter.setFilter(AppliedFilter.GENRES, position)
            }
            R.id.sp_years -> {
                categoriesPresenter.setFilter(AppliedFilter.YEARS, position)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun triggerEnd() {
        categoriesPresenter.filmsListPresenter.getNextFilms()
    }

    override fun showList() {
        currentView.findViewById<TextView>(R.id.fragment_categories_tv_msg).visibility = View.GONE
        currentView.findViewById<FragmentContainerView>(R.id.fragment_categories_fcv_container).visibility = View.VISIBLE
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