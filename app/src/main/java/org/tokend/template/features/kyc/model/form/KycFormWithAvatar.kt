package org.tokend.template.features.kyc.model.form

import org.tokend.sdk.api.base.model.RemoteFile

class KycFormWithAvatar(documents: MutableMap<String, RemoteFile>) : KycForm(documents) {
    val avatar: RemoteFile?
        get() = documents["kyc_avatar"]
}