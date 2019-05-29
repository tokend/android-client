package org.tokend.template

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_sas.*
import kotlinx.android.synthetic.main.layout_balance_change_main_data.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.childrenSequence
import org.jetbrains.anko.textColor
import org.tokend.template.activities.BaseActivity
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.util.AnimationUtil
import org.tokend.template.view.util.formatter.DateFormatter
import java.util.*

class SasActivity : BaseActivity() {

    override val allowUnauthorized = true

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sas)

        initToolbar()

        toolbar.title = "-101.4 USD"
        amount_text_view.text = "-101.4 USD"
        amount_text_view.textColor = ContextCompat.getColor(this, R.color.sent)

        toolbar.subtitle = "Payment"
        operation_name_text_view.text = "Payment"

        top_info_text_view.text = DateFormatter(this).formatLong(Date())
        bottom_info_text_view.text = "Fee: 1.4 USD"

        val a = DetailsItemsAdapter()
        val l = LinearLayoutManager(this)
        recycler.layoutManager = l
        recycler.adapter = a

        a.addData(
                DetailsItem(
                        text = "alice@mail.com",
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account),
                        hint = getString(R.string.tx_recipient)
                ),
                DetailsItem(
                        text = "Hello from the test screen",
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_label_outline),
                        hint = getString(R.string.payment_description)
                )
        )
    }

    private fun initToolbar() {
        toolbar.background = ColorDrawable(Color.WHITE)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitleAppearance)

        initToolbarAnimations()
    }

    private fun initToolbarAnimations() {
        // Force toolbar to create title and subtitle views.
        toolbar.title = "*"
        toolbar.subtitle = "*"

        val fadingToolbarViews = toolbar
                .childrenSequence()
                .filter { it is TextView }

        val fadeInDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val fadeOutDuration = collapsing_toolbar.scrimAnimationDuration

        fadingToolbarViews.forEach {
            it.visibility = View.INVISIBLE
        }

        collapsing_toolbar.scrimCallback = { scrimShown ->
            fadingToolbarViews.forEach {
                if (scrimShown) {
                    AnimationUtil.fadeInView(it, fadeInDuration)
                } else {
                    AnimationUtil.fadeOutView(it, fadeOutDuration)
                }
            }
        }
    }
}
