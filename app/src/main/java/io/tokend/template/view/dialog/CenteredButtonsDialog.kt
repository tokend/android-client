package io.tokend.template.view.dialog

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.extensions.dip
import kotlin.random.Random

class CenteredButtonsDialog(
    private val context: Context,
    private val buttons: List<Button>,
    private val title: String? = null,
    private val message: String? = null
) {
    class Button(
        val text: String,
        val dismissDialog: Boolean = true,
        @ColorRes
        val colorRes: Int = R.color.primary_action,
        val tag: Any = Random.nextInt()
    )

    fun show(clickListener: (Button) -> Unit): AlertDialog {
        var dialog: AlertDialog? = null
        val horizontalSpace = context.resources.getDimensionPixelSize(R.dimen.standard_padding)
        val verticalSpace = context.resources.getDimensionPixelSize(R.dimen.quarter_standard_margin)

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, verticalSpace, 0, verticalSpace)

                val addVerticalSpace = { height: Int ->
                    addView(Space(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, height)
                    })
                }

                if (title != null) {
                    addVerticalSpace(verticalSpace * 4)
                    addView(TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginStart = horizontalSpace
                            marginEnd = horizontalSpace
                        }

                        gravity = Gravity.CENTER
                        text = title
                        setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            context.resources.getDimensionPixelSize(R.dimen.text_size_heading)
                                .toFloat()
                        )
                    })
                }

                if (message != null) {
                    if (title != null) {
                        addVerticalSpace(verticalSpace * 2)
                    } else {
                        addVerticalSpace(verticalSpace * 4)
                    }

                    addView(TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginStart = horizontalSpace
                            marginEnd = horizontalSpace
                        }

                        gravity = Gravity.CENTER
                        text = message
                        setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                        //TypefaceUtil.applyRegularTypeface(this)
                    })
                }

                val addDivider = {
                    addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            context.dip(1)
                        )

                        background = ContextCompat.getDrawable(context, R.drawable.line_divider)
                    })
                }

                val addButton = { button: Button,
                                  withDivider: Boolean ->
                    val themedContext = ContextThemeWrapper(context, R.style.TextButton)

                    addView(Button(themedContext, null, R.style.TextButton).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                        text = button.text
                        setTextColor(ContextCompat.getColor(context, button.colorRes))
                        setOnClickListener {
                            if (button.dismissDialog) {
                                dialog?.dismiss()
                            }
                            clickListener(button)
                        }
                        //TypefaceUtil.applyMediumTypeface(this)
                    })

                    if (withDivider) {
                        addDivider()
                    }
                }

                if (title != null && message == null) {
                    addVerticalSpace(verticalSpace * 5)
                    addDivider()
                } else if (message != null) {
                    addVerticalSpace(verticalSpace * 4)
                    addDivider()
                }

                buttons.forEachIndexed { i, button ->
                    addButton(button, i < buttons.size - 1)
                }
            })
            .show()
            .also { dialog = it }
    }
}