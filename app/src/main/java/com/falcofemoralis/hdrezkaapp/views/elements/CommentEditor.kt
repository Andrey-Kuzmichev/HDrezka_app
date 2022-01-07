package com.falcofemoralis.hdrezkaapp.views.elements

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.models.CommentsModel
import com.falcofemoralis.hdrezkaapp.objects.Comment
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.utils.DialogManager
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException

class CommentEditor(
    val editorContainer: LinearLayout,
    private val context: Context,
    val filmId: String,
    private val iConnection: IConnection,
    val iCommentEditor: ICommentEditor
) {
    interface ICommentEditor {
        fun changeCommentEditorState(isKeyboard: Boolean)

        fun onCommentPost(comment: Comment, position: Int)

        fun onDialogVisible()

        fun onNothingEntered()
    }

    class CommentEditorEditText(context: Context?, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatEditText(context!!, attrs) {
        private var keyImeChangeListener: KeyImeChange? = null
        fun setKeyImeChangeListener(listener: KeyImeChange?) {
            keyImeChangeListener = listener
        }

        interface KeyImeChange {
            fun onKeyIme(keyCode: Int, event: KeyEvent?)
        }

        override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
            if (keyImeChangeListener != null) {
                keyImeChangeListener!!.onKeyIme(keyCode, event)
            }
            return false
        }
    }

    private var position: Int = 0
    private var parent: Int = 0
    private var indent: Int = 0
    var textArea: CommentEditorEditText

    init {
        val editorView: LinearLayout = LayoutInflater.from(context).inflate(R.layout.inflate_comment_editor, editorContainer) as LinearLayout
        textArea = editorView.findViewById(R.id.comment_editor_et_new_comment)

        editorView.findViewById<ImageView>(R.id.comment_editor_iv_tag_b).setOnClickListener { addTag(textArea, "[b]") }
        editorView.findViewById<ImageView>(R.id.comment_editor_iv_tag_i).setOnClickListener { addTag(textArea, "[i]") }
        editorView.findViewById<ImageView>(R.id.comment_editor_iv_tag_u).setOnClickListener { addTag(textArea, "[u]") }
        editorView.findViewById<ImageView>(R.id.comment_editor_iv_tag_s).setOnClickListener { addTag(textArea, "[s]") }
        editorView.findViewById<ImageView>(R.id.comment_editor_iv_tag_spoiler).setOnClickListener { addTag(textArea, "[spoiler]") }

        val dialog = DialogManager.getDialog(context, R.string.new_comment_header)
        dialog.setPositiveButton(context.getString(R.string.add)) { dialog, id ->
            GlobalScope.launch {
                try {
                    val comment: Comment = CommentsModel.postComment(filmId, textArea.text.toString(), parent, indent)

                    withContext(Dispatchers.Main) {
                        iCommentEditor.onCommentPost(comment, position)
                        textArea.text?.clear()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (e is HttpStatusException) {
                            if (e.statusCode == 403) {
                                Toast.makeText(context, context.getString(R.string.comment_need_apply), Toast.LENGTH_SHORT).show()
                            } else {
                                ExceptionHelper.catchException(e, iConnection)
                            }
                        } else {
                            ExceptionHelper.catchException(e, iConnection)
                        }
                    }
                }
            }

        }
        dialog.setNegativeButton(context.getString(R.string.cancel)) { dialog, id ->
            dialog.dismiss()
        }

        val d = dialog.create()
        editorView.findViewById<ImageView>(R.id.comment_editor_iv_add).setOnClickListener {
            if (textArea.text.toString().isNotEmpty()) {
                d.show()
                if (SettingsData.deviceType == DeviceType.TV) {
                    iCommentEditor.changeCommentEditorState(false)
                }
                iCommentEditor.onDialogVisible()
            } else {
                iCommentEditor.onNothingEntered()
            }
        }

        textArea.setKeyImeChangeListener(object : CommentEditorEditText.KeyImeChange {
            override fun onKeyIme(keyCode: Int, event: KeyEvent?) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (SettingsData.deviceType != DeviceType.TV) {
                        iCommentEditor.changeCommentEditorState(false)
                    }
                }
            }
        })
    }

    fun setCommentSource(position: Int, parent: Int, indent: Int, toUser: String) {
        this.position = position
        this.parent = parent
        this.indent = indent

        textArea.hint = context.getString(R.string.post_comment_hint) + " " + toUser
    }

    private fun addTag(area: EditText, startTag: String) {
        val s = area.selectionStart
        val e = area.selectionEnd
        area.text.insert(e, startTag.replaceRange(1, 1, "/"))
        area.text.insert(s, startTag)
    }
}