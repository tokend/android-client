package org.tokend.template.view.util

/**
 * Manages loading indicator visibility when there are few parallel tasks which requires it.
 * I.e. task A and task B running simultaneously can use can use this manager to display
 * loading state with different tags.
 * Loading indicator will be shown if at least one loading request is present
 * and will be hidden if all loading requests has been cancelled.
 */
class LoadingIndicatorManager(private val showLoading: () -> Unit,
                              private val hideLoading: () -> Unit) {
    private val requests = mutableSetOf<String>()

    var isLoading: Boolean = false
        private set

    /**
     * Sets loading state with given tag
     */
    fun setLoading(isLoading: Boolean, tag: String = "main") {
        if (isLoading) {
            show(tag)
        } else {
            hide(tag)
        }
    }

    /**
     * Shows loading with given tag
     */
    fun show(tag: String = "main") {
        requests.add(tag)
        updateVisibility()
    }

    /**
     * Hides loading with given tag
     */
    fun hide(tag: String = "main") {
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