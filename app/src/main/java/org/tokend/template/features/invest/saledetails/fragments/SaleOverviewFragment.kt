package org.tokend.template.features.invest.saledetails.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_sale_overview.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.tokend.template.R
import org.tokend.template.features.invest.logic.BlobManager
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import ru.noties.markwon.Markwon

class SaleOverviewFragment : BaseFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private lateinit var blobId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sale_overview, container, false)
    }

    override fun onInitAllowed() {
        arguments?.getString(BLOB_ID_EXTRA).also {
            if (it == null) {
                error_empty_view.showError(IllegalStateException(), errorHandlerFactory.getDefault())
            } else {
                blobId = it
                loadBlob()
            }
        }
    }

    private var loadingDisposable: Disposable? = null
    private fun loadBlob() {
        loadingDisposable?.dispose()
        loadingDisposable =
                BlobManager(apiProvider, walletInfoProvider)
                        .getBlob(blobId)
                        .map { it.valueString }
                        .map { markdownString ->
                            Markwon.markdown(requireContext(), markdownString)
                        }
                        .compose(ObservableTransformers.defaultSchedulersSingle())
                        .doOnSubscribe {
                            loadingIndicator.show()
                        }
                        .doOnEvent { _, _ ->
                            loadingIndicator.hide()
                        }
                        .subscribeBy(
                                onSuccess = {
                                    error_empty_view.hide()
                                    displayMarkdown(it)
                                },
                                onError = {
                                    error_empty_view.showError(it, errorHandlerFactory.getDefault()) {
                                        loadBlob()
                                    }
                                }
                        )
                        .addTo(compositeDisposable)
    }

    private fun displayMarkdown(content: CharSequence) {
        Markwon.setText(markdown_text_view, content)
    }

    companion object {
        private const val BLOB_ID_EXTRA = "blob_id"

        fun newInstance(blobId: String): SaleOverviewFragment {
            val fragment = SaleOverviewFragment()
            fragment.arguments = Bundle().apply {
                putString(BLOB_ID_EXTRA, blobId)
            }
            return fragment
        }
    }
}