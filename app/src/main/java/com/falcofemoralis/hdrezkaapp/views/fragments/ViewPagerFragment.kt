package com.falcofemoralis.hdrezkaapp.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.UpdateItem
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.views.adapters.ViewPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class ViewPagerFragment : Fragment() {
    private lateinit var currentView: View
    private lateinit var viewPager2: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentList: ArrayList<Fragment>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        currentView = inflater.inflate(R.layout.fragment_view_pager, container, false)

        setBottomBar()
        setAdapter()

        return currentView
    }

    private fun setBottomBar() {
        bottomNavigationView = currentView.findViewById(R.id.fragment_pager_nv_bottomBar)
        viewPager2 = currentView.findViewById(R.id.fragment_pager_viewPager2)
        viewPager2.offscreenPageLimit = 5

        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            val pagerItem = when (item.itemId) {
                R.id.nav_newest -> 0
                R.id.nav_categories -> 1
                R.id.nav_search -> 2
                R.id.nav_bookmarks -> 3
                R.id.nav_watch -> 4
                else -> 0
            }
            viewPager2.currentItem = pagerItem
            true
        }
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> bottomNavigationView.menu.findItem(R.id.nav_newest).isChecked = true
                    1 -> bottomNavigationView.menu.findItem(R.id.nav_categories).isChecked = true
                    2 -> bottomNavigationView.menu.findItem(R.id.nav_search).isChecked = true
                    3 -> bottomNavigationView.menu.findItem(R.id.nav_bookmarks).isChecked = true
                    4 -> bottomNavigationView.menu.findItem(R.id.nav_watch).isChecked = true
                }
            }
        })
    }

    fun setAdapter() {
        fragmentList = arrayListOf(
            NewestFilmsFragment(),
            CategoriesFragment(),
            SearchFragment(),
            BookmarksFragment(),
            WatchLaterFragment()

        )

        if(context != null){
            viewPager2.adapter = ViewPagerAdapter(fragmentList, childFragmentManager, lifecycle)
            SettingsData.mainScreen?.let {
                viewPager2.setCurrentItem(it, false)
            }
        }
    }

    fun updatePage(item: UpdateItem) {
        when (item) {
            UpdateItem.BOOKMARKS_CHANGED -> (fragmentList[3] as BookmarksFragment).redrawBookmarks()
            UpdateItem.BOOKMARKS_FILMS_CHANGED -> (fragmentList[3] as BookmarksFragment).redrawBookmarksFilms()
            UpdateItem.WATCH_LATER_CHANGED -> (fragmentList[4] as WatchLaterFragment).updateAdapter()
        }
    }

    fun setVoiceCommand() {
        if (fragmentList[viewPager2.currentItem] is SearchFragment) {
            (fragmentList[viewPager2.currentItem] as SearchFragment).showVoiceDialog()
        }
    }

    fun showVoiceCommand(spokenText: String?) {
        if (fragmentList[viewPager2.currentItem] is SearchFragment) {
            (fragmentList[viewPager2.currentItem] as SearchFragment).showVoiceResult(spokenText)
        }
    }
}