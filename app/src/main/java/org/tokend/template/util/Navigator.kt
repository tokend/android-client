package org.tokend.template.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.Fragment
import android.view.View
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import org.jetbrains.anko.singleTop
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.activities.MainActivity
import org.tokend.template.activities.SingleFragmentActivity
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.features.assets.AssetDetailsActivity
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.assets.buy.BuyWithAtomicSwapActivity
import org.tokend.template.features.assets.buy.view.AtomicSwapAsksFragment
import org.tokend.template.features.changepassword.ChangePasswordActivity
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.fees.view.FeesActivity
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.view.InvestmentConfirmationActivity
import org.tokend.template.features.invest.view.SaleActivity
import org.tokend.template.features.invest.view.SaleInvestActivity
import org.tokend.template.features.limits.LimitsActivity
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
import org.tokend.template.features.signin.AuthenticatorSignInActivity
import org.tokend.template.features.signin.SignInActivity
import org.tokend.template.features.signin.UnlockAppActivity
import org.tokend.template.features.signup.RecoverySeedActivity
import org.tokend.template.features.signup.SignUpActivity
import org.tokend.template.features.trade.TradeActivity
import org.tokend.template.features.wallet.details.*
import org.tokend.template.features.wallet.view.BalanceDetailsActivity
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.WithdrawalConfirmationActivity
import org.tokend.template.features.withdraw.model.WithdrawalRequest
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

    private fun fadeOut(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
        activity.overridePendingTransition(0, R.anim.activity_fade_out)
        activity.finish()
    }

    private fun createTransitionBundle(activity: Activity, vararg pairs: Pair<View?, String>): Bundle {
        val sharedViews = arrayListOf<android.support.v4.util.Pair<View, String>>()

        pairs.forEach {
            val view = it.first
            if (view != null) {
                sharedViews.add(android.support.v4.util.Pair(view, it.second))
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
        val intent = context?.intentFor<SignUpActivity>()
        performIntent(intent)
    }

    fun openRecovery(email: String? = null) {
        val intent = context?.intentFor<RecoveryActivity>(
                RecoveryActivity.EMAIL_EXTRA to email
        )
        performIntent(intent)
    }

    fun toSignIn(finishAffinity: Boolean = false) {
        val intent = context?.intentFor<SignInActivity>()
                ?.singleTop()
                ?.clearTop()
        performIntent(intent)
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
        val intent = context?.intentFor<UnlockAppActivity>()
                ?.singleTop()
                ?.clearTop()
        performIntent(intent)
        activity?.finish()
    }

    fun toMainActivity() {
        val intent = context?.intentFor<MainActivity>()
        performIntent(intent)
        activity?.let { fadeOut(it) }
    }

    fun openQrShare(title: String,
                    data: String,
                    shareLabel: String,
                    shareText: String? = data,
                    topText: String? = null,
                    bottomText: String? = null,
                    requestCode: Int = 0) {

        val intent = context?.intentFor<SingleFragmentActivity>(
                SingleFragmentActivity.DATA_EXTRA to data,
                SingleFragmentActivity.TITLE_EXTRA to title,
                SingleFragmentActivity.SHARE_DIALOG_TEXT_EXTRA to shareLabel,
                SingleFragmentActivity.TOP_TEXT_EXTRA to topText,
                SingleFragmentActivity.SHARE_TEXT_EXTRA to shareText,
                SingleFragmentActivity.BOTTOM_TEXT_EXTRA to bottomText,
                SingleFragmentActivity.SCREEN_ID to ShareQrFragment.ID
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openRecoverySeedSaving(requestCode: Int, seed: String) {
        val intent = context?.intentFor<RecoverySeedActivity>(
                RecoverySeedActivity.SEED_EXTRA to seed
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openPasswordChange(requestCode: Int) {
        val intent = context?.intentFor<ChangePasswordActivity>()
        performIntent(intent, requestCode = requestCode)
    }

    fun openWithdrawalConfirmation(requestCode: Int,
                                   withdrawalRequest: WithdrawalRequest) {
        val intent = context?.intentFor<WithdrawalConfirmationActivity>(
                WithdrawalConfirmationActivity.WITHDRAWAL_REQUEST_EXTRA to withdrawalRequest
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openSend(asset: String? = null, requestCode: Int) {
        val intent = context?.intentFor<SingleFragmentActivity>(
                SingleFragmentActivity.ASSET_EXTRA to asset,
                SingleFragmentActivity.SCREEN_ID to SendFragment.ID
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openAssetDetails(requestCode: Int,
                         asset: AssetRecord,
                         cardView: View? = null) {
        val transitionBundle = activity?.let {
            createTransitionBundle(it,
                    cardView to it.getString(R.string.transition_asset_card)
            )
        } ?: fragment?.let {
            createTransitionBundle(it.requireActivity(),
                    cardView to it.getString(R.string.transition_asset_card)
            )
        }
        val intent = context?.intentFor<AssetDetailsActivity>(
                AssetDetailsActivity.ASSET_EXTRA to asset
        )
        performIntent(intent, requestCode, transitionBundle)
    }

    fun openPaymentConfirmation(requestCode: Int, paymentRequest: PaymentRequest) {
        val intent = context?.intentFor<PaymentConfirmationActivity>(
                PaymentConfirmationActivity.PAYMENT_REQUEST_EXTRA to paymentRequest
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openOfferConfirmation(requestCode: Int, request: OfferRequest) {
        val intent = context?.intentFor<OfferConfirmationActivity>(
                OfferConfirmationActivity.OFFER_REQUEST_EXTRA to request
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openInvestmentConfirmation(requestCode: Int,
                                   request: OfferRequest,
                                   displayToReceive: Boolean = true,
                                   saleName: String? = null) {
        val intent = context?.intentFor<InvestmentConfirmationActivity>(
                OfferConfirmationActivity.OFFER_REQUEST_EXTRA to request,
                InvestmentConfirmationActivity.DISPLAY_TO_RECEIVE to displayToReceive,
                InvestmentConfirmationActivity.SALE_NAME_EXTRA to saleName
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openPendingOffers(requestCode: Int = 0, onlyPrimary: Boolean = false) {
        val intent = context?.intentFor<OffersActivity>(
                OffersActivity.ONLY_PRIMARY_EXTRA to onlyPrimary
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openSale(requestCode: Int, sale: SaleRecord) {
        val intent = context?.intentFor<SaleActivity>(
                SaleActivity.SALE_EXTRA to sale
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openAuthenticatorSignIn(requestCode: Int) {
        val intent = context?.intentFor<AuthenticatorSignInActivity>()
        performIntent(intent, requestCode = requestCode)
    }

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
            is BalanceChangeCause.SaleCancellation -> SaleCancellationDetailsActivity::class.java
            is BalanceChangeCause.OfferCancellation -> OfferCancellationDetailsActivity::class.java
            is BalanceChangeCause.AssetPairUpdate -> AssetPairUpdateDetailsActivity::class.java
            is BalanceChangeCause.Unknown -> UnknownDetailsActivity::class.java
        }

        val intent = Intent(context, activityClass).apply {
            putExtra(BalanceChangeDetailsActivity.BALANCE_CHANGE_EXTRA, change)
        }
        performIntent(intent)
    }

    fun openPendingOfferDetails(offer: OfferRecord,
                                requestCode: Int = 0) {
        val activityClass =
                if (offer.isInvestment)
                    PendingInvestmentDetailsActivity::class.java
                else
                    PendingOfferDetailsActivity::class.java

        val intent = Intent(context, activityClass).apply {
            putExtra(PendingOfferDetailsActivity.OFFER_EXTRA, offer)
        }
        performIntent(intent, requestCode = requestCode)
    }

    fun openTrade(assetPair: AssetPairRecord) {
        val intent = context?.intentFor<TradeActivity>(
                TradeActivity.ASSET_PAIR_EXTRA to assetPair
        )
        performIntent(intent)
    }

    fun openCreateOffer(baseAsset: Asset,
                        quoteAsset: Asset,
                        requiredPrice: BigDecimal? = null) {
        val intent = context?.intentFor<CreateOfferActivity>(
                CreateOfferActivity.BASE_ASSET_EXTRA to baseAsset,
                CreateOfferActivity.QUOTE_ASSET_EXTRA to quoteAsset,
                CreateOfferActivity.PRICE_STRING_EXTRA to requiredPrice
                        ?.let(BigDecimalUtil::toPlainString)
        )
        performIntent(intent)
    }

    fun openLimits() {
        val intent = context?.intentFor<LimitsActivity>()
        performIntent(intent)
    }

    fun openFees(asset: String? = null, feeType: Int = -1) {
        val intent = context?.intentFor<FeesActivity>(
                FeesActivity.EXTRA_ASSET to asset,
                FeesActivity.EXTRA_TYPE to feeType
        )
        performIntent(intent)
    }

    fun openDeposit(requestCode: Int, asset: String) {
        val intent = context?.intentFor<SingleFragmentActivity>(
                SingleFragmentActivity.ASSET_EXTRA to asset,
                SingleFragmentActivity.SCREEN_ID to DepositFragment.ID
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openWithdraw(requestCode: Int, asset: String) {
        val intent = context?.intentFor<SingleFragmentActivity>(
                SingleFragmentActivity.ASSET_EXTRA to asset,
                SingleFragmentActivity.SCREEN_ID to WithdrawFragment.ID
        )
        performIntent(intent, requestCode = requestCode)
    }

    fun openInvest(sale: SaleRecord) {
        val intent = context?.intentFor<SaleInvestActivity>(
                SaleInvestActivity.SALE_EXTRA to sale
        )
        performIntent(intent)
    }

    fun openAssetsExplorer() {
        val intent = context?.intentFor<SingleFragmentActivity>(
                SingleFragmentActivity.SCREEN_ID to ExploreAssetsFragment.ID
        )
        performIntent(intent)
    }

    fun openBalanceDetails(balanceId: String) {
        val intent = context?.intentFor<BalanceDetailsActivity>(
                BalanceDetailsActivity.BALANCE_ID_EXTRA to balanceId
        )
        performIntent(intent)
    }

    fun openAccountQrShare(walletInfo: WalletInfo) {
        val accountId = walletInfo.accountId
        openQrShare(
                data = accountId,
                title = context!!.getString(R.string.account_id_title),
                shareLabel = context!!.getString(R.string.share_account_id)
        )
    }

    fun openAtomicSwapBuy(assetCode: String,
                          askId: String) {
        val intent = context?.intentFor<BuyWithAtomicSwapActivity>(
                BuyWithAtomicSwapActivity.ASK_ID_EXTRA to askId,
                BuyWithAtomicSwapActivity.ASSET_CODE_EXTRA to assetCode
        )
        performIntent(intent)
    }

    fun openAtomicSwapsAsks(assetCode: String) {
        val intent = context?.intentFor<SingleFragmentActivity>(
                SingleFragmentActivity.ASSET_EXTRA to assetCode,
                SingleFragmentActivity.SCREEN_ID to AtomicSwapAsksFragment.ID
        )
        performIntent(intent)
    }
}
