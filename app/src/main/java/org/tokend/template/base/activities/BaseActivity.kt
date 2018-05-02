package org.tokend.template.base.activities

import android.os.Bundle
import android.view.WindowManager
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
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
}