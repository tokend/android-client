package org.tokend.template.features.invest.view.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.squareup.picasso.Picasso
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_sale_overview.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.layout_sale_picture.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.features.invest.logic.BlobManager
import org.tokend.template.features.invest.logic.InvestmentInfoManager
import org.tokend.template.features.invest.logic.SaleOverviewMarkdownLoader
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.view.SaleProgressWrapper
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.logic.FeeManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import ru.noties.markwon.Markwon
import java.math.BigDecimal

class SaleOverviewFragment : BaseFragment() {
    private val mainLoading = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private lateinit var sale: SaleRecord

    private lateinit var feeManager: FeeManager
    private lateinit var investmentInfoManager: InvestmentInfoManager

    private var existingOffers: Map<String, OfferRecord> = emptyMap()
    private var maxInvestAmount = BigDecimal.ZERO

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sale_overview, container, false)
    }

    override fun onInitAllowed() {
        val sale = arguments?.getSerializable(SALE_EXTRA) as? SaleRecord
                ?: return

        this.sale = sale

        feeManager = FeeManager(apiProvider)
        investmentInfoManager = InvestmentInfoManager(sale, repositoryProvider,
                walletInfoProvider, amountFormatter)

        displaySaleInfo()

        update()
        loadOverview()
    }

    // region Update
    private fun update() {
        repositoryProvider.balances()
                .updateIfNotFreshDeferred()
                .andThen(
                        investmentInfoManager
                                .getInvestmentInfo(
                                        FeeManager(apiProvider)
                                )
                )
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    mainLoading.show()
                }
                .doOnEvent { _, _ ->
                    mainLoading.hide()
                }
                .subscribeBy(
                        onSuccess = { result ->
                            this.sale = result.detailedSale
                            this.existingOffers = result.offersByAsset

                            onInvestmentInfoUpdated()
                        },
                        onError = {
                            it.printStackTrace()
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    /**
     * Updates only data required for amount calculations and validation.
     */
    private fun updateFinancial() {
        repositoryProvider.balances()
                .updateIfNotFreshDeferred()
                .andThen(
                        investmentInfoManager
                                .getFinancialInfo(
                                        FeeManager(apiProvider)
                                )
                )
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    mainLoading.show()
                }
                .doOnEvent { _, _ ->
                    mainLoading.hide()
                }
                .subscribeBy(
                        onSuccess = { result ->
                            this.sale = result.detailedSale
                            this.existingOffers = result.offersByAsset

                            onInvestmentInfoUpdated()
                        },
                        onError = {
                            it.printStackTrace()
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }
    // endregion

    // region Info display
    private fun displaySaleInfo() {
        sale_name_text_view.text =
                getString(R.string.template_sale_name_asset, sale.name, sale.baseAssetCode)
        sale_description_text_view.text = sale.shortDescription

        if (sale.youtubeVideo != null) {
            displayYoutubePreview()
        }

        displayChangeableSaleInfo()
        displaySalePhoto()
    }

    private fun displayChangeableSaleInfo() {
        SaleProgressWrapper(scroll_view, amountFormatter).displayProgress(sale)
    }

    private fun displaySalePhoto() {
        sale.logoUrl?.let {
            Picasso.with(requireContext())
                    .load(it)
                    .placeholder(R.color.saleImagePlaceholder)
                    .fit()
                    .centerCrop()
                    .into(sale_picture_image_view)
        }

        if (sale.isUpcoming) {
            sale_upcoming_image_view.visibility = View.VISIBLE
        } else {
            sale_upcoming_image_view.visibility = View.GONE
        }
    }

    private fun displayYoutubePreview() {
        video_preview_layout.visibility = View.VISIBLE

        video_preview_image_view.post {
            val width = video_preview_layout.width
            val height = (width * 720f / 1280f).toInt()

            video_preview_image_view.layoutParams = RelativeLayout.LayoutParams(width, height)

            Picasso.with(requireContext())
                    .load(sale.youtubeVideo?.previewUrl)
                    .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(),
                            R.color.saleImagePlaceholder)))
                    .resize(width, height)
                    .centerCrop()
                    .into(video_preview_image_view)
        }

        video_preview_layout.onClick {
            sale.youtubeVideo?.url
                    ?.also { url ->
                        requireContext().browse(url)
                    }
        }
    }
    // endregion

    // region Overview blob
    private fun loadOverview() {
        SaleOverviewMarkdownLoader(
                requireContext(),
                BlobManager(apiProvider, walletInfoProvider)
        )
                .load(sale.fullDescriptionBlob)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    mainLoading.show("overview")
                }
                .doOnEvent { _, _ ->
                    mainLoading.hide("overview")
                }
                .subscribeBy(
                        onSuccess = {
                            Markwon.setText(sale_overview_text_view, it)
                        },
                        onError = {}
                )
                .addTo(compositeDisposable)
    }
    // endregion

    // region Invest
    private fun onInvestmentInfoUpdated() {
        displayChangeableSaleInfo()
    }
    // endregion

    companion object {
        private const val SALE_EXTRA = "sale"

        fun newInstance(sale: SaleRecord): SaleOverviewFragment {
            val fragment = SaleOverviewFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(SALE_EXTRA, sale)
            }
            return fragment
        }
    }
}