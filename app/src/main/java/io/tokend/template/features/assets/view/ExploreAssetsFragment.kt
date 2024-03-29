package io.tokend.template.features.assets.view

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.tokend.template.R
import io.tokend.template.features.assets.adapter.AssetListItem
import io.tokend.template.features.assets.adapter.AssetsAdapter
import io.tokend.template.features.assets.logic.CreateBalanceUseCase
import io.tokend.template.features.assets.model.AssetRecord
import io.tokend.template.features.assets.storage.AssetsRepository
import io.tokend.template.features.balances.model.BalanceRecord
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.fragments.BaseFragment
import io.tokend.template.fragments.ToolbarProvider
import io.tokend.template.logic.TxManager
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.SearchUtil
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.util.*
import kotlinx.android.synthetic.main.fragment_explore.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.concurrent.TimeUnit

class ExploreAssetsFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create()

    private val loadingIndicator = LoadingIndicatorManager(
        showLoading = { swipe_refresh.isRefreshing = true },
        hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private var searchItem: MenuItem? = null

    private val assetsRepository: AssetsRepository
        get() = repositoryProvider.assets
    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances

    private val assetsAdapter = AssetsAdapter()
    private var filter: String? = null
        set(value) {
            if (value != field) {
                field = value
                onFilterChanged()
            }
        }

    private lateinit var layoutManager: GridLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
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

        assetsAdapter.registerAdapterDataObserver(
            ScrollOnTopItemUpdateAdapterObserver(assets_recycler_view)
        )

        ElevationUtil.initScrollElevation(assets_recycler_view, appbar_elevation_view)
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
            Observable.merge(
                assetsRepository.itemsSubject,
                balancesRepository.itemsSubject
            )
                .debounce(25, TimeUnit.MILLISECONDS)
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
                .debounce(20, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
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
        val balancesMap = balancesRepository
            .itemsList
            .associateBy(BalanceRecord::assetCode)

        val items = assetsRepository.itemsList
            .asSequence()
            .filter(AssetRecord::isActive)
            .map { asset ->
                AssetListItem(
                    asset,
                    balancesMap[asset.code]?.id
                )
            }
            .sortedWith(Comparator { o1, o2 ->
                return@Comparator o1.balanceExists.compareTo(o2.balanceExists)
                    .takeIf { it != 0 }
                    ?: assetCodeComparator.compare(o1.code, o2.code)
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
        } else if (item.balanceId != null) {
            Navigator.from(this).openBalanceDetails(item.balanceId)
        }
    }

    private fun openAssetDetails(view: View?, item: AssetListItem) {
        Navigator.from(this)
            .openAssetDetails(item.source, cardView = view)
            .addTo(activityRequestsBag)
            .doOnSuccess { update(force = true) }
    }

    private fun createBalance(asset: String) {
        val progress = ProgressDialogFactory.getDialog(requireContext())

        CreateBalanceUseCase(
            asset,
            repositoryProvider.balances,
            repositoryProvider.systemInfo,
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
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
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

    override fun onStart() {
        super.onStart()
        assets_recycler_view.suppressLayout(false)
    }

    override fun onStop() {
        super.onStop()
        assets_recycler_view.suppressLayout(true)
    }

    override fun onBackPressed(): Boolean {
        return searchItem?.isActionViewExpanded == false.also {
            searchItem?.collapseActionView()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        layoutManager.spanCount = ColumnCalculator.getColumnCount(requireActivity())
    }

    companion object {
        val ID = "explore-assets".hashCode().toLong()

        fun newInstance() = ExploreAssetsFragment()
    }
}