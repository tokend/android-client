package org.tokend.template.util.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import android.view.View
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.R
import org.tokend.template.activities.MainActivity
import org.tokend.template.activities.SingleFragmentActivity
import org.tokend.template.extensions.*
import org.tokend.template.features.assets.buy.BuyWithAtomicSwapActivity
import org.tokend.template.features.assets.buy.view.AtomicSwapAsksFragment
import org.tokend.template.features.assets.details.view.AssetDetailsActivity
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.features.assets.view.ExploreAssetsFragment
import org.tokend.template.features.balances.view.BalanceDetailsActivity
import org.tokend.template.features.changepassword.ChangePasswordActivity
import org.tokend.template.features.deposit.view.DepositAmountActivity
import org.tokend.template.features.deposit.view.DepositFragment
import org.tokend.template.features.fees.view.FeesActivity
import org.tokend.template.features.history.details.*
import org.tokend.template.features.history.model.BalanceChange
import org.tokend.template.features.history.model.details.BalanceChangeCause
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.view.InvestmentConfirmationActivity
import org.tokend.template.features.invest.view.SaleActivity
import org.tokend.template.features.invest.view.SaleInvestActivity
import org.tokend.template.features.kyc.capture.model.CameraCaptureResult
import org.tokend.template.features.kyc.capture.model.CameraCaptureTarget
import org.tokend.template.features.kyc.capture.view.CameraCaptureActivity
import org.tokend.template.features.kyc.view.SetKycActivity
import org.tokend.template.features.limits.view.LimitsActivity
import org.tokend.template.features.localaccount.importt.view.ImportLocalAccountActivity
import org.tokend.template.features.localaccount.view.LocalAccountDetailsActivity
import org.tokend.template.features.offers.CreateOfferActivity
import org.tokend.template.features.offers.OfferConfirmationActivity
import org.tokend.template.features.offers.OffersActivity
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.features.offers.model.OfferRequest
import org.tokend.template.features.offers.view.details.PendingInvestmentDetailsActivity
import org.tokend.template.features.offers.view.details.PendingOfferDetailsActivity
import org.tokend.template.features.qr.ShareQrFragment
import org.tokend.template.features.recovery.RecoveryActivity
import org.tokend.template.features.send.PaymentConfirmationActivity
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.signin.LocalAccountSignInActivity
import org.tokend.template.features.signin.SignInActivity
import org.tokend.template.features.signin.unlock.UnlockAppActivity
import org.tokend.template.features.signup.SignUpActivity
import org.tokend.template.features.trade.TradeActivity
import org.tokend.template.features.trade.pairs.model.AssetPairRecord
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.WithdrawalConfirmationActivity
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.logic.credentials.model.WalletInfoRecord
import org.tokend.template.util.IntentLock
import java.math.BigDecimal

/**
 * Performs transitions between screens.
 * 'open-' will open related screen as a child.<p>
 * 'to-' will open related screen and finish current.
 */
class Navigator private constructor() {
    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var context: Context? = null

    companion object {
        fun from(activity: Activity): Navigator {
            val navigator = Navigator()
            navigator.activity = activity
            navigator.context = activity
            return navigator
        }

        fun from(fragment: Fragment): Navigator {
            val navigator = Navigator()
            navigator.fragment = fragment
            navigator.context = fragment.requireContext()
            return navigator
        }

        fun from(context: Context): Navigator {
            val navigator = Navigator()
            navigator.context = context
            return navigator
        }
    }

    private fun performIntent(intent: Intent?, requestCode: Int? = null, bundle: Bundle? = null) {
        if (intent != null) {
            if (!IntentLock.checkIntent(intent, context)) return
            activity?.let {
                if (requestCode != null) {
                    it.startActivityForResult(intent, requestCode, bundle ?: Bundle.EMPTY)
                } else {
                    it.startActivity(intent, bundle ?: Bundle.EMPTY)
                }
                return
            }

            fragment?.let {
                if (requestCode != null) {
                    it.startActivityForResult(intent, requestCode, bundle ?: Bundle.EMPTY)
                } else {
                    it.startActivity(intent, bundle ?: Bundle.EMPTY)
                }
                return
            }

            context?.startActivity(intent.newTask(), bundle ?: Bundle.EMPTY)
        }
    }

