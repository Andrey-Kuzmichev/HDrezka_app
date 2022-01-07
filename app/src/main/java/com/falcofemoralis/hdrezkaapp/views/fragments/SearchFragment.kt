package com.falcofemoralis.hdrezkaapp.views.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.algolia.instantsearch.voice.ui.VoicePermissionDialogFragment
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.IProgressState
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.presenters.SearchPresenter
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.FragmentOpener
import com.falcofemoralis.hdrezkaapp.utils.Highlighter
import com.falcofemoralis.hdrezkaapp.views.elements.VoiceInputDialogFragmentOverride
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmListCallView
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.SearchView
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class SearchFragment : Fragment(), SearchView, FilmListCallView {
    private lateinit var currentView: View
    private lateinit var searchPresenter: SearchPresenter
    private lateinit var filmsListFragment: FilmsListFragment
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var imm: InputMethodManager
    private lateinit var hintLayout: LinearLayout
    private lateinit var fragmentListener: OnFragmentInteractionListener
    private lateinit var clearBtn: ImageView
    private lateinit var voiceBtn: ImageView

    private val SEARCH_DELAY_MS: Int = 400
    private var searchDelayTimeMs: Int = 0
    private var isSearching: Boolean = false

    enum class Tag {
        Voice,
        Permission;

        companion object {
            fun getVoiceTag(): Tag = Voice
            fun getTag(): Tag = Permission
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    private fun onFilmClickedListener(){
        imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        currentView = inflater.inflate(R.layout.fragment_search, container, false)
        clearBtn = currentView.findViewById(R.id.fragment_search_tv_clear)
        voiceBtn = currentView.findViewById(R.id.fragment_search_voice)

        filmsListFragment = FilmsListFragment()
        filmsListFragment.setCallView(this)
        filmsListFragment.setOnFilmClickedListener(::onFilmClickedListener)

        childFragmentManager.beginTransaction().replace(R.id.fragment_search_fcv_container, filmsListFragment).commit()

        autoCompleteTextView = currentView.findViewById(R.id.fragment_search_act_suggest)
        imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        hintLayout = currentView.findViewById(R.id.fragment_search_ll_hint)

        initSearchViews()

        return currentView
    }

    override fun onFilmsListCreated() {
        searchPresenter = SearchPresenter(this, filmsListFragment, requireContext())
        searchPresenter.initFilms()
        filmsListFragment.setProgressBarState(IProgressState.StateType.LOADED)
        super.onStart()
    }

    override fun onFilmsListDataInit() {
    }

    private fun initSearchViews() {
        // pressed enter
        autoCompleteTextView.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                autoCompleteTextView.dismissDropDown()
                val text: String = autoCompleteTextView.text.toString()

                //проверяем ведденный текст
                if (text.isEmpty()) {
                    Toast.makeText(context, getString(R.string.enter_film_name), Toast.LENGTH_SHORT).show()
                } else {
                    hintLayout.visibility = View.GONE
                    imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
                   // searchPresenter.setQuery(text)
                }
            }
            false
        }

        // entered text
        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!autoCompleteTextView.isPerformingCompletion) {
                    if (s.toString().isNotEmpty()) {
                        if(!isSearching) {
                            isSearching = true
                            GlobalScope.launch {
                                while (searchDelayTimeMs < SEARCH_DELAY_MS){
                                    Thread.sleep(1)
                                    searchDelayTimeMs++
                                }

                                withContext(Dispatchers.Main){
                                    searchPresenter.setQuery(s.toString())
                                }

                                isSearching = false
                                searchDelayTimeMs = 0
                            }
                        } else{
                            searchDelayTimeMs = 0
                        }

                        clearBtn.visibility = View.VISIBLE
                        hintLayout.visibility = View.GONE
                    } else {
                        clearBtn.visibility = View.GONE
                        hintLayout.visibility = View.VISIBLE
                    }
                }
            }
        })

        // pressed on film
        autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
            autoCompleteTextView.dismissDropDown()
            FragmentOpener.openWithData(this, fragmentListener, searchPresenter.activeSearchFilms[position], "film")
        }
       // autoCompleteTextView.requestFocus()

        clearBtn.setOnClickListener {
            autoCompleteTextView.setText("")
        }

        voiceBtn.setOnClickListener {
            when (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                true -> showVoiceDialog()
                false -> VoicePermissionDialogFragment().show(parentFragmentManager, Tag.Permission.name)
            }
        }

        if (SettingsData.deviceType == DeviceType.TV) {
            autoCompleteTextView.setOnClickListener {
                imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)

                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

            //  autoCompleteTextView.requestFocus()
        }

        //Highlighter.highlightButton(autoCompleteTextView, requireContext(), true)
        Highlighter.highlightImage(voiceBtn, requireContext())
        Highlighter.highlightImage(clearBtn, requireContext())
    }

    fun showVoiceDialog() {
        getPermissionDialog()?.dismiss()
        (getVoiceDialog() ?: VoiceInputDialogFragmentOverride()).let {
            it.setSuggestions(
                "Терминатор",
                "Аватар"
            )
            it.show(parentFragmentManager, Tag.Voice.name)
        }

        /* val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
             putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
             putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
         }

         // This starts the activity and populates the intent with the speech text.
         startActivityForResult(intent, SPEECH_REQUEST_CODE)*/
    }

    fun showVoiceResult(spokenText: String?) {
        if (spokenText != null) {
            //autoCompleteTextView.dismissDropDown()
            autoCompleteTextView.setText(spokenText)

            //проверяем ведденный текст
            if (spokenText.isEmpty()) {
                Toast.makeText(context, getString(R.string.enter_film_name), Toast.LENGTH_SHORT).show()
            } else {
                hintLayout.visibility = View.GONE
                imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
                searchPresenter.setQuery(spokenText)
            }
        }
    }

    private fun getVoiceDialog() = (parentFragmentManager.findFragmentByTag(Tag.Voice.name) as? VoiceInputDialogFragmentOverride)
    private fun getPermissionDialog() = parentFragmentManager.findFragmentByTag(Tag.getTag().name) as? VoicePermissionDialogFragment

    override fun redrawSearchFilms(films: ArrayList<String>) {
        context?.let {
            autoCompleteTextView.setAdapter(ArrayAdapter(it, R.layout.search_item, R.id.text_view_list_item, films))
            autoCompleteTextView.showDropDown()
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

    override fun triggerEnd() {
        searchPresenter.getNextFilms()
    }
}   