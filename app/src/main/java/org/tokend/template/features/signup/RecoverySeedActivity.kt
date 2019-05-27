package org.tokend.template.features.signup

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import kotlinx.android.synthetic.main.activity_recovery_seed.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.dip
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.input.SimpleTextWatcher

class RecoverySeedActivity : BaseActivity() {
    companion object {
        const val SEED_EXTRA = "seed"
    }

    override val allowUnauthorized = true

    private val seed: String?
        get() = intent.getNullableStringExtra(SEED_EXTRA)

    private var canContinue: Boolean = false
        set(value) {
            field = value
            action_button.enabled = value
        }

    private var seedsMatch = false

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        if (seed == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_recovery_seed)

        initToolbar()
        initFields()
        initButtons()

        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        canContinue = false
    }

    // region Init
    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.save_recovery_seed)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initFields() {
        seed_edit_text.apply {
            setText(seed)
            setSingleLine()
            setPaddings(0, 0, dip(40), 0)
            inputType = InputType.TYPE_NULL
            setTextIsSelectable(true)
        }

        confirm_seed_edit_text.apply {
            addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    checkSeedsMatch()
                    updateContinueAvailability()
                }
            })
            requestFocus()
            onEditorAction {
                if (canContinue) {
                    finishWithSuccess()
                }
            }
        }
    }

    private fun initButtons() {
        action_button.onClick {
            finishWithSuccess()
        }

        copy_button.onClick {
            clipboardManager.text = seed
            toastManager.short(R.string.seed_copied)
        }
    }
    // endregion

    private fun checkSeedsMatch() {
        seedsMatch = seed_edit_text.text.toString() ==
                confirm_seed_edit_text.text.toString()

        if (!seedsMatch && !confirm_seed_edit_text.text.isNullOrEmpty()) {
            confirm_seed_edit_text.error = getString(R.string.error_seed_mismatch)
        } else {
            confirm_seed_edit_text.error = null
        }
    }

    private fun updateContinueAvailability() {
        canContinue = seedsMatch
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
