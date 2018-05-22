package org.tokend.template.base.activities

import android.os.Bundle
import com.google.common.io.BaseEncoding
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_progress.*
import org.tokend.sdk.api.ApiFactory
import org.tokend.sdk.api.requests.AttributesEntity
import org.tokend.sdk.api.requests.DataEntity
import org.tokend.sdk.api.requests.models.VerifyWalletRequestBody
import org.tokend.template.R
import org.tokend.template.extensions.toCompletable
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class ProcessLinkActivity : BaseActivity() {
    private class LinkData {
        @SerializedName("status")
        val status: Int? = 200
        @SerializedName("type")
        val action: Int? = null
        @SerializedName("meta")
        val data: JsonElement? = null
    }

    private class VerificationData {
        @SerializedName("token")
        val token: String? = null
        @SerializedName("wallet_id")
        val walletId: String? = null
    }

    override val allowUnauthorized = true

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_process_link)

        progress.show()

        progress.post {
            processIntentUrl()
        }
    }

    private fun processIntentUrl() {
        val intentData = intent?.data

        if (intentData == null) {
            finish()
            return
        }

        val url = intentData.toString()

        if (url.contains("/r/")) {
            val split = url.split("/r/")
            if (split.size < 2) {
                finish()
                return
            }

            val encodedData = split[1]
            val decodedData = String(BaseEncoding.base64Url().decode(encodedData))

            try {
                val linkData = ApiFactory.getBaseGson().fromJson(decodedData,
                        LinkData::class.java)
                when (linkData.action) {
                    1 -> performVerification(ApiFactory.getBaseGson()
                            .fromJson(linkData.data,
                                    VerificationData::class.java))
                }
            } catch (e: Exception) {
                finish()
                return
            }
        } else {
            finish()
            return
        }
    }

    private fun performVerification(data: VerificationData) {
        apiProvider.getApi().verifyWallet(data.walletId,
                DataEntity(
                        AttributesEntity(
                                VerifyWalletRequestBody(data.token ?: "")
                        )
                )
        )
                .toCompletable()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .subscribeBy(
                        onComplete = {
                            ToastManager.short(R.string.email_verified)
                            Navigator.toSignIn(this)
                        },
                        onError = {
                            ErrorHandlerFactory.getDefault().handle(it)
                            finish()
                        }
                )
    }
}
