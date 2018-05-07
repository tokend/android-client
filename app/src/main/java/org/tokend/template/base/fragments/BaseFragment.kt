package org.tokend.template.base.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import org.tokend.template.App
import org.tokend.template.base.logic.AppTfaCallback
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import javax.inject.Inject

abstract class BaseFragment: Fragment() {
    @Inject
    lateinit var appTfaCallback: AppTfaCallback
    @Inject
    lateinit var accountProvider: AccountProvider
    @Inject
    lateinit var apiProvider: ApiProvider
    @Inject
    lateinit var walletInfoProvider: WalletInfoProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as? App)?.stateComponent?.inject(this)
    }
}