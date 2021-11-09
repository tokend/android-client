package io.tokend.template.features.amountscreen.view

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.tokend.template.R
import io.tokend.template.features.amountscreen.model.AmountInputResult
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.balances.model.BalanceRecord
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.fragments.BaseFragment
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.view.balancepicker.BalancePickerBottomDialog
import io.tokend.template.view.balancepicker.adapter.BalancePickerListItem
import io.tokend.template.view.util.input.AmountEditTextWrapper
import io.tokend.template.view.util.input.SoftInputUtil
import kotlinx.android.synthetic.main.fragment_amount_input.*
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

open class AmountInputFragment : BaseFragment() {
    protected lateinit var amountWrapper: AmountEditTextWrapper

    protected open val resultSubject: Subject<AmountInputResult> = PublishSubject.create()

    /**
     * Emits entered amount as [AmountInputResult]
     */
    open val resultObservable: Observable<AmountInputResult>
        get() = resultSubject

    protected val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances

    protected var mPickedItem: BalancePickerListItem? = null
        set(value) {
            field = value
            onPickedItemChanged()
        }
    protected val asset: Asset
        get() = mPickedItem!!.asset

    protected var balance: BalanceRecord? = null

    protected open val requiredAssetCode: String? by lazy {
        arguments?.getString(REQUIRED_ASSET_CODE_EXTRA)
    }

    protected open val requiredBalanceId: String? by lazy {
        arguments?.getString(REQUIRED_BALANCE_ID_EXTRA)
    }

    private var requiredItemSet = false

    private var errorMessage: String? = null
    protected open val hasError: Boolean
        get() = errorMessage != null

