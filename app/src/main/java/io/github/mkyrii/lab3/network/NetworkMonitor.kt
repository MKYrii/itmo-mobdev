package io.github.mkyrii.lab3.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var wasOnline = isOnline()

    var onNetworkAvailable: (() -> Unit)? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val onlineNow = isOnline()
            if (onlineNow && !wasOnline) {
                onNetworkAvailable?.invoke()
            }
            wasOnline = onlineNow
        }

        override fun onLost(network: Network) {
            wasOnline = isOnline()
        }
    }

    fun start() {
        wasOnline = isOnline()
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    fun stop() {
        connectivityManager.unregisterNetworkCallback(callback)
    }

    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
