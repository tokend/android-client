package org.tokend.template.view.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.widget.TextView
import org.jetbrains.anko.textColor
import org.tokend.template.R

class LimitTextView : TextView {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private val bounds = Rect()
    private val regex = Regex("/")

    override fun onDraw(canvas: Canvas?) {
        val textPaint = this.paint

        val separated = text.split(regex)
        val text = separated.first()

        textPaint.getTextBounds(text, 0, text.length, bounds)

        val width = textPaint.measureText(text)
        val height = bounds.height()
        val paddingStart = (this.width - width) / 2
        val paddingTop = (this.height - height) / 2

        canvas?.drawText(this.text.toString(), paddingStart, this.height.toFloat(), textPaint)
    }
}