package com.peanut.sdk.miuidialog

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.utils.MDUtil
import com.afollestad.materialdialogs.utils.MDUtil.resolveColor
import com.peanut.sdk.miuidialog.AddInFunction.gone
import com.peanut.sdk.miuidialog.AddInFunction.resolveLayout
import com.peanut.sdk.miuidialog.AddInFunction.resolveText
import com.peanut.sdk.miuidialog.AddInFunction.visible
import com.peanut.sdk.miuidialog.MIUIVersion.MIUI11
import com.peanut.sdk.miuidialog.content_wrapper.*

/**
 * MIUI11计划：
 *              list、按钮倒计时
 */
typealias DismissCallback = (MIUIDialog) -> Unit

/**
 * 创建一个MIUIDialog
 *
 * @param miuiVersion miui版本号, 不同的版本号的UI不一样, 代码都是一样的
 */
class MIUIDialog(private val context: Context, private val miuiVersion: Int = MIUI11) {
    private var dialog: MaterialDialog? = null

    private var dismissAction: DismissCallback? = null

    private var titleWrapper: TitleWrapper? = null
    private var inputWrapper: InputWrapper? = null
    private var messageWrapper: MessageWrapper? = null
    private var positiveWrapper: PositiveWrapper? = null
    private var negativeWrapper: NegativeWrapper? = null

    private var miuiView: View? = null
    private var miuiLight: Boolean = true

    /**
     * Shows a title, or header, at the top of the dialog.
     *
     * @param res The string resource to display as the title.
     * @param text The literal string to display as the title.
     */
    fun title(@StringRes res: Int? = null, text: String? = null): MIUIDialog = apply {
        MDUtil.assertOneSet("title", text, res)
        this.titleWrapper = TitleWrapper(res, text)
    }

    /**
     * Shows a message, below the title, and above the action buttons (and checkbox prompt).
     *
     * @param res The string resource to display as the message.
     * @param text The literal string to display as the message.
     */
    fun message(@StringRes res: Int? = null, text: CharSequence? = null, messageSetting: MessageSetting? = null): MIUIDialog = apply {
        MDUtil.assertOneSet("message", text, res)
        messageWrapper = MessageWrapper(res, text, messageSetting).apply {
            messageSetting?.invoke(this.MessageSettings())
        }
    }

    /**
     * Shows a positive action button, in the far right at the bottom of the dialog.
     *
     * @param res The string resource to display on the title.
     * @param text The literal string to display on the button.
     * @param click A listener to invoke when the button is pressed.
     */
    fun positiveButton(@StringRes res: Int? = null, text: CharSequence? = null,countdown:Int? = null, click: PositiveCallback? = null): MIUIDialog = apply {
        this.positiveWrapper = PositiveWrapper(res, text, countdown, click)
    }

    /**
     * Shows a negative action button, to the left of the positive action button (or at the far
     * right if there is no positive action button).
     *
     * @param res The string resource to display on the title.
     * @param text The literal string to display on the button.
     * @param click A listener to invoke when the button is pressed.
     */
    fun negativeButton(@StringRes res: Int? = null, text: CharSequence? = null, countdown:Int? = null, click: NegativeCallback? = null): MIUIDialog = apply {
        this.negativeWrapper = NegativeWrapper(res, text, countdown, click)
    }

    /** Applies multiple properties to the dialog and opens it. */
    fun show(func: MIUIDialog.() -> Unit): MIUIDialog = apply {
        func()
        this.show()
    }

    /**
     * Adds a listener that's invoked when the dialog is [MIUIDialog.onDismiss]'d. If this is called
     * multiple times, it overwriting , rather than appends additional callbacks.(这里和他不一样，也没必要弄几个callbacks)
     */
    fun onDismiss(callback: DismissCallback): MIUIDialog = apply {
        this.dismissAction = callback
    }

    /**
     * Shows an input field as the content of the dialog. Can be used with a message and checkbox
     * prompt, but cannot be used with a list.
     *
     * @param hint The literal string to display as the input field hint.
     * @param hintRes The string resource to display as the input field hint.
     * @param prefill The literal string to pre-fill the input field with.
     * @param prefillRes The string resource to pre-fill the input field with.
     * @param inputType The input type for the input field, e.g. phone or email. Defaults to plain text.
     * @param maxLength The max length for the input field, shows a counter and disables the positive
     *    action button if the input length surpasses it.(not available yet!)
     * @param waitForPositiveButton When true, the [callback] isn't invoked until the positive button
     *    is clicked. Otherwise, it's invoked every time the input text changes. Defaults to true if
     *    the dialog has buttons.
     * @param allowEmpty Defaults to false. When false, the positive action button is disabled unless
     *    the input field is not empty.
     * @param callback A listener to invoke for input text notifications.
     */
    fun input(
            hint: String? = null,
            @StringRes hintRes: Int? = null,
            prefill: CharSequence? = null,
            @StringRes prefillRes: Int? = null,
            inputType: Int = InputType.TYPE_CLASS_TEXT,
            maxLength: Int? = null,
            multiLines: Boolean = false,
            waitForPositiveButton: Boolean = true,
            allowEmpty: Boolean = false,
            callback: InputCallback? = null
    ): MIUIDialog = apply {
        inputWrapper = InputWrapper(hint, hintRes, prefill, prefillRes, inputType, maxLength, multiLines, waitForPositiveButton, allowEmpty, callback)
    }

