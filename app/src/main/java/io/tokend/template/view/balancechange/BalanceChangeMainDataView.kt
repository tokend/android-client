package io.tokend.template.view.balancechange

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import io.tokend.template.R
import io.tokend.template.extensions.childrenSequence
import io.tokend.template.extensions.dip
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.view.ScrimCallbackCollapsingToolbarLayout
import io.tokend.template.view.util.AnimationUtil
import io.tokend.template.view.util.formatter.AmountFormatter
import io.tokend.template.view.util.formatter.DateFormatters
import java.math.BigDecimal
import java.util.*

class BalanceChangeMainDataView(
    private val containerAppbar: AppBarLayout,
    private val amountFormatter: AmountFormatter
) {
    private val context = containerAppbar.context
    private val toolbar =
        containerAppbar.findViewById<Toolbar>(R.id.toolbar)
    private val collapsingToolbarLayout =
        containerAppbar.findViewById<ScrimCallbackCollapsingToolbarLayout>(R.id.collapsing_toolbar)
    private val elevationView = containerAppbar.findViewById<View>(R.id.appbar_elevation_view)
    private val amountTextView = containerAppbar.findViewById<TextView>(R.id.amount_text_view)
    private val operationNameTextView =
        containerAppbar.findViewById<TextView>(R.id.operation_name_text_view)
    private val topInfoTextView =
        containerAppbar.findViewById<TextView>(R.id.top_info_text_view)
    private val bottomInfoTextView =
        containerAppbar.findViewById<TextView>(R.id.bottom_info_text_view)

    init {
        initToolbarAnimations()
    }

    private fun initToolbarAnimations() {
        // Force toolbar to create title and subtitle views.
        toolbar.title = "*"
        toolbar.subtitle = "*"

        val fadingToolbarViews = toolbar
            .childrenSequence()
            .filter { it is TextView }

        val fadeDuration = collapsingToolbarLayout.scrimAnimationDuration

        // Title, subtitle.
        fadingToolbarViews.forEach {
            it.visibility = View.INVISIBLE
        }

        collapsingToolbarLayout.scrimCallback = { scrimShown ->
            fadingToolbarViews.forEach {
                if (scrimShown) {
                    AnimationUtil.fadeInView(it, fadeDuration)
                } else {
                    AnimationUtil.fadeOutView(it, fadeDuration)
                }
            }
        }

        // Elevation.
        elevationView.visibility = View.GONE
        var elevationIsVisible = false
        val elevationOffsetThreshold = -context.dip(8)

        containerAppbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                val elevationMustBeVisible = verticalOffset <= elevationOffsetThreshold
                if (elevationMustBeVisible != elevationIsVisible) {
                    if (elevationMustBeVisible) {
                        AnimationUtil.fadeInView(elevationView, fadeDuration)
                    } else {
                        AnimationUtil.fadeOutView(elevationView, fadeDuration)
                    }
                    elevationIsVisible = elevationMustBeVisible
                }
            }
        )
    }

    fun displayAmount(
        amount: BigDecimal,
        asset: Asset,
        isReceived: Boolean?
    ) {
        val sign =
            if (isReceived == false)
                "-"
            else
                ""

        val color = when (isReceived) {
            true -> ContextCompat.getColor(context, R.color.received)
            false -> ContextCompat.getColor(context, R.color.sent)
            else -> null
        }

        val amountString = sign + amountFormatter.formatAssetAmount(amount, asset)
        toolbar.title = amountString
        amountTextView.text = amountString

        if (color != null) {
            amountTextView.setTextColor(color)
        }
    }

    fun displayOperationName(name: String) {
        toolbar.subtitle = name
        operationNameTextView.text = name
    }

    fun displayDate(date: Date) {
        topInfoTextView.text = DateFormatters.long(context).format(date)
    }

    fun displayNonZeroFee(
        fee: BigDecimal,
        asset: Asset
    ) {
        if (fee.signum() > 0) {
            bottomInfoTextView.text = context.getString(
                R.string.template_fee,
                amountFormatter.formatAssetAmount(fee, asset)
            )
        } else {
            bottomInfoTextView.text = context.getString(R.string.no_fee_charged)
        }
    }
}