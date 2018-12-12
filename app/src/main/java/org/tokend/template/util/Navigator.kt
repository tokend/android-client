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
import org.tokend.sdk.api.base.model.operations.*
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.R
import org.tokend.template.activities.MainActivity
import org.tokend.template.activities.SingleFragmentActivity
import org.tokend.template.extensions.Asset
import org.tokend.template.extensions.Sale
import org.tokend.template.features.assets.AssetDetailsActivity
import org.tokend.template.features.changepassword.ChangePasswordActivity
import org.tokend.template.features.invest.activities.InvestmentConfirmationActivity
import org.tokend.template.features.invest.activities.InvestmentDetailsActivity
import org.tokend.template.features.invest.activities.SaleActivity
import org.tokend.template.features.invest.saledetails.SaleDetailsActivity
import org.tokend.template.features.offers.OfferConfirmationActivity
import org.tokend.template.features.offers.OffersActivity
import org.tokend.template.features.qr.ShareQrActivity
import org.tokend.template.features.recovery.RecoveryActivity
import org.tokend.template.features.send.PaymentConfirmationActivity
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.signin.AuthenticatorSignInActivity
import org.tokend.template.features.signin.SignInActivity
import org.tokend.template.features.signup.RecoverySeedActivity
import org.tokend.template.features.signup.SignUpActivity
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.wallet.txdetails.*
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
                         asset: Asset,
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

    fun openOfferConfirmation(fragment: Fragment, requestCode: Int,
                              offer: Offer) {
        fragment.startActivityForResult(fragment.context?.intentFor<OfferConfirmationActivity>(
                OfferConfirmationActivity.OFFER_EXTRA to offer
        ), requestCode)
    }

    fun openInvestmentConfirmation(activity: Activity, requestCode: Int,
                                   offer: Offer, offerToCancel: Offer? = null,
                                   displayToReceive: Boolean = true,
                                   assetName: String? = null) {
        activity.startActivityForResult(activity.intentFor<InvestmentConfirmationActivity>(
                OfferConfirmationActivity.OFFER_EXTRA to offer,
                OfferConfirmationActivity.OFFER_TO_CANCEL_EXTRA to offerToCancel,
                InvestmentConfirmationActivity.DISPLAY_TO_RECEIVE to displayToReceive,
                InvestmentConfirmationActivity.ASSET_NAME_EXTRA to assetName
        ), requestCode)
    }

    fun openPendingOffers(fragment: Fragment, requestCode: Int,
                          onlyPrimary: Boolean = false) {
        fragment.startActivityForResult(fragment.context?.intentFor<OffersActivity>(
                OffersActivity.ONLY_PRIMARY_EXTRA to onlyPrimary
        ), requestCode)
    }

    fun openSale(fragment: Fragment, requestCode: Int, sale: Sale) {
        fragment.startActivityForResult(fragment.requireContext().intentFor<SaleActivity>(
                SaleActivity.SALE_JSON_EXTRA to GsonFactory().getBaseGson().toJson(sale)
        ), requestCode)
    }

    fun openSaleDetails(activity: Activity, sale: Sale) {
        activity.startActivity(activity.intentFor<SaleDetailsActivity>(
                SaleDetailsActivity.SALE_JSON_EXTRA to GsonFactory().getBaseGson().toJson(sale)
        ))
    }

    fun openTransactionDetails(activity: Activity, tx: TransferOperation) {
        when (tx) {
            is PaymentOperation ->
                TxDetailsActivity
                        .start<PaymentDetailsActivity, PaymentOperation>(activity, tx)
            is IssuanceOperation ->
                TxDetailsActivity
                        .start<DepositDetailsActivity, IssuanceOperation>(activity, tx)
            is WithdrawalOperation ->
                TxDetailsActivity
                        .start<WithdrawalDetailsActivity, WithdrawalOperation>(activity, tx)
            is InvestmentOperation ->
                TxDetailsActivity
                        .start<InvestmentDetailsActivity, InvestmentOperation>(activity, tx)
            is OfferMatchOperation ->
                TxDetailsActivity
                        .start<OfferMatchDetailsActivity, OfferMatchOperation>(activity, tx)
            else ->
                (tx as? BaseTransferOperation)?.let {
                    TxDetailsActivity
                            .start<UnknownTxDetailsActivity, BaseTransferOperation>(activity, it)
                }
        }
    }

    fun openAuthenticatorSignIn(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(
                activity.intentFor<AuthenticatorSignInActivity>(),
                requestCode
        )
    }
}