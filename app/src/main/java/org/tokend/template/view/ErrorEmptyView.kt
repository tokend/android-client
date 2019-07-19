package org.tokend.template.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.annotation.Dimension
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.widget.ImageViewCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.util.errorhandler.ErrorHandler
import kotlin.math.roundToInt

/**
 * Used to display empty or error state.
 */
class ErrorEmptyView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attributeSet, defStyleAttr) {
    data class ButtonAction(
            val title: String,
            val listener: () -> Unit
    )

    private val messageTextView: TextView
    private val errorActionButton: Button
    private val emptyActionButton: Button
    private val iconImageView: AppCompatImageView

    private var emptyDrawable: Drawable? = null
    private var errorDrawable: Drawable? = null
    private var drawableSize = ViewGroup.LayoutParams.WRAP_CONTENT
    @ColorInt
    private var drawableTint: Int? = null

    private var emptyViewDenial: () -> Boolean = { false }

    init {
        visibility = View.GONE

        LayoutInflater.from(context).inflate(R.layout.layout_error_empty_view,
                this, true)

        messageTextView = findViewById(R.id.message_text_view)
        errorActionButton = findViewById(R.id.error_action_button)
        emptyActionButton = findViewById(R.id.empty_action_button)
        iconImageView = findViewById(R.id.icon_image_view)

        attributeSet?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ErrorEmptyView, defStyleAttr, 0)

            val emptyRes = typedArray.getResourceId(R.styleable.ErrorEmptyView_empty_drawable, 0)
            val errorRes = typedArray.getResourceId(R.styleable.ErrorEmptyView_error_drawable, 0)
            val drawableSize = typedArray.getDimension(R.styleable.ErrorEmptyView_drawable_size, 0f)
            val drawableTint = typedArray.getColor(R.styleable.ErrorEmptyView_drawable_tint_color, Int.MIN_VALUE)

            if (emptyRes != 0) {
                emptyDrawable = ResourcesCompat.getDrawable(resources, emptyRes, null)
            }

            if (errorRes != 0) {
                errorDrawable = ResourcesCompat.getDrawable(resources, errorRes, null)
            }

            if (drawableSize != 0f) {
                this.drawableSize = drawableSize.roundToInt()
            }

            if (drawableTint != Int.MIN_VALUE) {
                this.drawableTint = drawableTint
            }

