package com.falcofemoralis.hdrezkaapp.views.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.Actor
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.presenters.ActorPresenter
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.FragmentOpener
import com.falcofemoralis.hdrezkaapp.views.adapters.FilmsListRecyclerViewAdapter
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.ActorView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class ActorFragment : Fragment(), ActorView {
    private val ACTOR_ARG = "actor"

    private lateinit var currentView: View
    private lateinit var actorPresenter: ActorPresenter
    private lateinit var fragmentListener: OnFragmentInteractionListener
    private lateinit var scrollView: NestedScrollView
    private lateinit var progressBar: ProgressBar
    private var modalDialog: Dialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        currentView = inflater.inflate(R.layout.fragment_actor, container, false)

        scrollView = currentView.findViewById(R.id.fragment_actor_scroll)
        scrollView.visibility = View.GONE
        progressBar = currentView.findViewById(R.id.fragment_actor_pb_data_loading)

        actorPresenter = ActorPresenter(this, (arguments?.getSerializable(ACTOR_ARG) as Actor?)!!)
        actorPresenter.initActorData()

        return currentView
    }

    override fun setBaseInfo(actor: Actor) {
        try {
            currentView.findViewById<TextView>(R.id.fragment_actor_films_tv_name).text = actor.name
            currentView.findViewById<TextView>(R.id.fragment_actor_films_tv_name_orig).text = actor.nameOrig

            val photoView = currentView.findViewById<ImageView>(R.id.fragment_actor_films_iv_photo)
            Picasso.get().load(actor.photo).into(photoView)
            actor.photo?.let { setFullSizeImage(it) }
            photoView.setOnClickListener { openFullSizeImage() }

            if (SettingsData.deviceType == DeviceType.TV) {
                photoView.requestFocus()
            }

            currentView.findViewById<TextView>(R.id.fragment_actor_films_tv_career).text = getString(R.string.career, actor.careers)

            if (actor.age != null) {
                actor.birthday += " (${actor.age})"
            }

            if (actor.diedOnAge != null) {
                actor.deathday += " (${getString(R.string.death_in)} ${actor.diedOnAge})"
            }

            setInfo(actor.birthday, R.id.fragment_actor_films_tv_borndate, R.string.birthdate)
            setInfo(actor.birthplace, R.id.fragment_actor_films_tv_bornplace, R.string.birthplace)
            setInfo(actor.deathday, R.id.fragment_actor_films_tv_dieddate, R.string.deathdate)
            setInfo(actor.deathplace, R.id.fragment_actor_films_tv_diedplace, R.string.deathplace)

            scrollView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setInfo(data: String?, viewId: Int, textId: Int) {
        val v = currentView.findViewById<TextView>(viewId)
        if (data != null) {
            v.text = getString(textId, data)
        } else {
            v.visibility = View.GONE
        }
    }

    override fun setCareersList(careers: ArrayList<Pair<String, ArrayList<Film>>>) {
        val container: LinearLayout = currentView.findViewById(R.id.fragment_actor_ll_films)

        for (career in careers) {
            val layout = layoutInflater.inflate(R.layout.inflate_actor_career_layout, null)

            layout.findViewById<TextView>(R.id.career_header).text = career.first
            val recyclerView: RecyclerView = layout.findViewById(R.id.career_films)
            recyclerView.layoutManager = SettingsData.filmsInRow?.let { GridLayoutManager(requireContext(), it) }
            recyclerView.adapter = FilmsListRecyclerViewAdapter(career.second, ::listCallback, null)

            container.addView(layout)
        }
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

    private fun listCallback(film: Film) {
        FragmentOpener.openWithData(this, fragmentListener, film, "film")
    }

    fun setFullSizeImage(photoPath: String) {
        if (context != null) {
            val dialog = Dialog(requireActivity())
            val layout: RelativeLayout = layoutInflater.inflate(R.layout.modal_image, null) as RelativeLayout
            Picasso.get().load(photoPath).into(layout.findViewById(R.id.modal_image), object : Callback {
                override fun onSuccess() {
                    layout.findViewById<ProgressBar>(R.id.modal_progress).visibility = View.GONE
                    layout.findViewById<ImageView>(R.id.modal_image).visibility = View.VISIBLE
                }

                override fun onError(e: Exception) {
                }
            })
            dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(layout)

            val lp: WindowManager.LayoutParams = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window?.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = lp

            modalDialog = dialog
            layout.findViewById<Button>(R.id.modal_bt_close).setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    private fun openFullSizeImage() {
        modalDialog?.show()
    }
}