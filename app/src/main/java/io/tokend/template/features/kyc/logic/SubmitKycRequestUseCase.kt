package io.tokend.template.features.kyc.logic

import android.content.ContentResolver
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import io.tokend.template.features.account.data.model.ResolvedAccountRole
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import io.tokend.template.features.kyc.files.model.LocalFile
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.kyc.model.KycRequestState
import io.tokend.template.features.kyc.storage.KycRequestStateRepository
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.api.blobs.model.BlobType
import org.tokend.sdk.api.documents.model.DocumentType
import org.tokend.sdk.factory.JsonApiTools
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import java.util.*

/**
 * Creates and submits change-role request with general KYC data.
 * Sets new KYC state in [KycRequestStateRepository] on complete.
 * The role is defined by the [form] ([KycForm.role])
 *
 * @param form form to submit, without documents
 * @param alreadySubmittedDocuments documents from the current form, if there are any
 * @param newDocuments documents that needs to be uploaded and combined with [alreadySubmittedDocuments]
 * @param contentResolver used to upload new documents
 */
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
    private val explicitRoleToSet: ResolvedAccountRole? = null
) {
    private data class SubmittedRequestAttributes(
        val id: Long,
        val isReviewRequired: Boolean,
    )

    private lateinit var roleToSet: ResolvedAccountRole
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

    private fun getRoleToSet(): Single<ResolvedAccountRole> {
        if (explicitRoleToSet != null) {
            return Single.just(explicitRoleToSet)
        }

        // If the role is defined by the form, resolve an ID for it.
        val formRole = form.role
        return repositoryProvider
            .keyValueEntries
            .ensureEntries(listOf(formRole.key))
            .map { it[formRole.key] }
            .map { it as KeyValueEntryRecord.Number }
            .map { it.value }
            .map { ResolvedAccountRole(it, formRole) }
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
        val accountId = walletInfoProvider.getWalletInfo().accountId
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

        val formJson = JsonApiTools.objectMapper.writeValueAsString(form)

        return repositoryProvider
            .blobs
            .create(Blob(BlobType.KYC_FORM, formJson))
            .map(Blob::id)
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
            .systemInfo
            .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val account = accountProvider.getDefaultAccount()

        return Single.defer {
            val operation = CreateChangeRoleRequestOp(
                requestID = requestIdToSubmit,
                destinationAccount = PublicKeyFactory.fromAccountId(accountId),
                accountRoleToSet = roleToSet.id,
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
            .kycRequestState
            .set(newKycRequestState)

        if (!submittedRequestAttributes.isReviewRequired) {
            repositoryProvider
                .activeKyc
                .set(ActiveKyc.Form(form))

            repositoryProvider
                .account
                .updateRole(roleToSet)
        }

        return Single.just(true)
    }

    private companion object {
        private const val LOG_TAG = "SubmitKYC"
    }
}