package org.tokend.template.features.deposit.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.amountscreen.model.AmountInputResult

class DepositAmountActivity: BaseActivity() {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.fragment_user_flow)

        val bundle = intent.extras
        if (bundle == null) {
            finishWithMissingArgError("Extras bundle")
            return
        }

        initToolbar()

        val fragment = DepositAmountFragment.newInstance(bundle)

        fragment.resultObservable
                .subscribe(this::onAmountEntered)
                .addTo(compositeDisposable)

        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container_layout, fragment)
                .disallowAddToBackStack()
                .commit()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.deposit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun onAmountEntered(result: AmountInputResult) {
        setResult(
                Activity.RESULT_OK,
                Intent().putExtra(
                        RESULT_AMOUNT_EXTRA,
                        BigDecimalUtil.toPlainString(result.amount)
                )
        )
        finish()
    }

    companion object {
        const val RESULT_AMOUNT_EXTRA = "amount"

        fun getBundle(assetCode: String) =
                DepositAmountFragment.getBundle(assetCode)
    }
}