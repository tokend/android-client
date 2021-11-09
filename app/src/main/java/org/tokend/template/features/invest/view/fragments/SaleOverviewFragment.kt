package org.tokend.template.features.invest.view.fragments

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_sale_overview.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.layout_sale_picture.*
import org.tokend.template.R
import org.tokend.template.extensions.browse
import org.tokend.template.features.invest.logic.SaleOverviewMarkdownLoader
import org.tokend.template.features.invest.view.SaleProgressWrapper
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.ImageViewUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import ru.noties.markwon.Markwon

class SaleOverviewFragment : SaleFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
        showLoading = { progress.show() },
        hideLoading = { progress.hide() }
    )

    private val picturePlaceholder: Drawable by lazy {
        ColorDrawable(ContextCompat.getColor(requireContext(), R.color.imagePlaceholder))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        sale_description_text_view.text = sale.description

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
        ImageViewUtil.loadImage(
            sale_picture_image_view,
            sale.logoUrl,
            picturePlaceholder
        ) {
            centerCrop()
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

            ImageViewUtil.loadImage(
                video_preview_image_view,
                sale.youtubeVideo?.previewUrl,
                picturePlaceholder
            ) {
                override(width, height)
                centerCrop()
            }
        }

        video_preview_layout.setOnClickListener {
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
            repositoryProvider.blobs
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

    companion object {
        fun newInstance() = SaleOverviewFragment()
    }
}