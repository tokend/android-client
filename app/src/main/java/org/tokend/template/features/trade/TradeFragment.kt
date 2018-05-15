package org.tokend.template.features.trade

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.*
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_trade.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.CreateOrderDialog

import org.tokend.template.R
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.features.trade.adapter.OnOrderClickLIsteener
import org.tokend.template.features.trade.adapter.TradeAdapter
import org.tokend.template.features.trade.model.Order
import org.tokend.template.util.ToastManager
import java.math.BigDecimal

class TradeFragment : Fragment(), ToolbarProvider, OnOrderClickLIsteener {

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {
        initToolbar()
        initOrdersRecyclerView()
    }

    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.inflateMenu(R.menu.menu_trade)
        toolbar.title = context?.getString(R.string.trade_title)

        toolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.add_order -> {
                    CreateOrderDialog()
                            .showDialog(this.childFragmentManager, "dialog")
                            .subscribe({
                                ToastManager.short(it.type.name)
                            })
                    true
                }
                else -> false
            }
        }
    }

    private fun initOrdersRecyclerView() {
        orders_recycler_view.layoutManager = GridLayoutManager(activity?.baseContext, 2, LinearLayoutManager.VERTICAL, false)
        val adapter = TradeAdapter()
        adapter.listener = this
        orders_recycler_view.adapter = adapter
        adapter.setObjectList(orders())
    }

    override fun onOrderClick(order: Order) {
        CreateOrderDialog.withArgs(order)
                .showDialog(this.childFragmentManager, "dialog")
                .subscribe({

                })
    }

    private fun orders(): List<Order> {
        val list = ArrayList<Order>()

        for(i in 1..11) {
            list += Order(Order.OrderType.BUY ,i.toBigDecimal(), (i * 0.01).toBigDecimal(), "ETH")
        }

        for(i in 1..8) {
            list += Order(Order.OrderType.SELL, i.toBigDecimal(), (i * 0.02).toBigDecimal(), "ETH")
        }

        return list
    }
}
