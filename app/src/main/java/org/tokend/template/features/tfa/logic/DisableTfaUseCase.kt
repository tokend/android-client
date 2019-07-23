package org.tokend.template.features.tfa.logic

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.data.repository.TfaFactorsRepository

/**
 * Disables current active 2FA factor of given type
 */
class DisableTfaUseCase(
        private val factorType: TfaFactor.Type,
        private val factorsRepository: TfaFactorsRepository
) {
    fun perform(): Completable {
        return updateRepository()
                .flatMap {
                    deleteFactorIfActive()
                }
                .ignoreElement()
    }

    private fun updateRepository(): Single<Boolean> {
        return factorsRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
    }

    private fun deleteFactorIfActive(): Single<Boolean> {
        val currentFactor = factorsRepository
                .itemsList
                .find { it.type == factorType }

        return if (currentFactor != null && currentFactor.priority > 0)
            factorsRepository
                    .deleteFactor(currentFactor.id)
                    .toSingleDefault(true)
        else
            Single.just(false)
    }
}