package com.falcofemoralis.hdrezkaapp.models

import android.util.ArrayMap
import android.webkit.CookieManager
import com.falcofemoralis.hdrezkaapp.objects.Comment
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

object CommentsModel {
    private const val COMMENT_LINK = "/ajax/get_comments/"
    private const val COMMENT_ADD = "/ajax/add_comment/"
    private const val COMMENT_LIKE = "/engine/ajax/comments_like.php"
    private const val DELETE_COMMENT = "/engine/ajax/deletecomments.php"

    // filmId = news_id
    // t = unix time
    // cstart = page
    fun getCommentsFromPage(page: Int, filmId: String): ArrayList<Comment> {
        val unixTime = System.currentTimeMillis()
        val result: String = BaseModel.getJsoup(SettingsData.provider + COMMENT_LINK + "?t=$unixTime&news_id=$filmId&cstart=$page&type=0&comment_id=0&skin=hdrezka")
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .execute()
            .body()

        val jsonObject = JSONObject(result)
        val doc = Jsoup.parse(jsonObject.getString("comments"))

        val comments: ArrayList<Comment> = ArrayList()
        val list: Elements = doc.select("li.comments-tree-item")
        for (el in list) {
            comments.add(getCommentData(el))
        }

        if (comments.size == 0) {
            throw HttpStatusException("Empty list", 404, SettingsData.provider)
        }

        return comments
    }

    private fun getCommentData(el: Element): Comment {
        val id: Int = el.select("div")[0].attr("id").toString().replace("comment-id-", "").toInt()
        val avatarPath: String = el.select("div.ava img")[0].attr("src")
        val nickname: String = el.select("span.name")[0].text()
        val date: String = el.select("span.date")[0].text()
        val text: ArrayList<Pair<Comment.TextType, String>> = ArrayList()
        val textElements: Element = el.select("div.text div")[0]
        for (textItem in textElements.childNodes()) {
            when (textItem) {
                is TextNode -> {
                    text.add(Pair(Comment.TextType.REGULAR, textItem.text()))
                }
                is Element -> {
                    val type: Comment.TextType = when (textItem.tagName()) {
                        "b" -> Comment.TextType.BOLD
                        "i" -> Comment.TextType.INCLINED
                        "u" -> Comment.TextType.UNDERLINE
                        "s" -> Comment.TextType.CROSSED
                        "br" -> Comment.TextType.BREAK
                        else -> {
                            if (textItem.hasClass("text_spoiler")) {
                                Comment.TextType.SPOILER
                            } else {
                                Comment.TextType.REGULAR
                            }
                        }
                    }

                    if (textItem.text() != "спойлер") {
                        text.add(Pair(type, textItem.text()))
                    }
                }
            }
        }
        val indentElement: String = el.attr("data-indent")
        var indent = 0
        if (indentElement.isNotEmpty()) {
            indent = indentElement.toInt()
        }

        val likes = el.select("span.b-comment__likes_count i")[0].text().toInt()
        val isDisabled = el.select("span.show-likes-comment")[0].hasClass("disabled")
        val isSelfDisabled = el.select("span.show-likes-comment")[0].hasClass("self-disabled")

        val editorButtons = el.select("div.actions")[0].select("ul.edit li a")
        var isControls = false
        if (editorButtons.size > 0) {
            isControls = true
        }

        val comment = Comment(id, avatarPath, nickname, text, date, indent, likes, isDisabled, isControls)
        for (btn in editorButtons) {
            if (btn.text() == "Удалить") {
                comment.deleteHash = btn.attr("onclick").replace("sof.comments.deleteComment(${comment.id}, '", "").replace("', 0, 'ajax');", "")
            }
        }

        comment.isSelfDisabled = isSelfDisabled

        return comment
    }

    fun postComment(filmId: String, text: String, parent: Int, indent: Int): Comment {
        val data: ArrayMap<String, String> = ArrayMap()
        data["comments"] = text
        data["post_id"] = filmId
        data["parent"] = parent.toString()
        data["type"] = "0"
        data["g_recaptcha_response"] = ""
        data["has_adb"] = "1"

        val result: Document? = BaseModel.getJsoup(SettingsData.provider + COMMENT_ADD)
            .data(data)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider) + "; allowed_comments=1")
            .post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            val isSuccess: Boolean = jsonObject.getBoolean("success")
            val msg = jsonObject.getString("message")

            if (msg == "[\"Он отобразится на сайте после проверки администрацией.\"]") {
                throw HttpStatusException("comment must be apply by admin", 403, SettingsData.provider)
            }

            val doc = Jsoup.parse(msg)
            if (isSuccess) {
                val comment: Comment = getCommentData(doc)
                comment.indent = indent
                return comment
            } else {
                throw HttpStatusException("failed to post comment", 400, SettingsData.provider)
            }
        } else {
            throw HttpStatusException("failed to post comment", 400, SettingsData.provider)
        }
    }

    fun postLike(comment: Comment, type: Comment.LikeType) {
        BaseModel.getJsoup(SettingsData.provider + COMMENT_LIKE + "?id=${comment.id}")
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .execute()
            .body()

        if (type == Comment.LikeType.PLUS) {
            comment.likes += 1
            comment.isDisabled = true
        } else {
            comment.likes -= 1
            comment.isDisabled = false
        }
    }

    fun deleteComment(comment: Comment) {
        BaseModel.getJsoup(
            SettingsData.provider + DELETE_COMMENT +
                    "?id=${comment.id}" +
                    "&dle_allow_hash=${comment.deleteHash}"
                    + "&type=0&area=ajax"
        )
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .execute()
            .body()
    }
}