package org.tokend.template.data.repository

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.AccountsApiV3
import org.tokend.sdk.api.v3.balances.BalancesApi
import org.tokend.sdk.api.v3.balances.params.ConvertedBalancesParams
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.model.SimpleAsset
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.mapSuccessful
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.op_extensions.CreateBalanceOp

class BalancesRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val urlConfigProvider: UrlConfigProvider,
        private val mapper: ObjectMapper,
        private val conversionAssetCode: String?,
        itemsCache: RepositoryCache<BalanceRecord>
) : SimpleMultipleItemsRepository<BalanceRecord>(itemsCache) {

    var conversionAsset: Asset? = null

    override fun getItems(): Single<List<BalanceRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return if (conversionAssetCode != null)
            getConvertedBalances(
                    signedApi.v3.balances,
                    accountId,
                    urlConfigProvider,
                    mapper,
                    conversionAssetCode
            )
        else
            getBalances(
                    signedApi.v3.accounts,
                    accountId,
                    urlConfigProvider,
                    mapper
            )
    }

    private fun getConvertedBalances(signedBalancesApi: BalancesApi,
                                     accountId: String,
                                     urlConfigProvider: UrlConfigProvider,
                                     mapper: ObjectMapper,
                                     conversionAssetCode: String): Single<List<BalanceRecord>> {
        return signedBalancesApi
                .getConvertedBalances(
                        accountId = accountId,
                        assetCode = conversionAssetCode,
                        params = ConvertedBalancesParams(
                                include = listOf(
                                        ConvertedBalancesParams.Includes.BALANCE_ASSET,
                                        ConvertedBalancesParams.Includes.STATES,
                                        ConvertedBalancesParams.Includes.ASSET
                                )
                        )
                )
                .toSingle()
                .map { convertedBalances ->
                    conversionAsset = SimpleAsset(convertedBalances.asset)
                    convertedBalances.states.mapSuccessful {
                        BalanceRecord(it, urlConfigProvider.getConfig(),
                                mapper, conversionAsset)
                    }
                }
    }

    private fun getBalances(signedAccountsApi: AccountsApiV3,
                            accountId: String,
                            urlConfigProvider: UrlConfigProvider,
                            mapper: ObjectMapper): Single<List<BalanceRecord>> {
        return signedAccountsApi
                .getBalances(accountId)
                .toSingle()
                .map { sourceList ->
                    sourceList.mapSuccessful {
                        BalanceRecord(it, urlConfigProvider.getConfig(), mapper)
                    }
                }
    }

    /**
     * Creates balance for given assets,
     * updates repository on complete
     */
    fun create(accountProvider: AccountProvider,
               systemInfoRepository: SystemInfoRepository,
               txManager: TxManager,
               vararg assets: String): Completable {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))

        return systemInfoRepository.getNetworkParams()
                .flatMap { netParams ->
                    createBalanceCreationTransaction(netParams, accountId, account, assets)
                }
                .flatMap { transition ->
                    txManager.submit(transition)
                }
                .flatMapCompletable {
                    invalidate()
                    updateDeferred()
                }
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
    }

    private fun createBalanceCreationTransaction(networkParams: NetworkParams,
                                                 sourceAccountId: String,
                                                 signer: Account,
                                                 assets: Array<out String>): Single<Transaction> {
        return Single.defer {
            val operations = assets.map {
                CreateBalanceOp(sourceAccountId, it)
            }

            val transaction =
                    TransactionBuilder(networkParams, PublicKeyFactory.fromAccountId(sourceAccountId))
                            .apply {
                                operations.forEach {
                                    addOperation(Operation.OperationBody.ManageBalance(it))
                                }
                            }
                            .build()

            transaction.addSignature(signer)

            Single.just(transaction)
        }.subscribeOn(Schedulers.computation())
    }
}