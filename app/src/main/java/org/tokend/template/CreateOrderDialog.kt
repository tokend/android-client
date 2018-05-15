package org.tokend.template

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.*
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_dialog_crate_order.*
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.features.trade.model.Order
import org.tokend.template.util.inputChanges

class CreateOrderDialog : DialogFragment() {

    companion object {
        private const val EXTRA_ORDER = "extra_order"

        fun withArgs(order: Order): CreateOrderDialog {

            val dialog = CreateOrderDialog()

            val args = Bundle()
            args.putSerializable(EXTRA_ORDER, order)
            dialog.arguments = args

            return dialog
        }
    }

    private val dialogResultSubject = PublishSubject.create<Order>()
    private lateinit var disposable: CompositeDisposable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.fragment_dialog_crate_order, container)
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT)

        disposable = CompositeDisposable(
                btnClicks(),
                textFields()
        )

        arguments?.let { args ->
            val order: Order = args.getSerializable(EXTRA_ORDER) as Order

            price_edit_text.setText(order.price.toString())
            amount_edit_text.setText(order.amount.toString())
        }
    }

    override fun onStop() {
        super.onStop()
        disposable.dispose()
    }

    fun showDialog(manager: FragmentManager?, tag: String?): PublishSubject<Order> {
        super.show(manager, tag)
        return dialogResultSubject
    }

    private fun btnClicks() = CompositeDisposable(
            cancel_btn.clicks()
                    .subscribe({
                        dismiss()
                    }),

            buy_btn.clicks()
                    .subscribe({
                        dialogResultSubject.onNext(createOrder(Order.OrderType.BUY))
                        dismiss()
                    }),

            sell_btn.clicks()
                    .subscribe({
                        dialogResultSubject.onNext(createOrder(Order.OrderType.SELL))
                        dismiss()
                    })
    )

    private fun createOrder(type: Order.OrderType): Order {
        return Order(type,
                amount_edit_text.text.toString().toBigDecimal(),
                price_edit_text.text.toString().toBigDecimal(),
                "ETH")
    }

    private fun textFields() = Observable.combineLatest(
            price_edit_text.inputChanges(),
            amount_edit_text.inputChanges(),
            BiFunction<String, String, Pair<String, String>>({
                n ,a -> Pair(n, a)
            })).doOnNext({
                temp(it.first, it.second)
            }).map({
                it.first.isNotBlank() && it.second.isNotBlank()
            }).subscribe({
                buy_btn.isEnabled = it
                sell_btn.isEnabled = it
            })

    private fun temp(price: String, amount: String) {
        when(price.isNotBlank() && amount.isNotBlank()) {
            true -> {

                val total = AmountFormatter.formatAssetAmount(price.toBigDecimal() * amount.toBigDecimal()
                        , minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS,
                        abbreviation = false)

                total_amount_text_view.text =  "$total"
            }
            else -> {
                total_amount_text_view.text = "0.000000"
            }
        }
    }
}