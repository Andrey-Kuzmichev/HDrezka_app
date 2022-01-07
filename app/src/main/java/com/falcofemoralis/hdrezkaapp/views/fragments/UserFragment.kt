package com.falcofemoralis.hdrezkaapp.views.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.AuthType
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.falcofemoralis.hdrezkaapp.objects.UserData
import com.falcofemoralis.hdrezkaapp.presenters.UserPresenter
import com.falcofemoralis.hdrezkaapp.utils.DialogManager
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.FragmentOpener
import com.falcofemoralis.hdrezkaapp.views.MainActivity
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.UserView

class UserFragment : Fragment(), UserView {
    private lateinit var currentView: View
    private lateinit var userPresenter: UserPresenter
    private lateinit var popupWindowView: LinearLayout
    private lateinit var popupWindow: AlertDialog
    private lateinit var imm: InputMethodManager
    private lateinit var authPanel: LinearLayout
    private lateinit var exitPanel: TextView
    private lateinit var fragmentListener: OnFragmentInteractionListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentView = inflater.inflate(R.layout.fragment_user, container, false)
        userPresenter = UserPresenter(this, requireContext())
        imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        authPanel = currentView.findViewById(R.id.fragment_user_ll_auth_panel)
        exitPanel = currentView.findViewById(R.id.fragment_user_tv_exit)

        initExitButton()

        UserData.isLoggedIn?.let {
            initAuthPanel(it)
        }

        currentView.findViewById<TextView>(R.id.fragment_user_tv_settings).setOnClickListener {
            FragmentOpener.openFragment(this, SettingsFragment(), fragmentListener)
        }

        return currentView
    }

    private fun initAuthPanel(isLogged: Boolean) {
        if (isLogged) {
            //  popupWindowCloseBtn?.visibility = View.GONE
            authPanel.visibility = View.GONE
            exitPanel.visibility = View.VISIBLE
        } else {
            authPanel.visibility = View.VISIBLE
            exitPanel.visibility = View.GONE

            currentView.findViewById<TextView>(R.id.fragment_user_tv_login).setOnClickListener {
                showAuthDialog(AuthType.LOGIN)
            }

            currentView.findViewById<TextView>(R.id.fragment_user_tv_register).setOnClickListener {
                showAuthDialog(AuthType.REGISTER)
            }
        }
    }

    private fun showAuthDialog(type: AuthType) {
        activity?.let {
            val builder = DialogManager.getDialog(requireContext(), null)
            popupWindowView = requireActivity().layoutInflater.inflate(R.layout.dialog_auth, null) as LinearLayout

            val emailView = popupWindowView.findViewById<EditText>(R.id.dialog_auth_email)
            val usernameView = popupWindowView.findViewById<EditText>(R.id.dialog_auth_username)
            val nameView = popupWindowView.findViewById<EditText>(R.id.dialog_auth_name)
            val passwordView = popupWindowView.findViewById<EditText>(R.id.dialog_auth_password)

            if (type == AuthType.LOGIN) {
                builder.setTitle(getString(R.string.login))
                emailView.visibility = View.GONE
                usernameView.visibility = View.GONE
                val submitBtn = popupWindowView.findViewById<Button>(R.id.dialog_auth_submit)
                submitBtn.text = getString(R.string.submit_login)
                submitBtn.setOnClickListener {
                    val name: String = nameView.text.toString()
                    val password: String = passwordView.text.toString()

                    changeAuthLoadingState(true)
                    userPresenter.login(name, password)
                    imm.hideSoftInputFromWindow(popupWindowView.windowToken, 0)
                }
            } else {
                builder.setTitle(getString(R.string.register))
                nameView.visibility = View.GONE
                val submitBtn = popupWindowView.findViewById<Button>(R.id.dialog_auth_submit)
                submitBtn.text = getString(R.string.submit_register)
                submitBtn.setOnClickListener {
                    val email: String = emailView.text.toString()
                    val username: String = usernameView.text.toString()
                    val password: String = passwordView.text.toString()

                    changeAuthLoadingState(true)
                    userPresenter.register(email, username, password)
                    imm.hideSoftInputFromWindow(popupWindowView.windowToken, 0)
                }
            }

            builder.setView(popupWindowView)
            popupWindow = builder.create()
            popupWindow.show()
        }
    }

    override fun showError(text: String) {
        changeAuthLoadingState(false)

        val error = text.replace("</u>", "").replace("</b>", "")

        val errorView = popupWindowView.findViewById<TextView>(R.id.dialog_auth_error)
        errorView.visibility = View.VISIBLE
        errorView.text = error
    }

    private fun changeAuthLoadingState(isActive: Boolean) {
        val loadingBar = popupWindowView.findViewById<ProgressBar>(R.id.dialog_auth_loading)
        val submitBtn = popupWindowView.findViewById<Button>(R.id.dialog_auth_submit)

        if (isActive) {
            loadingBar.visibility = View.VISIBLE
            submitBtn.visibility = View.GONE
        } else {
            loadingBar.visibility = View.GONE
            submitBtn.visibility = View.VISIBLE
        }
    }

    override fun setUserAvatar() {
        (requireActivity() as MainActivity).setUserAvatar()
    }

    override fun completeAuth() {
        popupWindow.dismiss()

        initAuthPanel(true)

        activity?.let {
            (it as MainActivity).updatePager()
        }
    }

    override fun updateNotifyBtn() {
        activity?.let {
            (it as MainActivity).initSeriesUpdates()
        }
    }

    private fun initExitButton() {
        exitPanel.setOnClickListener {
            val builder = DialogManager.getDialog(requireContext(), R.string.confirm_exit)
            builder.setPositiveButton(getString(R.string.confirm)) { dialog, id ->
                initAuthPanel(false)
                userPresenter.exit()

                activity?.let { it1 ->
                    (it1 as MainActivity).updatePager()
                }
            }
            builder.setNegativeButton(getString(R.string.cancel)) { dialog, id ->
                dialog.dismiss()
            }

            val d = builder.create()
            d.show()
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
}