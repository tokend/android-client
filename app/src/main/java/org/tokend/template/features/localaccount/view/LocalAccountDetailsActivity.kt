package org.tokend.template.features.localaccount.view

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.text.Html
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_local_account_details.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.dip
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.localaccount.mnemonic.view.MnemonicPhraseDialog
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.view.util.LocalAccountLogoUtil
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.dialog.CopyDataDialogFactory
import org.tokend.template.view.dialog.SecretSeedDialog

class LocalAccountDetailsActivity : BaseActivity() {
    override val allowUnauthorized = true

    protected val adapter = DetailsItemsAdapter()

    private lateinit var localAccount: LocalAccount

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_local_account_details)

        initToolbar()
        initList()

        val localAccount = repositoryProvider.localAccount().item
        if (localAccount == null) {
            finishWithMissingArgError("There is no local account in the repository")
            return
        }
        this.localAccount = localAccount

        displayDetails()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.local_account_details_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initList() {
        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter
        (details_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        adapter.onItemClick { _, item ->
            when (item.id) {
                SECRET_SEED_ITEM_ID -> showSecretSeedWithConfirmation()
                ACCOUNT_ID_ITEM_ID -> showAccountId()
                MNEMONIC_PHRASE_ITEM_ID -> showMnemonicPhraseWithConfirmation()
                ERASE_ACCOUNT_ITEM_ID -> eraseAccountWithConfirmation()
            }
        }
    }

    private fun displayDetails() {
        val extraImageView = ImageView(this)
                .apply {
                    layoutParams = ViewGroup.LayoutParams(dip(32), dip(32))
                }

        LocalAccountLogoUtil.setLogo(extraImageView, localAccount)

        adapter.addData(
                DetailsItem(
                        id = ACCOUNT_ID_ITEM_ID,
                        text = localAccount.accountId,
                        singleLineText = true,
                        extraView = extraImageView,
                        hint = getString(R.string.local_account_public_key),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account)
                ),
                DetailsItem(
                        header = getString(R.string.local_account_actions),
                        id = SECRET_SEED_ITEM_ID,
                        text = getString(R.string.show_secret_seed),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account_key)
                )
        )

        if (localAccount.entropy != null) {
            adapter.addData(DetailsItem(
                    id = MNEMONIC_PHRASE_ITEM_ID,
                    text = getString(R.string.show_mnemonic_phrase),
                    icon = ContextCompat.getDrawable(this, R.drawable.ic_text)
            )
            )
        }

        adapter.addData(
                DetailsItem(
                        id = ERASE_ACCOUNT_ITEM_ID,
                        text = getString(R.string.erase_local_account),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_delete)
                )
        )
    }

    private fun showSecretSeedWithConfirmation() {
        SecretSeedDialog(
                this,
                localAccount.account,
                toastManager
        ).show()
    }

    private fun showAccountId() {
        CopyDataDialogFactory.getDialog(
                this,
                localAccount.accountId,
                getString(R.string.local_account_public_key),
                toastManager,
                getString(R.string.data_has_been_copied)
        )
    }

    private fun showMnemonicPhraseWithConfirmation() {
        val entropy = localAccount.entropy ?: return

        val phrase = mnemonicCode.toMnemonic(entropy).joinToString(" ")

        MnemonicPhraseDialog(this, phrase, toastManager).show()
    }

    private fun eraseAccountWithConfirmation() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.erase_local_account)
                .setMessage(Html.fromHtml(
                        getString(R.string.erase_local_account_confirmation)
                ))
                .setPositiveButton(R.string.yes) { _, _ ->
                    eraseAccountAndFinish()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun eraseAccountAndFinish() {
        repositoryProvider.localAccount().erase()
        toastManager.short(R.string.local_account_erased)
        finish()
    }

    private companion object {
        private const val SECRET_SEED_ITEM_ID = 1L
        private const val ACCOUNT_ID_ITEM_ID = 2L
        private const val ERASE_ACCOUNT_ITEM_ID = 3L
        private const val MNEMONIC_PHRASE_ITEM_ID = 4L
    }
}
