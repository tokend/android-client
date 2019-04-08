package org.tokend.template.features.offers

import android.os.Bundle
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity

class CreateOfferActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_offer)
    }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {

    }

    companion object {
        const val EXTRA_OFFER = "extra_offer"
    }
}
