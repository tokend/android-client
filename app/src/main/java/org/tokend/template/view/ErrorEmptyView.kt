package org.tokend.template.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.util.errorhandler.ErrorHandler

/**
 * Used to display empty or error state.
 */
class ErrorEmptyView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {

    private val messageTextView: TextView
    private val actionButton: Button
    private val iconImageView: AppCompatImageView

    private var emptyDrawable: Drawable? = null
    private var errorDrawable: Drawable? = null

    private var emptyViewDenial: () -> Boolean = { false }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        visibility = View.GONE

        LayoutInflater.from(context).inflate(R.layout.layout_error_empty_view,
                this, true)

        messageTextView = findViewById(R.id.message_text_view)
        actionButton = findViewById(R.id.action_button)
        iconImageView = findViewById(R.id.icon_image_view)

        attributeSet?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ErrorEmptyView, defStyleAttr, 0)

            val emptyRes = typedArray.getResourceId(R.styleable.ErrorEmptyView_empty_drawable, 0)
            val errorRes = typedArray.getResourceId(R.styleable.ErrorEmptyView_error_drawable, 0)

            if (emptyRes != 0) {
                emptyDrawable = ResourcesCompat.getDrawable(resources, emptyRes, null)
            }

            if (errorRes != 0) {
                errorDrawable = ResourcesCompat.getDrawable(resources, errorRes, null)
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

    private fun setIcon(drawable: Drawable?) {
        if (drawable != null) {
            iconImageView.visibility = View.VISIBLE
            iconImageView.setImageDrawable(drawable)
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
    fun showEmpty(@StringRes messageId: Int) {
        showEmpty(context.getString(messageId))
    }

    /**
     * Shows empty state with given message.
     */
    fun showEmpty(message: String) {
        visibility = View.VISIBLE

        messageTextView.text = message
        actionButton.visibility = View.GONE

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
        error ?: return

        visibility = View.VISIBLE

        messageTextView.text = error

        if (actionButtonClick != null) {
            actionButton.visibility = View.VISIBLE
            actionButton.onClick { actionButtonClick.invoke() }
        } else {
            actionButton.visibility = View.GONE
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
                       @StringRes messageId: Int) {
        adapter.registerAdapterDataObserver(getEmptyObserver(adapter) {
            context.getString(messageId)
        })
    }

    /**
     * Subscribes to [RecyclerView.Adapter] data changes in order to display empty state
     *
     * @param adapter adapter to observe
     * @param messageProvider provider of the empty state message
     */
    fun observeAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                       messageProvider: () -> String) {
        adapter.registerAdapterDataObserver(getEmptyObserver(adapter, messageProvider))
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
                                 messageProvider: () -> String):
            RecyclerView.AdapterDataObserver {
        return object : RecyclerView.AdapterDataObserver() {
            private fun operate() {
                if (adapter.itemCount > 0) {
                    hide()
                } else {
                    if (!emptyViewDenial()) {
                        showEmpty(messageProvider())
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