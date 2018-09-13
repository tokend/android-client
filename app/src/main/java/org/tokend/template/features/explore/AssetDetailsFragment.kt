package org.tokend.template.features.explore

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.list_item_asset.*
import kotlinx.android.synthetic.main.list_item_asset.view.*
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.extensions.Asset
import org.tokend.template.features.explore.adapter.AssetListItem
import org.tokend.template.features.explore.adapter.AssetListItemViewHolder
import org.tokend.template.util.FileDownloader
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory


class AssetDetailsFragment : BaseFragment() {
    private lateinit var asset: Asset

    private val assetCode: String? by lazy {
        arguments?.getString(ASSET_CODE_EXTRA)
    }

    private val isBalanceCreationEnabled: Boolean by lazy {
        arguments?.getBoolean(BALANCE_CREATION_EXTRA) ?: true
    }

    private val balanceExists: Boolean
        get() = repositoryProvider.balances().itemsSubject.value
                .find { it.asset == asset.code } != null

    private lateinit var fileDownloader: FileDownloader

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_asset_details, container, false)
        val card = view.asset_card
        card.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                card.viewTreeObserver.removeOnPreDrawListener(this)
                activity!!.supportStartPostponedEnterTransition()
                return true
            }
        })
        fileDownloader = FileDownloader(activity!!, urlConfigProvider.getConfig().storage)
        return view
    }

    override fun onInitAllowed() {
        getAsset()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe { loadingIndicator.show() }
                .doOnEvent { _, _ -> loadingIndicator.hide() }
                .subscribeBy(
                        onSuccess = {
                            asset = it
                            displayDetails()
                            initButtons()
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun getAsset(): Single<Asset> {
        return (arguments?.getSerializable(ASSET_EXTRA) as? Asset)
                .toMaybe()
                .switchIfEmpty(
                        assetCode?.let { repositoryProvider.assets().getSingle(it) }
                                ?: Single.error(IllegalStateException("Unable to obtain asset"))
                )
    }

    // region Display
    private fun displayDetails() {
        displayLogoAndName()
        displaySummary()
        displayTermsIfNeeded()
    }

    private fun displayLogoAndName() {
        AssetListItemViewHolder(asset_card).bind(
                AssetListItem(asset, balanceExists, urlConfigProvider.getConfig().storage)
        )
    }

    private fun displaySummary() {
        InfoCard(cards_layout)
                .setHeading(R.string.asset_summary_title, null)
                .addRow(R.string.asset_available,
                        AmountFormatter.formatAssetAmount(asset.available))
                .addRow(R.string.asset_issued, AmountFormatter.formatAssetAmount(asset.issued))
                .addRow(R.string.asset_maximum, AmountFormatter.formatAssetAmount(asset.maximum))
    }

    private fun displayTermsIfNeeded() {
        val terms = asset.details?.terms.takeIf { !it?.name.isNullOrEmpty() }
                ?: return

        val fileCardView =
                layoutInflater.inflate(R.layout.list_item_remote_file,
                        null, false)

        val fileLayout = fileCardView?.find<View>(R.id.file_content_layout) ?: return
        (fileCardView as? CardView)?.removeView(fileLayout)

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        fileLayout.layoutParams = layoutParams

        fileLayout.onClick {
            fileDownloader.download(this, terms)
        }

        val fileNameTextView = fileLayout.find<TextView>(R.id.file_name_text_view)
        fileNameTextView.text = terms.name

        val fileIconImageView = fileLayout.find<ImageView>(R.id.file_icon_image_view)
        fileIconImageView.setImageDrawable(
                ContextCompat.getDrawable(this.requireContext(), R.drawable.ic_file)
        )

        InfoCard(cards_layout)
                .setHeading(R.string.asset_terms_of_use, null)
                .addView(fileLayout)
    }
    // endregion

    private fun initButtons() {
        asset_details_button.visibility = View.GONE

        if (!balanceExists && isBalanceCreationEnabled) {
            asset_primary_action_button.visibility = View.VISIBLE
            asset_primary_action_button.text = getString(R.string.create_balance_action)
            asset_primary_action_button.onClick {
                createBalanceWithConfirmation()
            }
        } else {
            asset_primary_action_button.visibility = View.GONE
            asset_card_divider.visibility = View.GONE
        }
    }

    private fun createBalanceWithConfirmation() {
        AlertDialog.Builder(this.requireContext(), R.style.AlertDialogStyle)
                .setMessage(resources.getString(R.string.create_balance_confirmation, asset.code))
                .setPositiveButton(R.string.yes) { _, _ ->
                    createBalance()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun createBalance() {
        val progress = ProgressDialog(this.requireContext())
        progress.isIndeterminate = true
        progress.setMessage(getString(R.string.processing_progress))
        progress.setCancelable(false)

        repositoryProvider.balances()
                .create(accountProvider, repositoryProvider.systemInfo(),
                        TxManager(apiProvider), asset.code)
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    progress.show()
                }
                .doOnTerminate {
                    progress.dismiss()
                }
                .subscribeBy(
                        onComplete = {
                            onBalanceCreated()
                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )

    }

    private fun onBalanceCreated() {
        ToastManager(requireContext()).short(getString(R.string.template_asset_balance_created,
                asset.code))

        activity?.setResult(Activity.RESULT_OK)
        displayLogoAndName()
        initButtons()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fileDownloader.handlePermissionResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val ASSET_EXTRA = "asset"
        private const val ASSET_CODE_EXTRA = "asset_code"
        private const val BALANCE_CREATION_EXTRA = "balance_creation"

        fun newInstance(asset: Asset, balanceCreation: Boolean): AssetDetailsFragment {
            val fragment = AssetDetailsFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(ASSET_EXTRA, asset)
                putBoolean(BALANCE_CREATION_EXTRA, balanceCreation)
            }
            return fragment
        }

        fun newInstance(assetCode: String, balanceCreation: Boolean): AssetDetailsFragment {
            val fragment = AssetDetailsFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(ASSET_CODE_EXTRA, assetCode)
                putBoolean(BALANCE_CREATION_EXTRA, balanceCreation)
            }
            return fragment
        }
    }
}