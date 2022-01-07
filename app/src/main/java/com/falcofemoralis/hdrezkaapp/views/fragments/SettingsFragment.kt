package com.falcofemoralis.hdrezkaapp.views.fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.falcofemoralis.hdrezkaapp.BuildConfig
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.AuthType
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.constants.GridLayoutSizes
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.objects.UserData
import com.falcofemoralis.hdrezkaapp.presenters.UserPresenter
import com.falcofemoralis.hdrezkaapp.utils.DialogManager
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.views.MainActivity
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.UserView
import android.graphics.drawable.Drawable




class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener, UserView {
    private lateinit var preferences: SharedPreferences
    private var mActivity: FragmentActivity? = null
    private var mContext: Context? = null
    private var userPresenter: UserPresenter? = null
    private var popupWindowView: LinearLayout? = null
    private var popupWindow: AlertDialog? = null
    private var imm: InputMethodManager? = null
    private var registerBtn: Preference? = null
    private var loginBtn: Preference? = null
    private var exitBtn: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (SettingsData.deviceType == DeviceType.MOBILE) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        } else {
            setPreferencesFromResource(R.xml.root_preferences_tv, rootKey)
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)
        mActivity = requireActivity()
        mContext = requireContext()

        // Start of userFragment implementation
        // currentView = /
        userPresenter = UserPresenter(this, requireContext())
        imm = mActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        registerBtn = findPreference(getString(R.string.register_pref))
        loginBtn = findPreference(getString(R.string.login_pref))
        exitBtn = findPreference(getString(R.string.exit_pref))

        initExitButton()

        UserData.isLoggedIn?.let {
            initAuthPanel(it)
        }

        SettingsData.isPlayer?.let {
            setStateExternalPlayerPrefs(it)
        }

        SettingsData.isMaxQuality?.let {
            if (SettingsData.isPlayer != null && SettingsData.isPlayer == true) {
                setStateDefaultQuality(!it)
            }
        }

        findPreference<Preference?>("changeToTv")?.setOnPreferenceClickListener {
            showUiModeChangeDialog(DeviceType.TV)
            true
        }

        findPreference<Preference?>("changeToMobile")?.setOnPreferenceClickListener {
            showUiModeChangeDialog(DeviceType.MOBILE)
            true
        }

     /*   findPreference<Preference?>("selectedPlayerPackage")?.setOnPreferenceClickListener {
            if (mContext != null) {
                val packages =  mContext!!.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

                for (packageInfo in packages) {
                    Log.d("TEST", "Package name:" + packageInfo.packageName)
                    val icon = mContext!!.packageManager.getApplicationIcon(packageInfo.packageName)
                }
            }
            true
        }*/

        val versionType = SettingsData.deviceType?.name

        findPreference<Preference?>("app_version")?.summary = "${BuildConfig.VERSION_NAME} $versionType"
    }

    private fun showUiModeChangeDialog(deviceType: DeviceType) {
        if (mContext != null) {
            val builder = DialogManager.getDialog(mContext!!, R.string.are_you_sure)
            builder.setNegativeButton(R.string.cancel) { dialog, id ->
                dialog.dismiss()
            }

            builder.setPositiveButton(R.string.ok) { dialog, id ->
                dialog.dismiss()
                changeUiMode(deviceType)
            }

            if (deviceType == DeviceType.MOBILE) {
                builder.setMessage(R.string.change_to_mobile)
            } else {
                builder.setMessage(R.string.change_to_tv)
            }

            val d = builder.create()
            d.setOnShowListener {
                val btn: Button = d.getButton(AlertDialog.BUTTON_POSITIVE)
                btn.isFocusable = true
                btn.requestFocus()
            }
            d.show()
        }
    }

    private fun initExitButton() {
        exitBtn?.setOnPreferenceClickListener { //code for what you want it to do
            val builder = DialogManager.getDialog(requireContext(), R.string.confirm_exit, false)
            builder.setPositiveButton(getString(R.string.confirm)) { dialog, id ->
                initAuthPanel(false)
                userPresenter?.exit()

                mActivity?.let { it1 ->
                    (it1 as MainActivity).updatePager()
                }
            }
            builder.setNegativeButton(getString(R.string.cancel)) { dialog, id ->
                dialog.dismiss()
            }

            val d = builder.create()
            d.show()
            true
        }
    }

    private fun initAuthPanel(isLogged: Boolean) {
        if (isLogged) {
            //  popupWindowCloseBtn?.visibility = View.GONE
            registerBtn?.isVisible = false
            loginBtn?.isVisible = false
            exitBtn?.isVisible = true
        } else {
            registerBtn?.isVisible = true
            loginBtn?.isVisible = true
            exitBtn?.isVisible = false

            loginBtn?.setOnPreferenceClickListener {
                showAuthDialog(AuthType.LOGIN)
                true
            }

            registerBtn?.setOnPreferenceClickListener {
                showAuthDialog(AuthType.REGISTER)
                true
            }
        }
    }

    private fun showAuthDialog(type: AuthType) {
        mActivity?.let {
            val builder = context?.let { it1 -> DialogManager.getDialog(it1, null) }
            popupWindowView = requireActivity().layoutInflater.inflate(R.layout.dialog_auth, null) as LinearLayout

            val emailView = popupWindowView?.findViewById<EditText>(R.id.dialog_auth_email)
            val usernameView = popupWindowView?.findViewById<EditText>(R.id.dialog_auth_username)
            val nameView = popupWindowView?.findViewById<EditText>(R.id.dialog_auth_name)
            val passwordView = popupWindowView?.findViewById<EditText>(R.id.dialog_auth_password)

            if (type == AuthType.LOGIN) {
                builder?.setTitle(getString(R.string.login))
                emailView?.visibility = View.GONE
                usernameView?.visibility = View.GONE
                val submitBtn = popupWindowView?.findViewById<Button>(R.id.dialog_auth_submit)
                submitBtn?.text = getString(R.string.submit_login)
                submitBtn?.setOnClickListener {
                    val name: String = nameView?.text.toString()
                    val password: String = passwordView?.text.toString()

                    changeAuthLoadingState(true)
                    userPresenter?.login(name, password)
                    imm?.hideSoftInputFromWindow(popupWindowView?.windowToken, 0)
                }
            } else {
                builder?.setTitle(getString(R.string.register))
                nameView?.visibility = View.GONE
                val submitBtn = popupWindowView?.findViewById<Button>(R.id.dialog_auth_submit)
                submitBtn?.text = getString(R.string.submit_register)
                submitBtn?.setOnClickListener {
                    val email: String = emailView?.text.toString()
                    val username: String = usernameView?.text.toString()
                    val password: String = passwordView?.text.toString()

                    changeAuthLoadingState(true)
                    userPresenter?.register(email, username, password)
                    imm?.hideSoftInputFromWindow(popupWindowView?.windowToken, 0)
                }
            }

            builder?.setView(popupWindowView)
            popupWindow = builder?.create()
            popupWindow?.show()
        }
    }

    private fun changeAuthLoadingState(isActive: Boolean) {
        val loadingBar = popupWindowView?.findViewById<ProgressBar>(R.id.dialog_auth_loading)
        val submitBtn = popupWindowView?.findViewById<Button>(R.id.dialog_auth_submit)

        if (isActive) {
            loadingBar?.visibility = View.VISIBLE
            submitBtn?.visibility = View.GONE
        } else {
            loadingBar?.visibility = View.GONE
            submitBtn?.visibility = View.VISIBLE
        }
    }

    override fun showError(text: String) {
        changeAuthLoadingState(false)

        val error = text.replace("</u>", "").replace("</b>", "")

        val errorView = popupWindowView?.findViewById<TextView>(R.id.dialog_auth_error)
        errorView?.visibility = View.VISIBLE
        errorView?.text = error
    }

    override fun setUserAvatar() {
        (mActivity as MainActivity).setUserAvatar()
    }

    override fun completeAuth() {
        popupWindow?.dismiss()

        initAuthPanel(true)

        mActivity?.let {
            (it as MainActivity).updatePager()
        }
    }

    override fun updateNotifyBtn() {
        activity?.let {
            (it as MainActivity).initSeriesUpdates()
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View? = super.onCreateView(inflater, container, savedInstanceState)
        view?.findViewById<View>(android.R.id.list)?.setBackgroundColor(Color.MAGENTA);
        view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))
        return view
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "ownProvider" -> {
                val newProvider: String? = preferences.getString("ownProvider", "")
                mContext?.let {
                    if (newProvider != null) {
                        SettingsData.setProvider(newProvider, it, false)
                    }
                }
                applyProvider()
            }
            "isPlayer" -> {
                SettingsData.isPlayer = preferences.getBoolean("isPlayer", false)

                SettingsData.isPlayer?.let {
                    setStateExternalPlayerPrefs(it)
                }
            }
            "isMaxQuality" -> {
                SettingsData.isMaxQuality = preferences.getBoolean("isMaxQuality", false)
                SettingsData.isMaxQuality?.let { setStateDefaultQuality(!it) }
            }
            "isPlayerChooser" -> {
                SettingsData.isPlayerChooser = preferences.getBoolean("isPlayerChooser", false)
            }
            "isExternalDownload" -> {
                SettingsData.isExternalDownload = preferences.getBoolean("isExternalDownload", false)
            }
            "filmsInRow" -> {
                (preferences.getString("filmsInRow", (if (SettingsData.deviceType == DeviceType.TV) GridLayoutSizes.TV else GridLayoutSizes.MOBILE).toString())).let {
                    if (it != null) {
                        SettingsData.filmsInRow = it.toInt()
                        applyInterfaceChange()
                    }
                }
            }
            "isAutorotate" -> {
                SettingsData.isAutorotate = preferences.getBoolean("isAutorotate", true)
                mActivity?.requestedOrientation = if (SettingsData.isAutorotate == true) {
                    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            "rowMultiplier" -> {
                // SettingsData.rowMultiplier = preferences.getString("rowMultiplier", "3")?.toInt()
                //  applyInterfaceChange()
            }
            "autoPlayNextEpisode" -> {
                SettingsData.autoPlayNextEpisode = preferences.getBoolean("autoPlayNextEpisode", true)
            }
            "defaultQuality" -> {
                var defaultQuality: String? = preferences.getString("defaultQuality", null)
                if (defaultQuality == "Авто") {
                    defaultQuality = null
                }
                SettingsData.defaultQuality = defaultQuality
            }
            "isSubtitlesDownload" -> {
                SettingsData.isSubtitlesDownload = preferences.getBoolean("isSubtitlesDownload", true)
            }
            "isCheckNewVersion" -> {
                SettingsData.isCheckNewVersion = preferences.getBoolean("isCheckNewVersion", true)
            }
            "isAltLoading" -> {
                SettingsData.isAltLoading = preferences.getBoolean("isAltLoading", false)
            }
            "isSelectSubtitles" -> {
                SettingsData.isSelectSubtitle = preferences.getBoolean("isSelectSubtitles", true)
            }
        }
    }

    private fun setStateExternalPlayerPrefs(state: Boolean) {
        val prefsKey = arrayOf("isPlayerChooser", "isMaxQuality", "defaultQuality", "isSubtitlesDownload", "isSelectSubtitles")
        for (prefKey in prefsKey) {
            if (SettingsData.deviceType == DeviceType.TV && (prefKey == "isMaxQuality" || prefKey == "defaultQuality")) {
                continue
            }

            val pref: Preference? = findPreference(prefKey)
            if (pref != null) {
                pref.isEnabled = state
            }
        }
    }

    private fun setStateDefaultQuality(state: Boolean) {
        val pref: Preference? = findPreference("defaultQuality")
        if (pref != null) {
            pref.isEnabled = state
        }
    }

    private fun applyProvider() {
        mContext?.let { UserData.reset(it) }
        mActivity?.let { (it as MainActivity).updatePager() }
        mActivity?.let { (it as MainActivity).setUserAvatar() }
    }

    private fun applyInterfaceChange() {
        mActivity?.let { (it as MainActivity).updatePager() }
    }

    private fun changeUiMode(deviceType: DeviceType) {
        mContext?.let {
            SettingsData.setUIMode(deviceType, it)
        }
        mActivity?.let {
            (it as MainActivity).refreshActivity()
        }
    }
}