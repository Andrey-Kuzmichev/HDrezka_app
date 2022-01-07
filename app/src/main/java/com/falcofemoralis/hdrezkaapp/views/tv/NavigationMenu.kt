package com.falcofemoralis.hdrezkaapp.views.tv

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.NavigationMenuTabs
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.views.tv.interfaces.FragmentChangeListener
import com.falcofemoralis.hdrezkaapp.views.tv.interfaces.NavigationStateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nikartm.support.ImageBadgeView

class NavigationMenu : Fragment() {
    private lateinit var fragmentChangeListener: FragmentChangeListener
    private lateinit var navigationStateListener: NavigationStateListener
    private lateinit var currentView: View

    private lateinit var notify_IB: ImageBadgeView
    private lateinit var newest_IB: ImageButton
    private lateinit var categories_IB: ImageButton
    private lateinit var search_IB: ImageButton
    private lateinit var bookmarks_IB: ImageButton
    private lateinit var later_IB: ImageButton
    private lateinit var settings_IB: ImageButton

    private lateinit var notify_TV: TextView
    private lateinit var newest_TV: TextView
    private lateinit var categories_TV: TextView
    private lateinit var search_TV: TextView
    private lateinit var bookmarks_TV: TextView
    private lateinit var later_TV: TextView
    private lateinit var settings_TV: TextView

    private val seriesUpdates = NavigationMenuTabs.nav_menu_series_updates
    private val newestFilms = NavigationMenuTabs.nav_menu_newest
    private val categories = NavigationMenuTabs.nav_menu_categories
    private val search = NavigationMenuTabs.nav_menu_search
    private val bookmarks = NavigationMenuTabs.nav_menu_bookmarks
    private val later = NavigationMenuTabs.nav_menu_later
    private var settings = NavigationMenuTabs.nav_menu_settings

    private var lastSelectedMenu: String? = newestFilms
    private var menuTextAnimationDelay = 0 //200
    private var _context: Context? = null

    companion object {
        var notifyBtn: ImageBadgeView? = null
        var isFree = true
        var isFocusOut = false
        var closed = false
        var isLocked = false
        var isViewOnHover = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentView = inflater.inflate(R.layout.fragment_nav_menu, container, false) as View

        notify_IB = currentView.findViewById(R.id.notify_IB)
        newest_IB = currentView.findViewById(R.id.newest_IB)
        categories_IB = currentView.findViewById(R.id.categories_IB)
        search_IB = currentView.findViewById(R.id.search_IB)
        bookmarks_IB = currentView.findViewById(R.id.bookmarks_IB)
        later_IB = currentView.findViewById(R.id.later_IB)
        settings_IB = currentView.findViewById(R.id.settings_IB)

        notify_TV = currentView.findViewById(R.id.notify_TV)
        newest_TV = currentView.findViewById(R.id.newest_TV)
        categories_TV = currentView.findViewById(R.id.categories_TV)
        search_TV = currentView.findViewById(R.id.search_TV)
        bookmarks_TV = currentView.findViewById(R.id.bookmarks_TV)
        later_TV = currentView.findViewById(R.id.later_TV)
        settings_TV = currentView.findViewById(R.id.settings_TV)

        notifyBtn = notify_IB

        return currentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _context = context

        GlobalScope.launch {
            Thread.sleep(100)
            withContext(Dispatchers.Main) {
                //Navigation Menu Options Focus, Key Listeners
                setListener(notify_IB, notify_TV, seriesUpdates, R.drawable.ic_baseline_notifications_24_sel, R.drawable.ic_baseline_notifications_24, -1)

                setListener(newest_IB, newest_TV, newestFilms, R.drawable.ic_baseline_movie_24_sel, R.drawable.ic_baseline_movie_24, 0)

                setListener(categories_IB, categories_TV, categories, R.drawable.ic_baseline_categories_24_sel, R.drawable.ic_baseline_categories_24, 1)

                setListener(search_IB, search_TV, search, R.drawable.ic_baseline_search_24_sel, R.drawable.ic_baseline_search_24, 2)

                setListener(bookmarks_IB, bookmarks_TV, bookmarks, R.drawable.ic_baseline_bookmarks_24_sel, R.drawable.ic_baseline_bookmarks_24, 3)

                setListener(later_IB, later_TV, later, R.drawable.ic_baseline_watch_later_24_sel, R.drawable.ic_baseline_watch_later_24, 4)

                setListener(settings_IB, settings_TV, settings, R.drawable.ic_baseline_settings_24_sel, R.drawable.ic_baseline_settings_24, -1)

                setOnHoverListener()
            }
        }
    }

