package com.falcofemoralis.hdrezkaapp.controllers

import java.net.InetAddress
import java.net.Socket
import java.security.SecureRandom
import javax.net.ssl.*

class SocketFactory : SSLSocketFactory() {
    private val factory: SSLSocketFactory

    init {
        val instance = SSLContext.getInstance("TLS")
        instance.init(null as Array<KeyManager?>?, null as Array<TrustManager?>?, null as SecureRandom?)
        factory = instance.socketFactory
    }

    fun getSocket(socket: Socket?): Socket? {
        if (socket != null && (socket is SSLSocket)) {
            socket.enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")
        }
        return socket
    }

    override fun createSocket(): Socket {
        val createSocket: Socket = factory.createSocket()
        getSocket(createSocket)
        return createSocket
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return factory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return factory.supportedCipherSuites
    }

    override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        val createSocket: Socket = factory.createSocket(s, host, port, autoClose)
        getSocket(createSocket)
        return createSocket
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val createSocket: Socket = factory.createSocket(host, port)
        getSocket(createSocket)
        return createSocket
    }

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        val createSocket: Socket = factory.createSocket(host, port, localHost, localPort)
        getSocket(createSocket)
        return createSocket
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        val createSocket: Socket = factory.createSocket(host, port)
        getSocket(createSocket)
        return createSocket
    }

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        val createSocket: Socket = factory.createSocket(address, port, localAddress, localPort)
        getSocket(createSocket)
        return createSocket
    }
}