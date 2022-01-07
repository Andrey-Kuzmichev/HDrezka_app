package com.falcofemoralis.hdrezkaapp.views.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.AdapterAction
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.IMsg
import com.falcofemoralis.hdrezkaapp.interfaces.IProgressState
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.objects.UserData
import com.falcofemoralis.hdrezkaapp.objects.WatchLater
import com.falcofemoralis.hdrezkaapp.presenters.WatchLaterPresenter
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.FragmentOpener
import com.falcofemoralis.hdrezkaapp.views.adapters.WatchLaterRecyclerViewAdapter
import com.falcofemoralis.hdrezkaapp.views.tv.NavigationMenu
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.WatchLaterView

class WatchLaterFragment : Fragment(), WatchLaterView {
    private lateinit var currentView: View
    private lateinit var watchLaterPresenter: WatchLaterPresenter
    private lateinit var listView: RecyclerView
    private lateinit var fragmentListener: OnFragmentInteractionListener
    private lateinit var progressBar: ProgressBar
    private lateinit var msgView: TextView
    private lateinit var scrollView: NestedScrollView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentView = inflater.inflate(R.layout.fragment_watch_later_list, container, false)

        if(SettingsData.deviceType == DeviceType.TV) {
            NavigationMenu.isLocked = true
        }

        listView = currentView.findViewById(R.id.fragment_watch_later_list_rv)
        progressBar = currentView.findViewById(R.id.fragment_watch_later_list_pb_loading)
        msgView = currentView.findViewById(R.id.fragment_watch_later_list_tv_msg)

        scrollView = currentView.findViewById(R.id.fragment_watch_later_nsv_films)
        scrollView.setOnScrollChangeListener(object : NestedScrollView.OnScrollChangeListener {
            override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                val view = scrollView.getChildAt(scrollView.childCount - 1)
                val diff = view.bottom - (scrollView.height + scrollView.scrollY)

                if (diff == 0) {
                    setProgressBarState(IProgressState.StateType.LOADING)
                    watchLaterPresenter.getNextWatchLater()
                }
            }
        })

        if (UserData.isLoggedIn == true) {
            listView.layoutManager = LinearLayoutManager(context)
            watchLaterPresenter = WatchLaterPresenter(this)
            watchLaterPresenter.initList()
        } else {
            showMsg(IMsg.MsgType.NOT_AUTHORIZED)
        }

        return currentView
    }

    override fun setWatchLaterList(list: ArrayList<WatchLater>) {
        listView.adapter = WatchLaterRecyclerViewAdapter(requireContext(), list, ::listCallback, ::deleteWatchLater)
        progressBar.visibility = View.GONE

        if(SettingsData.deviceType == DeviceType.TV) {
            NavigationMenu.isLocked = false
        }
    }

    private fun listCallback(film: Film) {
        FragmentOpener.openWithData(this, fragmentListener, film, "film")
    }

    private fun deleteWatchLater(id: String) {
        watchLaterPresenter.deleteWatchLaterItem(id)
    }

    override fun showMsg(type: IMsg.MsgType) {
        msgView.visibility = View.VISIBLE
        listView.visibility = View.GONE
        progressBar.visibility = View.GONE

        if(context != null) {
            val text = when (type) {
                IMsg.MsgType.NOT_AUTHORIZED -> getString(R.string.register_user_only)
                IMsg.MsgType.NOTHING_ADDED -> getString(R.string.empty_watch_later)
                IMsg.MsgType.NOTHING_FOUND -> getString(R.string.nothing_found)
            }

            msgView.text = text
        }

        if(SettingsData.deviceType == DeviceType.TV) {
            NavigationMenu.isLocked = false
        }
    }

    override fun hideMsg() {
        msgView.visibility = View.GONE
    }

    override fun setProgressBarState(type: IProgressState.StateType) {
        progressBar.visibility = when (type) {
            IProgressState.StateType.LOADED -> View.GONE
            IProgressState.StateType.LOADING -> View.VISIBLE
        }
    }

    override fun showConnectionError(type: IConnection.ErrorType, errorText: String) {
        try{
            if(context != null){
                ExceptionHelper.showToastError(requireContext(), type, errorText)
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun redrawWatchLaterList(from: Int, count: Int, action: AdapterAction) {
        //listView.recycledViewPool.clear();
        when (action) {
            AdapterAction.ADD -> listView.adapter?.notifyItemRangeInserted(from, count)
            AdapterAction.UPDATE -> listView.adapter?.notifyItemRangeChanged(from, count)
            AdapterAction.DELETE -> listView.adapter?.notifyItemRangeRemoved(from, count)
        }
    }


    fun updateAdapter() {
        if (UserData.isLoggedIn == true) {
            watchLaterPresenter.updateList()
        }
    }
}