package org.tokend.template.view.util

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.AppCompatActivity

/**
 * Displays fragments in specified container
 * with transitions and back stack management
 */
class UserFlowFragmentDisplayer
private constructor(
        fragmentManagerGetter: () -> androidx.fragment.app.FragmentManager,
        @IdRes
        private val containerId: Int
) {
    constructor(activity: AppCompatActivity,
                @IdRes
                containerId: Int) : this({ activity.supportFragmentManager }, containerId)

    constructor(fragment: androidx.fragment.app.Fragment,
                @IdRes
                containerId: Int) : this({ fragment.childFragmentManager }, containerId)

    private val fragmentManager: androidx.fragment.app.FragmentManager by lazy(fragmentManagerGetter)

    /**
     * Displays given [fragment] in the container
     *
     * @param fragment fragment to display
     * @param tag unique tag for back stack
     * @param forward if set to true then "forward" transition will be used, "back" otherwise.
     * null will cause no transition at all
     */
    fun display(
            fragment: androidx.fragment.app.Fragment,
            tag: String,
            forward: Boolean?
    ) {
        fragmentManager.beginTransaction()
                .setTransition(
                        when (forward) {
                            true -> androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                            false -> androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
                            null -> androidx.fragment.app.FragmentTransaction.TRANSIT_NONE
                        }
                )
                .replace(containerId, fragment)
                .addToBackStack(tag)
                .commit()
    }

    /**
     * @return true if pop was successful,
     * false if only one fragment left in the stack
     */
    fun tryPopBackStack(): Boolean {
        return if (fragmentManager.backStackEntryCount <= 1) {
            false
        } else {
            fragmentManager.popBackStackImmediate()
            true
        }
    }

    /**
     * Clears fragments back stack
     */
    fun clearBackStack() {
        fragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}