    private fun createAndPerformIntent(activityClass: Class<*>,
                                       extras: Bundle? = null,
                                       requestCode: Int? = null,
                                       transitionBundle: Bundle? = null) {
        val intent = context?.let { Intent(it, activityClass) }
                ?: return

        if (extras != null) {
            intent.putExtras(extras)
        }

        performIntent(intent, requestCode, transitionBundle)
    }

    private fun <R : Any> createAndPerformRequest(request: ActivityRequest<R>,
                                                  activityClass: Class<*>,
                                                  extras: Bundle? = null,
                                                  transitionBundle: Bundle? = null
    ): ActivityRequest<R> {
        createAndPerformIntent(activityClass, extras, request.code, transitionBundle)
        return request
    }

    private fun createAndPerformSimpleRequest(activityClass: Class<*>,
                                              extras: Bundle? = null,
                                              transitionBundle: Bundle? = null
    ) = createAndPerformRequest(ActivityRequest.withoutResultData(),
            activityClass, extras, transitionBundle)

    private fun fadeOut(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
        activity.overridePendingTransition(0, R.anim.activity_fade_out)
        activity.finish()
    }

    private fun createTransitionBundle(activity: Activity, vararg pairs: Pair<View?, String>): Bundle {
        val sharedViews = arrayListOf<androidx.core.util.Pair<View, String>>()

        pairs.forEach {
            val view = it.first
            if (view != null) {
                sharedViews.add(androidx.core.util.Pair(view, it.second))
            }
        }

        return if (sharedViews.isEmpty()) {
            Bundle.EMPTY
        } else {
            ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                    *sharedViews.toTypedArray()).toBundle() ?: Bundle.EMPTY
        }
    }

    fun openSignUp() {
        context?.intentFor<SignUpActivity>()
                ?.also { performIntent(it) }
    }

    fun openRecovery(email: String? = null) = createAndPerformIntent(
            RecoveryActivity::class.java,
            RecoveryActivity.getBundle(email)
    )

    fun toSignIn(finishAffinity: Boolean = false) {
        context?.intentFor<SignInActivity>()
                ?.singleTop()
                ?.clearTop()
                ?.also { performIntent(it) }
        activity?.let {
            if (finishAffinity) {
                it.setResult(Activity.RESULT_CANCELED, null)
                ActivityCompat.finishAffinity(it)
            } else {
                it.finish()
            }
        }
    }

    fun toUnlock() {
        context?.intentFor<UnlockAppActivity>()
                ?.singleTop()
                ?.clearTop()
                ?.also { performIntent(it) }
        activity?.finish()
    }

    fun toMainActivity(finishAffinity: Boolean = false) {
        context?.intentFor<MainActivity>()
                ?.also { performIntent(it) }
        activity?.let {
            if (finishAffinity) {
                it.setResult(Activity.RESULT_CANCELED, null)
                ActivityCompat.finishAffinity(it)
            } else {
                fadeOut(it)
            }
        }
    }

    fun openQrShare(title: String,
                    data: String,
                    shareLabel: String,
                    shareText: String? = data,
                    topText: String? = null,
                    bottomText: String? = null
    ) = createAndPerformSimpleRequest(
            SingleFragmentActivity::class.java,
            SingleFragmentActivity.getBundle(
                    ShareQrFragment.ID,
                    ShareQrFragment.getBundle(data, title, shareLabel, shareText, topText, bottomText)
            )
    )

    fun openPasswordChange() = createAndPerformSimpleRequest(
            ChangePasswordActivity::class.java
    )

    fun openWithdrawalConfirmation(withdrawalRequest: WithdrawalRequest
    ) = createAndPerformSimpleRequest(
            WithdrawalConfirmationActivity::class.java,
            WithdrawalConfirmationActivity.getBundle(withdrawalRequest)
    )

    fun openSend(balanceId: String? = null) = createAndPerformSimpleRequest(
            SingleFragmentActivity::class.java,
            SingleFragmentActivity.getBundle(
                    SendFragment.ID,
                    SendFragment.getBundle(balanceId, true)
            )
    )

    fun openAssetDetails(asset: AssetRecord,
                         cardView: View? = null): ActivityRequest<Unit> {
        val transitionBundle = activity?.let {
            createTransitionBundle(it,
                    cardView to it.getString(R.string.transition_asset_card)
            )
        } ?: fragment?.let {
            createTransitionBundle(it.requireActivity(),
                    cardView to it.getString(R.string.transition_asset_card)
            )
        }

        return createAndPerformSimpleRequest(
                AssetDetailsActivity::class.java,
                AssetDetailsActivity.getBundle(asset),
                transitionBundle = transitionBundle
        )
    }

    fun openPaymentConfirmation(paymentRequest: PaymentRequest
    ) = createAndPerformSimpleRequest(
            PaymentConfirmationActivity::class.java,
            PaymentConfirmationActivity.getBundle(paymentRequest)
    )

    fun openOfferConfirmation(offerRequest: OfferRequest
    ) = createAndPerformSimpleRequest(
            OfferConfirmationActivity::class.java,
            OfferConfirmationActivity.getBundle(offerRequest)
    )

    fun openInvestmentConfirmation(investmentRequest: OfferRequest,
                                   displayToReceive: Boolean = true,
                                   saleName: String? = null
    ) = createAndPerformSimpleRequest(
            InvestmentConfirmationActivity::class.java,
            InvestmentConfirmationActivity
                    .getBundle(investmentRequest, displayToReceive, saleName)
    )

    fun openPendingOffers(onlyPrimary: Boolean = false
    ) = createAndPerformSimpleRequest(
            OffersActivity::class.java,
            OffersActivity.getBundle(onlyPrimary)
    )

    fun openSale(sale: SaleRecord) = createAndPerformSimpleRequest(
            SaleActivity::class.java,
            SaleActivity.getBundle(sale)
    )

    fun openBalanceChangeDetails(change: BalanceChange) {
        val activityClass = when (change.cause) {
            is BalanceChangeCause.AmlAlert -> AmlAlertDetailsActivity::class.java
            is BalanceChangeCause.Investment -> InvestmentDetailsActivity::class.java
            is BalanceChangeCause.MatchedOffer -> OfferMatchDetailsActivity::class.java
            is BalanceChangeCause.Issuance -> IssuanceDetailsActivity::class.java
            is BalanceChangeCause.Payment -> PaymentDetailsActivity::class.java
            is BalanceChangeCause.WithdrawalRequest -> WithdrawalDetailsActivity::class.java
            is BalanceChangeCause.Offer -> {
                openPendingOfferDetails(OfferRecord.fromBalanceChange(change))
                return
            }
            is BalanceChangeCause.AssetPairUpdate -> AssetPairUpdateDetailsActivity::class.java
            else -> DefaultBalanceChangeDetailsActivity::class.java
        }

        createAndPerformIntent(
                activityClass,
                BalanceChangeDetailsActivity.getBundle(change)
        )
    }

    fun openPendingOfferDetails(offer: OfferRecord) {
        val activityClass =
                if (offer.isInvestment)
                    PendingInvestmentDetailsActivity::class.java
                else
                    PendingOfferDetailsActivity::class.java

        createAndPerformIntent(
                activityClass,
                PendingOfferDetailsActivity.getBundle(offer)
        )
    }

    fun openTrade(assetPair: AssetPairRecord) = createAndPerformIntent(
            TradeActivity::class.java,
            TradeActivity.getBundle(assetPair)
    )

    fun openCreateOffer(baseAsset: Asset,
                        quoteAsset: Asset,
                        requiredPrice: BigDecimal? = null
    ) = createAndPerformIntent(
            CreateOfferActivity::class.java,
            CreateOfferActivity.getBundle(baseAsset, quoteAsset, requiredPrice)
    )

    fun openLimits() = createAndPerformIntent(
            LimitsActivity::class.java
    )

    fun openFees(asset: String? = null,
                 feeType: Int = -1) = createAndPerformIntent(
            FeesActivity::class.java,
            FeesActivity.getBundle(asset, feeType)
    )

    fun openDeposit(asset: String) = createAndPerformSimpleRequest(
            SingleFragmentActivity::class.java,
            SingleFragmentActivity.getBundle(
                    DepositFragment.ID,
                    DepositFragment.getBundle(asset)
            )
    )

    fun openWithdraw(balanceId: String) = createAndPerformSimpleRequest(
            SingleFragmentActivity::class.java,
            SingleFragmentActivity.getBundle(
                    WithdrawFragment.ID,
                    WithdrawFragment.getBundle(balanceId)
            )
    )

    fun openInvest(sale: SaleRecord) = createAndPerformIntent(
            SaleInvestActivity::class.java,
            SaleInvestActivity.getBundle(sale)
    )

    fun openAssetsExplorer() = createAndPerformIntent(
            SingleFragmentActivity::class.java,
            SingleFragmentActivity.getBundle(
                    ExploreAssetsFragment.ID,
                    null
            )
    )

    fun openBalanceDetails(balanceId: String) = createAndPerformIntent(
            BalanceDetailsActivity::class.java,
            BalanceDetailsActivity.getBundle(balanceId)
    )

    fun openAccountQrShare(walletInfo: WalletInfoRecord) {
        openAccountQrShare(walletInfo.accountId)
    }

    private fun openAccountQrShare(accountId: String) {
        openQrShare(
                data = accountId,
                title = context!!.getString(R.string.account_id_title),
                shareLabel = context!!.getString(R.string.share_account_id)
        )
    }

    fun openAtomicSwapBuy(assetCode: String,
                          askId: String) = createAndPerformIntent(
            BuyWithAtomicSwapActivity::class.java,
            BuyWithAtomicSwapActivity.getBundle(assetCode, askId)
    )

    fun openAtomicSwapsAsks(assetCode: String) = createAndPerformIntent(
            SingleFragmentActivity::class.java,
            SingleFragmentActivity.getBundle(
                    AtomicSwapAsksFragment.ID,
                    AtomicSwapAsksFragment.getBundle(assetCode)
            )
    )

    fun openLocalAccountSignIn() = createAndPerformSimpleRequest(
            LocalAccountSignInActivity::class.java
    )

    fun openLocalAccountDetails() = createAndPerformIntent(
            LocalAccountDetailsActivity::class.java
    )

    fun openLocalAccountImport() = createAndPerformIntent(
            ImportLocalAccountActivity::class.java
    )

    fun openDepositAmountInput(assetCode: String) = createAndPerformRequest(
            ActivityRequest { intent ->
                intent?.getBigDecimalExtra(DepositAmountActivity.RESULT_AMOUNT_EXTRA)
                        ?.takeIf { it.signum() > 0 }
            },
            DepositAmountActivity::class.java,
            DepositAmountActivity.getBundle(assetCode)
    )

    fun openCameraCapture(
        target: CameraCaptureTarget,
        canSkip: Boolean
    ) = createAndPerformRequest(
        ActivityRequest { data ->
            data?.getSerializableExtra(CameraCaptureActivity.CAPTURE_RESULT_EXTRA)
                    as? CameraCaptureResult
        },
        CameraCaptureActivity::class.java,
        CameraCaptureActivity.getBundle(target, canSkip)
    )

    fun openSetKyc() = createAndPerformSimpleRequest(
        SetKycActivity::class.java
    )
}
