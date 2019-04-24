package org.tokend.template.features.amountscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.SingleSubject
import kotlinx.android.synthetic.main.fragment_amount_input.*
import org.tokend.template.R
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog
import org.tokend.template.view.util.input.AmountEditTextWrapper
import java.math.BigDecimal

open class AmountInputFragment : BaseFragment() {
    class NoAssetsToDisplayException : Exception("There are no assets to display, screen can't operate")

    protected lateinit var amountWrapper: AmountEditTextWrapper

    protected val resultSubject = SingleSubject.create<BigDecimal>()

    /**
     * Emits entered amount or error
     *
     * @see NoAssetsToDisplayException
     */
    val resultSingle: Single<BigDecimal> = resultSubject

    protected val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    protected open var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

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

        setError("Ты пидор")

        updateActionButtonAvailability()
    }

    // region Init
    protected open fun initFields() {
        amountWrapper = AmountEditTextWrapper(amount_edit_text)
        amount_edit_text.requestFocus()
    }

    protected open fun initButtons() {
        action_button.text = getActionButtonText()
        action_button.setOnClickListener { postResult() }
    }

    protected open fun initAssetSelection() {
        val picker = getBalancePicker()

        asset_code_text_view.setOnClickListener {
            picker.show { result ->
                asset = result.assetCode
            }
        }
    }

    protected open fun initTitle() {
        title_text_view.text = getTitleText()
    }

    protected open fun initExtraView() {
        extra_view_frame.removeAllViews()
        getExtraView()?.also { extra_view_frame.addView(it) }
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
    protected open fun displayBalance() {
        val balance = balancesRepository
                .itemsList
                .find { it.assetCode == asset }
                ?.available
                ?: return

        balance_text_view.text = getString(
                R.string.template_balance,
                amountFormatter.formatAssetAmount(balance, asset)
        )
    }

    protected open fun displayAssets() {
        val assetsToDisplay = getAssetsToDisplay()

        if (assetsToDisplay.isEmpty()) {
            postError(NoAssetsToDisplayException())
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

        updateActionButtonAvailability()
    }

    protected open fun updateActionButtonAvailability() {
        action_button.isEnabled = !hasError
    }
    // endregion

    // region Result
    protected open fun postResult() {
        resultSubject.onSuccess(amountWrapper.scaledAmount)
    }

    protected open fun postError(e: Throwable) {
        resultSubject.onError(e)
    }
    // endregion

    /**
     * @return [BalancePickerBottomDialog] with required filter
     */
    protected open fun getBalancePicker(): BalancePickerBottomDialog {
        return BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                assetComparator,
                balancesRepository
        ) { balance ->
            balance.asset.isTransferable
        }
    }

    /**
     * @return collection of asset codes to display in picker,
     * empty collection will trigger [NoAssetsToDisplayException] error
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
     * @return view displayed above the action button
     */
    protected open fun getExtraView(): View? {
        return null
    }

    companion object {
        const val ASSET_EXTRA = "asset"
    }
}