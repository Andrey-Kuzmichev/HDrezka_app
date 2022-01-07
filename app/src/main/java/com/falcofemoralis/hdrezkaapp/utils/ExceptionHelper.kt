package com.falcofemoralis.hdrezkaapp.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection.ErrorType
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.views.MainActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.parser.ParseError
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

object ExceptionHelper {
    var activeDialog: AlertDialog? = null

    fun showToastError(context: Context, type: ErrorType, error: String) {
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                val textId: Int = when (type) {
                    ErrorType.EMPTY -> R.string.error_empty
                    ErrorType.TIMEOUT -> R.string.error_timeout
                    ErrorType.PARSING_ERROR -> R.string.error_parsing
                    ErrorType.NO_INTERNET -> R.string.no_connection
                    ErrorType.BLOCKED_SITE -> R.string.no_access
                    ErrorType.PROVIDER_TIMEOUT -> R.string.provider_timeout
                    ErrorType.MALFORMED_URL -> R.string.malformed_url
                    ErrorType.MODERATE_BY_ADMIN -> R.string.comment_need_apply
                    ErrorType.ERROR -> R.string.error_occured
                    ErrorType.EMPTY_SEARCH -> R.string.search_empty
                    ErrorType.FILM_BLOCKED -> R.string.blocked_in_region
                }

                if (type == ErrorType.BLOCKED_SITE) {
                    createDialog(textId, context)
                } else if (type == ErrorType.MALFORMED_URL) {
                    val urlErrorString = if (SettingsData.provider == null) {
                        context.getString(R.string.url_error_empty)
                    } else if (!SettingsData.provider!!.contains("http://") && !SettingsData.provider!!.contains("https://")) {
                        context.getString(R.string.url_error_no_protocol)
                    } else {
                        context.getString(R.string.url_error_malformed)

                    }
                    Toast.makeText(context, "${context.getString(textId)}: $urlErrorString ${context.getString(R.string.your_url)} ${SettingsData.provider}", Toast.LENGTH_LONG).show()
                } else if (type != ErrorType.PROVIDER_TIMEOUT) {
                    Toast.makeText(context, context.getString(textId) + ": " + error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createDialog(textId: Int, context: Context) {
        try {
            if (!(context as Activity).isFinishing) {
                val builder = DialogManager.getDialog(context, textId)
                builder.setPositiveButton(context.getString(R.string.provider_change)) { dialog, id ->
                    dialog.cancel()
                }
                builder.setNegativeButton(context.getString(R.string.cancel)) { dialog, id ->
                    dialog.dismiss()
                }
                builder.setOnCancelListener {
                    activeDialog = null
                    (context as MainActivity).showProviderEnter()
                }
                builder.setCancelable(false)

                if (activeDialog == null) {
                    activeDialog = builder.create()
                }
                activeDialog!!.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun catchException(e: Exception, view: IConnection) {

        if (e !is IllegalArgumentException &&
            e !is UnknownHostException &&
            e !is SocketTimeoutException &&
            e !is ConnectException &&
            e !is HttpStatusException &&
                    e !is SocketException &&
                    e !is SSLException &&
            e !is IOException
        ) {
            Firebase.crashlytics.recordException(e)
        }

        e.printStackTrace()

        val type: ErrorType = when (e) {
            is SocketTimeoutException -> ErrorType.TIMEOUT
            is HttpStatusException -> {
                when (e.statusCode) {
                    401 -> ErrorType.EMPTY_SEARCH
                    404 -> ErrorType.EMPTY
                    405 -> ErrorType.FILM_BLOCKED
                    503 -> ErrorType.PROVIDER_TIMEOUT
                    else -> ErrorType.ERROR
                }
            }
            is ParseError -> ErrorType.PARSING_ERROR
            is IllegalArgumentException -> ErrorType.MALFORMED_URL
            is IndexOutOfBoundsException -> ErrorType.BLOCKED_SITE
            is SSLHandshakeException -> ErrorType.BLOCKED_SITE
            is UnknownHostException -> ErrorType.MALFORMED_URL
            is ConnectException -> ErrorType.MALFORMED_URL
            is IOException -> ErrorType.PROVIDER_TIMEOUT
            else -> ErrorType.ERROR
        }

        view.showConnectionError(type, e.toString())
    }
}