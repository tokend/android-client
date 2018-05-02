package org.tokend.template.base.view.util

/**
 * Manages loading indicator visibility when there are few parallel tasks which requires it.
 */
class LoadingIndicatorManager(private val showLoading: () -> Unit,
                              private val hideLoading: () -> Unit) {
    private val requests = mutableSetOf<String>()

    var isLoading: Boolean = false
        private set

    fun setLoading(isLoading: Boolean, tag: String) {
        if (isLoading) {
            show(tag)
        } else {
            hide(tag)
        }
    }

    fun show(tag: String) {
        requests.add(tag)
        updateVisibility()
    }

    fun hide(tag: String) {
        requests.remove(tag)
        updateVisibility()
    }

    private fun updateVisibility() {
        if (requests.size > 0) {
            if (!isLoading) {
                showLoading()
            }
            isLoading = true
        } else {
            if (isLoading) {
                hideLoading()
            }
            isLoading = false
        }
    }
}