            typedArray.recycle()
        }
    }

    /***
     * Sets drawable that would be shown with empty message.
     */
    fun setEmptyDrawable(@DrawableRes id: Int) {
        setEmptyDrawable(ResourcesCompat.getDrawable(resources, id, null))
    }

    fun setEmptyDrawable(drawable: Drawable?) {
        emptyDrawable = drawable
    }

    fun getEmptyDrawable(): Drawable? {
        return emptyDrawable
    }

    /***
     * Sets drawable that would be shown with error message.
     */
    fun setErrorDrawable(@DrawableRes id: Int) {
        setErrorDrawable(ResourcesCompat.getDrawable(resources, id, null))
    }

    fun setErrorDrawable(drawable: Drawable?) {
        errorDrawable = drawable
    }

    fun getErrorDrawable(): Drawable? {
        return errorDrawable
    }

    /**
     * Sets drawable pixel size.
     */
    fun setDrawableSize(@Dimension(unit = Dimension.PX) size: Int?) {
        drawableSize = size ?: ViewGroup.LayoutParams.WRAP_CONTENT
    }

    fun getDrawableSize(): Int {
        return drawableSize
    }

    /**
     * Sets drawable tint color.
     */
    fun setDrawableTintColor(@ColorInt color: Int?) {
        drawableTint = color
    }

    @ColorInt
    fun getDrawableTintColor(): Int? {
        return drawableTint
    }

    private fun setIcon(drawable: Drawable?) {
        if (drawable != null) {
            iconImageView.visibility = View.VISIBLE
            iconImageView.layoutParams = iconImageView.layoutParams.apply {
                width = drawableSize
                height = drawableSize
            }

            iconImageView.setImageDrawable(drawable)

            drawableTint.also { tint ->
                if (tint != null) {
                    ImageViewCompat.setImageTintList(iconImageView, ColorStateList.valueOf(tint))
                } else {
                    ImageViewCompat.setImageTintList(iconImageView, null)
                }
            }
        } else {
            iconImageView.visibility = View.GONE
        }
    }

    /**
     * Hides the view.
     */
    fun hide() {
        visibility = View.GONE
    }

    /**
     * Shows empty state with given message.
     */
    fun showEmpty(@StringRes messageId: Int,
                  buttonAction: ButtonAction? = null) {
        showEmpty(context.getString(messageId), buttonAction)
    }

    /**
     * Shows empty state with given message.
     */
    fun showEmpty(message: String,
                  buttonAction: ButtonAction? = null) {
        visibility = View.VISIBLE

        messageTextView.text = message
        errorActionButton.visibility = View.GONE

        if (buttonAction != null) {
            emptyActionButton.visibility = View.VISIBLE
            emptyActionButton.text = buttonAction.title
            emptyActionButton.onClick { buttonAction.listener.invoke() }
        } else {
            emptyActionButton.visibility = View.GONE
        }


        setIcon(emptyDrawable)
    }

    /**
     * Shows error state
     *
     * @param throwable occurred error
     * @param errorHandler error handler to get error message from
     * @param actionButtonClick click listener for Retry button.
     * If not set the button will be invisible
     */
    fun showError(throwable: Throwable, errorHandler: ErrorHandler,
                  actionButtonClick: (() -> Unit)? = null) {
        showError(errorHandler.getErrorMessage(throwable),
                actionButtonClick)
    }

    /**
     * Shows error state
     *
     * @param error message to display
     * @param actionButtonClick click listener for Retry button.
     * If not set the button will be invisible
     */
    fun showError(error: String?, actionButtonClick: (() -> Unit)? = null) {
        showError(
                error,
                actionButtonClick?.let { ButtonAction(context.getString(R.string.retry), it) }
        )
    }

    /**
     * Shows error state
     *
     * @param error message to display
     * @param buttonAction if not set the button will be invisible
     */
    fun showError(error: String?, buttonAction: ButtonAction? = null) {
        error ?: return

        visibility = View.VISIBLE

        messageTextView.text = error
        emptyActionButton.visibility = View.GONE

        if (buttonAction != null) {
            errorActionButton.visibility = View.VISIBLE
            errorActionButton.text = buttonAction.title
            errorActionButton.onClick { buttonAction.listener.invoke() }
        } else {
            errorActionButton.visibility = View.GONE
        }

        setIcon(errorDrawable)
    }

    /**
     * Subscribes to [RecyclerView.Adapter] data changes in order to display empty state
     *
     * @param adapter adapter to observe
     * @param messageId empty state message id
     */
    fun observeAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                       @StringRes messageId: Int,
                       buttonAction: ButtonAction? = null) {
        adapter.registerAdapterDataObserver(
                getEmptyObserver(
                        adapter,
                        { context.getString(messageId) },
                        buttonAction
                )
        )
    }

    /**
     * Subscribes to [RecyclerView.Adapter] data changes in order to display empty state
     *
     * @param adapter adapter to observe
     * @param messageProvider provider of the empty state message
     */
    fun observeAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                       messageProvider: () -> String,
                       buttonAction: ButtonAction? = null) {
        adapter.registerAdapterDataObserver(
                getEmptyObserver(adapter, messageProvider, buttonAction)
        )
    }

    /**
     * Sets denial provider for empty view with adapter observation
     *
     * @param denial if returns true then empty state will not be displayed
     * even if observed adapter has no data
     *
     * @see observeAdapter
     */
    fun setEmptyViewDenial(denial: () -> Boolean) {
        this.emptyViewDenial = denial
    }

    private fun getEmptyObserver(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                                 messageProvider: () -> String,
                                 buttonAction: ButtonAction?):
            RecyclerView.AdapterDataObserver {
        return object : RecyclerView.AdapterDataObserver() {
            private fun operate() {
                if (adapter.itemCount > 0) {
                    hide()
                } else {
                    if (!emptyViewDenial()) {
                        showEmpty(messageProvider(), buttonAction)
                    } else {
                        hide()
                    }
                }
            }

            override fun onChanged() {
                operate()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                operate()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                operate()
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                operate()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                operate()
            }
        }
    }
}