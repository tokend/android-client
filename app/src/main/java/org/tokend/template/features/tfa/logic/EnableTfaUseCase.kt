package org.tokend.template.features.tfa.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.data.repository.tfa.TfaBackendsRepository
import java.util.concurrent.CancellationException

/**
 * Adds and enables 2FA factor of given type.
 * In order to enable 2FA factor user will be asked for OTP from it
 *
 * @param newFactorConfirmation will be called when the new factor is added but not yet enabled
 */
class EnableTfaUseCase(
        private val factorType: TfaFactor.Type,
        private val factorsRepository: TfaBackendsRepository,
        private val newFactorConfirmation: (TfaFactor) -> Single<Boolean>
) {
    private lateinit var newFactor: TfaFactor

    fun perform(): Completable {
        val scheduler = Schedulers.newThread()

        return updateRepository()
                .flatMap {
                    deleteOldFactorIfNeeded()
                }
                .flatMap {
                    addNewFactor()
                }
                .doOnSuccess { newFactor ->
                    this.newFactor = newFactor
                }
                .flatMap {
                    confirmNewFactor()
                }
                .observeOn(scheduler)
                .flatMap {
                    enableNewFactor()
                }
                .toCompletable()
    }

    private fun updateRepository(): Single<Boolean> {
        return factorsRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
    }

    private fun deleteOldFactorIfNeeded(): Single<Boolean> {
        val oldFactor = factorsRepository
                .itemsSubject
                .value
                .find {
                    it.type == factorType
                }

        return if (oldFactor == null)
            Single.just(false)
        else
            factorsRepository
                    .deleteBackend(oldFactor.id)
                    .toSingleDefault(true)
    }

    private fun addNewFactor(): Single<TfaFactor> {
        return factorsRepository.addBackend(factorType)
    }

    private fun confirmNewFactor(): Single<Boolean> {
        return newFactorConfirmation.invoke(newFactor)
                .map { isConfirmed ->
                    if (!isConfirmed) {
                        throw CancellationException("Flow has been canceled on confirmation")
                    }

                    isConfirmed
                }
    }

    private fun enableNewFactor(): Single<Boolean> {
        return factorsRepository
                .setBackendAsMain(newFactor.id)
                .toSingleDefault(true)
    }
}