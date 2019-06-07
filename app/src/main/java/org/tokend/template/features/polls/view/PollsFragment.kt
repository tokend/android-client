package org.tokend.template.features.polls.view

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.appbar.*
import kotlinx.android.synthetic.main.fragment_polls.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.polls.model.PollRecord
import org.tokend.template.features.polls.repository.PollsRepository
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import java.math.BigDecimal

class PollsFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val allowToolbar: Boolean by lazy {
        arguments?.getBoolean(ALLOW_TOOLBAR_EXTRA, true) ?: true
    }

    private val requiredOwnerAccountId: String? by lazy {
        arguments?.getString(ALLOW_TOOLBAR_EXTRA)
    }

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private var currentAsset: AssetRecord? = null
        set(value) {
            field = value
            onAssetChanged()
        }

    private val pollsRepository: PollsRepository?
        get() = currentAsset
                ?.ownerAccountId
                ?.let { repositoryProvider.polls(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_polls, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initAssetSelection()
        initSwipeRefresh()
    }

    // region Init
    private fun initToolbar() {
        if (allowToolbar) {
            toolbar.title = getString(R.string.polls_title)

            val dropDownButton = ImageButton(requireContext()).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_drop_down))
                background = null

                setOnClickListener {
                    openAssetPicker()
                }
            }

            toolbar.addView(dropDownButton)

            ElevationUtil.initScrollElevation(polls_list, appbar_elevation_view)
        } else {
            appbar_elevation_view.visibility = View.GONE
            appbar.visibility = View.GONE
        }
        toolbarSubject.onNext(toolbar)
    }

    private fun initAssetSelection() {
        val requiredOwnerAccountId = this.requiredOwnerAccountId

        val assets = balancesRepository
                .itemsList
                .map(BalanceRecord::asset)

        if (requiredOwnerAccountId != null) {
            assets
                    .find {
                        it.ownerAccountId == requiredOwnerAccountId
                    }
                    ?.also {
                        currentAsset = it
                    }
        } else {
            assets.firstOrNull().also { firstAsset ->
                if (firstAsset != null) {
                    currentAsset = firstAsset
                } else {
                    toolbar.subtitle = null
                    error_empty_view.showEmpty(R.string.no_assets_found)
                }
            }
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }

    }
    // endregion

    private fun openAssetPicker() {
        object : BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                balanceComparator,
                balancesRepository
        ) {
            // Available amounts are useless on this screen.
            override fun getAvailableAmount(assetCode: String,
                                            balance: BalanceRecord?): BigDecimal? = null
        }
                .show {
                    currentAsset = it.source?.asset
                }
    }

    private fun onAssetChanged() {
        toolbar.subtitle = getString(R.string.template_asset, currentAsset?.code.toString())
        subscribeToPolls()
        update()
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            pollsRepository?.updateIfNotFresh()
        } else {
            pollsRepository?.update()
        }
    }

    private var pollsDisposable: Disposable? = null
    private fun subscribeToPolls() {
        pollsDisposable?.dispose()

        val repository = pollsRepository
                ?: return

        pollsDisposable = CompositeDisposable(
                repository
                        .itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe(this::displayPolls),
                repository
                        .loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { loadingIndicator.setLoading(it, "polls") },
                repository
                        .errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            // TODO: Implement me
                            errorHandlerFactory.getDefault().handle(it)
                        }
        )
    }

    private fun displayPolls(polls: List<PollRecord>) {

    }

    companion object {
        val ID = "polls".hashCode().toLong() and 0xffff
        private const val ALLOW_TOOLBAR_EXTRA = "allow_toolbar"
        private const val OWNER_ACCOUNT_ID_EXTRA = "owner"

        fun newInstance(allowToolbar: Boolean,
                        ownerAccountId: String?): PollsFragment {
            val fragment = PollsFragment()
            fragment.arguments = Bundle().apply {
                putString(OWNER_ACCOUNT_ID_EXTRA, ownerAccountId)
                putBoolean(ALLOW_TOOLBAR_EXTRA, allowToolbar)
            }
            return fragment
        }
    }
}