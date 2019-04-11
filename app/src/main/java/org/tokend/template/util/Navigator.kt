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
import org.tokend.template.features.fees.FeesActivity
import org.tokend.template.features.invest.activities.InvestmentConfirmationActivity
import org.tokend.template.features.invest.activities.SaleActivity
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.saledetails.SaleDetailsActivity
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
import org.tokend.template.features.withdraw.WithdrawalConfirmationActivity
import org.tokend.template.features.withdraw.model.WithdrawalRequest

/**
 * Performs transitions between screens.
 * 'open-' will open related screen as a child.<p>
 * 'to-' will open related screen and finish current.
 */
object Navigator {

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

    fun openSignUp(activity: Activity) {
        activity.startActivity(activity.intentFor<SignUpActivity>())
    }

    fun openRecovery(activity: Activity,
                     email: String? = null) {
        activity.startActivity(activity.intentFor<RecoveryActivity>(
                RecoveryActivity.EMAIL_EXTRA to email
        ))
    }

    fun toSignIn(context: Context) {
        context.startActivity(context.intentFor<SignInActivity>()
                .newTask())
    }

    fun toSignIn(activity: Activity, finishAffinity: Boolean = false) {
        activity.startActivity(activity.intentFor<SignInActivity>()
                .singleTop()
                .clearTop())
        if (finishAffinity) {
            activity.setResult(Activity.RESULT_CANCELED, null)
            ActivityCompat.finishAffinity(activity)
        } else {
            activity.finish()
        }
    }

    fun toMainActivity(activity: Activity) {
        activity.startActivity(activity.intentFor<MainActivity>())
        fadeOut(activity)
    }

    fun openQrShare(activity: Activity,
                    title: String,
                    data: String,
                    shareLabel: String,
                    shareText: String? = data,
                    topText: String? = null) {
        activity.startActivity(activity.intentFor<ShareQrActivity>(
                ShareQrActivity.DATA_EXTRA to data,
                ShareQrActivity.TITLE_EXTRA to title,
                ShareQrActivity.SHARE_DIALOG_TEXT_EXTRA to shareLabel,
                ShareQrActivity.TOP_TEXT_EXTRA to topText,
                ShareQrActivity.SHARE_TEXT_EXTRA to shareText
        ))
    }

    fun openRecoverySeedSaving(activity: Activity, requestCode: Int, seed: String) {
        activity.startActivityForResult(activity.intentFor<RecoverySeedActivity>(
                RecoverySeedActivity.SEED_EXTRA to seed
        ), requestCode)
    }

    fun openPasswordChange(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(activity.intentFor<ChangePasswordActivity>(),
                requestCode)
    }

    fun openWithdrawalConfirmation(fragment: Fragment, requestCode: Int,
                                   withdrawalRequest: WithdrawalRequest) {
        val confirmationIntent = Intent(fragment.context, WithdrawalConfirmationActivity::class.java)
                .putExtra(WithdrawalConfirmationActivity.WITHDRAWAL_REQUEST_EXTRA, withdrawalRequest)
        fragment.startActivityForResult(confirmationIntent, requestCode)
    }

    fun openWallet(fragment: Fragment, requestCode: Int, asset: String) {
        fragment.startActivityForResult(
                Intent(fragment.context, SingleFragmentActivity::class.java)
                        .putExtra(SingleFragmentActivity.ASSET_EXTRA, asset)
                        .putExtra(SingleFragmentActivity.SCREEN_ID, WalletFragment.ID)
                , requestCode)
    }

    fun openSend(fragment: Fragment, asset: String,
                 requestCode: Int) {
        val toSend = Intent(fragment.context, SingleFragmentActivity::class.java)
                .putExtra(SingleFragmentActivity.SCREEN_ID, SendFragment.ID)
                .putExtra(SingleFragmentActivity.ASSET_EXTRA, asset)

        fragment.startActivityForResult(toSend, requestCode)
    }

    fun openAssetDetails(fragment: Fragment, requestCode: Int,
                         asset: AssetRecord,
                         cardView: View? = null) {
        val transitionBundle = createTransitionBundle(fragment.activity!!,
                cardView to fragment.activity!!.getString(R.string.transition_asset_card)
        )
        fragment.startActivityForResult(fragment.context!!.intentFor<AssetDetailsActivity>(
                AssetDetailsActivity.ASSET_EXTRA to asset
        ), requestCode, transitionBundle)
    }

    fun openPaymentConfirmation(fragment: Fragment, requestCode: Int,
                                paymentRequest: PaymentRequest) {
        val confirmationIntent = Intent(fragment.context, PaymentConfirmationActivity::class.java)
                .putExtra(PaymentConfirmationActivity.PAYMENT_REQUEST_EXTRA, paymentRequest)
        fragment.startActivityForResult(confirmationIntent, requestCode)
    }

