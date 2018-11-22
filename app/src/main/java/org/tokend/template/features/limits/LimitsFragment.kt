package org.tokend.template.features.limits

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_limits.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.jetbrains.anko.childrenSequence
import org.tokend.template.R
import org.tokend.template.data.repository.LimitsRepository
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager

class LimitsFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val limitsRepository: LimitsRepository
        get() = repositoryProvider.limits()

    private val assets: Set<String>
        get() = limitsRepository.itemSubject.value.entriesMap.keys

    private val limitTypes: Array<String>
     get() = resources.getStringArray(R.array.limit_types)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_limits, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.limits)

        initSwipeRefresh()
        initTabs()
        subscribeLimits()
        update()
    }

    private fun initTabs() {
        asset_tabs.onItemSelected { asset ->
            updateCards(asset.text)
        }
    }

    private fun updateCards(asset: String) {
        limit_cards_holder.removeAllViews()

        limitsRepository.itemSubject.value.getAssetEntry(asset)?.let { entry ->
            LimitCard(requireContext(),
                    limitTypes[0],
                    entry.limit.daily,
                    entry.statistics.daily)
                    .addTo(limit_cards_holder)

            LimitCard(requireContext(),
                    limitTypes[1],
                    entry.limit.weekly,
                    entry.statistics.weekly)
                    .addTo(limit_cards_holder)

            LimitCard(requireContext(),
                    limitTypes[2],
                    entry.limit.monthly,
                    entry.statistics.monthly)
                    .addTo(limit_cards_holder)

            LimitCard(requireContext(),
                    limitTypes[3],
                    entry.limit.annual,
                    entry.statistics.annual)
                    .addTo(limit_cards_holder)

            addSpacesBetweenViews()
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun onLimitsUpdated() {
        asset_tabs.setSimpleItems(assets)
    }

    private fun addSpacesBetweenViews() {
        limit_cards_holder.childrenSequence().forEach { view ->
            view.layoutParams.also { params ->
                params as ViewGroup.MarginLayoutParams
                params.bottomMargin = this.resources.getDimensionPixelSize(R.dimen.half_standard_margin)
                view.layoutParams = params
            }
        }
    }

    private var limitsDisposable: CompositeDisposable? = null
    private fun subscribeLimits() {
        limitsDisposable?.dispose()
        limitsDisposable = CompositeDisposable(
                limitsRepository.itemSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            onLimitsUpdated()
                        },
                limitsRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { error ->
                            if (assets.isEmpty()) {
                                error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                                    update(true)
                                }
                            } else {
                                errorHandlerFactory.getDefault().handle(error)
                            }
                        },
                limitsRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { isLoading ->
                            loadingIndicator.setLoading(isLoading, "limits")
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            limitsRepository.updateIfNotFresh()
        } else {
            limitsRepository.update()
        }
    }

    companion object {
        const val ID = 1119L
        fun newInstance(): LimitsFragment {
            val fragment = LimitsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}