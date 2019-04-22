package org.tokend.template.features.deposit

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_deposit.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import org.jetbrains.anko.runOnUiThread
import org.tokend.template.R
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.repository.AccountRepository
import org.tokend.template.data.repository.assets.AssetsRepository
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.picker.PickerItem
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.template.view.util.formatter.DateFormatter
import java.lang.ref.WeakReference
import java.util.*

class DepositFragment : BaseFragment(), ToolbarProvider {

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()
    private lateinit var accountRepository: AccountRepository
    private lateinit var assetsRepository: AssetsRepository
    private lateinit var timer: Timer
    private lateinit var timerTask: TimerTask

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private var currentAsset: AssetRecord? = null
        set(value) {
            field = value
            onAssetChanged()
        }

    private val externalAccount: AccountRecord.DepositAccount?
        get() = accountRepository
                .item
                ?.depositAccounts
                ?.find { it.type == currentAsset?.externalSystemType }

    private val requestedAssetCode: String? by lazy {
        arguments?.getString(EXTRA_ASSET)
    }

    private var requestedAssetSet = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_deposit, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountRepository = repositoryProvider.account()
        assetsRepository = repositoryProvider.assets()
    }

    override fun onInitAllowed() {
        initToolbar()
        initSwipeRefresh()
        initButtons()
        initHorizontalSwipes()
        initEmptyView()

        subscribeToAccount()
        subscribeToAssets()

        update()
    }

    // region Subscribe
    private var accountDisposable: CompositeDisposable? = null

    private fun subscribeToAccount() {
        accountDisposable?.dispose()
        accountDisposable = CompositeDisposable(
                accountRepository.itemSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            displayAddress()
                        },
                accountRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            loadingIndicator.setLoading(it, "account")
                        },
                accountRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            errorHandlerFactory.getDefault().handle(it)
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private var assetsDisposable: CompositeDisposable? = null
    private fun subscribeToAssets() {
        assetsDisposable?.dispose()
        assetsDisposable = CompositeDisposable(
                assetsRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            initAssets(it)
                        },
                assetsRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            loadingIndicator.setLoading(it, "assets")
                        },
                assetsRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            if (assetsRepository.isNeverUpdated) {
                                error_empty_view.showError(it, errorHandlerFactory.getDefault()) {
                                    update()
                                }
                            } else {
                                errorHandlerFactory.getDefault().handle(it)
                            }
                        }
        ).also { it.addTo(compositeDisposable) }
    }
    // endregion

    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.deposit_title)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initButtons() {
        show_qr_text_view.onClick {
            openQr()
        }

        share_btn.onClick {
            shareData()
        }

        get_address_btn.onClick {
            bindExternalAccount()
        }

        renew_btn.onClick {
            bindExternalAccount(isRenewal = true)
        }
    }

    private fun initAssets(assets: List<AssetRecord>) {
        val depositableAssets = assets.filter { it.isBackedByExternalSystem }

        if (depositableAssets.isEmpty()) {
            deposit_content_layout.visibility = View.GONE
            asset_tab_layout.visibility = View.GONE

            if (!assetsRepository.isNeverUpdated) {
                error_empty_view.showEmpty(R.string.error_deposit_unavailable)
            }
            return
        } else {
            asset_tab_layout.visibility = View.VISIBLE
            deposit_content_layout.visibility = View.VISIBLE
            error_empty_view.hide()
        }

        asset_tab_layout.onItemSelected {
            (it.tag as? AssetRecord)?.let { currentAsset = it }
        }

        asset_tab_layout.setItems(
                depositableAssets.map {
                    PickerItem(it.code, it)
                },
                depositableAssets.indexOfFirst { it.code == currentAsset?.code }
        )

        if (!requestedAssetSet) {
            asset_tab_layout.selectedItemIndex =
                    depositableAssets.indexOfFirst { it.code == requestedAssetCode }
            requestedAssetSet = true
        }
    }

    private fun initHorizontalSwipes() {
        val weakTabs = WeakReference(asset_tab_layout)

        val gestureDetector = GestureDetectorCompat(requireContext(), HorizontalSwipesGestureDetector(
                onSwipeToLeft = {
                    weakTabs.get()?.apply { selectedItemIndex++ }
                },
                onSwipeToRight = {
                    weakTabs.get()?.apply { selectedItemIndex-- }
                }
        ))

        swipe_refresh.setTouchEventInterceptor { motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
            false
        }
    }

    private fun initEmptyView() {
        error_empty_view.setEmptyDrawable(R.drawable.ic_deposit)
    }

    // region Timer
    private fun initTask() {
        timerTask = object : TimerTask() {
            override fun run() {
                requireContext().runOnUiThread {
                    if (isVisible) {
                        displayAddress()
                    }
                }
            }
        }
    }

    private fun initTimer() {
        timer = Timer(false)
    }

    override fun onStart() {
        super.onStart()
        initTask()
        initTimer()
        timer.schedule(timerTask, 0L, 1000L)
    }

    override fun onStop() {
        super.onStop()
        timerTask.cancel()
        timer.cancel()
        timer.purge()
    }
    // endregion

    private fun update(force: Boolean = false) {
        if (!force) {
            accountRepository.updateIfNotFresh()
            assetsRepository.updateIfNotFresh()
        } else {
            accountRepository.update()
            assetsRepository.update()
        }
    }

    private fun onAssetChanged() {
        displayAddress()
    }

    // region Address display
    private fun displayAddress() {
        externalAccount?.address.let { address ->
            val expirationDate = externalAccount?.expirationDate
            val isExpired = expirationDate != null && expirationDate <= Date()
            if (address != null && !isExpired) {
                displayExistingAddress(address, expirationDate)
            } else {
                displayAddressEmptyView()
            }
        }
    }

    private fun displayExistingAddress(address: String, expirationDate: Date?) {
        deposit_address_layout.visibility = View.VISIBLE
        no_address_layout.visibility = View.GONE

        address_text_view.onLongClick {
            requireContext().clipboardManager.text = address
            toastManager.short(R.string.deposit_address_copied)

            true
        }

        address_text_view.text = address
        to_make_deposit_text_view.text = getString(R.string.to_make_deposit_send_asset,
                currentAsset?.code)

        if (expirationDate != null) {
            address_expiration_card.visibility = View.VISIBLE
            address_expiration_text_view.text =
                    DateFormatter(requireActivity())
                            .formatLong(expirationDate)
            updateExpirationDateColor(expirationDate)
        } else {
            address_expiration_card.visibility = View.GONE
        }
    }

    private fun displayAddressEmptyView() {
        deposit_address_layout.visibility = View.GONE
        address_expiration_card.visibility = View.GONE
        no_address_layout.visibility = View.VISIBLE

        if (accountRepository.isNeverUpdated) {
            no_address_text_view.text = getString(R.string.loading_data)
            get_address_btn.visibility = View.GONE
        } else {
            no_address_text_view.text = getString(R.string.template_no_personal_asset_address,
                    currentAsset?.code)
            get_address_btn.visibility = View.VISIBLE
        }
    }

    private fun updateExpirationDateColor(expirationDate: Date) {
        val rest = expirationDate.time - System.currentTimeMillis()
        val colorId = when {
            rest < CRITICAL_EXPIRATION_WARNING_THRESHOLD -> R.color.error
            rest < EXPIRATION_WARNING_THRESHOLD -> R.color.warning
            else -> R.color.primary_text
        }

        address_expiration_text_view.setTextColor(
                ContextCompat.getColor(requireContext(), colorId)
        )
    }

    private fun shareData() {
        getAddressShareMessage()?.let { shareMessage ->
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    getString(R.string.app_name))
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(sharingIntent,
                    getString(R.string.share_address_label)))
        }
    }

    private fun openQr() {
        Navigator.from(this).openQrShare(
                title =
                "${getString(R.string.deposit_title)} ${asset_tab_layout.selectedItem?.text}",
                data = address_text_view.text.toString(),
                shareLabel = getString(R.string.share_address_label),
                shareText = getAddressShareMessage())
    }

    private fun getAddressShareMessage(): String? {
        return externalAccount?.let { externalAccount ->
            externalAccount.expirationDate?.let { expirationDate ->
                getString(R.string.template_deposit_address_with_expiration,
                        currentAsset?.code, externalAccount.address,
                        DateFormatter(requireActivity()).formatCompact(expirationDate)
                )
            } ?: getString(R.string.template_deposit_address,
                    currentAsset?.code, externalAccount.address)
        }
    }
    // endregion

    private fun bindExternalAccount(isRenewal: Boolean = false) {
        val asset = currentAsset?.code
                ?: return
        val type = currentAsset?.externalSystemType
                ?: return

        val progress = ProgressDialogFactory.getTunedDialog(context)

        BindExternalAccountUseCase(
                asset,
                type,
                walletInfoProvider,
                repositoryProvider.systemInfo(),
                repositoryProvider.balances(),
                accountRepository,
                accountProvider,
                TxManager(apiProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    progress.show()
                }
                .doOnTerminate {
                    progress.dismiss()
                }
                .subscribeBy(
                        onComplete = {
                            if (isRenewal) {
                                toastManager.long(getString(R.string.deposit_address_renewed))
                            }
                        },
                        onError = {
                            if (it is BindExternalAccountUseCase.NoAvailableExternalAccountsException) {
                                displayEmptyPoolError()
                            } else {
                                errorHandlerFactory.getDefault().handle(it)
                            }
                        }
                )
    }

    private fun displayEmptyPoolError() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                .setTitle(R.string.no_addresses_in_pool_title)
                .setMessage(R.string.no_addresses_in_pool_explanation)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    companion object {
        const val ID = 1112L
        private const val EXPIRATION_WARNING_THRESHOLD = 6 * 60 * 60 * 1000L
        private const val CRITICAL_EXPIRATION_WARNING_THRESHOLD = 30 * 60 * 1000L
        private const val EXTRA_ASSET = "extra_asset"

        fun newInstance(asset: String?): DepositFragment {
            val fragment = DepositFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_ASSET, asset)
            fragment.arguments = bundle
            return fragment
        }
    }
}