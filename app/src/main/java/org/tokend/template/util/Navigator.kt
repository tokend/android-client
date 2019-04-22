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
import org.tokend.template.R
import org.tokend.template.activities.MainActivity
import org.tokend.template.activities.SingleFragmentActivity
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.features.assets.AssetDetailsActivity
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.features.changepassword.ChangePasswordActivity
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.fees.FeesActivity
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.view.InvestmentConfirmationActivity
import org.tokend.template.features.invest.view.SaleActivity
import org.tokend.template.features.limits.LimitsActivity
import org.tokend.template.features.offers.CreateOfferActivity
import org.tokend.template.features.offers.OfferConfirmationActivity
import org.tokend.template.features.offers.OffersActivity
import org.tokend.template.features.offers.view.details.PendingInvestmentDetailsActivity
import org.tokend.template.features.offers.view.details.PendingOfferDetailsActivity
import org.tokend.template.features.qr.ShareQrActivity
import org.tokend.template.features.recovery.RecoveryActivity
import org.tokend.template.features.send.PaymentConfirmationActivity
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.signin.AuthenticatorSignInActivity
import org.tokend.template.features.signin.SignInActivity
import org.tokend.template.features.signup.RecoverySeedActivity
import org.tokend.template.features.signup.SignUpActivity
import org.tokend.template.features.trade.TradeActivity
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.wallet.details.*
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.WithdrawalConfirmationActivity
import org.tokend.template.features.withdraw.model.WithdrawalRequest

/**
 * Performs transitions between screens.
 * 'open-' will open related screen as a child.<p>
 * 'to-' will open related screen and finish current.
 */
class Navigator(private val source: Activity) {

    constructor(fragment: Fragment) : this(fragment.requireActivity())

    companion object {
        fun toSignIn(context: Context) {
            context.startActivity(context.intentFor<SignInActivity>()
                    .newTask())
        }
    }

    private fun fadeOut(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
        activity.overridePendingTransition(0, R.anim.activity_fade_out)
        activity.finish()
    }

    private fun createTransitionBundle(activity: Activity,
                                       vararg pairs: Pair<View?, String>): Bundle {
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
        source.startActivity(source.intentFor<SignUpActivity>())
    }

    fun openRecovery(email: String? = null) {
        source.startActivity(source.intentFor<RecoveryActivity>(
                RecoveryActivity.EMAIL_EXTRA to email
        ))
    }

    fun toSignIn(finishAffinity: Boolean = false) {
        source.startActivity(source.intentFor<SignInActivity>()
                .singleTop()
                .clearTop())
        if (finishAffinity) {
            source.setResult(Activity.RESULT_CANCELED, null)
            ActivityCompat.finishAffinity(source)
        } else {
            source.finish()
        }
    }

    fun toMainActivity() {
        source.startActivity(source.intentFor<MainActivity>())
        fadeOut(source)
    }

    fun openQrShare(title: String,
                    data: String,
                    shareLabel: String,
                    shareText: String? = data,
                    topText: String? = null) {
        source.startActivity(source.intentFor<ShareQrActivity>(
                ShareQrActivity.DATA_EXTRA to data,
                ShareQrActivity.TITLE_EXTRA to title,
                ShareQrActivity.SHARE_DIALOG_TEXT_EXTRA to shareLabel,
                ShareQrActivity.TOP_TEXT_EXTRA to topText,
                ShareQrActivity.SHARE_TEXT_EXTRA to shareText
        ))
    }

    fun openRecoverySeedSaving(requestCode: Int, seed: String) {
        source.startActivityForResult(source.intentFor<RecoverySeedActivity>(
                RecoverySeedActivity.SEED_EXTRA to seed
        ), requestCode)
    }

    fun openPasswordChange(requestCode: Int) {
        source.startActivityForResult(source.intentFor<ChangePasswordActivity>(),
                requestCode)
    }

    fun openWithdrawalConfirmation(requestCode: Int,
                                   withdrawalRequest: WithdrawalRequest) {
        val confirmationIntent = Intent(source, WithdrawalConfirmationActivity::class.java)
                .putExtra(WithdrawalConfirmationActivity.WITHDRAWAL_REQUEST_EXTRA, withdrawalRequest)
        source.startActivityForResult(confirmationIntent, requestCode)
    }

    fun openWallet(requestCode: Int, asset: String) {
        source.startActivityForResult(
                Intent(source, SingleFragmentActivity::class.java)
                        .putExtra(SingleFragmentActivity.ASSET_EXTRA, asset)
                        .putExtra(SingleFragmentActivity.SCREEN_ID, WalletFragment.ID)
                , requestCode)
    }

