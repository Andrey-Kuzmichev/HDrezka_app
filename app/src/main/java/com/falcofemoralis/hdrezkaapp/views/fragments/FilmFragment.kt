package com.falcofemoralis.hdrezkaapp.views.fragments

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.*
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.underline
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.clients.PlayerChromeClient
import com.falcofemoralis.hdrezkaapp.clients.PlayerJsInterface
import com.falcofemoralis.hdrezkaapp.clients.PlayerWebViewClient
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.constants.UpdateItem
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.models.ActorModel
import com.falcofemoralis.hdrezkaapp.objects.*
import com.falcofemoralis.hdrezkaapp.presenters.FilmPresenter
import com.falcofemoralis.hdrezkaapp.utils.*
import com.falcofemoralis.hdrezkaapp.utils.Highlighter.zoom
import com.falcofemoralis.hdrezkaapp.views.MainActivity
import com.falcofemoralis.hdrezkaapp.views.adapters.CommentsRecyclerViewAdapter
import com.falcofemoralis.hdrezkaapp.views.elements.CommentEditor
import com.falcofemoralis.hdrezkaapp.views.tv.player.PlayerActivity
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmView
import com.github.aakira.expandablelayout.ExpandableLinearLayout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.willy.ratingbar.ScaleRatingBar
import java.io.File


class FilmFragment : Fragment(), FilmView {
    private val FILM_ARG = "film"
    private lateinit var currentView: View
    private lateinit var filmPresenter: FilmPresenter
    private lateinit var fragmentListener: OnFragmentInteractionListener
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: NestedScrollView
    private lateinit var commentsList: RecyclerView
    private lateinit var imm: InputMethodManager
    private var commentsAdded: Boolean = false
    private var modalDialog: Dialog? = null
    private var commentEditor: CommentEditor? = null
    private var bookmarksDialog: AlertDialog? = null
    private var wl: PowerManager.WakeLock? = null
    private var isWebviewInstalled = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onDestroy() {
        if (isWebviewInstalled) {
            playerView?.destroy()
        }

        if (SettingsData.isPlayer == false) {
            activity?.window?.clearFlags(FLAG_KEEP_SCREEN_ON)

            if (SettingsData.deviceType == DeviceType.TV && wl?.isHeld == true) {
                try {
                    wl?.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        PlayerJsInterface.notifyanager?.cancel(0)

        super.onDestroy()
    }

    override fun onResume() {
        if (isWebviewInstalled) {
            presenter = filmPresenter
        }
        if (isFullscreen) {
            requireActivity().window.decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
        super.onResume()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            //do when hidden
            PlayerJsInterface.stop()
            PlayerJsInterface.notifyanager?.cancel(0)
        } else {
            //do when show
            presenter = filmPresenter
            isFullscreen = false
        }
    }

    companion object {
        var playerView: WebView? = null
        var presenter: FilmPresenter? = null
        var isFullscreen: Boolean = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (SettingsData.deviceType != DeviceType.TV && context?.packageManager != null && context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_WEBVIEW) == false) {
            Toast.makeText(requireContext(), getString(R.string.no_webview_installed), Toast.LENGTH_LONG).show()
            isWebviewInstalled = false
            return inflater.inflate(R.layout.empty_layout, container, false)
        }
        currentView = inflater.inflate(R.layout.fragment_film, container, false)

        progressBar = currentView.findViewById(R.id.fragment_film_pb_loading)
        playerView = currentView.findViewById(R.id.fragment_film_wv_player)
        commentsList = currentView.findViewById(R.id.fragment_film_rv_comments)

        filmPresenter = FilmPresenter(this, (arguments?.getSerializable(FILM_ARG) as Film?)!!)

        filmPresenter.initFilmData()

        initFlags()

        initScroll()

        initFullSizePoster()

        initDownloadBtn()

        initPlayer()

        return currentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (SettingsData.deviceType == DeviceType.TV) {
            currentView.findViewById<TextView>(R.id.fragment_film_tv_open_player).requestFocus()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("InvalidWakeLockTag")
    private fun initFlags() {
        if (SettingsData.isPlayer == false) {
            activity?.window?.addFlags(FLAG_KEEP_SCREEN_ON)

            if (SettingsData.deviceType == DeviceType.TV) {
                val pm: PowerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
                wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "tag")
                wl?.acquire(300 * 60 * 1000L)
            }
        }
    }

    private fun initScroll() {
        scrollView = currentView.findViewById(R.id.fragment_film_sv_content)
        scrollView.setOnScrollChangeListener(object : NestedScrollView.OnScrollChangeListener {
            override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                val view = scrollView.getChildAt(scrollView.childCount - 1)
                val diff = view.bottom - (scrollView.height + scrollView.scrollY)

                if (diff == 0) {
                    if (!commentsAdded) {
                        filmPresenter.initComments()
                        commentsAdded = true
                    }
                    filmPresenter.getNextComments()
                }
            }
        })
        scrollView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    private fun initFullSizePoster() {
        val btn = currentView.findViewById<ImageView>(R.id.fragment_film_iv_poster)
        btn.setOnClickListener {
            openFullSizeImage()
        }

        btn.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.scale_in_tv)
                v.startAnimation(anim)
                anim.fillAfter = true
            } else {
                val anim: Animation = AnimationUtils.loadAnimation(context, R.anim.scale_out_tv)
                v.startAnimation(anim)
                anim.fillAfter = true
            }
        }
    }

