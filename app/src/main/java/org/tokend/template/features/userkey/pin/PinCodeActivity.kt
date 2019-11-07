package org.tokend.template.features.userkey.pin

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.view.View
import com.rengwuxian.materialedittext.MaterialEditText
import kotlinx.android.synthetic.main.activity_pin_code.*
import kotlinx.android.synthetic.main.include_fingerprint_field_hint.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.extensions.getChars
import org.tokend.template.features.userkey.logic.persistence.UserKeyPersistor
import org.tokend.template.features.userkey.logic.persistence.UserKeyPersistorOnPreferences
import org.tokend.template.features.userkey.view.UserKeyActivity
import org.tokend.template.view.util.input.SimpleTextWatcher

open class PinCodeActivity : UserKeyActivity() {

    override val errorMessage: String
        get() = getString(R.string.error_invalid_pin)
    override val entryEditText: MaterialEditText
        get() = pin_code_edit_text
    override val userKeyPersistor: UserKeyPersistor by lazy {
        UserKeyPersistorOnPreferences(
                getSharedPreferences(PERSISTENCE_PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_pin_code)

        initToolbar()
        initFields()

        super.onCreateAllowed(savedInstanceState)
    }

    protected open fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.pin_code_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initFields() {
        pin_code_edit_text.filters = arrayOf(
                InputFilter.LengthFilter(PIN_CODE_LENGTH)
        )

        pin_code_edit_text.addTextChangedListener(
                object : SimpleTextWatcher() {
                    override fun afterTextChanged(s: Editable?) {
                        if (s != null && s.length == PIN_CODE_LENGTH) {
                            onUserKeyEntered(s.getChars())
                        }
                    }
                }
        )
    }

    protected open fun hideFingerprintHint() {
        fingerprint_hint_layout.visibility = View.GONE
    }

    companion object {
        private const val PERSISTENCE_PREFERENCES_NAME = "PinCodePersistence"
        protected const val PIN_CODE_LENGTH = 4
    }
}
