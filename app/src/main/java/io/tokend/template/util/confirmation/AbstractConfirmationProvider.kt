package io.tokend.template.util.confirmation

import android.os.Handler
import android.os.Looper
import io.reactivex.Completable
import io.reactivex.subjects.CompletableSubject
import java.util.concurrent.CancellationException

abstract class AbstractConfirmationProvider<T> : ConfirmationProvider<T> {
    override fun requestConfirmation(payload: T): Completable {
        val subject = CompletableSubject.create()

        Handler(Looper.getMainLooper()).post {
            try {
                onConfirmationRequested(payload) { isConfirmed ->
                    if (isConfirmed) {
                        subject.onComplete()
                    } else {
                        subject.onError(CancellationException("Confirmation canceled"))
                    }
                }
            } catch (e: Exception) {
                subject.onError(e)
            }
        }

        return subject
    }

    /**
     * Called to handle confirmation request in the main thread.
     * @param confirmationCallback must be called with confirmation result
     */
    protected abstract fun onConfirmationRequested(
        payload: T,
        confirmationCallback: (Boolean) -> Unit
    )
}