    fun openSend(asset: String, requestCode: Int) {
        val toSend = Intent(source, SingleFragmentActivity::class.java)
                .putExtra(SingleFragmentActivity.SCREEN_ID, SendFragment.ID)
                .putExtra(SingleFragmentActivity.ASSET_EXTRA, asset)

        source.startActivityForResult(toSend, requestCode)
    }

    fun openAssetDetails(requestCode: Int,
                         asset: AssetRecord,
                         cardView: View? = null) {
        val transitionBundle = createTransitionBundle(source,
                cardView to source.getString(R.string.transition_asset_card)
        )
        source.startActivityForResult(source.intentFor<AssetDetailsActivity>(
                AssetDetailsActivity.ASSET_EXTRA to asset
        ), requestCode, transitionBundle)
    }

    fun openPaymentConfirmation(requestCode: Int, paymentRequest: PaymentRequest) {
        val confirmationIntent = Intent(source, PaymentConfirmationActivity::class.java)
                .putExtra(PaymentConfirmationActivity.PAYMENT_REQUEST_EXTRA, paymentRequest)
        source.startActivityForResult(confirmationIntent, requestCode)
    }

    fun openOfferConfirmation(requestCode: Int, offer: OfferRecord) {
        source.startActivityForResult(source.intentFor<OfferConfirmationActivity>(
                OfferConfirmationActivity.OFFER_EXTRA to offer
        ), requestCode)
    }

    fun openInvestmentConfirmation(requestCode: Int,
                                   offer: OfferRecord,
                                   offerToCancel: OfferRecord? = null,
                                   displayToReceive: Boolean = true,
                                   saleName: String? = null) {
        source.startActivityForResult(
                Intent(source, InvestmentConfirmationActivity::class.java)
                        .putExtra(OfferConfirmationActivity.OFFER_EXTRA, offer)
                        .putExtra(OfferConfirmationActivity.OFFER_TO_CANCEL_EXTRA, offerToCancel)
                        .putExtra(InvestmentConfirmationActivity.DISPLAY_TO_RECEIVE, displayToReceive)
                        .putExtra(InvestmentConfirmationActivity.SALE_NAME_EXTRA, saleName
                        ),
                requestCode
        )
    }

    fun openPendingOffers(requestCode: Int = 0, onlyPrimary: Boolean = false) {
        source.startActivityForResult(source.intentFor<OffersActivity>(
                OffersActivity.ONLY_PRIMARY_EXTRA to onlyPrimary
        ), requestCode)
    }

    fun openSale(requestCode: Int, sale: SaleRecord) {
        source.startActivityForResult(source.intentFor<SaleActivity>(
                SaleActivity.SALE_EXTRA to sale
        ), requestCode)
    }

    fun openAuthenticatorSignIn(requestCode: Int) {
        source.startActivityForResult(
                source.intentFor<AuthenticatorSignInActivity>(),
                requestCode
        )
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

        source.startActivity(
                Intent(source, activityClass).apply {
                    putExtra(BalanceChangeDetailsActivity.BALANCE_CHANGE_EXTRA, change)
                }
        )
    }

    fun openPendingOfferDetails(offer: OfferRecord,
                                requestCode: Int = 0) {
        val activityClass =
                if (offer.isInvestment)
                    PendingInvestmentDetailsActivity::class.java
                else
                    PendingOfferDetailsActivity::class.java

        source.startActivityForResult(
                Intent(source, activityClass).apply {
                    putExtra(PendingOfferDetailsActivity.OFFER_EXTRA, offer)
                },
                requestCode
        )
    }

    fun openTrade(assetPair: AssetPairRecord) {
        source.startActivity(
                Intent(source, TradeActivity::class.java).apply {
                    putExtra(TradeActivity.ASSET_PAIR_EXTRA, assetPair)
                }
        )
    }

    fun openCreateOffer(offer: OfferRecord) {
        source.startActivity(
                Intent(source, CreateOfferActivity::class.java).apply {
                    putExtra(CreateOfferActivity.EXTRA_OFFER, offer)
                }
        )
    }

    fun openLimits() {
        source.startActivity(Intent(source, LimitsActivity::class.java))
    }

    fun openFees() {
        source.startActivity(Intent(source, FeesActivity::class.java))
    }

    fun openDeposit(requestCode: Int, asset: String) {
        source.startActivityForResult(
                Intent(source, SingleFragmentActivity::class.java)
                        .putExtra(SingleFragmentActivity.ASSET_EXTRA, asset)
                        .putExtra(SingleFragmentActivity.SCREEN_ID, DepositFragment.ID)
                , requestCode)
    }

    fun openWithdraw(requestCode: Int, asset: String) {
        source.startActivityForResult(
                Intent(source, SingleFragmentActivity::class.java)
                        .putExtra(SingleFragmentActivity.ASSET_EXTRA, asset)
                        .putExtra(SingleFragmentActivity.SCREEN_ID, WithdrawFragment.ID)
                , requestCode)
    }
}