    private lateinit var balancePicker: BalancePickerBottomDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_amount_input, container, false)
    }

    override fun onInitAllowed() {
        initLayout()
        initBalancePicker()
        initTitle()
        initFields()
        initButtons()
        initPickedItem()
        initSelection()
        initExtraView()
        initAdaptiveTextSize()

        subscribeToBalances()

        updateActionButtonAvailability()
    }

    // region Init
    protected open fun initLayout() {
        root_layout.minHeight = getMinLayoutHeight() + action_button.height
    }

    protected open fun initFields() {
        amountWrapper = AmountEditTextWrapper(amount_edit_text)
        amountWrapper.onAmountChanged { _, _ ->
            updateActionButtonAvailability()
        }
        amount_edit_text.requestFocus()
        SoftInputUtil.showSoftInputOnView(amount_edit_text)
    }

    /**
     * Sets up input field and asset code text size change depending
     * on [getSmallSizingHeightThreshold]
     */
    protected open fun initAdaptiveTextSize() {
        val minHeight = getSmallSizingHeightThreshold()

        var isSmallSize: Boolean? = null

        val heightChanges = PublishSubject.create<Int>()

        container.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            val oldHeight = oldBottom - oldTop
            val height = bottom - top
            if (height != oldHeight) {
                val rootLayoutHeight = height - action_button.height -
                        if (title_text_view.visibility == View.VISIBLE)
                            title_text_view.height
                        else
                            0
                heightChanges.onNext(rootLayoutHeight)
            }
        }

        heightChanges
            .debounce(100, TimeUnit.MILLISECONDS)
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { height ->
                val useSmallSize = height <= minHeight

                if (useSmallSize == isSmallSize) {
                    return@subscribe
                }

                isSmallSize = useSmallSize

                updateSizing(useSmallSize)
            }
            .addTo(compositeDisposable)
    }

    protected open fun updateSizing(useSmallSize: Boolean) {
        val resources = requireContext().resources

        val defaultInputSize = resources.getDimensionPixelSize(R.dimen.text_size_amount_input)
            .toFloat()
        val defaultAssetCodeSize = resources.getDimensionPixelSize(R.dimen.text_size_heading)
            .toFloat()
        val defaultTitleMargin = resources.getDimensionPixelSize(R.dimen.standard_margin)

        val smallInputSize = resources.getDimensionPixelSize(R.dimen.text_size_amount_input_small)
            .toFloat()
        val smallAssetCodeSize = resources.getDimensionPixelSize(R.dimen.text_size_default)
            .toFloat()
        val smallTitleMargin = 0

        val inputSize = if (useSmallSize) smallInputSize else defaultInputSize
        val assetCodeSize = if (useSmallSize) smallAssetCodeSize else defaultAssetCodeSize
        val titleMargin = if (useSmallSize) smallTitleMargin else defaultTitleMargin

        amount_edit_text.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            inputSize
        )
        asset_code_text_view.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            assetCodeSize
        )
        title_text_view.layoutParams =
            (title_text_view.layoutParams as ViewGroup.MarginLayoutParams)
                .apply {
                    setMargins(
                        defaultTitleMargin, titleMargin,
                        defaultTitleMargin, titleMargin
                    )
                }

        asset_code_text_view.maxWidth = (root_layout.width * 0.75).roundToInt()
    }

    protected open fun initButtons() {
        action_button.text = getActionButtonText()
        action_button.setOnClickListener { postResult() }
    }

    private fun initBalancePicker() {
        balancePicker = getBalancePicker()
    }

    protected open fun initPickedItem() {
        val itemsToDisplay = balancePicker
            .getItemsToDisplay()

        if (itemsToDisplay.isEmpty()) {
            return
        }

        var requiredItemFound = true
        if (!requiredItemSet) {
            requiredItemFound = itemsToDisplay
                .find {
                    it.asset.code == requiredAssetCode || it.source?.id == requiredBalanceId
                }
                ?.also { mPickedItem = it } != null
            requiredItemSet = true
        }

        if (!requiredItemFound) {
            mPickedItem = itemsToDisplay.first()
        }
    }

    protected open fun initSelection() {
        asset_code_text_view.setOnClickListener {
            SoftInputUtil.hideSoftInput(requireActivity())
            balancePicker.show(
                onItemPicked = { mPickedItem = it },
                onDismiss = {
                    amount_edit_text.requestFocus()
                    amount_edit_text.postDelayed({
                        SoftInputUtil.showSoftInputOnView(amount_edit_text)
                    }, 100)
                }
            )
        }

        if (mPickedItem != null) {
            onPickedItemChanged()
        }
    }

    protected open fun initTitle() {
        title_text_view.text = getTitleText()
    }

    protected open fun initExtraView() {
        extra_view_frame.removeAllViews()
        getExtraView(extra_view_frame)?.also { extra_view_frame.addView(it) }

        extra_amount_view_frame.removeAllViews()
        getExtraAmountView(extra_amount_view_frame)?.also { extra_amount_view_frame.addView(it) }
    }
    // endregion

    protected open fun subscribeToBalances() {
        balancesRepository
            .itemsSubject
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { onBalancesUpdated() }
            .addTo(compositeDisposable)
    }

    protected open fun onPickedItemChanged() {
        balance = mPickedItem?.source
            ?: balancesRepository.itemsList.find { it.assetCode == asset.code }
        displayBalance()
        amountWrapper.maxPlacesAfterComa = asset.trailingDigits
        asset_code_text_view.text = asset.code
    }

    protected open fun onBalancesUpdated() {
        val currentBalance = balance
        balance =
            if (currentBalance != null)
                balancesRepository.itemsList.find { it.id == currentBalance.id }
            else
                balancesRepository.itemsList.find { it.assetCode == asset.code }

        displayBalance()
        updateActionButtonAvailability()
    }

    // region Display
    /**
     * @see [balance]
     */
    protected open fun displayBalance() {
        val available = balance?.available ?: BigDecimal.ZERO
        val asset = balance?.asset ?: return
        balance_text_view.text = getString(
            R.string.template_balance,
            amountFormatter.formatAssetAmount(available, asset)
        )
    }

    /**
     * Sets error message below the amount input
     *
     * @see hasError
     */
    protected open fun setError(message: String?) {
        errorMessage = message?.takeIf { it.isNotEmpty() }
        error_text_view.text = errorMessage
        error_text_view.visibility =
            if (errorMessage != null)
                View.VISIBLE
            else
                View.GONE
    }

    protected open fun updateActionButtonAvailability() {
        checkAmount()

        action_button.isEnabled = !hasError
                && amountWrapper.scaledAmount.signum() > 0
    }
    // endregion

    protected open fun checkAmount() {
        when {
            !isEnoughBalance() ->
                setError(getString(R.string.error_insufficient_balance))
            else ->
                setError(null)
        }
    }

    protected open fun isEnoughBalance(): Boolean {
        return amountWrapper.scaledAmount <= (balance?.available ?: BigDecimal.ZERO)
    }

    protected open fun postResult() {
        resultSubject.onNext(
            AmountInputResult(
                amount = amountWrapper.scaledAmount,
                asset = asset,
                balance = balance
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
            balanceComparator,
            balancesRepository
        )
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

    /**
     * @param parent use for inflation but avoid automatic adding
     *
     * @return view displayed below the amount input
     */
    protected open fun getExtraAmountView(parent: ViewGroup): View? {
        return null
    }

    /**
     * @return minimal allowed height of the layout before scrolling appears
     * in px
     */
    protected open fun getMinLayoutHeight(): Int {
        return requireContext().resources.getDimensionPixelSize(R.dimen.amount_input_min_height)
    }

    /**
     * @return minimal height of the layout before switching to small sizing
     */
    protected open fun getSmallSizingHeightThreshold(): Int {
        return requireContext().resources.getDimensionPixelSize(R.dimen.amount_input_height_threshold)
    }

    companion object {
        private const val REQUIRED_ASSET_CODE_EXTRA = "asset"
        private const val REQUIRED_BALANCE_ID_EXTRA = "balance_id"

        fun getBundle(
            requiredAssetCode: String? = null,
            requiredBalanceId: String? = null
        ) = Bundle().apply {
            putString(REQUIRED_ASSET_CODE_EXTRA, requiredAssetCode)
            putString(REQUIRED_BALANCE_ID_EXTRA, requiredBalanceId)
        }
    }
}