package org.tokend.template.view.balancepicker

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.SingleSubject
import kotlinx.android.synthetic.main.dialog_balance_picker.view.*
import kotlinx.android.synthetic.main.include_appbar_elevation.view.*
import kotlinx.android.synthetic.main.include_error_empty_view.view.*
import kotlinx.android.synthetic.main.layout_progress.view.*
import org.jetbrains.anko.layoutInflater
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SearchUtil
import org.tokend.template.view.balancepicker.adapter.BalancePickerItemsAdapter
import org.tokend.template.view.balancepicker.adapter.BalancePickerListItem
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Modal bottom sheet with balances list and search,
 * allows user to pick a balance
 *
 * @see show
 * @see resultSingle
 */
class BalancePickerBottomDialogFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var amountFormatter: AmountFormatter
    @Inject
    lateinit var repositoryProvider: RepositoryProvider
    @Inject
    lateinit var assetComparator: Comparator<String>

    private lateinit var compositeDisposable: CompositeDisposable

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private lateinit var dialogView: View
    private lateinit var adapter: BalancePickerItemsAdapter

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { dialogView.progress.show() },
            hideLoading = { dialogView.progress.hide() }
    )

    private val comparator = Comparator<BalancePickerListItem> { first, second ->
        assetComparator.compare(first.assetCode, second.assetCode)
    }

    private var filter: String? = null
        set(value) {
            if (value != field) {
                field = value
                onFilterChanged()
            }
        }

    private val resultSubject = SingleSubject.create<BalanceRecord>()

    val resultSingle: Single<BalanceRecord> = resultSubject

    override fun getTheme() = R.style.RoundedBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as? App)?.stateComponent?.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        compositeDisposable = CompositeDisposable()

        dialogView = requireContext().layoutInflater
                .inflate(R.layout.dialog_balance_picker, null, false)

        initDialogView()
        subscribeToBalances()

        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(true)

        dialog.setOnDismissListener {
            compositeDisposable.dispose()
        }

        dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        return dialog
    }

    private fun initDialogView() {
        initHeight()
        initSearch()
        initList()
    }

    private fun initHeight() {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val displayHeight = displayMetrics.heightPixels

        dialogView.minimumHeight = (displayHeight * 0.4).roundToInt()
    }

    private fun initList() {
        adapter = BalancePickerItemsAdapter(amountFormatter)

        val balancesList = dialogView.balances_list
        balancesList.layoutManager = LinearLayoutManager(requireContext())
        balancesList.adapter = adapter

        adapter.onItemClick { _, item ->
            item.source?.also { record ->
                resultSubject.onSuccess(record)
                dismiss()
            }
        }

        val errorEmptyView = dialogView.error_empty_view
        errorEmptyView
                .setPadding(
                        0,
                        requireContext().resources.getDimensionPixelSize(R.dimen.double_padding),
                        0, 0
                )
        errorEmptyView.setEmptyDrawable(R.drawable.ic_balance)
        errorEmptyView.observeAdapter(adapter, R.string.no_balances_found)
        errorEmptyView.setEmptyViewDenial { balancesRepository.isNeverUpdated }

        ElevationUtil.initScrollElevation(balancesList, dialogView.appbar_elevation_view)
    }

    private fun initSearch() {
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
            searchEditText.text.clear()
            SoftInputUtil.hideSoftInput(searchEditText)
        }
    }

    private fun subscribeToBalances() {
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

    private fun displayBalances() {
        val balances = balancesRepository.itemsList
        val items = balances
                .let { items ->
                    filter?.let { filter ->
                        items.filter { item ->
                            SearchUtil.isMatchGeneralCondition(filter, item.assetCode, item.asset.name)
                        }
                    } ?: items
                }
                .map {
                    BalancePickerListItem(it)
                }
                .sortedWith(comparator)

        adapter.setData(items)
    }

    private fun onFilterChanged() {
        displayBalances()
    }
}