    private fun setListener(ib: ImageView, tv: TextView, lastMenu: String, selectedImage: Int, unselectedImage: Int, id: Int) {
        if (SettingsData.mainScreen == id) {
            lastSelectedMenu = lastMenu
            setMenuIconFocusView(selectedImage, ib)
            setMenuNameFocusView(tv, true)
        }

        ib.setOnFocusChangeListener { v, hasFocus ->
            if (isFree && !isLocked) {
                if (hasFocus) {
                    if (isNavigationOpen()) {
                        setFocusedView(ib, selectedImage)
                        setMenuNameFocusView(tv, true)
                        focusIn(ib)
                    } else {
                        closed = false
                        openNav()
                    }
                } else {
                    if (isNavigationOpen()) {
                        // false by default,
                        if (isFocusOut) {
                            isFocusOut = false
                        } else {
                            setOutOfFocusedView(ib, unselectedImage)
                        }
                        setMenuNameFocusView(tv, false)
                        focusOut(ib)
                    }
                }
            }
        }

        ib.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {//only when key is pressed down
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (!closed) {
                            isFocusOut = true
                            closeNav()
                            navigationStateListener.onStateChanged(false, lastSelectedMenu)
                        }
                    }
                    KeyEvent.KEYCODE_ENTER -> {
                        closed = true
                        lastSelectedMenu = lastMenu
                        fragmentChangeListener.switchFragment(lastMenu)
                        focusOut(ib)
                        // closeNav()
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!ib.isFocusable)
                            ib.isFocusable = true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER -> {
                        closed = true
                        lastSelectedMenu = lastMenu
                        fragmentChangeListener.switchFragment(lastMenu)
                        closeNav()
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (isNavigationOpen()) {
                            closeNav()
                        }
                    }
                }
            }
            false
        }

        ib.setOnClickListener {
            if (isNavigationOpen()) {
                lastSelectedMenu = lastMenu
                fragmentChangeListener.switchFragment(lastMenu)
                closeNav()
                closed = true
            } else {
                openNav()
            }
        }

        tv.setOnClickListener {
            if (isNavigationOpen()) {
                highlightMenuSelection(lastMenu)
                lastSelectedMenu = lastMenu
                fragmentChangeListener.switchFragment(lastMenu)
                closeNav()
            }
        }

        tv.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    //   highlightMenuSelection(lastMenu)
                    tv.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary_red
                        )
                    )
                }
                MotionEvent.ACTION_UP -> {
                    //  unHighlightMenuSelections(lastMenu)
                    tv.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    )
                }
            }

            v?.onTouchEvent(event) ?: true
        }

        fun onHoverListener(event: MotionEvent) {
            if (isFree && !isLocked) {
                when (event.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        isViewOnHover = true

                        if (lastSelectedMenu != lastMenu) {
                            setFocusedView(ib, selectedImage)
                            setMenuNameFocusView(tv, true)
                            focusIn(ib)
                        }
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        if (lastSelectedMenu != lastMenu) {
                            if (isFocusOut) {
                                isFocusOut = false
                            } else {
                                setOutOfFocusedView(ib, unselectedImage)
                            }
                            setMenuNameFocusView(tv, false)
                            focusOut(ib)
                        }
                    }
                }
            }
        }

        ib.setOnHoverListener { v, event ->
            onHoverListener(event)
            true
        }

        tv.setOnHoverListener { v, event ->
            onHoverListener(event)
            true
        }
    }

    private fun setOnHoverListener() {
        currentView.findViewById<ConstraintLayout>(R.id.open_nav_CL).setOnHoverListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    if (isViewOnHover) {
                        isViewOnHover = false
                    } else {
                        openNav()
                        closed = false
                    }
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    if (!isViewOnHover) {
                        closeNav()
                        closed = true
                    }
                }
            }
            true

        }
    }

    private fun setOutOfFocusedView(view: ImageView, resource: Int) {
        setMenuIconFocusView(resource, view)
    }

    private fun setFocusedView(view: ImageView, resource: Int) {
        setMenuIconFocusView(resource, view)
    }

    /**
     * Setting animation when focus is lost
     */
    private fun focusOut(v: View) {
        val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.2f, 1f)
        val set = AnimatorSet()
        set.play(scaleX).with(scaleY)
        set.start()
    }

    /**
     * Setting the animation when getting focus
     */
    private fun focusIn(v: View) {
        val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.2f)
        val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.2f)
        val set = AnimatorSet()
        set.play(scaleX).with(scaleY)
        set.start()
    }

    private fun setMenuIconFocusView(resource: Int, view: ImageView) {
        view.setImageResource(resource)
    }

    private fun setMenuNameFocusView(view: TextView, inFocus: Boolean) {
        if (inFocus) {
            view.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary_red
                )
            )
        } else
            view.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
    }

    private fun openNav() {
        enableNavMenuViews(View.VISIBLE)
        val lp = FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
        currentView.layoutParams = lp
        navigationStateListener.onStateChanged(true, lastSelectedMenu)

        when (lastSelectedMenu) {
            seriesUpdates -> {
                notify_IB.requestFocus()
                setMenuNameFocusView(notify_TV, true)
            }
            categories -> {
                categories_IB.requestFocus()
                setMenuNameFocusView(categories_TV, true)
            }
            newestFilms -> {
                newest_IB.requestFocus()
                setMenuNameFocusView(newest_TV, true)
            }
            search -> {
                search_IB.requestFocus()
                setMenuNameFocusView(search_TV, true)
            }
            bookmarks -> {
                bookmarks_IB.requestFocus()
                setMenuNameFocusView(bookmarks_TV, true)
            }
            later -> {
                later_IB.requestFocus()
                setMenuNameFocusView(later_TV, true)
            }
            settings -> {
                settings_IB.requestFocus()
                setMenuNameFocusView(settings_TV, true)
            }
        }
    }

    private fun closeNav() {
        enableNavMenuViews(View.GONE)
        val lp = FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
        currentView.layoutParams = lp

        //highlighting last selected menu icon
        highlightMenuSelection(lastSelectedMenu)

        //Setting out of focus views for menu icons, names
        unHighlightMenuSelections(lastSelectedMenu)
    }

    private fun unHighlightMenuSelections(lastSelectedMenu: String?) {
        if (!lastSelectedMenu.equals(seriesUpdates, true)) {
            setOutOfFocusedView(notify_IB, R.drawable.ic_baseline_notifications_24)
            setMenuNameFocusView(notify_TV, false)
        }
        if (!lastSelectedMenu.equals(newestFilms, true)) {
            setOutOfFocusedView(newest_IB, R.drawable.ic_baseline_movie_24)
            setMenuNameFocusView(newest_TV, false)
        }
        if (!lastSelectedMenu.equals(categories, true)) {
            setOutOfFocusedView(categories_IB, R.drawable.ic_baseline_categories_24)
            setMenuNameFocusView(categories_TV, false)
        }
        if (!lastSelectedMenu.equals(search, true)) {
            setOutOfFocusedView(search_IB, R.drawable.ic_baseline_search_24)
            setMenuNameFocusView(search_TV, false)
        }
        if (!lastSelectedMenu.equals(bookmarks, true)) {
            setOutOfFocusedView(bookmarks_IB, R.drawable.ic_baseline_bookmarks_24)
            setMenuNameFocusView(bookmarks_TV, false)
        }
        if (!lastSelectedMenu.equals(later, true)) {
            setOutOfFocusedView(later_IB, R.drawable.ic_baseline_watch_later_24)
            setMenuNameFocusView(later_TV, false)
        }
        if (!lastSelectedMenu.equals(settings, true)) {
            setOutOfFocusedView(settings_IB, R.drawable.ic_baseline_settings_24)
            setMenuNameFocusView(settings_TV, false)
        }
    }

    private fun highlightMenuSelection(lastSelectedMenu: String?) {
        when (lastSelectedMenu) {
            seriesUpdates -> {
                setFocusedView(notify_IB, R.drawable.ic_baseline_notifications_24_sel)
            }
            newestFilms -> {
                setFocusedView(newest_IB, R.drawable.ic_baseline_movie_24_sel)
            }
            categories -> {
                setFocusedView(categories_IB, R.drawable.ic_baseline_categories_24_sel)
            }
            search -> {
                setFocusedView(search_IB, R.drawable.ic_baseline_search_24_sel)
            }
            bookmarks -> {
                setFocusedView(bookmarks_IB, R.drawable.ic_baseline_bookmarks_24_sel)
            }
            later -> {
                setFocusedView(later_IB, R.drawable.ic_baseline_watch_later_24_sel)
            }
            settings -> {
                setFocusedView(settings_IB, R.drawable.ic_baseline_settings_24_sel)
            }
        }
    }

    private fun enableNavMenuViews(visibility: Int) {
        if (visibility == View.GONE) {
            animateMenuNamesEntry(notify_TV, visibility, 1, R.anim.slide_in_right_menu_name)
            /* menuTextAnimationDelay = 0//200 //reset
             newest_TV.visibility = visibility
             categories_TV.visibility = visibility
             search_TV.visibility = visibility
             bookmarks_TV.visibility = visibility
             later_TV.visibility = visibility
             settings_TV.visibility = visibility*/
        } else {
            animateMenuNamesEntry(notify_TV, visibility, 1, R.anim.slide_in_left_menu_name)
        }
    }

    private fun animateMenuNamesEntry(view: View, visibility: Int, viewCode: Int, anim: Int) {
        if (_context == null) {
            return
        }

        view.postDelayed({
            val animate = AnimationUtils.loadAnimation(_context, anim)

            if (visibility == View.GONE) {
                val duration = context?.resources?.getInteger(R.integer.animation_duration)?.toLong()
                duration?.let {
                    animate.duration = it
                }
                animate.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        view.visibility = visibility
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })
            } else {
                view.visibility = visibility
            }
            view.startAnimation(animate)

            menuTextAnimationDelay = 0 // 100

            // step by step animation
            when (viewCode) {
                1 -> {
                    animateMenuNamesEntry(newest_TV, visibility, viewCode + 1, anim)
                }
                2 -> {
                    animateMenuNamesEntry(categories_TV, visibility, viewCode + 1, anim)
                }
                3 -> {
                    animateMenuNamesEntry(search_TV, visibility, viewCode + 1, anim)
                }
                4 -> {
                    animateMenuNamesEntry(bookmarks_TV, visibility, viewCode + 1, anim)
                }
                5 -> {
                    animateMenuNamesEntry(later_TV, visibility, viewCode + 1, anim)
                }
                6 -> {
                    animateMenuNamesEntry(settings_TV, visibility, viewCode + 1, anim)
                }
            }
        }, menuTextAnimationDelay.toLong())
    }

    private fun isNavigationOpen() = newest_TV.visibility == View.VISIBLE

    fun setFragmentChangeListener(callback: FragmentChangeListener) {
        this.fragmentChangeListener = callback
    }

    fun setNavigationStateListener(callback: NavigationStateListener) {
        this.navigationStateListener = callback
    }
}
