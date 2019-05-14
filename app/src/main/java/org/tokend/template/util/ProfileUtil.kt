package org.tokend.template.util

import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.features.kyc.model.KycState
import org.tokend.template.features.kyc.model.form.SimpleKycForm

object ProfileUtil {

    fun getAvatarUrl(kycState: KycState?,
                     urlConfigProvider: UrlConfigProvider): String? {
        val submittedForm = (kycState as? KycState.Submitted<*>)?.formData
        val avatar = (submittedForm as? SimpleKycForm)?.avatar
        return avatar?.getUrl(urlConfigProvider.getConfig().storage)
    }
}