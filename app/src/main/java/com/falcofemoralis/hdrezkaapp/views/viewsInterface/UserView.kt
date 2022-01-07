package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.presenters.UserPresenter

interface UserView : IConnection {
    fun showError(text: String)

    fun setUserAvatar()

    fun completeAuth()

    fun updateNotifyBtn()
}