package org.tokend.template.features.kyc.logic

import android.content.ContentResolver
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.api.blobs.model.BlobType
import org.tokend.sdk.api.documents.model.DocumentType
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.template.features.kyc.model.ActiveKyc
import org.tokend.template.features.kyc.model.KycForm
import org.tokend.template.features.kyc.model.KycRequestState
import org.tokend.template.features.kyc.files.model.LocalFile
import org.tokend.template.logic.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import java.util.*

class SubmitKycRequestUseCase(
    private val form: KycForm,
    private val walletInfoProvider: WalletInfoProvider,
    private val accountProvider: AccountProvider,
    private val repositoryProvider: RepositoryProvider,
    private val apiProvider: ApiProvider,
    private val txManager: TxManager,
    private val alreadySubmittedDocuments: Map<String, RemoteFile>? = null,
    private val newDocuments: Map<String, LocalFile>? = null,
    private val contentResolver: ContentResolver? = null,
    private val requestIdToSubmit: Long = 0L,
    private val explicitRoleToSet: Long? = null
) {
    private data class SubmittedRequestAttributes(
        val id: Long,
        val isReviewRequired: Boolean
    )

    private var roleToSet: Long = 0L
    private lateinit var uploadedDocuments: Map<String, RemoteFile>
    private lateinit var formBlobId: String
    private lateinit var networkParams: NetworkParams
    private lateinit var transactionResultXdr: String
    private lateinit var submittedRequestAttributes: SubmittedRequestAttributes
    private lateinit var newKycRequestState: KycRequestState

    fun perform(): Completable {
        return getRoleToSet()
            .doOnSuccess { roleToSet ->
                this.roleToSet = roleToSet
            }
            .flatMap {
                uploadNewDocuments()
            }
            .doOnSuccess { uploadedDocuments ->
                this.uploadedDocuments = uploadedDocuments
            }
            .flatMap {
                uploadFormAsBlob()
            }
            .doOnSuccess { formBlobId ->
                this.formBlobId = formBlobId
            }
            .flatMap {
                getNetworkParams()
            }
            .doOnSuccess { networkParams ->
                this.networkParams = networkParams
            }
            .flatMap {
                getTransaction()
            }
            .flatMap { transaction ->
                txManager.submit(transaction)
            }
            .doOnSuccess { response ->
                this.transactionResultXdr = response.resultMetaXdr!!
            }
            .flatMap {
                getSubmittedRequestAttributes()
            }
            .doOnSuccess { submittedRequestAttributes ->
                this.submittedRequestAttributes = submittedRequestAttributes
                Log.i(LOG_TAG, submittedRequestAttributes.toString())
            }
            .flatMap {
                getNewKycRequestState()
            }
            .doOnSuccess { newKycState ->
                this.newKycRequestState = newKycState
            }
            .flatMap {
                updateRepositories()
            }
            .ignoreElement()
    }

    private fun getRoleToSet(): Single<Long> {
        if (explicitRoleToSet != null && explicitRoleToSet > 0) {
            return Single.just(explicitRoleToSet)
        }

        val key = form.getRoleKey()
        return repositoryProvider
            .keyValueEntries()
            .ensureEntries(listOf(key))
            .map { it[key] }
            .map { it as KeyValueEntryRecord.Number }
            .map { it.value }
    }

    private fun uploadNewDocuments(): Single<Map<String, RemoteFile>> {
        val newDocuments = this.newDocuments
            ?: return Single.just(emptyMap())

        return Single.concat(
            newDocuments.map { (key, file) ->
                val documentType =
                    try {
                        DocumentType.valueOf(key.toUpperCase(Locale.ENGLISH))
                    } catch (_: Exception) {
                        DocumentType.ALPHA
                    }

                uploadFile(documentType, file).map { key to it }
            }
        )
            .collect<MutableMap<String, RemoteFile>>({ mutableMapOf() }) { res, (key, file) ->
                res[key] = file
            }
            .map { it }
    }

    private fun uploadFile(type: DocumentType, local: LocalFile): Single<RemoteFile> {
        val signedApi = apiProvider.getSignedApi()
            ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
            ?: return Single.error(IllegalStateException("No wallet info found"))
        val contentResolver = this.contentResolver
            ?: return Single.error(IllegalStateException("Content resolver is required to upload files"))

        return signedApi
            .documents
            .requestUpload(
                accountId = accountId,
                documentType = type,
                contentType = local.mimeType
            )
            .toSingle()
            .flatMap { uploadPolicy ->
                apiProvider
                    .getApi()
                    .documents
                    .upload(
                        policy = uploadPolicy,
                        contentType = local.mimeType,
                        fileName = local.name,
                        length = local.size,
                        inputStreamProvider = {
                            contentResolver.openInputStream(local.uri)
                                ?: throw IllegalStateException(
                                    "Unable to open a stream for file " +
                                            local.name
                                )
                        }
                    )
                    .toSingle()
            }
    }

    private fun uploadFormAsBlob(): Single<String> {
        form.documents = mutableMapOf<String, RemoteFile>().apply {
            putAll(alreadySubmittedDocuments ?: emptyMap())
            putAll(uploadedDocuments)
        }

        val formJson = GsonFactory().getBaseGson().toJson(form)

        return repositoryProvider
            .blobs()
            .create(Blob(BlobType.KYC_FORM, formJson))
            .map(Blob::id)
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
            .systemInfo()
            .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
            ?: return Single.error(IllegalStateException("No wallet info found"))
        val account = accountProvider.getAccount()
            ?: return Single.error(IllegalStateException("Cannot obtain current account"))

        return Single.defer {
            val operation = CreateChangeRoleRequestOp(
                requestID = requestIdToSubmit,
                destinationAccount = PublicKeyFactory.fromAccountId(accountId),
                accountRoleToSet = roleToSet,
                creatorDetails = "{\"blob_id\":\"$formBlobId\"}",
                allTasks = null,
                ext = CreateChangeRoleRequestOp.CreateChangeRoleRequestOpExt.EmptyVersion()
            )

            val transaction =
                TransactionBuilder(networkParams, accountId)
                    .addOperation(Operation.OperationBody.CreateChangeRoleRequest(operation))
                    .build()

            transaction.addSignature(account)

            Single.just(transaction)
        }.subscribeOn(Schedulers.newThread())
    }

    private fun getSubmittedRequestAttributes(): Single<SubmittedRequestAttributes> {
        return {
            val request =
                (TransactionMeta.fromBase64(transactionResultXdr) as TransactionMeta.EmptyVersion)
                    .operations
                    .first()
                    .changes
                    .filter {
                        it is LedgerEntryChange.Created || it is LedgerEntryChange.Updated
                    }
                    .map {
                        if (it is LedgerEntryChange.Created)
                            it.created.data
                        else
                            (it as LedgerEntryChange.Updated).updated.data
                    }
                    .filterIsInstance(LedgerEntry.LedgerEntryData.ReviewableRequest::class.java)
                    .first()
                    .reviewableRequest

            SubmittedRequestAttributes(
                id = request.requestID,
                isReviewRequired = request.tasks.pendingTasks > 0
            )
        }.toSingle()
    }

    private fun getNewKycRequestState(): Single<KycRequestState.Submitted<KycForm>> {
        return Single.just(
            if (submittedRequestAttributes.isReviewRequired)
                KycRequestState.Submitted.Pending(form, submittedRequestAttributes.id, roleToSet)
            else
                KycRequestState.Submitted.Approved(form, submittedRequestAttributes.id, roleToSet)
        )
    }

    private fun updateRepositories(): Single<Boolean> {
        repositoryProvider
            .kycRequestState()
            .set(newKycRequestState)

        if (!submittedRequestAttributes.isReviewRequired) {
            repositoryProvider
                .activeKyc()
                .set(ActiveKyc.Form(form))

            repositoryProvider
                .account()
                .updateRole(roleToSet)
        }

        return Single.just(true)
    }

    private companion object {
        private const val LOG_TAG = "SubmitKYC"
    }
}