    fun openOfferConfirmation(activity: Activity, requestCode: Int,
                              offer: OfferRecord) {
        activity.startActivityForResult(activity.intentFor<OfferConfirmationActivity>(
                OfferConfirmationActivity.OFFER_EXTRA to offer
        ), requestCode)
    }

    fun openInvestmentConfirmation(activity: Activity,
                                   requestCode: Int,
                                   offer: OfferRecord,
                                   offerToCancel: OfferRecord? = null,
                                   displayToReceive: Boolean = true,
                                   assetName: String? = null) {
        activity.startActivityForResult(activity.intentFor<InvestmentConfirmationActivity>(
                OfferConfirmationActivity.OFFER_EXTRA to offer,
                OfferConfirmationActivity.OFFER_TO_CANCEL_EXTRA to offerToCancel,
                InvestmentConfirmationActivity.DISPLAY_TO_RECEIVE to displayToReceive,
                InvestmentConfirmationActivity.ASSET_NAME_EXTRA to assetName
        ), requestCode)
    }

    fun openPendingOffers(fragment: Fragment, requestCode: Int = 0,
                          onlyPrimary: Boolean = false) {
        fragment.startActivityForResult(fragment.context?.intentFor<OffersActivity>(
                OffersActivity.ONLY_PRIMARY_EXTRA to onlyPrimary
        ), requestCode)
    }

    fun openSale(fragment: Fragment, requestCode: Int, sale: SaleRecord) {
        fragment.startActivityForResult(fragment.requireContext().intentFor<SaleActivity>(
                SaleActivity.SALE_EXTRA to sale
        ), requestCode)
    }

    fun openSaleDetails(activity: Activity, sale: SaleRecord) {
        activity.startActivity(activity.intentFor<SaleDetailsActivity>(
                SaleDetailsActivity.SALE_EXTRA to sale
        ))
    }

    fun openAuthenticatorSignIn(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(
                activity.intentFor<AuthenticatorSignInActivity>(),
                requestCode
        )
    }

    fun openBalanceChangeDetails(activity: Activity,
                                 change: BalanceChange) {
        val activityClass = when (change.cause) {
            is BalanceChangeCause.AmlAlert -> AmlAlertDetailsActivity::class.java
            is BalanceChangeCause.Investment -> InvestmentDetailsActivity::class.java
            is BalanceChangeCause.MatchedOffer -> OfferMatchDetailsActivity::class.java
            is BalanceChangeCause.Issuance -> IssuanceDetailsActivity::class.java
            is BalanceChangeCause.Payment -> PaymentDetailsActivity::class.java
            is BalanceChangeCause.Withdrawal -> WithdrawalDetailsActivity::class.java
            is BalanceChangeCause.Offer -> {
                openPendingOfferDetails(
                        activity,
                        OfferRecord.fromBalanceChange(change)
                )
                return
            }
            is BalanceChangeCause.SaleCancellation -> SaleCancellationDetailsActivity::class.java
            is BalanceChangeCause.OfferCancellation -> OfferCancellationDetailsActivity::class.java
            is BalanceChangeCause.AssetPairUpdate -> AssetPairUpdateDetailsActivity::class.java
            is BalanceChangeCause.Unknown -> UnknownDetailsActivity::class.java
        }

        activity.startActivity(
                Intent(activity, activityClass).apply {
                    putExtra(BalanceChangeDetailsActivity.BALANCE_CHANGE_EXTRA, change)
                }
        )
    }

    fun openPendingOfferDetails(activity: Activity,
                                offer: OfferRecord,
                                requestCode: Int = 0) {
        val activityClass =
                if (offer.isInvestment)
                    PendingInvestmentDetailsActivity::class.java
                else
                    PendingOfferDetailsActivity::class.java

        activity.startActivityForResult(
                Intent(activity, activityClass).apply {
                    putExtra(PendingOfferDetailsActivity.OFFER_EXTRA, offer)
                },
                requestCode
        )
    }

    fun openTrade(fragment: Fragment,
                  assetPair: AssetPairRecord) {
        fragment.startActivity(
                Intent(fragment.requireContext(), TradeActivity::class.java).apply {
                    putExtra(TradeActivity.ASSET_PAIR_EXTRA, assetPair)
                }
        )
    }

    fun openCreateOffer(fragment: Fragment, offer: OfferRecord) {
        fragment.startActivity(
                Intent(fragment.requireContext(), CreateOfferActivity::class.java).apply {
                    putExtra(CreateOfferActivity.EXTRA_OFFER, offer)
                }
        )
    }
  
    fun openLimits(fragment: Fragment) {
        fragment.startActivity(Intent(fragment.requireContext(), LimitsActivity::class.java))
    }

    fun openFees(fragment: Fragment) {
        fragment.startActivity(Intent(fragment.requireContext(), FeesActivity::class.java))
    }
}