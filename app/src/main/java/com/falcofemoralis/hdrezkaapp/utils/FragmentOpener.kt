package com.falcofemoralis.hdrezkaapp.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.Actor
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.views.fragments.ActorFragment
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment
import com.falcofemoralis.hdrezkaapp.views.fragments.SettingsFragment
import com.falcofemoralis.hdrezkaapp.views.tv.NavigationMenu
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable

object FragmentOpener {
    private var isCommitInProgress = false

    fun <T> openWithData(source: Fragment?, fragmentListener: OnFragmentInteractionListener, data: T, dataTag: String) where T : Serializable {
        if (isCommitInProgress) {
            return
        }

        isCommitInProgress = true
        val dataBundle = Bundle()
        dataBundle.putSerializable(dataTag, data)

        val frag = when (data) {
            is Actor -> ActorFragment()
            is Film -> FilmFragment()
            else -> Fragment()
        }

        if (SettingsData.deviceType == DeviceType.TV) {
            NavigationMenu.isFree = false
        }
        fragmentListener.onFragmentInteraction(source, frag, OnFragmentInteractionListener.Action.NEXT_FRAGMENT_HIDE, true, null, dataBundle, ::callback, null)
    }

    fun openFragment(source: Fragment, fragment: Fragment, fragmentListener: OnFragmentInteractionListener){
        if (isCommitInProgress) {
            return
        }

        isCommitInProgress = true
        if (SettingsData.deviceType == DeviceType.TV) {
            NavigationMenu.isFree = false
        }
        fragmentListener.onFragmentInteraction(source, fragment, OnFragmentInteractionListener.Action.NEXT_FRAGMENT_HIDE, true, null, null, ::callback, null)
    }

    fun callback() {
        GlobalScope.launch {
            delay(1000)
            isCommitInProgress = false

            if (SettingsData.deviceType == DeviceType.TV) {
                NavigationMenu.isFree = true
            }
        }
    }
}