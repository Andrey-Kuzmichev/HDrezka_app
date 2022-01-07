package com.falcofemoralis.hdrezkaapp.views.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.italic
import androidx.core.text.strikeThrough
import androidx.core.text.underline
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.models.CommentsModel
import com.falcofemoralis.hdrezkaapp.objects.Comment
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.utils.UnitsConverter
import com.falcofemoralis.hdrezkaapp.views.elements.CommentEditor
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CommentsRecyclerViewAdapter(
    private val context: Context,
    private val comments: ArrayList<Comment>,
    private val commentEditor: CommentEditor?,
    private val iConnection: IConnection,
    private val filmView: FilmView
) :
    RecyclerView.Adapter<CommentsRecyclerViewAdapter.ViewHolder>() {
    enum class CommentColor {
        DARK,
        LIGHT
    }

    private val INDENT_WEIGHT = 20
    private val spoilerTag = "||"
    private val spoilerText = context.getString(R.string.spoiler)
    private var isNextColor = true
    private var lastColor: CommentColor = CommentColor.DARK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.inflate_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // init
        if (position == 0) {
            lastColor = CommentColor.DARK
        }

        val comment = comments[position]

        // set header
        holder.infoView.text = "${comment.nickname}, ${comment.date}"

        var glide = Glide.with(context).load(comment.avatarPath).fitCenter()
        glide = glide.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: Boolean): Boolean {
                return false
            }

            override fun onResourceReady(p0: Drawable?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                GlobalScope.launch {
                    withContext(Dispatchers.Main){
                        holder.avatarProgressBar.visibility = View.GONE
                        holder.avatarView.visibility = View.VISIBLE
                    }
                }
                return false
            }
        })
        glide = glide.error (R.drawable.no_avatar) // TODO
        glide = glide.override(Target.SIZE_ORIGINAL)
        glide.into(holder.avatarView)
        glide.submit()



        // set text with spoilers
        holder.textView.text = ""
        val spoilers: ArrayList<String> = ArrayList()
        val str = SpannableStringBuilder()
        for ((i, item) in comment.text.withIndex()) {
            when (item.first) {
                Comment.TextType.REGULAR -> str.append(item.second)
                Comment.TextType.SPOILER -> {
                    spoilers.add(item.second)
                    holder.textView.text = str.append("$spoilerTag${item.second}$spoilerTag")
                }
                Comment.TextType.BOLD -> str.bold { append(item.second) }
                Comment.TextType.INCLINED -> str.italic { append(item.second) }
                Comment.TextType.CROSSED -> str.strikeThrough { append(item.second) }
                Comment.TextType.UNDERLINE -> str.underline { append(item.second) }
                Comment.TextType.BREAK -> str.append("\n")

            }
            if (i != comment.text.size - 1) {
                if (comment.text[i + 1].first != Comment.TextType.REGULAR) {
                    str.append(" ")
                }
            }
        }
        holder.textView.text = str

        if (spoilers.size > 0) {
            holder.textView.createSpoilerText(spoilers)
        }

        // set indent margin
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(UnitsConverter.getPX(context, comment.indent * INDENT_WEIGHT), UnitsConverter.getPX(context, 6), 0, 0)
        holder.layout.layoutParams = params

        if (comment.indent > 0) {
            holder.indentLineView.visibility = View.VISIBLE
            isNextColor = false
        } else {
            holder.indentLineView.visibility = View.GONE
            isNextColor = true
        }

        // mix color
        if (isNextColor) {
            lastColor = when (lastColor) {
                CommentColor.LIGHT -> CommentColor.DARK
                CommentColor.DARK -> CommentColor.LIGHT
            }
        }
        holder.layout.setBackgroundColor(
            ContextCompat.getColor(
                context, when (lastColor) {
                    CommentColor.DARK -> R.color.dark_background
                    CommentColor.LIGHT -> R.color.light_background
                }
            )
        )


        if (commentEditor != null) {
            // set reply btn
            var isNextComment = true
            var nextCommentPos = 0
            var n = 1
            while (isNextComment) {
                if (position + n == comments.size) {
                    isNextComment = false
                    nextCommentPos = position + n
                } else {
                    val nextComment: Comment = comments[position + n]
                    if (nextComment.indent == comment.indent || comment.indent > nextComment.indent) {
                        nextCommentPos = position + n
                        isNextComment = false
                    } else {
                        n++
                    }
                }
            }

            holder.replyBtn.setOnClickListener {
                if (commentEditor.editorContainer.visibility == View.GONE) {
                    commentEditor.iCommentEditor.changeCommentEditorState(true)
                }
                commentEditor.setCommentSource(nextCommentPos, comment.id, comment.indent + 1, comment.nickname)
            }
        } else {
            holder.replyBtn.visibility = View.GONE
        }

        if (commentEditor != null) {
            // set like btn

            setLike(holder, comment.likes, comment.isDisabled or comment.isSelfDisabled)
            holder.likeBtn.setOnClickListener {
                if (!comment.isSelfDisabled) {
                    val type = if (comment.isDisabled) {
                        Comment.LikeType.MINUS
                    } else {
                        Comment.LikeType.PLUS
                    }

                    GlobalScope.launch {
                        try {
                            CommentsModel.postLike(comment, type)
                            withContext(Dispatchers.Main) {
                                setLike(holder, comment.likes, comment.isDisabled)
                            }
                        } catch (e: Exception) {
                            ExceptionHelper.catchException(e, iConnection)
                        }
                    }


                    if (!comment.isDisabled) {

                    }
                }
            }
        } else {
            holder.likeBtn.setOnClickListener {
                Toast.makeText(context, context.getString(R.string.need_register), Toast.LENGTH_SHORT).show()
            }
        }

        // controls btn
        val visible = if (comment.isControls) {
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.deleteBtn.visibility = visible
        if (visible == View.VISIBLE) {
            holder.deleteBtn.setOnClickListener {
                GlobalScope.launch {
                    try {
                        CommentsModel.deleteComment(comment)
                        comments.removeAt(position)

                        withContext(Dispatchers.Main) {
                            filmView.redrawComments()
                        }
                    } catch (e: Exception) {
                        ExceptionHelper.catchException(e, iConnection)
                    }
                }
            }
        }
    }

    private fun setLike(holder: ViewHolder, n: Int, isDisabled: Boolean) {
        holder.likeCounter.text = "($n)"

        if (isDisabled) {
            holder.likeCounter.setTextColor(ContextCompat.getColor(context, R.color.active_like))
            ImageViewCompat.setImageTintList(holder.likeIcon, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.active_like)))
        } else {
            holder.likeCounter.setTextColor(ContextCompat.getColor(context, R.color.primary_red))
            ImageViewCompat.setImageTintList(holder.likeIcon, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_red)))
        }
    }

    private fun TextView.createSpoilerText(spoilers: ArrayList<String>) {
        var text = this.text.toString()

        val spoilersToShow: ArrayList<String> = ArrayList()
        val ranges = arrayListOf<Pair<Pair<Int, Int>, Boolean>>()
        var index = 0
        while (text.contains(spoilerTag)) {
            val start = text.indexOf(spoilerTag)
            val end = text.indexOf(spoilerTag, start + spoilerTag.length)

            text = text.replaceRange(start, end + spoilerTag.length, spoilerText)

            ranges.add(Pair(Pair(start, start + spoilerText.length), false))
            spoilersToShow.add(spoilers[index])

            index++
        }

        this.movementMethod = LinkMovementMethod.getInstance()

        updateTextView(text, this, ranges, spoilersToShow)
    }

    private fun updateTextView(plainText: String, textView: TextView, ranges: ArrayList<Pair<Pair<Int, Int>, Boolean>>, spoilers: ArrayList<String>) {
        var text = plainText
        var diff = 0
        // replace [Spoiler] with original text
        for ((index, item) in (ranges.clone() as ArrayList<Pair<Pair<Int, Int>, Boolean>>).withIndex()) {
            if (item.second) {
                val range = item.first
                text = plainText.replaceRange(range.first, range.second, spoilers[index])
                diff += spoilers[index].length - spoilerText.length

                spoilers.removeAt(index)
                ranges.removeAt(index)
            }
        }

        // Create clickable span
        val spannableString = SpannableString(text)
        for ((index, item) in ranges.withIndex()) {
            if (!item.second) {

                val range = Pair(item.first.first, item.first.second)

                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        ranges[index] = Pair(range, true)
                        updateTextView(plainText, textView, ranges, spoilers)
                    }
                }, range.first + diff, range.second + diff, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                spannableString.setSpan(
                    ForegroundColorSpan(Color.RED),
                    range.first + diff,
                    range.second + diff,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        textView.text = spannableString
    }


    override fun getItemCount(): Int = comments.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.inflate_comment_layout)
        val avatarView: ImageView = view.findViewById(R.id.inflate_comment_avatar)
        val avatarProgressBar: ProgressBar = view.findViewById(R.id.inflate_comment_avatar_loading)
        val infoView: TextView = view.findViewById(R.id.inflate_comment_nickname_date)
        val textView: TextView = view.findViewById(R.id.inflate_comment_text)
        val indentLineView: View = view.findViewById(R.id.inflate_comment_indent)
        val replyBtn: TextView = view.findViewById(R.id.inflate_comment_reply)
        val likeBtn: LinearLayout = view.findViewById(R.id.inflate_comment_like)
        val likeCounter: TextView = view.findViewById(R.id.inflate_comment_like_counter)
        val likeIcon: ImageView = view.findViewById(R.id.inflate_comment_like_icon)

        //   val editBtn: TextView = view.findViewById(R.id.inflate_comment_edit)
        val deleteBtn: TextView = view.findViewById(R.id.inflate_comment_delete)
    }
}