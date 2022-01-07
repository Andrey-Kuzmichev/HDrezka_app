package com.falcofemoralis.hdrezkaapp.objects


class Playlist {
    data class PlaylistItem(val season: String, val episode: String)

    private val playlist: MutableList<PlaylistItem>
    private var currentPosition: Int

    init {
        playlist = ArrayList()
        currentPosition = 0
    }

/*    */
    /**
     * Clears the videos from the playlist.
     */
    fun clear() {
        playlist.clear()
    }

    /**
     * Adds a video to the end of the playlist.
     *
     * @param stream to be added to the playlist.
     */
    fun add(stream: PlaylistItem) {
        playlist.add(stream)
    }

    /**
     * Sets current position in the playlist.
     *
     * @param currentPosition
     */
    fun setCurrentPosition(currentPosition: Int) {
        this.currentPosition = currentPosition
    }

    fun getCurrentItem(): PlaylistItem {
        return playlist[currentPosition]
    }

    /**
     * Returns the size of the playlist.
     *
     * @return The size of the playlist.
     */
    fun size(): Int {
        return playlist.size
    }

    /**
     * Moves to the next video in the playlist. If already at the end of the playlist, null will
     * be returned and the position will not change.
     *
     * @return The next video in the playlist.
     */
    operator fun next(): PlaylistItem? {
        if (currentPosition + 1 < size()) {
            currentPosition++
            return playlist[currentPosition]
        }
        return null
    }

    /**
     * Moves to the previous video in the playlist. If the playlist is already at the beginning,
     * null will be returned and the position will not change.
     *
     * @return The previous video in the playlist.
     */
    fun previous(): PlaylistItem? {
        if (currentPosition - 1 >= 0) {
            currentPosition--
            return playlist[currentPosition]
        }
        return null
    }
}