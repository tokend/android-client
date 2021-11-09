package io.tokend.template.util.confirmation

import io.reactivex.Completable
import java.util.concurrent.CancellationException

/**
 * Used to confirm or discard some action.
 */
interface ConfirmationProvider<T> {
    /**
     * Request action confirmation with given payload.
     * @return [Completable] that completes on successful confirmation,
     * otherwise fails with [CancellationException].
     */
    fun requestConfirmation(payload: T): Completable
}