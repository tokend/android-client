package org.tokend.template.features.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.send.amount.model.PaymentAmountAndDescription
import org.tokend.template.features.send.amount.view.PaymentAmountAndDescriptionFragment
import org.tokend.template.features.send.logic.CreatePaymentRequestUseCase
import org.tokend.template.features.send.model.PaymentRecipient
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.send.recipient.view.PaymentRecipientFragment
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.wallet.WalletEventsListener
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ProgressDialogFactory
import java.math.BigDecimal

class SendFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val requiredAsset: String?
        get() = arguments?.getString(ASSET_EXTRA)

    private var recipient: PaymentRecipient? = null
    private var amount: BigDecimal = BigDecimal.ZERO
    private var asset: String = ""
    private var description: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.send_title)

        initSwipeRefresh()

        subscribeToBalances()

        balancesRepository.updateIfNotFresh()

        toRecipientScreen()
    }

    // region Init
    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { balancesRepository.update() }
    }
    // endregion

    private var balancesDisposable: CompositeDisposable? = null

    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable = CompositeDisposable(
                balancesRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            onBalancesUpdated()
                        },
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { loadingIndicator.setLoading(it, "balances") }
        ).also { it.addTo(compositeDisposable) }
    }


    private fun onBalancesUpdated() {
    }

    private fun toRecipientScreen() {
        val fragment = PaymentRecipientFragment()

        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onRecipientSelected,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)

        displayFragment(fragment, "recipient", null)
    }

    private fun onRecipientSelected(recipient: PaymentRecipient) {
        this.recipient = recipient
        toAmountScreen()
    }

    private fun toAmountScreen() {
        val recipient = recipient?.displayedValue!!

        val fragment = PaymentAmountAndDescriptionFragment.newInstance(recipient, requiredAsset)

        fragment
                .resultObservable
                .map { it as PaymentAmountAndDescription }
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onAmountEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )

        displayFragment(fragment, "amount", true)
    }

    private fun onAmountEntered(result: PaymentAmountAndDescription) {
        this.amount = result.amount
        this.asset = result.assetCode
        this.description = result.description

        createAndConfirmPaymentRequest()
    }

    private var paymentRequestDisposable: Disposable? = null
    private fun createAndConfirmPaymentRequest() {
        val recipient = recipient ?: return

        val progress = ProgressDialogFactory.getTunedDialog(requireContext()).apply {
            setCanceledOnTouchOutside(true)
            setOnCancelListener {
                paymentRequestDisposable?.dispose()
            }
            setMessage(getString(R.string.loading_data))
        }

        paymentRequestDisposable?.dispose()
        paymentRequestDisposable = CreatePaymentRequestUseCase(
                recipient,
                amount,
                asset,
                description,
                walletInfoProvider,
                FeeManager(apiProvider),
                balancesRepository
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    progress.show()
                }
                .doOnEvent { _, _ ->
                    progress.hide()
                }
                .subscribeBy(
                        onSuccess = this::onPaymentRequestCreated,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
    }

    private fun onPaymentRequestCreated(request: PaymentRequest) {
        Navigator
                .from(this)
                .openPaymentConfirmation(
                        PAYMENT_CONFIRMATION_REQUEST,
                        request
                )
    }

    private fun displayFragment(
            fragment: Fragment,
            tag: String,
            forward: Boolean?
    ) {
        childFragmentManager.beginTransaction()
                .setTransition(
                        when (forward) {
                            true -> FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                            false -> FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
                            null -> FragmentTransaction.TRANSIT_NONE
                        }
                )
                .replace(R.id.fragment_container_layout, fragment)
                .addToBackStack(tag)
                .commit()
    }

    override fun onBackPressed(): Boolean {
        return if (childFragmentManager.backStackEntryCount == 1) {
            true
        } else {
            childFragmentManager.popBackStackImmediate()
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PAYMENT_CONFIRMATION_REQUEST -> {
                    val confirmedRequest =
                            data?.getSerializableExtra(
                                    PaymentConfirmationActivity.PAYMENT_REQUEST_EXTRA
                            ) as? PaymentRequest
                    if (confirmedRequest != null) {
                        (activity as? WalletEventsListener)
                                ?.onPaymentRequestConfirmed(confirmedRequest)
                    }
                }
            }
        }
    }

    companion object {
        private const val ASSET_EXTRA = "asset"
        const val ID = 1118L
        val PAYMENT_CONFIRMATION_REQUEST = "confirm_payment".hashCode() and 0xffff

        fun newInstance(asset: String? = null): SendFragment {
            val fragment = SendFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, asset)
            }
            return fragment
        }
    }
}