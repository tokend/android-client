package org.tokend.template.features.invest.sale_details.fragments

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
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.features.invest.BlobManager
import org.tokend.template.util.ObservableTransformers
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
                error_empty_view.showError(IllegalStateException())
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
                                    displayMarkdown(it.valueString)
                                },
                                onError = {
                                    error_empty_view.showError(it) {
                                        loadBlob()
                                    }
                                }
                        )
                        .addTo(compositeDisposable)
    }

    private fun displayMarkdown(content: String) {
        Markwon.setMarkdown(markdown_text_view, content)
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