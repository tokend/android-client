package org.tokend.template.features.assets

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_explore.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.data.repository.assets.AssetsRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.assets.adapter.AssetListItem
import org.tokend.template.features.assets.adapter.AssetsAdapter
import org.tokend.template.features.assets.logic.CreateBalanceUseCase
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SearchUtil
import org.tokend.template.view.util.ColumnCalculator
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.MenuSearchViewManager
import org.tokend.template.view.util.ProgressDialogFactory

class ExploreAssetsFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private var searchItem: MenuItem? = null

    private val assetsRepository: AssetsRepository
        get() = repositoryProvider.assets()
    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val assetsAdapter = AssetsAdapter()
    private var filter: String? = null
        set(value) {
            if (value != field) {
                field = value
                onFilterChanged()
            }
        }

    private lateinit var layoutManager: GridLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.explore_assets_title)

        initSwipeRefresh()
        initAssetsList()
        initMenu()

        subscribeToAssets()
        subscribeToBalances()

        update()
    }

    // region Init
    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initAssetsList() {
        val columns = ColumnCalculator.getColumnCount(requireActivity())

        layoutManager = GridLayoutManager(context, columns)

        assets_recycler_view.layoutManager = layoutManager

        assets_recycler_view.adapter = assetsAdapter

        error_empty_view.setEmptyDrawable(R.drawable.ic_coins)
        error_empty_view.observeAdapter(assetsAdapter, R.string.no_assets_found)
        error_empty_view.setEmptyViewDenial { assetsRepository.isNeverUpdated || balancesRepository.isNeverUpdated }

        assetsAdapter.onItemClick { view, item ->
            if (view?.tag == "primary_action") {
                performPrimaryAssetAction(item)
            } else {
                openAssetDetails(view, item)
            }
        }
    }

    private fun initMenu() {
        toolbar.inflateMenu(R.menu.explore)
        val menu = toolbar.menu

        searchItem = menu?.findItem(R.id.search) ?: return
        val searchItem = menu.findItem(R.id.search) ?: return

        try {
            val searchManager = MenuSearchViewManager(searchItem, toolbar, compositeDisposable)

            searchManager.queryHint = getString(R.string.search)
            searchManager
                    .queryChanges
                    .compose(ObservableTransformers.defaultSchedulers())
                    .subscribe { newValue ->
                        filter = newValue.takeIf { it.isNotEmpty() }
                    }
                    .addTo(compositeDisposable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // endregion

    // region Assets
    private var assetsDisposable: CompositeDisposable? = null

    private fun subscribeToAssets() {
        assetsDisposable?.dispose()
        assetsDisposable = CompositeDisposable(
                Observable.zip(
                        assetsRepository.itemsSubject
                                .filter { assetsRepository.isFresh },
                        balancesRepository.itemsSubject
                                .filter { balancesRepository.isFresh },
                        BiFunction { _: Any, _: Any -> }
                )
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            displayAssets()
                        },
                assetsRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            loadingIndicator.setLoading(it, "assets")
                        },
                assetsRepository.errorsSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { error ->
                            if (!assetsAdapter.hasData) {
                                error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                                    update(true)
                                }
                            } else {
                                errorHandlerFactory.getDefault().handle(error)
                            }
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun displayAssets() {
        val balances = balancesRepository.itemsList
        val items = assetsRepository.itemsList
                .asSequence()
                .map { asset ->
                    AssetListItem(
                            asset,
                            balances.find { it.assetCode == asset.code } != null
                    )
                }
                .sortedWith(Comparator { o1, o2 ->
                    return@Comparator o1.balanceExists.compareTo(o2.balanceExists)
                            .takeIf { it != 0 }
                            ?: o1.code.compareTo(o2.code)
                })
                .toList()
                .let { items ->
                    filter?.let {
                        items.filter { item ->
                            SearchUtil.isMatchGeneralCondition(it, item.code, item.name)
                        }
                    } ?: items
                }

        assetsAdapter.setData(items)
    }

    private fun onFilterChanged() {
        displayAssets()
    }
    // endregion

    private var balancesDisposable: Disposable? = null
    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable =
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            loadingIndicator.setLoading(it, "balances")
                        }
                        .addTo(compositeDisposable)
    }

    private fun performPrimaryAssetAction(item: AssetListItem) {
        if (!item.balanceExists) {
            createBalanceWithConfirmation(item.code)
        } else {
            Navigator.openWallet(this, 0, item.code)
        }
    }

    private fun openAssetDetails(view: View?, item: AssetListItem) {
        Navigator.openAssetDetails(this, CREATE_REQUEST, item.source,
                cardView = view
        )
    }

    private fun createBalance(asset: String) {
        val progress = ProgressDialogFactory.getTunedDialog(context)

        CreateBalanceUseCase(
                asset,
                repositoryProvider.balances(),
                repositoryProvider.systemInfo(),
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
                            toastManager.short(
                                    getString(R.string.template_asset_balance_created, asset)
                            )
                            displayAssets()
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
    }

    private fun createBalanceWithConfirmation(asset: String) {
        AlertDialog.Builder(this.context!!, R.style.AlertDialogStyle)
                .setMessage(resources.getString(R.string.create_balance_confirmation, asset))
                .setPositiveButton(R.string.yes) { _, _ ->
                    createBalance(asset)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
            assetsRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
            assetsRepository.update()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST && resultCode == Activity.RESULT_OK) {
            update(true)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        assets_recycler_view.isLayoutFrozen = false
    }

    override fun onStop() {
        super.onStop()
        assets_recycler_view.isLayoutFrozen = true
    }

    override fun onBackPressed(): Boolean {
        return searchItem?.isActionViewExpanded == false.also {
            searchItem?.collapseActionView()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        layoutManager.spanCount = ColumnCalculator.getColumnCount(requireActivity())
    }

    companion object {
        const val ID = 1114L
        const val CREATE_REQUEST = 314
    }
}