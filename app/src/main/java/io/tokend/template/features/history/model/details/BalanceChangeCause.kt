package io.tokend.template.features.history.model.details

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.SimpleAsset
import io.tokend.template.features.history.model.SimpleFeeRecord
import io.tokend.template.features.send.model.PaymentRequest
import io.tokend.template.util.RecordWithPolicy
import org.tokend.sdk.api.generated.resources.*
import org.tokend.wallet.xdr.AssetPairPolicy
import java.io.Serializable
import java.math.BigDecimal

/**
 * Financial action resulted in the balance change
 */
sealed class BalanceChangeCause : Serializable {
    object Unknown : BalanceChangeCause()

    // ------- AML Alert -------- //

    class AmlAlert(
        val reason: String?
    ) : BalanceChangeCause() {
        constructor(op: CreateAmlAlertRequestOpResource) : this(
            reason = op.creatorDetails?.get("reason")?.asText()?.takeIf { it.isNotEmpty() }
        )
    }

    // ------- Offer -------- //

    open class Offer(
        val offerId: Long?,
        val orderBookId: Long,
        val price: BigDecimal,
        val isBuy: Boolean,
        val baseAmount: BigDecimal,
        val baseAssetCode: String,
        val quoteAssetCode: String,
        val fee: SimpleFeeRecord
    ) : BalanceChangeCause() {
        constructor(op: ManageOfferOpResource) : this(
            offerId = op.offerId,
            orderBookId = op.orderBookId,
            price = op.price,
            isBuy = op.isBuy,
            baseAmount = op.baseAmount,
            baseAssetCode = op.baseAsset.id,
            quoteAssetCode = op.quoteAsset.id,
            fee = SimpleFeeRecord(op.fee)
        )

        val quoteAmount: BigDecimal = baseAmount * price

        val isInvestment: Boolean
            get() = orderBookId != 0L
    }

    // ------- Offer Match -------- //

    open class MatchedOffer(
        offerId: Long,
        orderBookId: Long,
        price: BigDecimal,
        isBuy: Boolean,
        baseAmount: BigDecimal,
        baseAssetCode: String,
        quoteAssetCode: String,
        fee: SimpleFeeRecord,
        val charged: ParticularBalanceChangeDetails,
        val funded: ParticularBalanceChangeDetails

    ) : Offer(offerId, orderBookId, price, isBuy, baseAmount, baseAssetCode, quoteAssetCode, fee) {
        constructor(
            op: ManageOfferOpResource,
            effect: EffectMatchedResource
        ) : this(
            offerId = effect.offerId,
            orderBookId = effect.orderBookId,
            price = effect.price,
            isBuy = op.isBuy,
            baseAmount = op.baseAmount,
            baseAssetCode = op.baseAsset.id,
            quoteAssetCode = op.quoteAsset.id,
            fee = SimpleFeeRecord(op.fee),
            charged = ParticularBalanceChangeDetails(effect.charged),
            funded = ParticularBalanceChangeDetails(effect.funded)
        )

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
        baseAmount: BigDecimal,
        baseAssetCode: String,
        quoteAssetCode: String,
        fee: SimpleFeeRecord,
        charged: ParticularBalanceChangeDetails,
        funded: ParticularBalanceChangeDetails
    ) : MatchedOffer(
        offerId, orderBookId, price, true,
        baseAmount, baseAssetCode, quoteAssetCode, fee, charged, funded
    ) {
        constructor(effect: EffectMatchedResource) : this(
            offerId = effect.offerId,
            orderBookId = effect.orderBookId,
            price = effect.price,
            baseAmount = effect.funded.amount, // Investments are always fund in base asset
            baseAssetCode = effect.funded.assetCode,
            quoteAssetCode = effect.charged.assetCode, // Investments always charge in quote asset
            fee = SimpleFeeRecord(effect.charged.fee), // Fee is always paid in quote asset
            charged = ParticularBalanceChangeDetails(effect.charged),
            funded = ParticularBalanceChangeDetails(effect.funded)
        )
    }

    // ------- Sale cancellation -------- //

    object SaleCancellation : BalanceChangeCause()

    // ------- Offer cancellation -------- //

    object OfferCancellation : BalanceChangeCause()

    // ------- Issuance -------- //

    class Issuance(
        val cause: String?,
        val reference: String?
    ) : BalanceChangeCause() {
        constructor(op: CreateIssuanceRequestOpResource) : this(
            cause = op.creatorDetails?.get("cause")?.asText(),
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
        var sourceName: String?,
        var destName: String?
    ) : BalanceChangeCause() {
        constructor(op: PaymentOpResource) : this(
            sourceAccountId = op.accountFrom.id,
            destAccountId = op.accountTo.id,
            destFee = SimpleFeeRecord(op.destinationFee),
            sourceFee = SimpleFeeRecord(op.sourceFee),
            isDestFeePaidBySource = op.sourcePayForDestination(),
            subject = op.subject.takeIf { it.isNotEmpty() },
            destName = null,
            sourceName = null
        )

        constructor(request: PaymentRequest) : this(
            sourceAccountId = request.senderAccountId,
            destAccountId = request.recipient.accountId,
            subject = request.paymentSubject,
            sourceFee = request.fee.senderFee,
            destFee =
            if (request.fee.senderPaysForRecipient)
                SimpleFeeRecord.ZERO
            else
                request.fee.recipientFee,
            isDestFeePaidBySource = request.fee.senderPaysForRecipient,
            destName = request.recipient.nickname,
            sourceName = null
        )

        /**
         * @return true if given [accountId] is the receiver of the payment
         */
        fun isReceived(accountId: String): Boolean {
            return destAccountId == accountId
        }

        /**
         * @return receiver or sender account ID based on [yourAccountId]
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

        /**
         * Sets receiver or sender name based on [yourAccountId]
         *
         * @see isReceived
         */
        fun setCounterpartyName(
            yourAccountId: String,
            counterpartyName: String
        ) {
            if (isReceived(yourAccountId)) {
                sourceName = counterpartyName
            } else {
                destName = counterpartyName
            }
        }
    }

    // ------- WithdrawalRequest -------- //

    class WithdrawalRequest(
        val destinationAddress: String
    ) : BalanceChangeCause() {
        constructor(op: CreateWithdrawRequestOpResource) : this(
            destinationAddress = op.creatorDetails.get("address").asText()
        )
    }

    // ------- Physical/current price restriction or policy update for asset pair ------ //
    class AssetPairUpdate(
        val baseAsset: Asset,
        val quoteAsset: Asset,
        val physicalPrice: BigDecimal,
        override val policy: Int
    ) : BalanceChangeCause(), RecordWithPolicy {

        constructor(op: ManageAssetPairOpResource) : this(
            baseAsset = SimpleAsset(op.baseAsset),
            quoteAsset = SimpleAsset(op.quoteAsset),
            physicalPrice = op.physicalPrice,
            policy = op.policies?.value ?: 0
        )

        val isRestrictedByCurrentPrice: Boolean
            get() = hasPolicy(AssetPairPolicy.CURRENT_PRICE_RESTRICTION.value)

        val isRestrictedByPhysicalPrice: Boolean
            get() = hasPolicy(AssetPairPolicy.PHYSICAL_PRICE_RESTRICTION.value)

        val isTradeable: Boolean
            get() = hasPolicy(AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value)
    }

    // ------- Atomic swap ask creation ------- //
    object AtomicSwapAskCreation : BalanceChangeCause()

    // ------- Atomic swap bid creation ------- //
    object AtomicSwapBidCreation : BalanceChangeCause()
}