    private fun initDownloadBtn() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                filmPresenter.showTranslations(true)
            } else {
                Toast.makeText(requireContext(), getString(R.string.perm_write_hint), Toast.LENGTH_LONG).show()
            }
        }
        val downloadBtn = currentView.findViewById<View>(R.id.fragment_film_btn_download)
        downloadBtn.setOnClickListener {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    filmPresenter.showTranslations(true)
                }
                else -> {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    requestPermissionLauncher.launch(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
        }

        Highlighter.highlightText(downloadBtn, requireContext())
    }

    override fun setTrailer(link: String?) {
        val trailerBtn = currentView.findViewById<TextView>(R.id.fragment_film_tv_trailer)
        if (link != null && link.isNotEmpty()) {
            trailerBtn.setOnClickListener {
                val linkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(filmPresenter.film.youtubeLink))

                try {
                    startActivity(linkIntent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.no_yt_player), Toast.LENGTH_LONG).show()
                }
            }
            Highlighter.highlightText(trailerBtn, requireContext())
        } else {
            trailerBtn.visibility = View.GONE
        }
    }

    override fun showMsg(msgType: IConnection.ErrorType) {
        if (msgType == IConnection.ErrorType.PARSING_ERROR) {
            Toast.makeText(requireContext(), getString(R.string.server_error_503), Toast.LENGTH_SHORT).show()
        } else if (msgType == IConnection.ErrorType.EMPTY) {
            Toast.makeText(requireContext(), getString(R.string.blocked_in_region), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPlayer() {
        val openPlayBtn = currentView.findViewById<TextView>(R.id.fragment_film_tv_open_player)

        if (SettingsData.deviceType == DeviceType.TV) {
            openPlayBtn.requestFocus()
        }

        if (SettingsData.isPlayer == true || SettingsData.deviceType == DeviceType.TV) {
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    filmPresenter.showTranslations(false)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.perm_write_hint), Toast.LENGTH_LONG).show()
                }
            }

            openPlayBtn.setOnClickListener {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                        filmPresenter.showTranslations(false)
                    }
                    else -> {
                        // You can directly ask for the permission.
                        // The registered ActivityResultCallback gets the result of this request.
                        requestPermissionLauncher.launch(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }
                }
            }

            if (SettingsData.deviceType == DeviceType.MOBILE) {
                currentView.findViewById<LinearLayout>(R.id.fragment_film_ll_player_container).visibility = View.GONE
            }

            Highlighter.highlightButton(openPlayBtn, requireContext())
        } else {
            filmPresenter.initPlayer()

            openPlayBtn.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun setPlayer(link: String) {
        val container: LinearLayout = currentView.findViewById(R.id.fragment_film_ll_player_container)

        playerView?.settings?.userAgentString = SettingsData.useragent
        playerView?.settings?.javaScriptEnabled = true
        playerView?.settings?.domStorageEnabled = true
        playerView?.addJavascriptInterface(WebAppInterface(requireActivity()), "Android")
        playerView?.addJavascriptInterface(PlayerJsInterface(requireContext()), "JSOUT")
        playerView?.webViewClient = PlayerWebViewClient(requireContext(), this, filmPresenter.film) {
            container.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            currentView.findViewById<ProgressBar>(R.id.fragment_film_pb_player_loading).visibility = View.GONE
            playerView?.visibility = View.VISIBLE
        }

        playerView?.webChromeClient = activity?.let { PlayerChromeClient(it) }
        playerView?.loadUrl(link)
    }

    class WebAppInterface(private val act: FragmentActivity) {
        @JavascriptInterface
        fun updateWatchLater() {
            (act as MainActivity).redrawPage(UpdateItem.WATCH_LATER_CHANGED)
        }
    }

    override fun setFilmBaseData(film: Film) {
        filmPresenter.initActors()
        filmPresenter.initFullSizeImage()

        Picasso.get().load(film.posterPath).into(currentView.findViewById<ImageView>(R.id.fragment_film_iv_poster))

        currentView.findViewById<TextView>(R.id.fragment_film_tv_title).text = film.title
        val origTileView = currentView.findViewById<TextView>(R.id.fragment_film_tv_origtitle)
        if (!film.origTitle.isNullOrEmpty()) {
            origTileView.text = film.origTitle
        } else {
            origTileView.visibility = View.GONE
        }

        val dateView = currentView.findViewById<TextView>(R.id.fragment_film_tv_releaseDate)
        if (film.date != null) {
            dateView.text = when (SettingsData.deviceType) {
                DeviceType.TV -> "${film.date} ${film.year}"
                else -> getString(R.string.release_date, "${film.date} ${film.year}")
            }
        } else {
            dateView.visibility = View.GONE
        }
        currentView.findViewById<TextView>(R.id.fragment_film_tv_runtime).text = when (SettingsData.deviceType) {
            DeviceType.TV -> film.runtime
            else -> getString(R.string.runtime, film.runtime)
        }
        currentView.findViewById<TextView>(R.id.fragment_film_tv_type).text = when (SettingsData.deviceType) {
            DeviceType.TV -> film.type
            else -> getString(R.string.film_type, film.type)
        }

        currentView.findViewById<TextView>(R.id.fragment_film_tv_plot).text = film.description

        // data loaded
        scrollView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    override fun setFilmRatings(film: Film) {
        setRating(R.id.fragment_film_tv_ratingIMDB, R.string.imdb, film.ratingIMDB, film.votesIMDB)
        setRating(R.id.fragment_film_tv_ratingKP, R.string.kp, film.ratingKP, film.votesKP)
        setRating(R.id.fragment_film_tv_ratingWA, R.string.wa, film.ratingWA, film.votesWA)
        setRating(R.id.fragment_film_tv_ratingHR, R.string.hr, film.ratingHR, "(${film.votesHR})")
    }

    private fun setRating(viewId: Int, stringId: Int, rating: String?, votes: String?) {
        val ratingTextView: TextView = currentView.findViewById(viewId)
        if (rating != null && votes != null && rating.isNotEmpty() && votes.isNotEmpty()) {
            val ss = SpannableStringBuilder()
            ss.bold { ss.underline { append(getString(stringId)) } }
            ss.append(": $rating $votes")
            ratingTextView.text = ss
        } else {
            ratingTextView.visibility = View.GONE
        }
    }

    override fun setActors(actors: ArrayList<Actor>?) {
        val actorsLayout: LinearLayout = currentView.findViewById(R.id.fragment_film_ll_actorsLayout)

        if (actors == null) {
            currentView.findViewById<LinearLayout>(R.id.fragment_film_ll_actorsContainer).visibility = View.GONE
            return
        }

        context?.let {
            for (actor in actors.reversed()) {
                val layout: LinearLayout = LayoutInflater.from(it).inflate(R.layout.inflate_actor, null) as LinearLayout
                val nameView: TextView = layout.findViewById(R.id.actor_name)
                val careerView: TextView = layout.findViewById(R.id.actor_career)
                val actorLayout: LinearLayout = layout.findViewById(R.id.actor_layout)
                val actorPhoto: ImageView = layout.findViewById(R.id.actor_photo)
                nameView.text = actor.name
                careerView.text = actor.careers

                if (actor.photo != null && actor.photo!!.isNotEmpty() && actor.photo?.contains(ActorModel.NO_PHOTO) == false) {
                    val actorProgress: ProgressBar = layout.findViewById(R.id.actor_loading)

                    actorProgress.visibility = View.VISIBLE
                    actorLayout.visibility = View.GONE
                    Picasso.get().load(actor.photo).into(actorPhoto, object : Callback {
                        override fun onSuccess() {
                            actorProgress.visibility = View.GONE
                            actorLayout.visibility = View.VISIBLE
                            actorsLayout.addView(layout, 0)
                        }

                        override fun onError(e: Exception) {
                            e.printStackTrace()
                        }
                    })
                } else {
                    actorsLayout.addView(layout)
                }

                layout.setOnClickListener {
                    FragmentOpener.openWithData(this, fragmentListener, actor, "actor")
                }
                zoom(requireContext(), layout, actorPhoto, nameView, null, careerView)
            }
        }
    }

    override fun setDirectors(directors: ArrayList<Actor>) {
        val directorsView: TextView = currentView.findViewById(R.id.fragment_film_tv_directors)
        val spannablePersonNamesList: ArrayList<SpannableString> = ArrayList()
        for (director in directors) {
            spannablePersonNamesList.add(setClickableActorName(directorsView, director))
        }

        directorsView.movementMethod = LinkMovementMethod.getInstance()
        directorsView.text = getString(R.string.directors)
        directorsView.append(" ")
        for ((index, item) in spannablePersonNamesList.withIndex()) {
            directorsView.append(item)

            if (index != spannablePersonNamesList.size - 1) {
                directorsView.append(", ")
            }
        }
    }

    private fun setClickableActorName(textView: TextView, actor: Actor): SpannableString {
        val ss = SpannableString(actor.name)
        val fr = this
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                FragmentOpener.openWithData(fr, fragmentListener, actor, "actor")
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }
        ss.setSpan(clickableSpan, 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return ss
    }

    override fun setCountries(countries: ArrayList<String>) {
        var countriesText = ""
        for ((index, country) in countries.withIndex()) {
            countriesText += country

            if (index != countries.size - 1) {
                countriesText += ", "
            }
        }

        currentView.findViewById<TextView>(R.id.fragment_film_tv_countries).text = getString(R.string.countries, countriesText)
    }

    override fun setGenres(genres: ArrayList<String>) {
        val genresLayout: LinearLayout = currentView.findViewById(R.id.fragment_film_ll_genres)

        for ((i, genre) in genres.withIndex()) {
            val genreView = LayoutInflater.from(context).inflate(R.layout.inflate_tag, null) as TextView
            genreView.text = genre
            if (SettingsData.deviceType == DeviceType.TV && i == genres.size - 1) {
                genreView.nextFocusRightId = R.id.fragment_film_tv_directors
            }
            Highlighter.highlightText(genreView, requireContext(), true)
            genresLayout.addView(genreView)
        }
    }

    override fun setFullSizeImage(posterPath: String) {
        if (context != null) {
            val dialog = Dialog(requireActivity())
            val layout: RelativeLayout = layoutInflater.inflate(R.layout.modal_image, null) as RelativeLayout
            Picasso.get().load(posterPath).into(layout.findViewById(R.id.modal_image), object : Callback {
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
            val closeBtn = layout.findViewById<Button>(R.id.modal_bt_close)
            closeBtn.setOnClickListener {
                dialog.dismiss()
            }

            Highlighter.highlightButton(closeBtn, requireContext())
        }
    }

    private fun openFullSizeImage() {
        modalDialog?.show()
    }

    override fun setSchedule(schedule: ArrayList<Pair<String, ArrayList<Schedule>>>) {
        if (schedule.size == 0) {
            currentView.findViewById<LinearLayout>(R.id.fragment_film_ll_schedule_container).visibility = View.GONE
            return
        }

        // get mount layout
        val scheduleLayout: LinearLayout = currentView.findViewById(R.id.fragment_film_ll_schedule)
        for (sch in schedule) {
            // create season layout
            val layout: LinearLayout = layoutInflater.inflate(R.layout.inflate_schedule_layout, null) as LinearLayout
            val expandedList: ExpandableLinearLayout = layout.findViewById(R.id.inflate_layout_list)
            layout.findViewById<TextView>(R.id.inflate_layout_header).text = sch.first
            layout.findViewById<LinearLayout>(R.id.inflate_layout_button).setOnClickListener {
                expandedList.toggle()
            }

            // fill episodes layout
            for ((i, item) in sch.second.withIndex()) {
                val itemLayout: LinearLayout = layoutInflater.inflate(R.layout.inflate_schedule_item, null) as LinearLayout
                itemLayout.findViewById<TextView>(R.id.inflate_item_episode).text = item.episode
                itemLayout.findViewById<TextView>(R.id.inflate_item_name).text = item.name
                itemLayout.findViewById<TextView>(R.id.inflate_item_date).text = item.date

                val watchBtn = itemLayout.findViewById<ImageView>(R.id.inflate_item_watch)
                val nextEpisodeIn = itemLayout.findViewById<TextView>(R.id.inflate_item_next_episode)
                if (item.nextEpisodeIn == "✓" || item.nextEpisodeIn == "сегодня") {
                    watchBtn.visibility = View.VISIBLE

                    changeWatchState(item.isWatched, watchBtn)
                    watchBtn.setOnClickListener {
                        if (UserData.isLoggedIn == true) {
                            filmPresenter.updateWatch(item, watchBtn)
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.need_register), Toast.LENGTH_SHORT).show()
                        }
                    }
                    nextEpisodeIn.visibility = View.GONE
                } else {
                    watchBtn.visibility = View.GONE
                    nextEpisodeIn.visibility = View.VISIBLE
                    nextEpisodeIn.text = item.nextEpisodeIn
                }


                val color = if (i % 2 == 0) {
                    R.color.light_background
                } else {
                    R.color.dark_background
                }
                itemLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), color))

                expandedList.addView(itemLayout)
            }
            scheduleLayout.addView(layout)
        }
    }

    override fun changeWatchState(state: Boolean, btn: ImageView) {
        if (state) {
            ImageViewCompat.setImageTintList(btn, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_red)))
        } else {
            ImageViewCompat.setImageTintList(btn, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
        }
    }

    override fun setCollection(collection: ArrayList<Film>) {
        if (collection.size == 0) {
            currentView.findViewById<LinearLayout>(R.id.fragment_film_ll_collection_container).visibility = View.GONE
            return
        }

        val collectionLayout: LinearLayout = currentView.findViewById(R.id.fragment_film_tv_collection_list)
        for (i in collection.lastIndex downTo 0) {
            val film = collection.reversed()[i]
            val layout: LinearLayout = layoutInflater.inflate(R.layout.inflate_collection_item, null) as LinearLayout
            layout.findViewById<TextView>(R.id.inflate_collection_item_n).text = (i + 1).toString()
            layout.findViewById<TextView>(R.id.inflate_collection_item_name).text = film.title
            layout.findViewById<TextView>(R.id.inflate_collection_item_year).text = film.year
            layout.findViewById<TextView>(R.id.inflate_collection_item_rating).text = film.ratingKP

            if (film.filmLink?.isNotEmpty() == true) {
                val outValue = TypedValue()
                requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                layout.setBackgroundResource(outValue.resourceId)
                layout.setOnClickListener {
                    FragmentOpener.openWithData(this, fragmentListener, film, "film")
                }
            } else {
                layout.findViewById<TextView>(R.id.inflate_collection_item_name).setTextColor(requireContext().getColor(R.color.gray))
            }
            collectionLayout.addView(layout)
        }
    }

    override fun setRelated(relatedList: ArrayList<Film>) {
        if (relatedList.size == 0) {
            currentView.findViewById<LinearLayout>(R.id.fragment_film_ll_related_list_container).visibility = View.GONE
            return
        }

        val relatedLayout = currentView.findViewById<LinearLayout>(R.id.fragment_film_tv_related_list)

        for (film in relatedList) {
            val layout: LinearLayout = layoutInflater.inflate(R.layout.inflate_film, null) as LinearLayout
            val titleView: TextView = layout.findViewById(R.id.film_title)
            val infoView: TextView = layout.findViewById(R.id.film_info)
            val posterView: ImageView = layout.findViewById(R.id.film_poster)
            zoom(requireContext(), layout, posterView, titleView, null, infoView)
            layout.findViewById<TextView>(R.id.film_type).visibility = View.GONE
            titleView.text = film.title
            titleView.textSize = 12F
            infoView.text = film.relatedMisc
            infoView.textSize = 12F

            val filmPoster: ImageView = layout.findViewById(R.id.film_poster)
            Picasso.get().load(film.posterPath).into(filmPoster, object : Callback {
                override fun onSuccess() {
                    layout.findViewById<ProgressBar>(R.id.film_loading).visibility = View.GONE
                    layout.findViewById<RelativeLayout>(R.id.film_posterLayout).visibility = View.VISIBLE
                }

                override fun onError(e: Exception) {
                }
            })

            val params = LinearLayout.LayoutParams(
                UnitsConverter.getPX(requireContext(), 80),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val m = UnitsConverter.getPX(requireContext(), 5)
            params.setMargins(m, m, m, m)
            layout.layoutParams = params
            layout.setOnClickListener {
                FragmentOpener.openWithData(this, fragmentListener, film, "film")
            }

            relatedLayout.addView(layout)
        }
    }

    override fun updateBookmarksPager() {
        requireActivity().let {
            (it as MainActivity).redrawPage(UpdateItem.BOOKMARKS_CHANGED)
        }
    }

    override fun updateBookmarksFilmsPager() {
        requireActivity().let {
            (it as MainActivity).redrawPage(UpdateItem.BOOKMARKS_FILMS_CHANGED)
        }
    }

    override fun updateWatchPager() {
        requireActivity().let {
            (it as MainActivity).redrawPage(UpdateItem.WATCH_LATER_CHANGED)
        }
    }

    override fun setBookmarksList(bookmarks: ArrayList<Bookmark>) {
        val bookmarksBtn: View = currentView.findViewById(R.id.fragment_film_btn_bookmark)
        if (UserData.isLoggedIn == true) {
            val data: Array<String?> = arrayOfNulls(bookmarks.size)
            val checkedItems = BooleanArray(bookmarks.size)

            for ((index, bookmark) in bookmarks.withIndex()) {
                data[index] = bookmark.name
                checkedItems[index] = bookmark.isChecked == true
            }

            activity?.let {
                val builder = context?.let { it1 -> DialogManager.getDialog(it1, R.string.choose_bookmarks) }
                builder?.setMultiChoiceItems(data, checkedItems) { dialog, which, isChecked ->
                    filmPresenter.setBookmark(bookmarks[which].catId)
                    updateBookmarksFilmsPager()
                    checkedItems[which] = isChecked
                }
                builder?.setPositiveButton(getString(R.string.ok)) { dialog, id ->
                    dialog.dismiss()
                }

                // new catalog btn
                val catalogDialogBuilder = context?.let { it1 -> DialogManager.getDialog(it1, R.string.new_cat) }

                val dialogCatLayout: LinearLayout = layoutInflater.inflate(R.layout.dialog_new_cat, null) as LinearLayout
                val input: EditText = dialogCatLayout.findViewById(R.id.dialog_cat_input)

                catalogDialogBuilder?.setView(dialogCatLayout)
                catalogDialogBuilder?.setPositiveButton(getString(R.string.ok)) { dialog, id ->
                    filmPresenter.createNewCatalogue(input.text.toString())
                    Toast.makeText(requireContext(), getString(R.string.created_cat), Toast.LENGTH_SHORT).show()
                }
                catalogDialogBuilder?.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                    dialog.cancel()
                }

                val n = catalogDialogBuilder?.create()

                builder?.setNeutralButton(getString(R.string.new_cat)) { dialog, id ->
                    n?.show()
                }

                if (bookmarksDialog != null) {
                    bookmarksDialog?.dismiss()
                }
                bookmarksDialog = builder?.create()
                bookmarksBtn.setOnClickListener {
                    bookmarksDialog?.show()
                }
            }

            Highlighter.highlightText(bookmarksBtn, requireContext())
        } else {
            /*       if (SettingsData.deviceType != DeviceType.TV) {
                       currentView.findViewById<LinearLayout>(R.id.fragment_film_ll_title_layout).layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.85f)
                   }*/
            bookmarksBtn.visibility = View.GONE
        }
    }

    override fun setShareBtn(title: String, link: String) {
        val shareBtn: View = currentView.findViewById(R.id.fragment_film_btn_share)
        shareBtn.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val body: String = getString(R.string.share_body, title, link)
            sharingIntent.putExtra(Intent.EXTRA_TEXT, body)
            startActivity(sharingIntent)
        }

        Highlighter.highlightText(shareBtn, requireContext())
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

    override fun setCommentsList(list: ArrayList<Comment>, filmId: String) {
        commentsList.adapter = CommentsRecyclerViewAdapter(requireContext(), list, commentEditor, this, this)
    }

    override fun redrawComments() {
        commentsList.adapter?.notifyDataSetChanged()
    }

    override fun setCommentsProgressState(state: Boolean) {
        currentView.findViewById<ProgressBar>(R.id.fragment_film_pb_comments_loading).visibility = if (state) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun setCommentEditor(filmId: String) {
        val commentEditorCont: LinearLayout = currentView.findViewById(R.id.fragment_film_ll_comment_editor_container) as LinearLayout
        val commentEditorOpener: TextView = currentView.findViewById(R.id.fragment_film_view_comment_editor_opener)

        if (UserData.isLoggedIn == true) {
            commentEditor = CommentEditor(commentEditorCont, requireContext(), filmId, this, this)
            imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            commentEditorOpener.setOnClickListener {
                commentEditor?.setCommentSource(0, 0, 0, "")
                changeCommentEditorState(true)
            }
        } else {
            commentEditorCont.visibility = View.GONE
            commentEditorOpener.visibility = View.GONE
        }
    }

    override fun changeCommentEditorState(isKeyboard: Boolean) {
        if (commentEditor != null) {
            if (commentEditor?.editorContainer?.visibility == View.VISIBLE) {
                if (commentsAdded) {
                    if (isKeyboard) imm.hideSoftInputFromWindow(commentEditor?.textArea?.windowToken, 0)
                    commentEditor?.editorContainer?.animate()?.translationY(commentEditor?.editorContainer?.height!!.toFloat())?.setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            commentEditor?.editorContainer?.visibility = View.GONE
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationRepeat(animation: Animator?) {
                        }
                    })
                }
            } else {
                commentEditor?.editorContainer?.visibility = View.VISIBLE
                commentEditor?.textArea?.requestFocus()
                if (isKeyboard) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                commentEditor?.editorContainer?.animate()?.translationY(0F)?.setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationRepeat(animation: Animator?) {
                    }
                })
            }
        }
    }

    override fun onCommentPost(comment: Comment, position: Int) {
        filmPresenter.addComment(comment, position)
        commentEditor?.editorContainer?.visibility = View.GONE
    }

    override fun onDialogVisible() {
        commentEditor?.editorContainer?.visibility = View.GONE
        imm.hideSoftInputFromWindow(commentEditor?.textArea?.windowToken, 0)
    }

    override fun onNothingEntered() {
        Toast.makeText(requireContext(), getString(R.string.enter_comment_text), Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setHRrating(rating: Float, isActive: Boolean) {
        if (SettingsData.deviceType == DeviceType.TV) {
            val tvRatingBar: ScaleRatingBar = currentView.findViewById(R.id.fragment_film_srb_rating_hdrezka_tv)

            if (rating == -1f) {
                currentView.findViewById<RelativeLayout>(R.id.fragment_film_rating_layout).visibility = View.GONE
                tvRatingBar.visibility = View.GONE
                return
            }
            currentView.findViewById<RelativeLayout>(R.id.fragment_film_rating_layout).visibility = View.GONE
            tvRatingBar.rating = rating
        } else {
            val selectableRatingBar: ScaleRatingBar = currentView.findViewById(R.id.fragment_film_srb_rating_hdrezka_select)
            val ratingBar: ScaleRatingBar = currentView.findViewById(R.id.fragment_film_srb_rating_hdrezka)

            if (rating == -1f) {
                currentView.findViewById<RelativeLayout>(R.id.fragment_film_rating_layout).visibility = View.GONE
                return
            }
            selectableRatingBar.setIsIndicator(isActive)
            ratingBar.rating = rating
            selectableRatingBar.setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    selectableRatingBar.visibility = View.GONE

                    if (UserData.isLoggedIn == true) {
                        filmPresenter.updateRating(selectableRatingBar.rating)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.need_register), Toast.LENGTH_SHORT).show()
                    }
                }
                false
            }
        }
    }

    override fun showTranslations(translations: ArrayList<Voice>, isDownload: Boolean, isMovie: Boolean) {
        if (isMovie) {
            if (translations.size > 1) {
                val builder = DialogManager.getDialog(requireContext(), R.string.translation)
                val sv: ScrollView = layoutInflater.inflate(R.layout.inflate_translations, null) as ScrollView
                val layout: LinearLayout = sv.getChildAt(0) as LinearLayout
                var activeNameTextView: TextView? = null

                for (translation in translations) {
                    val textView: TextView = layoutInflater.inflate(R.layout.inflate_translation_item, null) as TextView
                    translation.name?.let { textView.text = it }
                    textView.setOnClickListener {
                        filmPresenter.getAndOpenFilmStream(translation, isDownload)

                        if (activeNameTextView != null && !isDownload && UserData.isLoggedIn == true) {
                            activeNameTextView?.setTextColor(requireContext().getColor(R.color.text_color))
                            activeNameTextView = textView
                            activeNameTextView?.setTextColor(requireContext().getColor(R.color.primary_red))

                            filmPresenter.film.lastVoiceId = translation.id
                        }
                    }

                    if (filmPresenter.film.lastVoiceId == translation.id && UserData.isLoggedIn == true) {
                        activeNameTextView = textView
                    }

                    layout.addView(textView)
                }

                if (activeNameTextView != null && !isDownload) {
                    activeNameTextView?.setTextColor(requireContext().getColor(R.color.primary_red))
                    activeNameTextView?.requestFocus()
                }

                builder.setView(sv)
                builder.create().show()
            } else if (translations.size == 1) {
                filmPresenter.getAndOpenFilmStream(translations[0], isDownload)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_empty), Toast.LENGTH_SHORT).show()
            }
        } else {
            val transView: LinearLayout = layoutInflater.inflate(R.layout.dialog_translation_series, null) as LinearLayout
            val translationsSpinner: SmartMaterialSpinner<String> = transView.findViewById(R.id.translations_spinner)
            var selectedTranslation = 0
            var d: AlertDialog? = null
            val translationProgressBar = transView.findViewById<ProgressBar>(R.id.translations_progress)
            val container = transView.findViewById<LinearLayout>(R.id.translations_container)

            fun showTranslationsSeries(seasons: LinkedHashMap<String, ArrayList<String>>) {
                var activeNameTextView: TextView? = null

                for ((season, episodes) in seasons) {
                    // костыль т.к .collapsed() не срабатывает
                    val layout: LinearLayout = if (filmPresenter.film.lastSeason == season && !isDownload) {
                        layoutInflater.inflate(R.layout.inflate_season_layout_expanded, null) as LinearLayout
                    } else {
                        layoutInflater.inflate(R.layout.inflate_season_layout, null) as LinearLayout
                    }

                    val expandedList: ExpandableLinearLayout = layout.findViewById(R.id.inflate_season_layout_list)

                    layout.findViewById<TextView>(R.id.inflate_season_layout_header).text = "Сезон ${season}"
                    layout.findViewById<LinearLayout>(R.id.inflate_season_layout_button).setOnClickListener {
                        expandedList.toggle()
                    }

                    for (episode in episodes) {
                        val nameTextView = layoutInflater.inflate(R.layout.inflate_season_item, null) as TextView
                        nameTextView.text = "Эпизод ${episode}"
                        nameTextView.setOnClickListener {
                            filmPresenter.getAndOpenEpisodeStream(translations[selectedTranslation], season, episode, isDownload)

                            if (activeNameTextView != null && !isDownload && UserData.isLoggedIn == true) {
                                activeNameTextView?.setTextColor(requireContext().getColor(R.color.text_color))
                                activeNameTextView = nameTextView
                                activeNameTextView?.setTextColor(requireContext().getColor(R.color.primary_red))

                                filmPresenter.film.lastVoiceId = translations[selectedTranslation].id
                                filmPresenter.film.lastSeason = season
                                filmPresenter.film.lastEpisode = episode
                            }
                        }

                        if (filmPresenter.film.lastSeason == season && filmPresenter.film.lastEpisode == episode && filmPresenter.film.lastVoiceId == translations[selectedTranslation].id && UserData.isLoggedIn == true) {
                            activeNameTextView = nameTextView
                        }

                        expandedList.addView(nameTextView)
                    }
                    container.addView(layout)
                }

                translationProgressBar.visibility = View.GONE

                val displayMetrics = DisplayMetrics()
                if (Build.VERSION.SDK_INT >= 30) {
                    requireActivity().display?.apply {
                        getRealMetrics(displayMetrics)
                    }
                } else {
                    // getMetrics() method was deprecated in api level 30
                    requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                }

                val dialogSize: Float =
                    if (seasons.size >= 10) 1f
                    else if (seasons.size >= 5) 0.8f
                    else if (seasons.size >= 1) 0.7f
                    else 1f

                val displayHeight = displayMetrics.heightPixels
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(d?.window?.attributes)
                val dialogWindowHeight = (displayHeight * dialogSize).toInt()
                layoutParams.height = dialogWindowHeight
                d?.window?.attributes = layoutParams

                if (activeNameTextView != null && !isDownload) {
                    activeNameTextView?.setTextColor(requireContext().getColor(R.color.primary_red))
                    activeNameTextView?.isFocusableInTouchMode = true
                    activeNameTextView?.requestFocus()
                }

                // end func
            }

            if (translations.size > 1) {
                val voicesNames: ArrayList<String> = ArrayList()
                for ((i, translation) in translations.withIndex()) {
                    translation.name?.let {
                        voicesNames.add(it)

                        if (translation.id == filmPresenter.film.lastVoiceId) {
                            selectedTranslation = i
                        }
                    }
                }
                translationsSpinner.item = voicesNames
                translationsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedTranslation = position
                        container.removeAllViews()
                        translationProgressBar.visibility = View.VISIBLE
                        filmPresenter.initTranslationsSeries(translations[position], ::showTranslationsSeries)
                    }
                }
                translationsSpinner.setSelection(selectedTranslation)
            } else if (translations.size == 1) {
                translationsSpinner.visibility = View.GONE
                translationProgressBar.visibility = View.GONE
                filmPresenter.initTranslationsSeries(translations[0], ::showTranslationsSeries)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_empty), Toast.LENGTH_SHORT).show()
            }

            val builder = DialogManager.getDialog(requireContext(), null)
            builder.setView(transView)
            d = builder.create()
            d.show()
        }
    }

    override fun showStreams(streams: ArrayList<Stream>, filmTitle: String, title: String, isDownload: Boolean, translation: Voice) {
        val builder = DialogManager.getDialog(requireContext(), R.string.choose_quality)
        val qualities: ArrayList<String> = ArrayList()
        if (streams.size > 0) {
            for (stream in streams) {
                qualities.add(stream.quality)
            }

            builder.setAdapter(ArrayAdapter(requireContext(), R.layout.simple_list_item, qualities)) { dialog, which ->
                openStream(streams[which], filmTitle, title, isDownload, translation)
            }
            builder.show()
        } else {
            Toast.makeText(requireContext(), R.string.blocked_in_region, Toast.LENGTH_SHORT).show()
        }
    }

    override fun openStream(stream: Stream, filmTitle: String, title: String, isDownload: Boolean, translation: Voice) {
        val url = stream.url.replace("#EXT-X-STREAM-INF:", "")

        fun initStream(subtitle: Subtitle?) {
            if (isDownload) {
                val manager: DownloadManager? = requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
                if (manager != null) {
                    val parseUri = Uri.parse(url)
                    val fileName = StringBuilder()
                    fileName.append(filmTitle)
                    if (title.isNotEmpty()) {
                        fileName.append(" $title")
                    }

                    if (stream.quality.isNotEmpty()) {
                        fileName.append(" ${stream.quality}")
                    }

                    fileName.append(" ${parseUri.lastPathSegment}")

                    if (SettingsData.isExternalDownload == true) {
                        val sharingIntent = Intent(Intent.ACTION_SEND)
                        sharingIntent.type = "text/plain"
                        val body: String = getString(R.string.share_body, fileName.toString(), parseUri)
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, body)
                        startActivity(sharingIntent)
                    } else {
                        val request = DownloadManager.Request(parseUri)
                        request.setTitle(fileName)
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        request.allowScanningByMediaScanner()
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName.toString())
                        manager.enqueue(request)
                        Toast.makeText(requireContext(), getString(R.string.download_started), Toast.LENGTH_SHORT).show()
                    }

                    if (subtitle != null) {
                        downloadSubtitle(subtitle.url, "$fileName.vtt")
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.no_manager), Toast.LENGTH_LONG).show()
                }
            } else {
                if (UserData.isLoggedIn == true) {
                    filmPresenter.updateWatchLater(translation)
                }

                if (SettingsData.isPlayer == false && SettingsData.deviceType == DeviceType.TV) {
                    val intent = Intent(requireContext(), PlayerActivity::class.java)
                    intent.putExtra(PlayerActivity.FILM, filmPresenter.film)
                    intent.putExtra(PlayerActivity.STREAM, stream)
                    intent.putExtra(PlayerActivity.TRANSLATION, translation)
                    if (subtitle != null) {
                        intent.putExtra(PlayerActivity.SUBTITLE, subtitle)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    val newFilmTitle: String

                    if (translation.seasons != null && translation.seasons!!.size > 0) {
                        newFilmTitle = "Сезон ${translation.selectedEpisode?.first} - Эпизод ${translation.selectedEpisode?.second} $filmTitle"
                    } else {
                        newFilmTitle = filmTitle
                    }

                    intent.setDataAndType(Uri.parse(url), "video/*")
                    intent.putExtra("title", newFilmTitle)

                    if (subtitle != null && SettingsData.isSubtitlesDownload == true) {
                        val filename = newFilmTitle.replace(" ", "").replace("/", "") + ".vtt"
                        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/$filename"

                        downloadSubtitle(subtitle.url, filename)

                        intent.putExtra("subtitles_location", path)
                        val subs: Array<Uri> = arrayOf(Uri.parse(subtitle.url))
                        intent.putExtra("subs", subs)
                        val subsnames: Array<String> = arrayOf(subtitle.lang)
                        intent.putExtra("subs.name", subsnames)
                        intent.putExtra("subs.filename", subsnames)
                    }

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    /*    if (SettingsData.selectedPlayerPackage != null) {
                            intent.setPackage(SettingsData.selectedPlayerPackage)
                        }*/

                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), getString(R.string.no_player), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val builder = DialogManager.getDialog(requireContext(), R.string.title_select_caption)
        val subtitlesNames: ArrayList<String> = ArrayList()

        if (translation.subtitles != null && translation.subtitles!!.size > 0 && SettingsData.isSelectSubtitle == true) {
            for (subtitle in translation.subtitles!!) {
                subtitlesNames.add(subtitle.lang)
            }

            builder.setAdapter(ArrayAdapter(requireContext(), R.layout.simple_list_item, subtitlesNames)) { dialog, which ->
                initStream(translation.subtitles!![which])
            }
            builder.create().show()
        } else {
            initStream(null)
        }
    }

    override fun hideActors() {
        currentView.findViewById<LinearLayout>(R.id.fragment_film_ll_actorsContainer).visibility = View.GONE
    }

    private fun downloadSubtitle(url: String, filename: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/$filename")
        if (file.exists()) {
            return
        }

        val manager: DownloadManager? = requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
        if (manager != null) {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle(filename)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            request.allowScanningByMediaScanner()
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            manager.enqueue(request)
            // Toast.makeText(requireContext(), getString(R.string.download_started), Toast.LENGTH_SHORT).show()
        }
    }
}