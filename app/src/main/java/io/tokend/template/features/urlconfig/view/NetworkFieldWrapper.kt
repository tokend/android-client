package io.tokend.template.features.urlconfig.view

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.BuildConfig
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.urlconfig.logic.UpdateUrlConfigFromScannedUseCase
import io.tokend.template.features.urlconfig.model.InvalidUrlConfigSourceException
import io.tokend.template.fragments.BaseFragment
import io.tokend.template.logic.providers.UrlConfigProvider
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.PermissionManager
import io.tokend.template.util.QrScannerUtil
import io.tokend.template.util.errorhandler.CompositeErrorHandler
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.util.errorhandler.SimpleErrorHandler
import io.tokend.template.util.navigation.ActivityRequest
import io.tokend.template.view.ToastManager
import io.tokend.template.view.util.ProgressDialogFactory
import kotlinx.android.synthetic.main.layout_network_field.view.*

/**
 * Wraps the network field and enables all the associated logic.
 *
 * @see UpdateUrlConfigFromScannedUseCase
 */
class NetworkFieldWrapper
private constructor(
    private val activity: BaseActivity?,
    private val fragment: BaseFragment?,
    private val fieldRootLayout: ViewGroup,
    private val cameraPermission: PermissionManager,
    private val compositeDisposable: CompositeDisposable,
    private val activityRequestsBag: MutableCollection<ActivityRequest<*>>
) {
    constructor(
        activity: BaseActivity,
        fieldRootLayout: ViewGroup,
        cameraPermission: PermissionManager,
        compositeDisposable: CompositeDisposable,
        activityRequestsBag: MutableCollection<ActivityRequest<*>>
    ) : this(
        activity, null, fieldRootLayout, cameraPermission, compositeDisposable, activityRequestsBag
    )

    constructor(
        fragment: BaseFragment,
        fieldRootLayout: ViewGroup,
        cameraPermission: PermissionManager,
        compositeDisposable: CompositeDisposable,
        activityRequestsBag: MutableCollection<ActivityRequest<*>>
    ) : this(
        null, fragment, fieldRootLayout, cameraPermission, compositeDisposable, activityRequestsBag
    )

    private val networkEditText: EditText = fieldRootLayout.network_edit_text
    private val scanQrButton: View = fieldRootLayout.scan_qr_button

    private val urlConfigProvider: UrlConfigProvider =
        (activity?.urlConfigProvider ?: fragment?.urlConfigProvider)!!
    private var networkUpdateListener: () -> Unit = {}
    private val toastManager: ToastManager =
        (activity?.toastManager ?: fragment?.toastManager)!!

    init {
        if (BuildConfig.IS_NETWORK_SPECIFIED_BY_USER) {
            fieldRootLayout.visibility = View.VISIBLE
            scanQrButton.setOnClickListener {
                tryOpenQrScanner()
            }

            displayNetwork()
        } else {
            fieldRootLayout.visibility = View.GONE
        }
    }

    private fun displayNetwork() {
        networkEditText.setText(urlConfigProvider.getConfig().apiDomain)
    }

    fun onNetworkUpdated(networkUpdateListener: () -> Unit) {
        this.networkUpdateListener = networkUpdateListener
    }

    private fun tryOpenQrScanner() {
        activity?.also { activity ->
            cameraPermission.check(activity) {
                QrScannerUtil
                    .openScanner(activity, activity.getString(R.string.network_qr_scan_prompt))
                    .addTo(activityRequestsBag)
                    .doOnSuccess(this::tryToUpdateUrlConfig)
            }
        }

        fragment?.also { fragment ->
            cameraPermission.check(fragment) {
                QrScannerUtil
                    .openScanner(fragment, fragment.getString(R.string.network_qr_scan_prompt))
                    .addTo(activityRequestsBag)
                    .doOnSuccess(this::tryToUpdateUrlConfig)
            }
        }
    }

    private fun tryToUpdateUrlConfig(content: String) {
        lateinit var disposable: Disposable

        val progress = ProgressDialogFactory.getDialog(fieldRootLayout.context) {
            disposable.dispose()
        }

        disposable = UpdateUrlConfigFromScannedUseCase(
            scannedContent = content,
            urlConfigProvider = activity?.urlConfigProvider ?: fragment?.urlConfigProvider!!
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
                    displayNetwork()
                    networkUpdateListener.invoke()
                },
                onError = updateErrorHandler::handleIfPossible
            )
            .addTo(compositeDisposable)
    }

    private val updateErrorHandler: ErrorHandler
        get() = CompositeErrorHandler(
            SimpleErrorHandler { error ->
                if (error is InvalidUrlConfigSourceException) {
                    toastManager.short(R.string.error_incorrect_network_qr)
                    true
                } else {
                    false
                }
            },
            (activity?.errorHandlerFactory?.getDefault()
                ?: fragment?.errorHandlerFactory?.getDefault())!!
        )
}