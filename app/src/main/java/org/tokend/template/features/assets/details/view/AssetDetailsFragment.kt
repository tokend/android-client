package org.tokend.template.features.assets.details.view

import android.app.Activity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
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
import kotlinx.android.synthetic.main.fragment_asset_details.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.list_item_asset.*
import kotlinx.android.synthetic.main.list_item_asset.view.*
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.assets.adapter.AssetListItem
import org.tokend.template.features.assets.adapter.AssetListItemViewHolder
import org.tokend.template.features.assets.logic.CreateBalanceUseCase
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.logic.TxManager
import org.tokend.template.util.FileDownloader
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ProgressDialogFactory

open class AssetDetailsFragment : BaseFragment() {
    protected lateinit var asset: AssetRecord

    protected val assetCode: String? by lazy {
        arguments?.getString(ASSET_CODE_EXTRA)
    }

    private val isBalanceCreationEnabled: Boolean by lazy {
        arguments?.getBoolean(BALANCE_CREATION_EXTRA) ?: true
    }

    protected val balanceId: String?
        get() = repositoryProvider.balances().itemsList
                .find { it.assetCode == asset.code }?.id

    private lateinit var fileDownloader: FileDownloader

    protected val loadingIndicator = LoadingIndicatorManager(
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
        fileDownloader = FileDownloader(
                requireActivity(),
                urlConfigProvider.getConfig().storage,
                toastManager
        )
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

    private fun getAsset(): Single<AssetRecord> {
        return (arguments?.getSerializable(ASSET_EXTRA) as? AssetRecord)
                .toMaybe()
                .switchIfEmpty(
                        assetCode?.let { repositoryProvider.assets().getSingle(it) }
                                ?: Single.error(IllegalStateException("Unable to obtain asset"))
                )
    }

    // region Display
    protected open fun displayDetails() {
        displayLogoAndName()
        displaySummary()
        displayTermsIfNeeded()
    }

    private fun displayLogoAndName() {
        AssetListItemViewHolder(asset_card).bind(
                AssetListItem(asset, balanceId)
        )
    }

    protected open fun displaySummary() {
        val card = InfoCard(cards_layout)
                .setHeading(R.string.asset_summary_title, null)
                .addRow(R.string.asset_available,
                        amountFormatter.formatAssetAmount(asset.available, asset,
                                withAssetCode = false))
                .addRow(R.string.asset_issued,
                        amountFormatter.formatAssetAmount(asset.issued, asset,
                                withAssetCode = false))
                .addRow(R.string.asset_maximum,
                        amountFormatter.formatAssetAmount(asset.maximum, asset,
                                withAssetCode = false))

        card.addRow("", null)

        if (asset.isTransferable) {
            card.addRow(R.string.asset_can_be_transferred, null)
        } else {
            card.addRow(R.string.asset_can_not_be_transferred, null)
        }

        if (asset.isWithdrawable) {
            card.addRow(R.string.asset_can_be_withdrawn, null)
        } else {
            card.addRow(R.string.asset_can_not_be_withdrawn, null)
        }
    }

    private fun displayTermsIfNeeded() {
        val terms = asset.terms?.takeIf { !it.name.isNullOrEmpty() }
                ?: return

        val fileCardView =
                layoutInflater.inflate(R.layout.list_item_remote_file,
                        cards_layout, false)

        val fileLayout = fileCardView?.find<View>(R.id.file_content_layout) ?: return
        (fileCardView as? androidx.cardview.widget.CardView)?.removeView(fileLayout)

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
                ContextCompat.getDrawable(this.requireContext(), R.drawable.ic_file_download_outline)
        )

        InfoCard(cards_layout)
                .setHeading(R.string.asset_terms_of_use, null)
                .addView(fileLayout)
    }
    // endregion

    private fun initButtons() {
        asset_details_button.visibility = View.GONE

        if (balanceId == null && isBalanceCreationEnabled) {
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
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun createBalance() {
        val progress = ProgressDialogFactory.getDialog(requireContext())

        CreateBalanceUseCase(
                asset.code,
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
                            onBalanceCreated()
                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )

    }

    private fun onBalanceCreated() {
        toastManager.short(getString(R.string.template_asset_balance_created, asset.code))

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
        const val ASSET_CODE_EXTRA = "asset_code"
        const val BALANCE_CREATION_EXTRA = "balance_creation"

        fun newInstance(bundle: Bundle): AssetDetailsFragment =
                AssetDetailsFragment().withArguments(bundle)

        fun getBundle(assetCode: String, balanceCreation: Boolean) = Bundle().apply {
            putSerializable(ASSET_CODE_EXTRA, assetCode)
            putBoolean(BALANCE_CREATION_EXTRA, balanceCreation)
        }

        fun getBundle(asset: AssetRecord, balanceCreation: Boolean) = Bundle().apply {
            putSerializable(ASSET_EXTRA, asset)
            putBoolean(BALANCE_CREATION_EXTRA, balanceCreation)
        }
    }
}