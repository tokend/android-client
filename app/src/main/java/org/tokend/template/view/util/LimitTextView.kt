package org.tokend.template.view.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.TextView
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

/**
 * Displays limits or statistics values centered by slash
 */
class LimitTextView : TextView {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private val bounds = Rect()
    private var left: String = ""
    private var total: String = ""

    var unformatted: String = ""

    override fun onDraw(canvas: Canvas?) {
        val textPaint = this.paint
        textPaint.color = textColors.defaultColor

        textPaint.getTextBounds(left, 0, left.length, bounds)

        val width = textPaint.measureText(left)
        val x = this.width / 2 - width
        val y = textPaint.textSize

        canvas?.drawText("$left $total", x, y, textPaint)
    }

    fun setValues(used: BigDecimal, total: BigDecimal, asset: String, amountFormatter: AmountFormatter) {

        val zeroForAsset = BigDecimalUtil
                .scaleAmount(BigDecimal.ZERO, amountFormatter.getDecimalDigitsCount(asset))

        unformatted = when (total) {
            zeroForAsset -> "$DASH_SYMBOL $SLASH_SYMBOL $DASH_SYMBOL"
            else -> "${total - used} $SLASH_SYMBOL $total"
        }

        when (total == zeroForAsset || total == MAX) {
            true -> {
                this.left = "$DASH_SYMBOL $SLASH_SYMBOL"
                this.total = DASH_SYMBOL
            }
            else -> {
                val leftFormat =
                        amountFormatter.formatAssetAmount(total - used, asset, abbreviation = true)
                val totalFormat =
                        amountFormatter.formatAssetAmount(total, asset, abbreviation = true)

                this.left = "$leftFormat $SLASH_SYMBOL"
                this.total = totalFormat
            }
        }
        val text = "$left ${this.total}"
        this.text = text
        invalidate()
    }

    companion object {
        private const val SLASH_SYMBOL = "/"
        private const val DASH_SYMBOL = "—"

        private val MAX = BigDecimal("9223372036854.775807")
    }
}