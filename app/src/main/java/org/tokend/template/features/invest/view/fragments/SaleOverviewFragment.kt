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
import org.tokend.template.features.invest.logic.BlobManager
import org.tokend.template.features.invest.logic.SaleOverviewMarkdownLoader
import org.tokend.template.features.invest.view.SaleProgressWrapper
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import ru.noties.markwon.Markwon

class SaleOverviewFragment : SaleFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sale_overview, container, false)
    }

    override fun onInitAllowed() {
        super.onInitAllowed()

        subscribeToInvestmentInfo()
        displaySaleInfo()

        loadOverview()
    }

    private fun subscribeToInvestmentInfo() {
        investmentInfoRepository
                .itemSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayChangeableSaleInfo()
                }
                .addTo(compositeDisposable)

        investmentInfoRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loadingIndicator.setLoading(it) }
                .addTo(compositeDisposable)
    }

    // region Info display
    private fun displaySaleInfo() {
        sale_name_text_view.text =
                getString(R.string.template_sale_name_asset, sale.name, sale.baseAsset.code)
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
        val blobId = sale.fullDescriptionBlob
                ?: return

        SaleOverviewMarkdownLoader(
                requireContext(),
                BlobManager(apiProvider)
        )
                .load(blobId)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    loadingIndicator.show("overview")
                }
                .doOnEvent { _, _ ->
                    loadingIndicator.hide("overview")
                }
                .subscribeBy(
                        onSuccess = {
                            Markwon.setText(sale_overview_text_view, it)
                        },
                        onError = {
                            sale_overview_text_view.text =
                                    errorHandlerFactory.getDefault()
                                            .getErrorMessage(it)
                        }
                )
                .addTo(compositeDisposable)
    }
    // endregion
}