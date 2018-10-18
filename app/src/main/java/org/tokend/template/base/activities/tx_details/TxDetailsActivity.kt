package org.tokend.template.base.activities.tx_details

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import com.google.gson.Gson
import org.jetbrains.anko.intentFor
import org.tokend.sdk.api.base.model.operations.TransferOperation
import org.tokend.sdk.api.base.model.operations.OperationState
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.LocalizedName
import org.tokend.template.util.DateFormatter
import kotlin.reflect.KClass

abstract class TxDetailsActivity<in T : TransferOperation>(
        private val typeClass: KClass<out T>
) : BaseActivity() {
    companion object {
        const val ITEM_JSON_EXTRA = "item_json"

        inline fun <reified A : TxDetailsActivity<B>, B> start(activity: Activity, item: B) {
            activity.startActivity(activity.intentFor<A>(
                    ITEM_JSON_EXTRA to Gson().toJson(item)))
        }

        inline fun <reified A : TxDetailsActivity<B>, B> startForResult(activity: Activity,
                                                                        item: B, code: Int) {
            activity.startActivityForResult(activity.intentFor<A>(
                    ITEM_JSON_EXTRA to Gson().toJson(item)), code)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val item = getItemFromIntent()
        if (item != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            displayDetails(item)
        } else {
            finish()
        }
    }

    abstract fun displayDetails(item: T)

    private fun getItemFromIntent(): T? {
        try {
            val jsonString = intent.getStringExtra(ITEM_JSON_EXTRA)
            return Gson().fromJson<T>(jsonString, typeClass.java)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    protected open fun displayDate(tx: TransferOperation, cardsLayout: ViewGroup) {
        InfoCard(cardsLayout)
                .setHeading(R.string.date, null)
                .addRow(DateFormatter(this).formatLong(tx.date), null)
    }

    protected open fun displayStateIfNeeded(tx: TransferOperation, cardsLayout: ViewGroup) {
        if (tx.state != OperationState.SUCCESS) {
            InfoCard(cardsLayout)
                    .setHeading(R.string.tx_state, null)
                    .addRow(LocalizedName(this).forTransactionState(tx.state), null)
        }
    }
}