    /** Enables or disables an action button. */
    fun setActionButtonEnabled(
            which: WhichButton,
            enabled: Boolean
    ) {
        when(which){
            WhichButton.POSITIVE -> {
                miuiView?.findViewById<Button>(R.id.miui_button_positive)?.let {
                    it.isEnabled = enabled
                    it.setTextColor(if (enabled) Color.parseColor("#0b94f2") else Color.GRAY)
                }
            }
            WhichButton.NEGATIVE -> {
                miuiView?.findViewById<Button>(R.id.miui_button_negative)?.let {
                    it.isEnabled = enabled
                    it.setTextColor(if (enabled) ThemedColor.mainColor(miuiLight) else Color.GRAY)
                }
            }
            else -> {
                //miui不支持显示中立按钮
            }
        }
    }

    /**
     * Cancel the dialog.
     */
    fun cancel() = dialog?.cancel()

    /**
     * Gets the input EditText for the dialog.
     */
    fun getInputField() = miuiView?.findViewById<EditText>(R.id.miui_input)

    private fun calculateVisionLight() {
        //处理主题色
        miuiLight = resolveColor(attr = R.attr.md_background_color, context = context) {
            resolveColor(attr = R.attr.colorBackgroundFloating, context = context)
        }.let {
            val r: Float = (it shr 16 and 0xff) / 255.0f
            val g: Float = (it shr 8 and 0xff) / 255.0f
            val b: Float = (it and 0xff) / 255.0f
            0.299 * r + 0.587 * g + 0.114 * b
        }.let {
            it > 0.5
        }
    }

    private fun show() {
        //处理不同的MIUI版本
        miuiView = when (miuiVersion) {
            MIUI11 -> context.resolveLayout(miuiLight, dayLayoutRes = R.layout.miui11layout, nightLayoutRes = R.layout.miui11layout_night)
            else -> null
        }
        calculateVisionLight()
        miuiView?.let {
            populateTitle(it)
            populateMessage(it)
            populateInput(it)
            populatePositiveButton(it)
            populateNegativeButton(it)
        }
        dialog = MaterialDialog(context, BottomSheet(layoutMode = LayoutMode.WRAP_CONTENT)).show {
            customView(view = miuiView, noVerticalPadding = true)
            onDismiss {
                dismissAction?.invoke(this@MIUIDialog)
            }
        }
    }

    private fun populateTitle(view: View) {
        if (titleWrapper != null) {
            view.findViewById<TextView>(R.id.miui_title).let {
                it.gone()
                titleWrapper?.let { wrapper ->
                    it.text = context.resolveText(res = wrapper.res, text = wrapper.text)
                    it.visible()
                }
            }
        }
    }

    private fun populateMessage(view: View) {
        view.findViewById<TextView>(R.id.miui_message).let {
            it.gone()
            messageWrapper?.populate(it,context)
        }
    }

    private fun populateInput(view: View) {
        view.findViewById<EditText>(R.id.miui_input).let {
            it.gone()
            inputWrapper?.populate(it,context,this)
        }
    }

    private fun populatePositiveButton(view: View) {
        view.findViewById<Button>(R.id.miui_button_positive).let {
            it.gone()
            view.findViewById<LinearLayout>(R.id.miui_action_panel).gone()
            positiveWrapper?.let { wrapper ->
                it.visible()
                val userText = context.resolveText(res = wrapper.res, text = wrapper.text)
                it.text = userText
                wrapper.countdown?.let { second->
                    object :Thread(){
                        override fun run() {
                            var i = -1
                            while (second-++i>0){
                                Handler(context.mainLooper).post {
                                    this@MIUIDialog.setActionButtonEnabled(WhichButton.POSITIVE,false)
                                    it.text = String.format("%s(%d)",userText,second-i)
                                }
                                sleep(1000)
                            }
                            Handler(context.mainLooper).post {
                                it.text = userText
                                this@MIUIDialog.setActionButtonEnabled(WhichButton.POSITIVE,true)
                            }
                        }
                    }.start()
                }
                it.setOnClickListener {
                    wrapper.click?.invoke(this)
                    if (inputWrapper?.waitForPositiveButton == true)
                        inputWrapper?.callback?.invoke(this.getInputField()?.text, this)
                    cancel()
                }
                view.findViewById<LinearLayout>(R.id.miui_action_panel).visible()
            }
        }
    }

    private fun populateNegativeButton(view: View) {
        view.findViewById<Button>(R.id.miui_button_negative).let {
            it.gone()
            view.findViewById<LinearLayout>(R.id.miui_action_panel).gone()
            negativeWrapper?.let { wrapper ->
                it.visible()
                val userText = context.resolveText(res = wrapper.res, text = wrapper.text)
                it.text = userText
                wrapper.countdown?.let { second->
                    object :Thread(){
                        override fun run() {
                            var i = -1
                            while (second-++i>0){
                                Handler(context.mainLooper).post {
                                    it.text = String.format("%s(%d)",userText,second-i)
                                }
                                sleep(1000)
                            }
                            Handler(context.mainLooper).post {
                                it.text = userText
                                it.performClick()
                            }
                        }
                    }.start()
                }
                it.setOnClickListener {
                    wrapper.click?.invoke(this)
                    cancel()
                }
                view.findViewById<LinearLayout>(R.id.miui_action_panel).visible()
            }
        }
    }
}