/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.falcofemoralis.hdrezkaapp.views.tv.grid

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.*
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.AdapterAction
import com.falcofemoralis.hdrezkaapp.interfaces.IProgressState
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.utils.FragmentOpener
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmListCallView
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmsListView

class FilmsListGridFragment : VerticalGridSupportFragment(), OnItemViewSelectedListener, OnItemViewClickedListener, FilmsListView {
    private lateinit var fragmentListener: OnFragmentInteractionListener
    private var callView: FilmListCallView? = null
    private var mAdapter: ArrayObjectAdapter? = null

    companion object {
        private val ZOOM_FACTOR: Int = FocusHighlight.ZOOM_FACTOR_MEDIUM
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRowAdapter()

        if (callView != null) {
            callView?.onFilmsListCreated()
        } else {
            Toast.makeText(requireContext(), getString(R.string.error_occured), Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRowAdapter() {
        val videoGridPresenter = VerticalGridPresenter(ZOOM_FACTOR)
        SettingsData.filmsInRow?.let {
            videoGridPresenter.numberOfColumns = it
        }
        // note: The click listeners must be called before setGridPresenter for the event listeners
        // to be properly registered on the viewholders.
        setOnItemViewSelectedListener(this)
        setOnItemViewClickedListener(this)
        setGridPresenter(videoGridPresenter)

        val cardPresenterSelector: PresenterSelector = CardPresenterSelector(requireActivity())
        // VideoCardViewPresenter videoCardViewPresenter = new VideoCardViewPresenter(getActivity());
        mAdapter = ArrayObjectAdapter(cardPresenterSelector)
        setAdapter(mAdapter)
        prepareEntranceTransition()
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        if (item is Film) {
            FragmentOpener.openWithData(this, fragmentListener, item, "film")
        }
    }

    override fun onItemSelected(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        val itemPos = mAdapter?.indexOf(item)
        if ((mAdapter!!.size() - itemPos!!) < 10) {
            setProgressBarState(IProgressState.StateType.LOADING)
            callView?.triggerEnd()
        }
    }

    override fun setFilms(films: ArrayList<Film>) {
        for (film in films) {
            mAdapter?.add(film)
        }
        startEntranceTransition()
    }

    override fun redrawFilms(from: Int, count: Int, action: AdapterAction, films: ArrayList<Film>) {
        if (films.size > 0 && action == AdapterAction.ADD) {
            mAdapter?.size()?.let { mAdapter?.addAll(it, films) }
        } else if (films.size == 0 && action == AdapterAction.DELETE) {
            mAdapter?.clear()
        } else {
            // ignored
        }

        mAdapter?.notifyArrayItemRangeChanged(from, count)

        /*   when (action) {
               AdapterAction.ADD -> mAdapter?.notifyItemRangeInserted(from, count)
               AdapterAction.UPDATE -> mAdapter?.notifyItemRangeChanged(from, count)
            //   AdapterAction.DELETE -> mAdapter?.notifyItemRangeRemoved(from, count)
           }*/
        callView?.onFilmsListDataInit()
    }

    override fun setCallView(cv: FilmListCallView) {
        callView = cv
    }

    override fun setProgressBarState(type: IProgressState.StateType) {
        /*progressBar.visibility = when (type) {
            IProgressState.StateType.LOADED -> View.GONE
            IProgressState.StateType.LOADING -> View.VISIBLE
        }*/
    }
}