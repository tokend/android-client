package io.tokend.template.test

import io.tokend.template.features.assets.logic.CreateBalanceUseCase
import io.tokend.template.features.signin.logic.PostSignInManager
import io.tokend.template.features.signin.logic.SignInUseCase
import io.tokend.template.features.systeminfo.storage.SystemInfoRepository
import io.tokend.template.features.urlconfig.model.UrlConfig
import io.tokend.template.logic.providers.UrlConfigProvider
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.*
import io.tokend.template.logic.session.Session
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.generated.resources.ReviewableRequestResource
import org.tokend.sdk.api.v3.assets.params.AssetsPageParams
import org.tokend.sdk.api.v3.requests.model.RequestState
import org.tokend.sdk.api.v3.requests.params.AssetRequestPageParams
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.sdk.utils.extentions.bitmask
import org.tokend.sdk.utils.extentions.decodeHex
import org.tokend.sdk.utils.extentions.encodeHexString
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import org.tokend.wallet.xdr.op_extensions.CreateFeeOp
import java.math.BigDecimal
import java.security.SecureRandom

object Util {
    fun getUrlConfigProvider(url: String = Config.API): UrlConfigProvider {
        return UrlConfigProviderFactory().createUrlConfigProvider(
            UrlConfig(url, "", "")
        )
    }

    fun getVerifiedWallet(
        email: String,
        password: CharArray,
        apiProvider: ApiProvider,
        session: Session,
        repositoryProvider: RepositoryProvider?
    ): WalletCreateResult {
        val createResult = apiProvider.getKeyServer()
            .createAndSaveWallet(email, password, apiProvider.getApi().v3.keyValue)
            .execute().get()

        println("Email is $email")
        println("Account id is " + createResult.rootAccount.accountId)
        println(
            "Password is " +
                    password.joinToString("")
        )

        SignInUseCase(
            email,
            password,
            apiProvider.getKeyServer(),
            session,
            null,
            null,
            repositoryProvider?.let { PostSignInManager(it)::doPostSignIn }
        ).perform().blockingAwait()

        return createResult
    }

    fun makeAccountGeneral(
        walletInfoProvider: WalletInfoProvider,
        apiProvider: ApiProvider,
        systemInfoRepository: SystemInfoRepository,
        txManager: TxManager
    ) {
        setAccountRole(
            "account_role:general", walletInfoProvider, apiProvider,
            systemInfoRepository, txManager
        )
    }

    fun makeAccountCorporate(
        walletInfoProvider: WalletInfoProvider,
        apiProvider: ApiProvider,
        systemInfoRepository: SystemInfoRepository,
        txManager: TxManager
    ) {
        setAccountRole(
            "account_role:corporate", walletInfoProvider, apiProvider,
            systemInfoRepository, txManager
        )
    }

    private fun setAccountRole(
        roleKey: String,
        walletInfoProvider: WalletInfoProvider,
        apiProvider: ApiProvider,
        systemInfoRepository: SystemInfoRepository,
        txManager: TxManager
    ) {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val api = apiProvider.getApi()

        val roleToSet = api
            .v3
            .keyValue
            .getById(roleKey)
            .execute()
            .get()
            .value
            .u32!!

        val netParams = systemInfoRepository
            .getNetworkParams()
            .blockingGet()

        val sourceAccount = Config.ADMIN_ACCOUNT

        val op = CreateChangeRoleRequestOp(
            requestID = 0,
            destinationAccount = PublicKeyFactory.fromAccountId(accountId),
            accountRoleToSet = roleToSet,
            creatorDetails = "{}",
            allTasks = 0,
            ext = CreateChangeRoleRequestOp.CreateChangeRoleRequestOpExt.EmptyVersion()
        )

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
            .addOperation(Operation.OperationBody.CreateChangeRoleRequest(op))
            .build()

        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()
    }

