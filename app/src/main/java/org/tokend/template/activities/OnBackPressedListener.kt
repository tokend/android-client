package org.tokend.template.activities

interface OnBackPressedListener {
    /**
     * @returns: true if fragment needs to be closed, otherwise false
     */
    fun onBackPressed(): Boolean
}