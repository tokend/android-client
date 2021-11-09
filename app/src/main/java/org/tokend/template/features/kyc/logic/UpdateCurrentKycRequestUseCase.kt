package org.tokend.template.features.kyc.logic

import android.content.ContentResolver
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.kyc.files.model.LocalFile
import org.tokend.template.features.kyc.model.KycForm
import org.tokend.template.features.kyc.model.KycRequestState
import org.tokend.template.features.kyc.storage.KycRequestStateRepository
import org.tokend.template.logic.TxManager

/**
 * Takes current [KycRequestState] from [KycRequestStateRepository]
 * and submits new request with same id and role.
 *
 * If there is no current request or it is approved submits a new one.
 *
 * @see SubmitKycRequestUseCase
 */
class UpdateCurrentKycRequestUseCase(
    private val newForm: KycForm,
    private val walletInfoProvider: WalletInfoProvider,
    private val accountProvider: AccountProvider,
    private val repositoryProvider: RepositoryProvider,
    private val apiProvider: ApiProvider,
    private val txManager: TxManager,
    private val alreadySubmittedDocuments: Map<String, RemoteFile>? = null,
    private val newDocuments: Map<String, LocalFile>? = null,
    private val contentResolver: ContentResolver? = null
) {
    private data class CurrentRequestAttributes(
        val id: Long,
        val roleToSet: Long?
    )

    private lateinit var currentRequestAttributes: CurrentRequestAttributes

    fun perform(): Completable {
        return getCurrentRequestAttributes()
            .doOnSuccess { currentRequestAttributes ->
                this.currentRequestAttributes = currentRequestAttributes
            }
            .flatMap {
                submitKycRequest()
            }
            .ignoreElement()
    }

    private fun getCurrentRequestAttributes(): Single<CurrentRequestAttributes> {
        return repositoryProvider.kycRequestState
            .updateIfNotFreshDeferred()
            .andThen(Single.defer {
                val currentState = (repositoryProvider.kycRequestState.item
                        as? KycRequestState.Submitted<*>)
                    ?.takeIf { it !is KycRequestState.Submitted.Approved<*> && it !is KycRequestState.Submitted.PermanentlyRejected<*> }

                CurrentRequestAttributes(
                    id = currentState?.requestId ?: 0L,
                    roleToSet = currentState?.roleToSet
                ).toSingle()
            })
    }

    private fun submitKycRequest(): Single<Boolean> {
        return SubmitKycRequestUseCase(
            form = newForm,
            alreadySubmittedDocuments = alreadySubmittedDocuments,
            newDocuments = newDocuments,
            explicitRoleToSet = currentRequestAttributes.roleToSet,
            requestIdToSubmit = currentRequestAttributes.id,
            repositoryProvider = repositoryProvider,
            walletInfoProvider = walletInfoProvider,
            accountProvider = accountProvider,
            apiProvider = apiProvider,
            contentResolver = contentResolver,
            txManager = txManager
        )
            .perform()
            .toSingleDefault(true)
    }
}