    fun getSomeMoney(
        asset: String,
        amount: BigDecimal,
        repositoryProvider: RepositoryProvider,
        accountProvider: AccountProvider,
        txManager: TxManager
    ): BigDecimal {
        val netParams = repositoryProvider.systemInfo.getNetworkParams().blockingGet()

        val hasBalance = repositoryProvider.balances
            .itemsList.find { it.assetCode == asset } != null

        if (!hasBalance) {
            CreateBalanceUseCase(
                asset,
                repositoryProvider.balances,
                repositoryProvider.systemInfo,
                accountProvider,
                txManager
            ).perform().blockingAwait()
        }

        val balanceId = repositoryProvider.balances
            .itemsList
            .find { it.assetCode == asset }!!
            .id

        val issuance = IssuanceRequest(
            asset,
            netParams.amountToPrecised(amount),
            PublicKeyFactory.fromBalanceId(balanceId),
            "{}",
            Fee(0, 0, Fee.FeeExt.EmptyVersion()),
            IssuanceRequest.IssuanceRequestExt.EmptyVersion()
        )

        val op = CreateIssuanceRequestOp(
            issuance,
            "${System.currentTimeMillis()}",
            0,
            CreateIssuanceRequestOp.CreateIssuanceRequestOpExt.EmptyVersion()
        )

        val sourceAccount = Config.ADMIN_ACCOUNT

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
            .addOperation(Operation.OperationBody.CreateIssuanceRequest(op))
            .build()
        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()

        repositoryProvider.balances.updateBalance(balanceId, amount)

        return amount
    }

    fun addFeeForAccount(
        rootAccountId: String,
        apiProvider: ApiProvider,
        txManager: TxManager,
        feeType: FeeType,
        feeSubType: Int = 0,
        asset: String
    ): Boolean {
        val sourceAccount = Config.ADMIN_ACCOUNT

        val netParams =
            apiProvider.getApi().general.getSystemInfo().execute().get().toNetworkParams()

        val fixedFee = netParams.amountToPrecised(BigDecimal("0.050000"))
        val percentFee = netParams.amountToPrecised(BigDecimal("0.001000"))
        val upperBound = netParams.amountToPrecised(BigDecimal.TEN)
        val lowerBound = netParams.amountToPrecised(BigDecimal.ONE)

        val feeOp =
            CreateFeeOp(
                feeType,
                asset,
                fixedFee,
                percentFee,
                upperBound,
                lowerBound,
                feeSubType.toLong(),
                accountId = rootAccountId
            )

        val op = Operation.OperationBody.SetFees(feeOp)

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
            .addOperation(op)
            .build()

        tx.addSignature(sourceAccount)

        val response = txManager.submit(tx).blockingGet()

        return response.isSuccess
    }

