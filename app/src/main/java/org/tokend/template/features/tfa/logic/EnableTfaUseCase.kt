package org.tokend.template.features.tfa.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.data.repository.tfa.TfaFactorsRepository
import org.tokend.template.features.tfa.model.TfaFactorCreationResult
import org.tokend.template.util.confirmation.ConfirmationProvider

/**
 * Adds and enables 2FA factor of given type.
 * In order to enable 2FA factor user will be asked for OTP from it
 *
 * @param newFactorConfirmation will be called when the new factor is added but not yet enabled
 */
class EnableTfaUseCase(
        private val factorType: TfaFactor.Type,
        private val factorsRepository: TfaFactorsRepository,
        private val newFactorConfirmation: ConfirmationProvider<TfaFactorCreationResult>
) {
    private lateinit var creationResult: TfaFactorCreationResult
    private val newFactorId: Long
        get() = creationResult.newFactor.id

    fun perform(): Completable {
        val scheduler = Schedulers.newThread()

        return updateRepository()
                .flatMap {
                    deleteOldFactorIfNeeded()
                }
                .flatMap {
                    addNewFactor()
                }
                .doOnSuccess { creationResult ->
                    this.creationResult = creationResult
                }
                .flatMap {
                    confirmNewFactor()
                }
                .observeOn(scheduler)
                .flatMap {
                    enableNewFactor()
                }
                .ignoreElement()
    }

    private fun updateRepository(): Single<Boolean> {
        return factorsRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
    }

    private fun deleteOldFactorIfNeeded(): Single<Boolean> {
        val oldFactor = factorsRepository
                .itemsList
                .find {
                    it.type == factorType
                }

        return if (oldFactor == null)
            Single.just(false)
        else
            factorsRepository
                    .deleteFactor(oldFactor.id)
                    .toSingleDefault(true)
    }

    private fun addNewFactor(): Single<TfaFactorCreationResult> {
        return factorsRepository.addFactor(factorType)
    }

    private fun confirmNewFactor(): Single<Boolean> {
        return newFactorConfirmation
                .requestConfirmation(creationResult)
                .toSingleDefault(true)
    }

    private fun enableNewFactor(): Single<Boolean> {
        return factorsRepository
                .setFactorAsMain(newFactorId)
                .toSingleDefault(true)
    }
}