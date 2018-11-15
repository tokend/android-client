package org.tokend.template.activities

import android.support.v7.widget.Toolbar
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*

abstract class ToolbarActivity : BaseActivity() {
    protected fun initToolbar(title: String? = null, needBackButton: Boolean = true) {
        setSupportActionBar(getToolbar())
        setTitle(title)
        if (needBackButton) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    protected fun initToolbar(titleResId: Int = 0, needUpButton: Boolean = true) =
            initToolbar(if (titleResId != 0) getString(titleResId) else null, needUpButton)

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    protected open fun getToolbar(): Toolbar? {
        return toolbar
    }
}