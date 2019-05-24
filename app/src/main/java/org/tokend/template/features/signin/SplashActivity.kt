package org.tokend.template.features.signin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.tokend.template.R
import org.tokend.template.util.Navigator

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Navigator.from(this).toSignIn(true)
    }
}
