package org.tokend.template.view.balancepicker

import android.app.Dialog
import android.content.Context
import android.support.design.widget.BottomSheetDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.dialog_balance_picker.view.*
import kotlinx.android.synthetic.main.include_appbar_elevation.view.*
import kotlinx.android.synthetic.main.include_error_empty_view.view.*
import kotlinx.android.synthetic.main.layout_progress.view.*
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SearchUtil
import org.tokend.template.view.balancepicker.adapter.BalancePickerItemsAdapter
import org.tokend.template.view.balancepicker.adapter.BalancePickerListItem
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Modal bottom sheet with balances list and search,
 * allows user to pick a balance
 *
 * @param balancesFilter filter applied to [balancesRepository] items
 * @param requiredAssets collection of asset codes that will be displayed
 * no matter if there are balances for them
 */
open class BalancePickerBottomDialog(
        private val context: Context,
        private val amountFormatter: AmountFormatter,
        private val balancesComparator: Comparator<BalanceRecord>,
        private val balancesRepository: BalancesRepository,
        private val requiredAssets: Collection<Asset>? = null,
        private val balancesFilter: ((BalanceRecord) -> Boolean)? = null
) {
    protected lateinit var adapter: BalancePickerItemsAdapter

    protected lateinit var compositeDisposable: CompositeDisposable

    protected var filter: String? = null
        set(value) {
            if (value != field) {
                field = value
                onFilterChanged()
            }
        }

    open fun show(onItemPicked: (BalancePickerListItem) -> Unit) {
        show(onItemPicked, null)
    }

    open fun show(onItemPicked: (BalancePickerListItem) -> Unit,
                  onDismiss: (() -> Unit)?) {
        compositeDisposable = CompositeDisposable()
        filter = null

        val dialogView =
                context.layoutInflater.inflate(R.layout.dialog_balance_picker, null)
        val dialog = BottomSheetDialog(context, R.style.RoundedBottomSheetDialog)

        initDialogView(dialog, dialogView, onItemPicked)
        dialog.setContentView(dialogView)

        subscribeToBalances(
                LoadingIndicatorManager(
                        showLoading = { dialogView.progress.show() },
                        hideLoading = { dialogView.progress.hide() }
                )
        )

        dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        dialog.setOnDismissListener {
            compositeDisposable.dispose()
            onDismiss?.invoke()
        }

        dialog.show()

        adjustDialogSize(dialog, dialogView)
    }

    protected open fun initDialogView(dialog: Dialog,
                                      dialogView: View,
                                      callback: (BalancePickerListItem) -> Unit) {
        initSearch(dialogView)
        initList(dialog, dialogView, callback)
    }

    protected open fun adjustDialogSize(dialog: Dialog,
                                        dialogView: View) {
        val displayMetrics = DisplayMetrics()
        dialog.window?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

        dialogView.minimumHeight = (displayHeight * 0.3).roundToInt()

        val maxWidth = dialog.context.resources.getDimensionPixelSize(R.dimen.max_content_width)
        if (displayWidth > maxWidth) {
            dialog.window?.setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    protected open fun initList(dialog: Dialog,
                                dialogView: View,
                                callback: (BalancePickerListItem) -> Unit) {
        adapter = BalancePickerItemsAdapter(amountFormatter)

        val balancesList = dialogView.balances_list
        balancesList.layoutManager = LinearLayoutManager(context)
        balancesList.adapter = adapter

        adapter.onItemClick { _, item ->
            callback(item)
            dialog.dismiss()
        }

        val errorEmptyView = dialogView.error_empty_view
        errorEmptyView
                .setPadding(
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.standard_padding),
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.standard_padding)
                )
        errorEmptyView.setEmptyDrawable(R.drawable.ic_balance)
        errorEmptyView.observeAdapter(adapter, R.string.no_balances_found)
        errorEmptyView.setEmptyViewDenial { balancesRepository.isNeverUpdated }

        ElevationUtil.initScrollElevation(balancesList, dialogView.appbar_elevation_view)
    }

    protected open fun initSearch(dialogView: View) {
        val searchEditText = dialogView.search_balance_edit_text
        val cancelSearchButton = dialogView.cancel_search_icon

        val getQuery = {
            searchEditText.text?.trim()?.toString()?.takeIf { it.isNotEmpty() }
        }

        searchEditText.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                val query = getQuery()
                if (query == null) {
                    filter = null
                }
                cancelSearchButton.visibility =
                        if (query == null)
                            View.GONE
                        else
                            View.VISIBLE
            }
        })

        RxTextView.textChanges(searchEditText)
                .skipInitialValue()
                .debounce(400, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    filter = getQuery()
                }
                .addTo(compositeDisposable)

        cancelSearchButton.setOnClickListener {
            searchEditText.text?.clear()
            SoftInputUtil.hideSoftInput(searchEditText)
        }
    }

    protected open fun subscribeToBalances(loadingIndicator: LoadingIndicatorManager) {
        balancesRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayBalances() }
                .addTo(compositeDisposable)

        balancesRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loadingIndicator.setLoading(it) }
                .addTo(compositeDisposable)
    }

    protected open fun displayBalances() {
        val filteredBalances = balancesRepository
                .itemsList
                .sortedWith(balancesComparator)
                .let { items ->
                    if (balancesFilter != null)
                        items.filter(balancesFilter)
                    else
                        items
                }

        val unfilteredItems =
                requiredAssets?.map { requiredAsset ->
                    val balance =
                            filteredBalances.find { it.assetCode == requiredAsset.code }

                    if (balance != null)
                        BalancePickerListItem(
                                source = balance,
                                available = getAvailableAmount(requiredAsset.code, balance)
                        )
                    else
                        BalancePickerListItem(
                                asset = requiredAsset,
                                available = getAvailableAmount(requiredAsset.code, null),
                                isEnough = true,
                                logoUrl = null,
                                source = null
                        )
                } ?: filteredBalances.map {
                    BalancePickerListItem(
                            source = it,
                            available = getAvailableAmount(it.assetCode, it)
                    )
                }


        val items = unfilteredItems
                .let { items ->
                    filter?.let { filter ->
                        items.filter { item ->
                            SearchUtil.isMatchGeneralCondition(filter,
                                    item.asset.code, item.asset.name)
                        }
                    } ?: items
                }

        adapter.setData(items)
    }

    private fun onFilterChanged() {
        displayBalances()
    }

    protected open fun getAvailableAmount(assetCode: String,
                                          balance: BalanceRecord?): BigDecimal {
        return balance?.available ?: BigDecimal.ZERO
    }
}