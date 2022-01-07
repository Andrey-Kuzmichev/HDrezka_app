package com.falcofemoralis.hdrezkaapp.objects

data class Comment(
    val id: Int,
    val avatarPath: String,
    val nickname: String,
    val text: ArrayList<Pair<TextType, String>>,
    val date: String,
    var indent: Int,
    var likes: Int,
    var isDisabled: Boolean,
    var isControls: Boolean
) {
    var deleteHash: String? = null
    var isSelfDisabled: Boolean = false

    enum class TextType {
        REGULAR,
        SPOILER,
        BOLD,
        INCLINED,
        UNDERLINE,
        CROSSED,
        BREAK
    }

    enum class LikeType{
        PLUS,
        MINUS
    }
}
