package org.tokend.template.util.navigator

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import org.tokend.template.util.navigator.ActivityRequest.Companion
import java.util.*

/**
 * Represents request for an activity result started
 * by [Activity.startActivityForResult]. It allows you declare all the result
 * processing as a callback without need to move it to onActivityResult
 *
 * @param code request code passed to startActivityForResult
 * @param resultProvider provides result of type [R] from activity result intent
 *
 * @see doOnSuccess
 * @see handleActivityResult
 * @see addTo
 *
 * @see Companion.withoutResultData
 **/
class ActivityRequest<R : Any>(
        val code: Int = Random().nextInt() and 0xffff,
        private val resultProvider: (Intent?) -> R?
) {
    private var resultCallback: ((R) -> Unit)? = null

    var isCompleted: Boolean = false
        private set

    /**
     * Specifies callback for [Activity.RESULT_OK]
     */
    fun doOnSuccess(resultWithDataCallback: (R) -> Unit) = also {
        it.resultCallback = resultWithDataCallback
    }

    /**
     * Adds this request to the bag of pending requests of calling Activity or Fragment.
     * The Activity or Fragment then must call [handleActivityResult] for each item of the bag
     */
    fun addTo(requestsBag: MutableCollection<ActivityRequest<*>>) = also {
        requestsBag.add(this)
    }

    /**
     * Must be called inside [Activity.onActivityResult] or [Fragment.onActivityResult]
     * in order for result callback to be called if the result is related to this request
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.code && resultCode == Activity.RESULT_OK) {
            val result = resultProvider(data)

            val resultCallback = resultCallback

            if (result != null && resultCallback != null) {
                resultCallback(result)
            }

            isCompleted = true
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is ActivityRequest<*> && other.code == this.code
    }

    override fun hashCode(): Int {
        return code
    }

    companion object {
        /**
         * @return request that does not take any data from activity result intent
         *
         * @param code request code passed to startActivityForResult
         *
         */
        fun withoutResultData(code: Int = Random().nextInt() and 0xffff) =
                ActivityRequest(code) {}
    }
}