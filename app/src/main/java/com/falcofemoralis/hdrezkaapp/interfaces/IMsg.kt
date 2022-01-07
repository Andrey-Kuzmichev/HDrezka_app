package com.falcofemoralis.hdrezkaapp.interfaces

interface IMsg {
    enum class MsgType {
        NOT_AUTHORIZED,
        NOTHING_FOUND,
        NOTHING_ADDED
    }
    fun showMsg(type: MsgType)

    fun hideMsg()
}