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
import org.tokend.sdk.api.models.Asset
import org.tokend.sdk.api.models.Offer
import org.tokend.template.R
import org.tokend.template.base.activities.*
import org.tokend.template.base.activities.qr.ShareQrActivity
import org.tokend.template.base.activities.signup.RecoverySeedActivity
import org.tokend.template.base.activities.signup.SignUpActivity
import org.tokend.template.base.fragments.SendFragment
import org.tokend.template.base.fragments.WalletFragment
import org.tokend.template.base.logic.payment.PaymentRequest
import org.tokend.template.features.explore.AssetDetailsActivity
import org.tokend.template.features.trade.OfferConfirmationActivity
import org.tokend.template.features.trade.adapter.OffersActivity
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

    fun toSignIn(activity: Activity) {
        activity.startActivity(activity.intentFor<SignInActivity>()
                .singleTop()
                .clearTop())
        activity.setResult(Activity.RESULT_CANCELED, null)
        ActivityCompat.finishAffinity(activity)
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

    fun openWallet(fragment: Fragment, asset: String) {
        val toSend = Intent(fragment.context, SingleFragmentActivity::class.java)
                .putExtra(SingleFragmentActivity.ASSET_EXTRA, asset)
                .putExtra(SingleFragmentActivity.SCREEN_ID, WalletFragment.ID)

        fragment.startActivity(toSend)
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

    fun openPendingOffers(fragment: Fragment, requestCode: Int) {
        fragment.startActivityForResult(fragment.context?.intentFor<OffersActivity>(
        ), requestCode)
    }

}