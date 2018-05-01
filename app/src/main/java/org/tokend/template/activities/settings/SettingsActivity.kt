package org.tokend.template.activities.settings

import android.os.Bundle
import android.support.v4.app.FragmentTransaction
import kotlinx.android.synthetic.main.layout_progress.*
import org.tokend.template.R
import org.tokend.template.activities.NavigationActivity

class SettingsActivity : NavigationActivity() {
    private lateinit var defaultScreen: SettingsFragment
    private val screenMap = mutableMapOf<String, SettingsFragment>()
    private var firstAddedFragment = true

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_settings)

        initToolbar(R.string.settings_title)
        initNavigationDrawer()

        initScreens()
        displayScreen(null)
    }

    override fun getSelectedNavigationItemId(): Long = SETTINGS_ITEM

    private fun initScreens() {
        defaultScreen = GeneralSettingsFragment()
    }

    private fun displayScreen(key: String?) {
        val fragment = if (key != null && screenMap.containsKey(key))
            screenMap[key]
        else
            defaultScreen

        displayFragment(fragment)
    }

    private fun displayFragment(fragment: SettingsFragment?) {
        val transaction = supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_layout, fragment)

        if (!firstAddedFragment) {
            transaction
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }

        if (firstAddedFragment) {
            firstAddedFragment = false
        }

        transaction.commit()
    }

    fun onNavigateToScreen(screenKey: String?) {
        displayScreen(screenKey)
    }

    fun showProgress() {
        progress.show()
    }

    fun hideProgress() {
        progress.hide()
    }
}
