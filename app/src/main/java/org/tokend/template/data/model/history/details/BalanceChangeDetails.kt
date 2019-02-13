package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.*
import org.tokend.template.data.model.history.SimpleFeeRecord
import java.io.Serializable
import java.math.BigDecimal

sealed class BalanceChangeDetails : Serializable {
    object Unknown : BalanceChangeDetails()

    // ------- AML Alert -------- //

    class AmlAlert(
            val reason: String?
    ) : BalanceChangeDetails() {
        constructor(op: OpCreateAMLAlertRequestDetailsResource) : this(
                reason = op.reason.takeIf { it.isNotEmpty() }
        )
    }

    // ------- Offer Match -------- //

    open class OfferMatch(
            val offerId: Long,
            val orderBookId: Long,
            val price: BigDecimal,
            val isBuy: Boolean,
            val quoteAssetCode: String,
            val charged: ParticularBalanceChangeDetails,
            val funded: ParticularBalanceChangeDetails

    ) : BalanceChangeDetails() {
        constructor(op: OpManageOfferDetailsResource,
                    effect: EffectMatchedResource) : this(
                offerId = effect.offerId,
                orderBookId = effect.orderBookId,
                price = effect.price,
                isBuy = op.isBuy,
                quoteAssetCode = op.quoteAsset.id,
                charged = ParticularBalanceChangeDetails(effect.charged),
                funded = ParticularBalanceChangeDetails(effect.funded)
        )

        val chargedInQuote = charged.assetCode == quoteAssetCode

        val fundedInQuote = funded.assetCode == quoteAssetCode

        val baseAssetCode =
                if (chargedInQuote)
                    funded.assetCode
                else
                    charged.assetCode

        val isPrimaryMarket = orderBookId != 0L

        /**
         * @return true if given balance was funded by this match
         */
        fun isReceivedByBalance(balanceId: String): Boolean {
            return funded.balanceId == balanceId
        }

        /**
         * @return true if this match has funded in given asset
         */
        fun isReceivedByAsset(assetCode: String): Boolean {
            return funded.assetCode == assetCode
        }
    }

    // ------- Investment -------- //

    class Investment(
            offerId: Long,
            orderBookId: Long,
            price: BigDecimal,
            quoteAssetCode: String,
            charged: ParticularBalanceChangeDetails,
            funded: ParticularBalanceChangeDetails
    ) : OfferMatch(offerId, orderBookId, price, true, quoteAssetCode, charged, funded) {
        constructor(effect: EffectMatchedResource) : this(
                offerId = effect.offerId,
                orderBookId = effect.orderBookId,
                price = effect.price,
                quoteAssetCode = effect.charged.assetCode, // Investments always charge in quote asset
                charged = ParticularBalanceChangeDetails(effect.charged),
                funded = ParticularBalanceChangeDetails(effect.funded)
        )
    }

    // ------- Issuance -------- //

    class Issuance(
            val cause: String?,
            val reference: String?
    ): BalanceChangeDetails() {
        constructor(op: OpCreateIssuanceRequestDetailsResource): this(
                cause = op.externalDetails?.get("cause")?.asText(),
                reference = op.reference
        )
    }

    // ------- Payment -------- //

    class Payment(
            val sourceAccountId: String,
            val destAccountId: String,
            val sourceFee: SimpleFeeRecord,
            val destFee: SimpleFeeRecord,
            val isDestFeePaidBySource: Boolean,
            val subject: String?,
            val reference: String?,
            val sourceName: String?,
            val destName: String?
    ) : BalanceChangeDetails() {
        constructor(op: OpPaymentDetailsResource): this(
                sourceAccountId = op.accountFrom.id,
                destAccountId = op.accountTo.id,
                destFee = SimpleFeeRecord(op.destinationFee),
                sourceFee = SimpleFeeRecord(op.sourceFee),
                isDestFeePaidBySource = op.sourcePayForDestination(),
                reference = op.reference,
                subject = op.subject.takeIf { it.isNotEmpty() },
                destName = null,
                sourceName = null
        )

        /**
         * @return true if given [accountId] is the receiver of the payment
         */
        fun isReceived(accountId: String): Boolean {
            return destAccountId == accountId
        }

        /**
         * @return receiver or sender account ID on [yourAccountId]
         *
         * @see isReceived
         */
        fun getCounterpartyAccountId(yourAccountId: String): String {
            return if (isReceived(yourAccountId))
                sourceAccountId
            else
                destAccountId
        }

        /**
         * @return receiver or sender name if it's set based on [yourAccountId]
         *
         * @see isReceived
         */
        fun getCounterpartyName(yourAccountId: String): String? {
            return if (isReceived(yourAccountId))
                sourceName
            else
                destName
        }
    }

    // ------- Payout -------- //

    class Payout(
            /**
             *  Max amount of asset, that owner wants to pay out
             */
            val maxPayoutAmount: BigDecimal,
            /**
             * Min tokens amount which will be payed for one balance
             */
            val minPayoutAmount: BigDecimal,
            /**
             * Min tokens amount for which holder will received dividends
             */
            val minAssetHolderAmount: BigDecimal,

            /**
             * Amount of tokens that were actually payed out
             */
            val actualPayoutAmount: BigDecimal,

            val expectedFee: SimpleFeeRecord,
            val actualFee: SimpleFeeRecord,
            val assetCode: String,
            val sourceAccountId: String
    ) : BalanceChangeDetails() {
        constructor(op: OpPayoutDetailsResource) : this(
                maxPayoutAmount = op.maxPayoutAmount,
                minPayoutAmount = op.minPayoutAmount,
                minAssetHolderAmount = op.minAssetHolderAmount,
                expectedFee = SimpleFeeRecord(op.expectedFee),
                actualFee = SimpleFeeRecord(op.actualFee),
                actualPayoutAmount = op.actualPayoutAmount,
                assetCode = op.asset.id,
                sourceAccountId = op.sourceAccount.id
        )

        /**
         * @return true if account with given ID is an issuer of the payout
         */
        fun isIssuer(accountId: String): Boolean {
            return sourceAccountId == accountId
        }
    }

    // ------- Withdrawal -------- //

    class Withdrawal(
            val destinationAddress: String
    ): BalanceChangeDetails() {
        constructor(op: OpCreateWithdrawRequestDetailsResource): this(
                destinationAddress = op.externalDetails.get("address").asText()
        )
    }
}