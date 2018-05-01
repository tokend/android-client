package org.tokend.template.activities

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.WindowManager
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.BuildConfig

abstract class BaseActivity : RxAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.SECURE_CONTENT) {
            try {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        onCreateAllowed(savedInstanceState)
    }

    abstract fun onCreateAllowed(savedInstanceState: Bundle?)

    // region Toolbar
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
    // endregion
}