    fun createAsset(
        apiProvider: ApiProvider,
        txManager: TxManager,
        externalSystemType: String? = null
    ): String {
        val sourceAccount = Config.ADMIN_ACCOUNT

        val code = SecureRandom.getSeed(3).encodeHexString().toUpperCase()

        val systemInfo =
            apiProvider.getApi()
                .general
                .getSystemInfo()
                .execute()
                .get()
        val netParams = systemInfo.toNetworkParams()

        val statsAssetExists =
            apiProvider.getApi()
                .v3
                .assets
                .get(params = AssetsPageParams(policies = setOf(AssetPolicy.STATS_QUOTE_ASSET)))
                .execute()
                .get()
                .items
                .isNotEmpty()

        val assetDetailsJson = GsonFactory().getBaseGson().toJson(
            mapOf(
                "name" to "$code token",
                "external_system_type" to externalSystemType
            )
        )

        var policies = listOf(
            AssetPolicy.TRANSFERABLE.value,
            AssetPolicy.WITHDRAWABLE.value
        ).map(Int::toLong).bitmask().toInt()

        if (!statsAssetExists) {
            policies = policies or AssetPolicy.STATS_QUOTE_ASSET.value
        }

        val request = ManageAssetOp.ManageAssetOpRequest.CreateAssetCreationRequest(
            ManageAssetOp.ManageAssetOpRequest.ManageAssetOpCreateAssetCreationRequest(
                AssetCreationRequest(
                    code = code,
                    preissuedAssetSigner = PublicKeyFactory.fromAccountId(
                        systemInfo.adminAccountId
                    ),
                    maxIssuanceAmount = netParams.amountToPrecised(BigDecimal("10000")),
                    policies = policies,
                    initialPreissuedAmount = netParams.amountToPrecised(BigDecimal("10000")),
                    creatorDetails = assetDetailsJson,
                    ext = AssetCreationRequest.AssetCreationRequestExt.EmptyVersion(),
                    sequenceNumber = 0,
                    trailingDigitsCount = 6,
                    type = 0
                ),
                0,
                ManageAssetOp.ManageAssetOpRequest
                    .ManageAssetOpCreateAssetCreationRequest
                    .ManageAssetOpCreateAssetCreationRequestExt
                    .EmptyVersion()
            )
        )

        val manageOp = ManageAssetOp(
            0, request,
            ManageAssetOp.ManageAssetOpExt.EmptyVersion()
        )

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
            .addOperation(Operation.OperationBody.ManageAsset(manageOp))
            .build()

        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()

        val requestToReview =
            ApiProviderFactory().createApiProvider(
                urlConfigProvider = getUrlConfigProvider(),
                account = Config.ADMIN_ACCOUNT
            )
                .getSignedApi()
                .v3
                .requests
                .getAssetCreateRequests(
                    AssetRequestPageParams(
                        assetCode = code,
                        pagingParams = PagingParamsV2(
                            order = PagingOrder.DESC,
                            limit = 1
                        ),
                        includes = listOf(AssetRequestPageParams.Includes.ASSET)
                    )
                )
                .execute()
                .get()
                .items
                .firstOrNull()

        if (requestToReview != null) {
            reviewRequestIfNeeded(requestToReview, netParams, txManager)
        }

        return code
    }

    fun reviewRequestIfNeeded(
        requestToReview: ReviewableRequestResource,
        networkParams: NetworkParams,
        txManager: TxManager
    ) {
        if (requestToReview.stateI == RequestState.PENDING.i) {
            val requestType = ReviewableRequestType
                .values()
                .first { it.value == requestToReview.xdrType.value }

            val reviewOp = ReviewRequestOp(
                requestID = requestToReview.id.toLong(),
                requestHash = XdrByteArrayFixed32(requestToReview.hash.decodeHex()),
                action = ReviewRequestOpAction.APPROVE,
                reason = "",
                reviewDetails = ReviewDetails(
                    0, requestToReview.pendingTasks.toInt(),
                    "{}", ReviewDetails.ReviewDetailsExt.EmptyVersion()
                ),
                ext = ReviewRequestOp.ReviewRequestOpExt.EmptyVersion(),
                requestDetails = object :
                    ReviewRequestOp.ReviewRequestOpRequestDetails(requestType) {}
            )

            val reviewTx = TransactionBuilder(networkParams, Config.ADMIN_ACCOUNT.accountId)
                .addOperation(Operation.OperationBody.ReviewRequest(reviewOp))
                .addSigner(Config.ADMIN_ACCOUNT)
                .build()

            txManager.submit(reviewTx).blockingGet()
        }
    }

    private val random = java.util.Random()

    fun getEmail(): String {
        val adjectives = listOf(
            "adorable", "immortal", "quantum", "casual", "hierarchicalDeterministic",
            "fresh", "lovely", "strange", "sick", "creative", "lucky", "successful", "tired"
        )
        val nouns = listOf(
            "lawyer", "pumpkin", "wallet", "oleg", "dog", "tester", "pen",
            "robot", "think", "bottle", "flower", "AtbBag", "dungeonMaster", "kitten"
        )

        val salt = 1000 + random.nextInt(9000)

        val domain = "mail.com"

        val pickRandom: (List<String>) -> String = { it[random.nextInt(it.size)] }

        return "${pickRandom(adjectives)}+${pickRandom(nouns).capitalize()}+$salt@$domain"
    }
}