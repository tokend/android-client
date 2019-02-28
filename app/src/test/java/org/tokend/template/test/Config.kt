package org.tokend.template.test

import org.tokend.template.BuildConfig
import org.tokend.wallet.Account

object Config {
    const val API = BuildConfig.API_URL
    val ADMIN_SEED = "SAMJKTZVW5UOHCDK5INYJNORF2HRKYI72M5XSZCBYAHQHR34FFR4Z6G4".toCharArray()
    val ADMIN_ACCOUNT = Account.fromSecretSeed(ADMIN_SEED)
    val DEFAULT_PASSWORD = "qwe123".toCharArray()
}