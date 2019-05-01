package org.tokend.template.features.amountscreen.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_amount_input.*
import org.tokend.template.R
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.amountscreen.model.AmountInputResult
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog
import org.tokend.template.view.util.input.AmountEditTextWrapper
import org.tokend.template.view.util.input.SoftInputUtil
import java.math.BigDecimal

open class AmountInputFragment : BaseFragment() {
    protected lateinit var amountWrapper: AmountEditTextWrapper

    protected open val resultSubject: Subject<in AmountInputResult> = PublishSubject.create<AmountInputResult>()

    /**
     * Emits entered amount as [AmountInputResult]
     */
    open val resultObservable: Observable<in AmountInputResult>
        get() = resultSubject

    protected val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    protected open var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    protected val balance: BigDecimal
        get() = balancesRepository
                .itemsList
                .find { it.assetCode == asset }
                ?.available
                ?: BigDecimal.ZERO

    protected open val requestedAsset: String? by lazy {
        arguments?.getString(ASSET_EXTRA)
    }

    protected var requestedAssetSet = false

    private var errorMessage: String? = null
    protected open val hasError: Boolean
        get() = errorMessage != null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_amount_input, container, false)
    }

    override fun onInitAllowed() {
        initTitle()
        initFields()
        initButtons()
        initAssetSelection()
        initExtraView()

        subscribeToBalances()

        updateActionButtonAvailability()
    }

    // region Init
    protected open fun initFields() {
        amountWrapper = AmountEditTextWrapper(amount_edit_text)
        amountWrapper.onAmountChanged { _, _ ->
            updateActionButtonAvailability()
        }
        amount_edit_text.requestFocus()
        SoftInputUtil.showSoftInputOnView(amount_edit_text)
    }

    protected open fun initButtons() {
        action_button.text = getActionButtonText()
        action_button.setOnClickListener { postResult() }
    }

    protected open fun initAssetSelection() {
        val picker = getBalancePicker()

        asset_code_text_view.setOnClickListener {
            SoftInputUtil.hideSoftInput(requireActivity())
            picker.show(
                    onItemPicked = { result ->
                        asset = result.assetCode
                    },
                    onDismiss = {
                        amount_edit_text.requestFocus()
                        amount_edit_text.postDelayed({
                            SoftInputUtil.showSoftInputOnView(amount_edit_text)
                        }, 100)
                    }
            )
        }

        if (asset.isNotEmpty()) {
            onAssetChanged()
        }
    }

    protected open fun initTitle() {
        title_text_view.text = getTitleText()
    }

    protected open fun initExtraView() {
        extra_view_frame.removeAllViews()
        getExtraView(extra_view_frame)?.also { extra_view_frame.addView(it) }
    }
    // endregion

    protected open fun subscribeToBalances() {
        balancesRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { onBalancesUpdated() }
                .addTo(compositeDisposable)
    }

    protected open fun onAssetChanged() {
        displayBalance()
        amountWrapper.maxPlacesAfterComa = amountFormatter.getDecimalDigitsCount(asset)
        asset_code_text_view.text = asset
    }

    protected open fun onBalancesUpdated() {
        displayAssets()
        displayBalance()
    }

    // region Display
    /**
     * @see [balance]
     */
    protected open fun displayBalance() {
        balance_text_view.text = getString(
                R.string.template_balance,
                amountFormatter.formatAssetAmount(balance, asset)
        )
    }

    protected open fun displayAssets() {
        val assetsToDisplay = getAssetsToDisplay()
        Log.i("Oleg", "${assetsToDisplay.size} $asset $requestedAssetSet")
        if (assetsToDisplay.isEmpty()) {
            return
        }

        if (!requestedAssetSet) {
            requestedAsset?.also { asset = it }
            requestedAssetSet = true
        }

        if (!assetsToDisplay.contains(asset)) {
            asset = assetsToDisplay.first()
        }
    }

    /**
     * Sets error message below the amount input
     *
     * @see hasError
     */
    protected open fun setError(message: String?) {
        errorMessage = message?.takeIf { it.isNotEmpty() }
        error_text_view.text = errorMessage
    }

    protected open fun updateActionButtonAvailability() {
        checkAmount()

        action_button.isEnabled = !hasError
                && amountWrapper.scaledAmount.signum() > 0
    }
    // endregion

    protected open fun checkAmount() {
        when {
            amountWrapper.scaledAmount > balance ->
                setError(getString(R.string.error_insufficient_balance))
            else ->
                setError(null)
        }
    }

    protected open fun postResult() {
        resultSubject.onNext(
                AmountInputResult(
                        amount = amountWrapper.scaledAmount,
                        assetCode = asset
                )
        )
    }

    /**
     * @return [BalancePickerBottomDialog] with required filter
     */
    protected open fun getBalancePicker(): BalancePickerBottomDialog {
        return BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                assetComparator,
                balancesRepository
        )
    }

    /**
     * @return collection of asset codes to display in picker
     */
    protected open fun getAssetsToDisplay(): Collection<String> {
        return balancesRepository
                .itemsList
                .map(BalanceRecord::assetCode)
                .sortedWith(assetComparator)
    }

    /**
     * @return text displayed on the top of the screen
     */
    protected open fun getTitleText(): String? {
        return getString(R.string.app_name)
    }

    /**
     * @return text displayed on the action button
     */
    protected open fun getActionButtonText(): String {
        return getString(R.string.continue_action)
    }

    /**
     * @param parent use for inflation but avoid automatic adding
     *
     * @return view displayed above the action button
     */
    protected open fun getExtraView(parent: ViewGroup): View? {
        return null
    }

    companion object {
        const val ASSET_EXTRA = "asset"
    }
}