package io.tokend.template.features.userkey.pin

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import com.rengwuxian.materialedittext.MaterialEditText
import io.tokend.template.R
import io.tokend.template.extensions.getChars
import io.tokend.template.features.userkey.logic.persistence.UserKeyPersistence
import io.tokend.template.features.userkey.logic.persistence.UserKeyPersistenceOnPreferences
import io.tokend.template.features.userkey.view.UserKeyActivity
import io.tokend.template.view.util.input.SimpleTextWatcher
import kotlinx.android.synthetic.main.activity_pin_code.*
import kotlinx.android.synthetic.main.include_biometrics_field_hint.*
import kotlinx.android.synthetic.main.toolbar.*

open class PinCodeActivity : UserKeyActivity() {

    override val errorMessage: String
        get() = getString(R.string.error_invalid_pin)
    override val entryEditText: MaterialEditText
        get() = pin_code_edit_text
    override val userKeyPersistence: UserKeyPersistence by lazy {
        UserKeyPersistenceOnPreferences(
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
            SimpleTextWatcher { pin ->
                if (pin != null && pin.length == PIN_CODE_LENGTH) {
                    onUserKeyEntered(pin.getChars())
                }
            }
        )
    }

    protected open fun hideBiometricsHint() {
        biometrics_hint_layout.visibility = View.GONE
    }

    companion object {
        private const val PERSISTENCE_PREFERENCES_NAME = "PinCodePersistence"
        protected const val PIN_CODE_LENGTH = 4
    }
}
