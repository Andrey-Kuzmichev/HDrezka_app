package com.falcofemoralis.hdrezkaapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.system.exitProcess

object ConnectionManager {
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        var connection: Boolean
        when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
        }

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        connection = wm?.isWifiEnabled ?: false
        if (connection) {
            return true
        }

        connection = isAddressAvailable()
        if (connection) {
            return true
        }

  /*      connection = actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        if (connection) {
            return true
        }*/

        return false
    }

    private fun isAddressAvailable(): Boolean {
        try {
            val command = "ping -c 1 google.com"
            return Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
        return false
    }

    fun showConnectionErrorDialog(context: Context, type: IConnection.ErrorType, retryCallback: () -> Unit) {
        if (type == IConnection.ErrorType.NO_INTERNET) {
            val builder = DialogManager.getDialog(context,  R.string.no_connection, false)
            builder.setPositiveButton(context.getString(R.string.exit)) { dialog, id ->
                exitProcess(0)
            }
            builder.setNegativeButton(context.getString(R.string.retry)) { dialog, id ->
                retryCallback()
            }
            val d = builder.create()
            d.show()
        }
    }
}