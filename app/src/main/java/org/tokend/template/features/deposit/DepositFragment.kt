package org.tokend.template.features.deposit

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_deposit.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.responses.AccountResponse
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.logic.repository.AccountRepository
import org.tokend.template.base.view.picker.PickerItem
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class DepositFragment : BaseFragment(), ToolbarProvider {

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()
    private lateinit var accountRepository: AccountRepository

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { activity_progress.show() },
            hideLoading = { activity_progress.hide() }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_deposit, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountRepository = repositoryProvider.account()
    }

    override fun onInitAllowed() {

        initToolbar()

        accountRepository.updateIfNotFresh()

        accountRepository.itemSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe ({
                    initAssets(it.externalAccounts)
                })
        accountRepository.loadingSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it, "deposit")
                }
        accountRepository.errorsSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    ErrorHandlerFactory.getDefault().handle(it)
                }

        initButtons()
    }

    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.deposit_title)
    }

    private fun initButtons() {
        show_qr_text_view.onClick{
            Navigator.openQrShare(this.requireActivity(),
                    "${getString(R.string.deposit_title)} ${asset_tab_layout.selectedItem?.text}",
                    address_text_view.text.toString(),
                    getString(R.string.share_address_label))
        }

        share_btn.onClick {
            shareData()
        }
    }

    private fun initAssets(externalAccounts: List<AccountResponse.ExternalAccount>) {
        val assets = ArrayList<PickerItem>()
        for(account in externalAccounts) {
            assets += PickerItem(account.type.name)
        }

        asset_tab_layout.onItemSelected({
            val index = assets.indexOfFirst{item -> item == it }
            val address = externalAccounts[index].data
            val replacement = asset_tab_layout.selectedItem?.text
            address_text_view.text = address

            to_make_deposit_text_view.text = getString(R.string.to_make_deposit_send_asset, replacement)
        })

        asset_tab_layout.setItems(assets)
    }

    private fun shareData() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                getString(R.string.app_name))
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, address_text_view.text)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_address_label)))
    }
}