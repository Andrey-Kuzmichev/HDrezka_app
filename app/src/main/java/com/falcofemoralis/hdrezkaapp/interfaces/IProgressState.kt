package com.falcofemoralis.hdrezkaapp.interfaces

interface IProgressState {
    enum class StateType {
        LOADING,
        LOADED
    }

    fun setProgressBarState(type: StateType)
}