package io.tokend.template.features.invest.logic

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.tokend.template.features.invest.repository.SalesRepository
import io.tokend.template.features.invest.view.adapter.SalesAdapter
import io.tokend.template.util.errorhandler.ErrorHandlerFactory
import io.tokend.template.view.ErrorEmptyView
import io.tokend.template.view.PaginationRecyclerView
import io.tokend.template.view.util.LoadingIndicatorManager

class SalesSubscriptionManager(
    private val list: PaginationRecyclerView,
    private val adapter: SalesAdapter,
    private val loadingIndicator: LoadingIndicatorManager,
    private val errorEmptyView: ErrorEmptyView,
    private val compositeDisposable: CompositeDisposable,
    private val errorHandlerFactory: ErrorHandlerFactory,
    private val onError: () -> Unit
) {

    private var salesDisposable: CompositeDisposable? = null

    fun subscribeTo(
        repository: SalesRepository,
        baseAsset: String? = null,
        force: Boolean = false
    ) {

        salesDisposable?.dispose()
        attachTo(repository)
        salesDisposable = CompositeDisposable(
            repository.forQuery(baseAsset).itemsSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter.setData(it)
                },
            repository.loadingSubject
                .observeOn(AndroidSchedulers.mainThread())
                .doOnDispose {
                    loadingIndicator.setLoading(false)
                    adapter.hideLoadingFooter()
                }
                .subscribe { loading ->
                    if (loading) {
                        if (repository.isOnFirstPage) {
                            loadingIndicator.setLoading(true)
                        } else {
                            adapter.showLoadingFooter()
                        }
                    } else {
                        loadingIndicator.setLoading(false)
                        adapter.hideLoadingFooter()
                    }
                },
            repository.errorsSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { handleSalesError(it) }

        ).also { it.addTo(compositeDisposable) }

        if (force) {
            repository.update()
        } else {
            repository.updateIfNotFresh()
        }
    }

    private fun attachTo(repository: SalesRepository) {
        errorEmptyView.setEmptyViewDenial { repository.isNeverUpdated }
        list.listenBottomReach({ adapter.getDataItemCount() }) {
            repository.loadMore() || repository.noMoreItems
        }
    }

    private fun handleSalesError(error: Throwable) {
        if (!adapter.hasData) {
            errorEmptyView.showError(error, errorHandlerFactory.getDefault()) {
                onError.invoke()
            }
        } else {
            errorHandlerFactory.getDefault().handle(error)
        }
    }
}