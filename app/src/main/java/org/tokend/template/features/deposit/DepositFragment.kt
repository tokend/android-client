package org.tokend.template.features.deposit

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
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
import org.jetbrains.anko.onClick
import org.jetbrains.anko.runOnUiThread
import org.tokend.template.R
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.repository.AccountRepository
import org.tokend.template.data.repository.assets.AssetsRepository
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.picker.PickerItem
import org.tokend.template.view.util.CopyDataDialogFactory
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

    private val adapter = DetailsItemsAdapter()

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
        initList()
        initSwipeRefresh()
        initHorizontalSwipes()
        initEmptyView()
        initActions()

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

    private fun initList() {
        details_list.layoutManager = LinearLayoutManager(context)
        details_list.adapter = adapter
        (details_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initActions() {
        adapter.onItemClick { _, item ->
            when (item.id) {
                EXISTING_ADDRESS_ITEM_ID -> {
                    CopyDataDialogFactory.getDialog(
                            requireContext(),
                            item.text,
                            getString(R.string.personal_address),
                            toastManager,
                            getString(R.string.deposit_address_copied)
                    )
                }
                SHARE_ITEM__ID -> shareData()
                QR_ITEM_ID -> openQr()
                RENEW_ITEM_ID -> bindExternalAccount(true)
            }
        }

        get_address_button.onClick {
            bindExternalAccount()
        }
    }

    private fun initAssets(assets: List<AssetRecord>) {
        val depositableAssets = assets
                .filter { it.isBackedByExternalSystem }
                .sortedWith(kotlin.Comparator { o1, o2 ->
                    assetComparator.compare(o1.code, o2.code)
                })

        if (depositableAssets.isEmpty()) {
            asset_tab_layout.visibility = View.GONE
            details_list.visibility = View.GONE
            no_address_layout.visibility = View.GONE

            if (!assetsRepository.isNeverUpdated) {
                error_empty_view.showEmpty(R.string.error_deposit_unavailable)
            }
            return
        } else {
            asset_tab_layout.visibility = View.VISIBLE
            details_list.visibility = View.VISIBLE
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
            if (!assetsRepository.isNeverUpdated) {
                adapter.clearData()
                val expirationDate = externalAccount?.expirationDate
                val isExpired = expirationDate != null && expirationDate <= Date()
                if (address != null && !isExpired) {
                    displayExistingAddress(address, expirationDate)
                } else {
                    displayAddressEmptyView()
                }
            }
        }
    }

    private fun displayExistingAddress(address: String, expirationDate: Date?) {
        no_address_layout.visibility = View.GONE
        details_list.visibility = View.VISIBLE
        adapter.addOrUpdateItem(
                DetailsItem(
                        id = EXISTING_ADDRESS_ITEM_ID,
                        text = address,
                        singleLineText = true,
                        hint = getString(R.string.to_make_deposit_send_asset, currentAsset?.code),
                        header = getString(R.string.personal_address),
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_forward)
                )
        )

        val shareIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_share)
        shareIcon?.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.icons), PorterDuff.Mode.SRC_ATOP)

        adapter.addData(
                DetailsItem(
                        id = SHARE_ITEM__ID,
                        singleLineText = true,
                        text = getString(R.string.share),
                        icon = shareIcon
                )
        )

        adapter.addData(
                DetailsItem(
                        id = QR_ITEM_ID,
                        text = getString(R.string.show_qr_label),
                        singleLineText = true,
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_qr_code_scan)
                )
        )

        if (expirationDate != null) {
            updateExpirationDate(expirationDate)

            adapter.addData(
                    DetailsItem(
                            id = RENEW_ITEM_ID,
                            text = getString(R.string.renew_personal_address_action),
                            singleLineText = true,
                            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_renew)
                    )
            )
        }
    }

    private fun displayAddressEmptyView() {
        details_list.visibility = View.GONE
        no_address_layout.visibility = View.VISIBLE
        no_address_text_view.text = getString(
                R.string.template_no_personal_asset_address, currentAsset?.code
        )
    }

    private fun updateExpirationDate(expirationDate: Date) {
        val rest = expirationDate.time - System.currentTimeMillis()
        val colorId = when {
            rest < CRITICAL_EXPIRATION_WARNING_THRESHOLD -> R.color.error
            rest < EXPIRATION_WARNING_THRESHOLD -> R.color.warning
            else -> R.color.primary_text
        }

        adapter.addOrUpdateItem(
                DetailsItem(
                        id = EXPIRATION_ITEM_ID,
                        text = DateFormatter(requireActivity()).formatLong(expirationDate),
                        singleLineText = true,
                        textColor = ContextCompat.getColor(requireContext(), colorId),
                        header = getString(R.string.deposit_address_expiration_date),
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar)
                )
        )
    }

    private fun shareData() {
        getAddressShareMessage()?.let { shareMessage ->
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.app_name))
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(sharingIntent,
                    getString(R.string.share_address_label)))
        }
    }

    private fun openQr() {
        externalAccount?.address?.let { address ->
            Navigator.from(this).openQrShare(
                    title =
                    "${getString(R.string.deposit_title)} ${asset_tab_layout.selectedItem?.text}",
                    data = address,
                    shareLabel = getString(R.string.share_address_label),
                    shareText = getAddressShareMessage())
        }
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

        private val EXISTING_ADDRESS_ITEM_ID = "existing_address".hashCode().toLong()
        private val SHARE_ITEM__ID = "share".hashCode().toLong()
        private val QR_ITEM_ID = "qr".hashCode().toLong()
        private val EXPIRATION_ITEM_ID = "expiration_date".hashCode().toLong()
        private val RENEW_ITEM_ID = "renew".hashCode().toLong()

        fun newInstance(asset: String?): DepositFragment {
            val fragment = DepositFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_ASSET, asset)
            fragment.arguments = bundle
            return fragment
        }
    }
}