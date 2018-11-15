package org.tokend.template.features.tfa.logic

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.data.repository.tfa.TfaBackendsRepository

/**
 * Disables current active 2FA factor of given type
 */
class DisableTfaUseCase(
        private val factorType: TfaFactor.Type,
        private val factorsRepository: TfaBackendsRepository
) {
    fun perform(): Completable {
        return updateRepository()
                .flatMap {
                    deleteFactorIfActive()
                }
                .toCompletable()
    }

    private fun updateRepository(): Single<Boolean> {
        return factorsRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
    }

    private fun deleteFactorIfActive(): Single<Boolean> {
        val currentFactor = factorsRepository
                .itemsSubject
                .value
                .find { it.type == factorType }

        return if (currentFactor != null && currentFactor.attributes.priority > 0)
            factorsRepository
                    .deleteBackend(currentFactor.id)
                    .toSingleDefault(true)
        else
            Single.just(false)
    }
}