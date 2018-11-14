package org.tokend.template.features.invest

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.tokend.template.base.view.ErrorEmptyView
import org.tokend.template.base.view.PaginationRecyclerView
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.features.invest.adapter.SalesAdapter
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class SalesSubscriptionManager(private val list: PaginationRecyclerView,
                               private val adapter: SalesAdapter,
                               private val loadingIndicator: LoadingIndicatorManager,
                               private val errorEmptyView: ErrorEmptyView,
                               private val compositeDisposable: CompositeDisposable,
                               private val errorHandlerFactory: ErrorHandlerFactory,
                               private val onError: () -> Unit) {

    private var salesDisposable: CompositeDisposable? = null

    fun subscribeTo(repository: SalesRepository,
                    name: String? = null,
                    baseAsset: String? = null,
                    force: Boolean = false) {

        salesDisposable?.dispose()
        attachTo(repository)
        salesDisposable = CompositeDisposable(
                repository.forQuery(name, baseAsset).itemsSubject
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

        if(force) {
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